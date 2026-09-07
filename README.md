# amy-server

AMY 站点后端服务：艺术家/视频元数据管理、视频分片上传与 FFmpeg 多清晰度 HLS 转码、SSE 实时推送。采用 JWT + Redis 在线会话的无状态认证，静态资源与转码产物均落盘并由 `file:` 映射对外提供 HTTP 访问。

## 功能特性

- **认证**：用户名密码登录（RSA 加密传输）→ Spring Security 6 + JJWT(HS512) 签发 token，会话有效性由 Redis 在线表维护，支持同账号单点登录互踢；`X_PLATFORM`（CLIENT/BROWSER/MOBILE）区分登录端
- **艺术家管理**：分类/艺术家增查，头像落盘 + 重名校验
- **视频管理**：元数据字典（类型/标签/发布方）、分片上传、任务化转码
- **HLS 自适应转码**：FFmpeg 单命令产出多清晰度（4K/2K/1080p/720p/480p）自适应流，`-master_pl_name` 原生生成主播放列表 `index.m3u8`，无模板渲染
- **SSE 推送**：订阅式事件通道（`SseEmitter` 会话管理、心跳保活、消息广播）
- **多视频根目录**：转码写入与静态访问均基于配置的启用的视频根（`video-roots`）

## 技术栈

| 关注点 | 技术 |
|---|---|
| 运行时 / 语言 | JDK 21（字节码目标 21，Maven Wrapper 3.9.16） |
| Web 框架 | Spring Boot 4.1.0 MVC |
| ORM | MyBatis-Flex 1.11.8（纯注解，无 XML） |
| 数据库 | MariaDB（`sys_user` + `mda_*` 业务表） |
| 缓存 | Redis（Jackson 序列化 + `@class` 白名单） |
| 认证 | Spring Security 6 + JJWT 0.12.6（HS512）签发，Hutool JWT 解析 |
| 模板 | FreeMarker `.ftlh`（依赖保留，HLS 渲染用途已随重构移除，见「已知问题」） |
| 转码 | net.bramp.ffmpeg 0.8.0，参数全部外置 YAML |
| 推送 | `SseEmitter` + 内存会话表 + 定时心跳 |
| 工具 | Hutool、Guava、Commons Codec（MurmurHash3 缓存键）、mica-ip2region（IP 归属地） |

## 快速开始

### 环境要求

- JDK 21
- MariaDB、Redis（dev 环境使用远程开发库，见 [application-dev.yaml](src/main/resources/config/application-dev.yaml)）
- FFmpeg/FFprobe（可执行文件路径在各 profile 中配置）
- Maven 无需单独安装（项目自带 Wrapper）

### 本地启动（dev profile）

dev profile 连接远程开发库（凭据内置于 `application-dev.yaml`）：

```bash
# Linux/macOS（git-bash）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows CMD
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

应用监听 **9999** 端口，启动后打印本地访问地址并生成 pid 文件。

### 测试

pom 默认 `skipTests=true`，需要测试时显式开启（注意：仓库唯一的测试是 `@SpringBootTest` 上下文加载，无数据源配置时无法启动，请在可连库环境下运行）：

```bash
./mvnw test -DskipTests=false
```

### 打包 / 构建镜像

```bash
./mvnw package                       # 产出 target/amy-server-0.0.1-SNAPSHOT.jar（跳过测试）
docker build -t amy-server .         # Dockerfile 拷贝 target/*.jar，需先打包
```

代码推送到 `master` 后由 GitHub Actions 自动完成打包与镜像发布（见下「CI」节）。

## 配置说明

配置文件位于 `src/main/resources/config/`（非默认目录）：

| 文件 | 内容 |
|---|---|
| `application.yaml` | 公共配置：端口 9999、multipart 上限 50MB/55MB、FreeMarker、全部 `ffmpeg.*-args` 转码参数、`jwt.*`/`rsa.*`/`login.*` 键 |
| `application-dev.yaml` | dev：远程开发库/Redis 凭据、`jwt.base64-secret`、本机静态目录与 ffmpeg 路径 |
| `application-prod.yaml` | prod：库/缓存全部走环境变量；静态目录固定 `/amy/*`；ffmpeg 走 PATH |

### prod 环境变量

| 变量 | 说明 |
|---|---|
| `DATASOURCE_HOST` / `PORT` / `NAME` / `USERNAME` / `PASSWORD` | MariaDB 连接 |
| `REDIS_HOST` / `PORT` / `DB` / `PASSWORD` | Redis 连接 |

主要配置组（均为 `@ConfigurationProperties`，见 [property/](src/main/java/site/ashenstation/amyserver/property/)）：

| 前缀 | 说明 |
|---|---|
| `jwt.*` | 请求头名、token 前缀、在线会话 key、有效期等 |
| `rsa.*` | RSA 私钥（Base64，解密前端密码） |
| `login.*` | `single-login` 单点互踢开关 |
| `static-resource-directory-properties.*` | 各落盘目录与访问前缀、`video-roots` 多根与启用根 |
| `ffmpeg.*` | ffmpeg/ffprobe 路径 + 各步转码参数串（空格分隔） |

## 主要 API

公共响应结构：业务异常统一返回 `BadRequestException` 形式的 `ApiError` 体。

### 认证 `/api/auth`（除登录外均需 Bearer token，头名 `X_PLATFORM` 标明登录端）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST（匿名） | `/login-by-username-password` | 登录：`{username, password(RSA加密)}` + 头 `X_PLATFORM` |
| GET | `/info` | 当前用户信息（头像拼前缀、密码置空） |
| DELETE | `/logout` | 注销，删除 Redis 在线会话 |

### 艺术家 `/api/artist`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/add` | 新增（multipart：name/description/avatarFile/category） |
| GET | `/{id}` | 详情 |
| GET | `/list` | 按分类展平列表 `ArtistByCategoryVo` |
| GET | `/category/list` | 分类列表 |
| GET | `/all` | 全部艺术家 |

### 视频 `/api/video`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/get-types` / `/get-tags` / `/get-publisher` | 元数据字典 |
| POST | `/createUploadTask` | 创建上传任务（multipart `CreateVideoDto`），返回任务 id（= 视频 id） |

### 上传 `/api/upload`（核心流水线，见下节）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/chunk` | 分片上传：`{id, index, chunk}`，index 从 1 起 |
| GET | `/check-chunk` | 分片校验（当前为占位实现，恒返回空） |
| GET | `/next-step?id=` | 触发转码下一步（异步，标注 `@UploadProcess`，可重复调用） |

### 推送 `/api/events`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/subscribe` | SSE `text/event-stream` 订阅（需登录，每 15s 心跳保活） |

### 静态资源

`/resource/**` 固定匿名放行，由 `WebConfig` 将各落盘目录映射为 `file:` 静态资源。视频播放地址形如 `/resource/video/<taskId>/index.m3u8`。

### 匿名放行规则

`@AnonymousGetMapping/PostMapping/...` 标注的接口启动时自动登记并放行；`/resource/**`、`/ws/**`、swagger 路径、`/api/version/amy/publish` 固定放行，其余一律 `authenticated()`。

## 视频上传 → 转码 HLS 流水线

1. `POST /api/video/createUploadTask`（multipart `CreateVideoDto`）：缺 id 的 publisher/tag 先落库，海报转存 poster 目录，任务外壳 JSON 写入 `upload-temp-directory/<id>/config`，返回任务 id（= 视频 id）
2. `POST /api/upload/chunk`：分片写入 `temp/<id>/chunk_<index>`（index 从 1 起）
3. `GET /api/upload/next-step?id=`：`@UploadProcess` 切面异步触发 `VideoService.processVideoUploadNext`（接口可重复调用）：
   - 校验分片连续性 → `FileUtils.mergeFileChunk` 合并，产物**直接落在启用视频根 `<taskId>/` 下**
   - ffprobe 源时长/分辨率 → tag/artist 映射 `insertBatch` + 视频落库（status = `conversion`）
   - 非 mp4 源先按 `conversion-to-mp4-args` 转 mp4 → 按 `conversion-to-ts-args` copy 转 ts
4. `FFmpegUtils.generateAdaptiveFFmpegCommand` 生成**单命令自适应 HLS**（参数为 argv 列表逐个直传进程，含空格的 `-filter_complex`/`-var_stream_map` 整体作为单元素）：
   - `filter_complex`：视频 split 分路 + 每路 `scale`/`pad` 到档位分辨率（宽度取偶数，480p 为 848，规避硬件编码器奇数宽报错）；`-map` 各输出并绑定 `-c:v:i` / `-b:v:i`
   - `-var_stream_map` 各档带 `name:`（`v4k`/`v2k`/…），替换输出路径中的 `%v` → 输出 `%v/index.m3u8`、切片 `%v/segment_%03d.ts`（hls_time 6、vod，各档目录预创建）
   - `-master_pl_name index.m3u8` 由 FFmpeg 原生生成主播放列表（无需模板渲染）
5. 主播放列表就绪后 status → `normal`，清理合并源文件与中间 mp4/ts，最后删除整个 `temp/<id>` 上传目录

档位（分辨率 ≤ 源分辨率即产出，均不满足时转码失败）：

| 档位目录/流名 | 分辨率 | 码率（kbps） |
|---|---|---|
| `v4k` | 3840 × 2160 | 15000 |
| `v2k` | 2560 × 1440 | 8000 |
| `v1080p` | 1920 × 1080 | 5000 |
| `v720p` | 1280 × 720 | 2500 |
| `v480p` | 848 × 480 | 1200 |

产物结构（编码器、码率、档位改动只动 YAML 配置；播放时前端指向主列表自适应切换）：

```
<启用视频根>/<taskId>/
├── index.m3u8            # 主播放列表（-master_pl_name 生成）
├── v1080p/index.m3u8 + segment_*.ts   # 各档子目录，仅含实际产出的档
├── v720p/…
└── v480p/…
```

## 项目结构

```
src/main/java/site/ashenstation/amyserver
├── AppRun.java              # 入口：@EnableAsync/@EnableScheduling/@MapperScan
├── annotation/              # @UploadProcess 切面标记、@Anonymous*Mapping 等
├── config/                  # Async/Security/Redis/Web + 异常 + UploadProcessAspect
├── controller/              # Auth / Artist / Sse / Upload / Video
├── dto/ · vo/               # 请求 / 返回对象
├── entity/ · mapper/        # 实体（纯注解）与 BaseMapper
├── enums/                   # LoginPlatform、VideoStatus、SseMessageEvent 等
├── property/                # @ConfigurationProperties 配置组
├── service/                 # Auth、User、OnlineUser、Artist、Sse、Upload、Video
└── utils/                   # TokenProvider、SecurityUtils、RedisUtils、FFmpegUtils、FileUtils…
```

## 数据库

实体注解映射，无 XML；`mda_video.id` 为 varchar 字符串主键（服务端 UUID 生成，非自增）。

- `sys_user`：用户（密码 BCrypt(12)）
- `mda_artist` / `mda_artist_category`：艺术家与分类
- `mda_video`：视频主表，`status` = `normal/delete/conversion`（`VideoStatus` 枚举，`find` 未命中默认 normal）
- `mda_video_tag` / `mda_video_type` / `mda_video_publisher`：字典
- `mda_video_tag_map` / `mda_video_artist_map`：多对多关联（videoId 字符串关联）

## Docker 部署与 CI

- **Dockerfile**：基础镜像 `registry.cn-hangzhou.aliyuncs.com/ashen_station/java-ffmpeg:21-jre-alpine-3.21`（含 ffmpeg），拷贝 `target/*.jar`，以 `prod` profile 启动，工作目录 `/amy`（预建各静态目录），开 5006 调试端口
- **GitHub Actions**（[.github/workflows/docker-image.yml](.github/workflows/docker-image.yml)）：`master` 推送 / PR / 手动触发
  1. JDK 21 + Maven 依赖缓存，`./mvnw -DskipTests package`
  2. 登录阿里云 ACR → buildx 构建（`type=gha` 分层缓存）
  3. `master` 推送发布 `ashen_station/amy-server:latest` + `:<commit sha>` 双 tag；PR 仅构建验证不推送

仓库需配置 Actions secrets：`ALIYUN_ACR_USERNAME` / `ALIYUN_ACR_PASSWORD`（缺失时 `master` 推送会在校验步骤显式失败）。

## 已知问题 / 待办

详见 [CLAUDE.md](CLAUDE.md)「已知问题 / 待办」节，当前包括：

- `TokenProvider.checkRenewal` 续期逻辑未接入调用链
- 转码失败无状态回退（卡在 `conversion`）与重试机制
- `check-chunk` 为占位实现
- 重构遗留：FreeMarker 依赖与 `templates/m3u8/index.ftlh` 已不参与 HLS 渲染，`VideoService` 注入的 `freemarkerConfig`、`TransactionTemplate` 未使用
- Dockerfile `EXPOSE 8080` 与公共配置端口 9999 不一致（部署时需确认映射）
- Spring Security 无接口粒度权限（authorities 恒为空）
