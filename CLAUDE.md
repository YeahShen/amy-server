# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库工作时提供指引。代码注释均为中文，本文件同样使用中文。

## 项目简介

amy-server 是 AMY 站点的后端服务：支持艺术家/视频元数据管理、视频分片上传与 FFmpeg 转码（多清晰度自适应 HLS）、SSE 实时推送、CI 构建镜像发布与服务器 Webhook 自动部署，采用 JWT + Redis 在线会话做无状态认证。

## Build & Run

```bash
# 开发环境启动（dev profile，连远程开发库）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 运行测试（注意：pom 默认 skipTests=true，需测试请加 -DskipTests=false）
./mvnw test -DskipTests=false

# 构建 JAR（默认跳过测试，产出 target/amy-server-0.0.1-SNAPSHOT.jar）
./mvnw package
```

应用默认监听 **9999** 端口（`application.yaml` 中 `server.port`，非 8080）。`AppRun` 启动时通过 `ApplicationPidFileWriter` 生成 pid 文件，并在日志打印本地访问地址。`mvnw` 以 100644 入库，Linux（含 CI runner）上执行前需 `chmod +x`。

## 架构总览

Spring Boot 4.1.0（JDK 21 编译与字节码目标，Maven Wrapper 3.9.16），MVC + MyBatis-Flex + Spring Data Redis。

### 技术栈

| 关注点 | 技术 |
|---|---|
| Web 框架 | Spring Boot MVC（`spring-boot-starter-webmvc`，Boot 4 命名） |
| ORM | MyBatis-Flex 1.11.8（注解驱动 `BaseMapper` + APT 生成的 `entity.table.*TableDef`） |
| 数据库 | MariaDB（`sys_user` + `mda_*` 业务表） |
| 缓存 | Redis（Jackson 序列化 + `@class` 类型白名单，缓存默认 TTL 2 小时） |
| 认证 | Spring Security 6 + JJWT 0.12.6（HS512）签发，Hutool JWT 解析 claim；会话有效性由 Redis 在线表维护 |
| 转码 | net.bramp.ffmpeg 0.8.0 封装 ffmpeg/ffprobe；编码器与参数按 profile 外置 YAML：dev 硬编 `h264_amf`（本机 AMD，ffmpeg n8.0 win64-gpl），prod 硬编 `h264_qsv`（Intel，走 PATH） |
| 模板 | FreeMarker（依赖保留；主播放列表已改由 FFmpeg `-master_pl_name` 原生生成，**无模板渲染用途**，`templates/m3u8/index.ftlh` 为遗留文件） |
| 异步 | `@Async("AmyTaskExecutor")`（核心 5 / 最大 10 / 队列 100） |
| SSE | `SseEmitter`，内存 `ConcurrentHashMap` 维护会话，定时心跳 |
| 工具 | Hutool 5.8.46、Guava 33.6、Commons Codec、mica-ip2region（IP 归属地） |

### 包结构

```
site.ashenstation.amyserver
├── AppRun.java                    # 入口：@EnableAsync/@EnableScheduling/@MapperScan + 启动日志
│                                  #   另为 @RestController：GET / 匿名探活，返回 "AMY STATION @"
├── annotation/
│   ├── UploadProcess.java         # 切面标记：方法返回后异步触发上传下一步
│   ├── UpdateResourceCache.java
│   └── rest/AnonymousXxxMapping   # 匿名访问注解（与普通 Mapping 注解等价并登记 URL）
├── config/
│   ├── AsyncConfig.java           # AmyTaskExecutor 线程池
│   ├── aspect/UploadProcessAspect # @AfterReturning 拿到返回值后调 UploadService.nextStep
│   ├── exception/                 # BadRequestException、ApiError（错误响应体）
│   ├── redis/                     # RedisConfiguration、JacksonRedisSerializer、RedisObjectMapper
│   ├── security/                  # SpringSecurityConfig、TokenConfigurer/TokenFilter、EntryPoint/AccessDeniedHandler
│   └── web/WebConfig.java         # @EnableWebMvc：CORS 过滤器 + file: 静态资源映射
├── controller/                    # Auth / Artist / Sse / Upload / Video
├── dto/                           # 请求/传输对象（见下）
├── entity/                        # User(sys_user) 与 mda_* 业务实体
├── enums/                         # LoginPlatform、UploadTaskType、VideoStatus、SseMessageEvent 等
├── mapper/                        # 各实体 BaseMapper（无 XML）
├── property/                      # @ConfigurationProperties（见"配置组"）
├── service/                       # Auth、UserDetail、OnlineUser、UserCache、Artist、Sse、Upload、Video
├── utils/                         # TokenProvider、SecurityUtils、RedisUtils、FFmpegUtils、FileUtils 等
└── vo/                            # AuthResVo、ArtistByCategoryVo
```

## 功能模块

### 1. 认证与用户（已实现，非脚手架）

- **登录** `POST /api/auth/login-by-username-password`（匿名）：`AuthByUsernamePasswordDto{username, password(RSA 加密)}` + 请求头 `X_PLATFORM`（值见 `LoginPlatform`：CLIENT/BROWSER/MOBILE）→ `RsaUtils` 用 `rsa.private_key`（Base64）解密 → `AuthenticationManager`（`UserDetailService` 按用户名查库 + BCrypt(12) 校验，`JwtUserDto` 缓存于 Redis）→ `TokenProvider` 签发 **JJWT HS512** token。
  - JWT claims：`sub`=用户名，另有 `username`、`user_id`、`uid`（一次会话的 UUID）、`platform`（常量在 `AmyConstants`）。**token 本身带 expiration（时长 = `jwt.token-validity-in-seconds`）**。
  - 若 `login.single-login: true`：同用户名同平台旧会话被踢出（删 Redis key）。
  - 登录后在线会话写入 Redis：key `app_online_token:<platform>:<username>:<uid>` → `OnlineUserDto`。
- **请求鉴权** `TokenFilter`（经 `TokenConfigurer` 注入链，位于 `UsernamePasswordAuthenticationFilter` 之前）：解析 Bearer token → 取 claims → 拼 loginKey → `OnlineUserService.getOne` 命中（会话仍在线）才写入 `SecurityContext`（principal 为无权限的 Spring `User`，authorities 为空）。即**有效性完全由 Redis 在线表决定**。
- **续期** `TokenProvider.checkRenewal` 已实现（到期前 `detect` 范围内续 `renew`），但**尚未接入任何调用链**，属于待接线代码。
- **其他接口**：`GET /api/auth/info`（当前用户，头像拼前缀、密码置空）、`DELETE /api/auth/logout`（删在线会话）。注意 header 名是 `X_PLATFORM`（`jwt.client-header`），不是 `X-Client-Type`。
- **匿名访问**：controller 上用 `@AnonymousGetMapping/PostMapping/...` 标注的接口由 `AnonTagUtils` 启动时扫描并登记，`SpringSecurityConfig` 按 HTTP 方法放行；`/`（AppRun 探活）、`/resource/**`、`/ws/**`、swagger 路径、`/api/version/amy/publish` 固定放行，其余 `anyRequest().authenticated()`。
- `SecurityUtils`（静态工具）：`getCurrentUserId()`（从请求头 token 的 `user_id` claim 取）、`getTokenUid()`（`uid` claim），供 SSE 会话键、视频创建者 id 使用。

### 2. Redis / 缓存

- `RedisConfiguration`：`RedisTemplate` key 用 String、value 用 `JacksonRedisSerializer`；`RedisCacheManager` 默认 TTL 2h、value 同序列化器；缓存 Key 由自定义 `KeyGenerator` 生成——将 (class、method、package、params) 序列化为 JSON 后做 **MurmurHash3**（`commons-codec` 的 `MurmurHash3.hash32x86`），缓存读写异常只记日志不抛。
- **`RedisObjectMapper` 是静态持有者，禁止注册为 Spring Bean**——否则会被 Spring MVC 的 HTTP 消息转换器拾取，把 `@class` 字段泄漏进响应体。其多态反序列化白名单仅放行 `java.util`、`java.time`、`site.ashenstation` 包（`BasicPolymorphicTypeValidator`）。
- `RedisUtils`：主要 Redis 访问入口，带 set 重试（3 次）、过期时间、hash/list/set 及 key 扫描等便捷操作。

### 3. 艺术家 Artist

- `POST /api/artist/add`（multipart：name、description、avatarFile、category.id/title）：重名校验（`BadRequestException`）；分类 id 为空则先插 `mda_artist_category`；头像以 `fastSimpleUUID().png` 落盘到配置的 artist-avatar 目录，DB 只存文件名。
- `GET /api/artist/list`：`mda_artist_category` LEFT JOIN `mda_artist` 展平为 `ArtistByCategoryVo`（每行含分类 + 一个艺术家），`processAvatarUrl` 统一拼静态前缀。
- 另有 `GET /{id}`、`GET /category/list`、`GET /all`；查询结果中 avatar 均替换为 `/resource/artist-avatar/xxx.png` 形式的可访问 URL。

### 4. 视频：元数据 + 分片上传 + 转码 HLS（重点，改这里前先看懂）

前端元数据字典接口：`GET /api/video/get-types|get-tags|get-publisher`。

**核心流水线**：

1. `POST /api/video/createUploadTask`（multipart `CreateVideoDto`：title/description/serialNumber/type.id/publisher/tag[]/artist[]/poster 文件/seriesId，creatorId 由服务端从 JWT 取）：
   - 缺失 id 的 publisher/tag 先插入库；海报生成 `simpleUUID().webp` 落 poster 目录，dto 只留 `posterName`。
   - 生成任务/视频 id（`IdUtil.simpleUUID()` 字符串，**`mda_video.id` 为 varchar 非自增**），在 `upload-temp-directory/<id>/` 下写 `config` 文件：JSON 外壳 `UploadTaskDto<CreateVideoDto>{id, type: VIDEO, data}`（data 保留原始 dto）。
   - 返回 id（= videoId = 任务目录名）。
2. `POST /api/upload/chunk`：`UploadChunkDto{id, index, chunk}` 写入 `temp/<id>/chunk_<index>`（1 起）。`GET /api/upload/check-chunk` 为占位实现（恒返回空 `loseChunk`）。
3. `GET /api/upload/next-step?id=` 返回 `UploadProcessorKeyDto{userId, uid, taskId}`，标注 `@UploadProcess` → `UploadProcessAspect` 在 `@AfterReturning` 中调 `UploadService.nextStep`（**@Async AmyTaskExecutor**，立即返回）：
   - 读 `config` 外壳为 `UploadTaskDto<JsonNode>`（避免泛型擦成 LinkedHashMap），按 `type` 路由；VIDEO → `VideoService.processVideoUploadNext`。
   - **接口可重复调用**：临时目录已被清理（任务处理完）则 `log.warn` 直接跳过。
4. `VideoService.processVideoUploadNext`：
   - 校验分片连续性（`chunk_1..N`）→ `FileUtils.mergeFileChunk` 合并成 `启用视频根/<taskId>/<fileMainName>.<ext>`（fileMainName = `fastSimpleUUID()`，**合并直接发生在视频根**，临时目录只存分片与 config）。
   - ffprobe 源时长 → `saveVideoInformation` 组装 `VideoTemporaryInformationDto{video, videoTagMaps, videoArtistMaps}`：video.id = 任务 id，status = **CONVERSION**；随后 tag/artist 映射 `insertBatch` + `videoMapper.insert`（**当前未包事务**，注入的 `TransactionTemplate` 尚未使用）。
   - 转码链路（executor 工作目录 = 启用视频根 `<taskId>/`，`FFmpegUtils.getExecutor(File)`，相对路径才生效）：
     - 非 mp4 源先按 `conversion-to-mp4-args` 转 mp4（硬编 crf 等参数见 YAML）→ 按 `conversion-to-ts-args` copy 转 ts；
     - ffprobe 源分辨率 → `generateAdaptiveFFmpegCommand(maxW, maxH, videoEncoder, audioEncoder)` 生成**单命令自适应 HLS**：`filter_complex`（视频 split + 每路 scale/pad 到档位分辨率，宽度取偶数、480p=848 规避硬编奇数宽）+ 每输出 `-map` 并绑定 `-c:v:i`/`-b:v:i`，`-var_stream_map` 带 `name:`，输出 `%v/index.m3u8` + `%v/segment_%03d.ts`（hls_time 6、vod），`-master_pl_name index.m3u8` 生成主播放列表（**无模板渲染**）。
     - **档位**：源分辨率 ≥ 档位分辨率即产出（顺序 3840×2160→848×480），均不满足转码失败。档目录 = `v4k`/`v2k`/`v1080p`/`v720p`/`v480p`（= var_stream_map 的 name，`%v` 被替换为该名），**执行前须预建目录**（ffmpeg 不自动建）。
   - 主播放列表就绪后经 `UpdateEntity` 局部更新 `mda_video.status = NORMAL`（CONVERSION → NORMAL）。
   - 清理：删除合并源文件/中间 mp4/ts，最后删除整个 `temp/<id>` 上传目录。
   - **失败时**：异常向上抛，异步调用链内捕获记 error（调用方收不到）；`temp/<id>` 与视频根下中间产物**保留**，DB 停留 CONVERSION——可再次调 next-step 重试，无自动回滚/失败态。
   - 转码参数全部来自 profile YAML `ffmpeg.*`（见配置节），改清晰度/码率/编码器只动配置。

静态访问：`WebConfig` 将配置的每个视频根 `file:` 目录映射到 `/resource/video/**`，前端播放 `…/resource/video/<taskId>/index.m3u8`（多清晰度自适应流）。

### 5. SSE 推送

- `GET /api/events/subscribe`（SSE `text/event-stream`，需登录）：创建 `SseEmitter`（0 = 永不过期），会话键 = `userId:uid`（与旧 WebSocket 设计一致），onCompletion/Timeout/Error 自动清理。
- 每 15s `@Scheduled` 心跳：推送 SSE 注释行 `: ping` 保持连接并探活（浏览器 EventSource 会忽略注释，不触发 onmessage）。
- `sendToAll` / `SendMessage(id, …)` 广播 `name=message` 事件（进度推送 `SseMessageDto` + `SseMessageEvent` 类型）。

### 6. 静态资源与目录约定

`StaticResourceDirectoryProperties`（前缀 `static-resource-directory-properties`）集中管理所有落盘目录与访问前缀：

- user-avatar / artist-avatar / poster：单目录，`WebConfig` 按前缀映射（`file:` URI，Windows 反斜杠先转正斜杠）。
- **video-roots 支持多根**：`video-roots` 为列表（每个 `{name, path}`），`enable-video-root` 指定当前启用根；转码写入与 HTTP 访问均基于该列表（见 `VideoService.resolveEnabledVideoRoot`）。
- upload-temp-directory：仅存放任务 config 与分片；处理成功即整体删除（失败保留供重试）。

## 配置文件

YAML 位于 `src/main/resources/config/`（非默认目录），共三个：

| 文件 | 内容 |
|---|---|
| `application.yaml` | 公共：端口 9999、multipart 上限（50MB/55MB）、FreeMarker（`.ftlh`、classpath:/templates/、cache:false）、`jwt.*` 头名等公共键、`rsa.*`、`login.*` |
| `application-dev.yaml` | dev：远程开发库/Redis 凭据（明文）、`jwt.base64-secret`/有效期、静态目录（本机 `C:\Users\ayuan\Documents\AMY\static\...`）、ffmpeg 本机 n8.0-win64-gpl 路径与 `h264_amf` 参数 |
| `application-prod.yaml` | prod：DB/Redis 全部走环境变量（`DATASOURCE_HOST/PORT/NAME/USERNAME/PASSWORD`、`REDIS_HOST/PORT/DB/PASSWORD`）、静态目录 `/amy/*`、ffmpeg 走 PATH 与 `h264_qsv` 参数 |

**注意**：`ffmpeg.*` 转码参数**不在公共 yaml**，dev/prod 各自定义完整（可执行路径 + `conversion-to-mp4-args` / `conversion-to-ts-args` / `video-encoder` / `audio-encoder`）。

配置组（`property/` 下的 `@ConfigurationProperties`）：

| 类 | 前缀 | 说明 |
|---|---|---|
| `SecurityProperties` | `jwt.*` | header/token 前缀/在线 key/有效期等；`base64-secret` 在 dev/prod yaml 内 |
| `RsaProperties` | `rsa.*` | `private_key`（Base64） |
| `LoginProperties` | `login.*` | `single-login` |
| `StaticResourceDirectoryProperties` | `static-resource-directory-properties.*` | 见上节 |
| `FFmpegProperties` | `ffmpeg.*` | 可执行路径 + mp4/ts 转码参数串（空格分隔传入 `addExtraArgs`）+ `video-encoder`/`audio-encoder` |

## 数据库层

- 实体注解映射，无 XML：`@Table`、`@Id`（`KeyType.Auto` 自增；`mda_video.id` 无自增，字符串主键由服务端 UUID 赋给）、`@Column`；枚举用 `@EnumValue` 标注存库取值。
- 表：`sys_user`（用户）、`mda_artist` / `mda_artist_category`、`mda_video` / `mda_video_tag` / `mda_video_type` / `mda_video_publisher`、`mda_video_tag_map` / `mda_video_artist_map`（videoId 为字符串关联）。
- **`VideoStatus`**：NORMAL/DELETE/CONVERSION，DB 存小写字符串 `normal/delete/conversion`；`find(String)` 未命中默认 NORMAL。
- Mapper 直接继承 `BaseMapper`；联表用 `QueryChain` + APT 生成的 `entity.table.*TableDef`（例：`ArtistService.getArtistList` 的 left join `listAs`）。
- `User` 表用户字段含头像（存文件名，返回前拼前缀）。

## 部署（Docker + GitHub Actions）

**Dockerfile**：基础镜像为阿里云 ACR 的 `java-ffmpeg:21-jre-alpine-3.21`（含 ffmpeg），拷贝 `target/*.jar`（需先 `mvnw package`），以 `prod` profile 启动，开 5006 调试端口（jdwp），工作目录 `/amy` 预建各静态目录。⚠️ Dockerfile `EXPOSE 8080` 与公共配置端口 **9999** 不一致（已知问题，见下）。

**CI/CD**（[.github/workflows/deploy.yml](.github/workflows/deploy.yml)）：**仅推送 `v*` tag 时触发**（如 `v1.0`；master 常规推送与 PR 均不触发；同一时间只保留最新一次运行），发布镜像并自动部署，是正式发版通道。流程：checkout → JDK 21 + Maven 缓存 → `mvn versions:set -DnewVersion=${GITHUB_REF_NAME#v}`（jar 版本取 tag 名**去 v**，如 `v1.0` → `1.0`；pom 保留 `0.0.1-SNAPSHOT` 基线；`github.event.release` 在 tag 推送事件下为 null，**须用 `github.ref_name` 取 tag 名**）→ `./mvnw -DskipTests package` → 校验 ACR 凭据 → buildx（`type=gha` 缓存）→ 登录 ACR → build-push-action 构建推送 `registry.cn-hangzhou.aliyuncs.com/ashen_station/amy-server:latest` + `:<tag名（带 v）>`（**`provenance: false`/`sbom: false` 必须保留**——ACR 不支持 OCI artifact 制品，默认的 provenance/SBOM 会被拒）→ 请求 `https://hooks.ashen-station.top/hooks/deploy-my-app` 触发服务器部署 → 按 `success()`/`failure()` 发部署成功/失败邮件（163 SMTP）。

需要配置的 Actions secrets：`ALIYUN_ACR_USERNAME` / `ALIYUN_ACR_PASSWORD`（ACR 登录）、`WEBHOOK_SECRET`（部署 hook 头）、`MAIL_USERNAME` / `MAIL_PASSWORD`（163 授权码）。注意 `secrets` 不能直接出现在步骤 `if:` 表达式（解析期报错），需先映射到 `env` 或改用事件条件判断。

## 已知问题 / 待办（2026-09 现状）

- `TokenProvider.checkRenewal` 未接入任何调用链；登录时效完全依赖 JWT expiration + 在线会话。
- `VideoService` 注入的 `TransactionTemplate`、`freemarkerConfig` 均未使用：三处 insert 不在同一事务；FreeMarker 依赖与 `templates/m3u8/index.ftlh` 为重构遗留（主列表已由 FFmpeg 生成，可清理）。
- 转码失败无状态回退（卡在 CONVERSION，临时目录与中间产物保留可手动重放 next-step，无自动重试/告警）。
- `check-chunk` 接口是占位实现。
- Spring Security 已实现但无接口粒度权限（authorities 恒为空）。
- Dockerfile `EXPOSE 8080` 与公共配置端口 9999 不一致。
- CI 只在 `v*` tag 推送时运行（验证构建需本地执行或临时 tag）。

## 代码规范

- 全类 Lombok（`@Data`、`@Slf4j`、`@RequiredArgsConstructor`，构造器注入）；类型写法与既有代码保持一致。
- 注释一律中文；提交信息用 `feat/fix/docs/chore` + 中文描述（可用 `/gitpush`）。
- MyBatis-Flex 纯注解映射，**不写 XML mapper**；新增实体用 `@Table/@Id/@Column`，新增联表查询优先 `QueryChain` + TableDef。
- 配置分组一律走 `@ConfigurationProperties`（前缀见上表），不要散落 `@Value`（静态工具类中注入配置属历史惯例，新增请避免）。
- pom 中 `maven.compiler.proc=full` 是 Lombok 生效前提（maven-compiler-plugin 3.15+ 默认关闭 classpath 注解处理），勿删；pom 默认 `skipTests=true`。
- 异常统一抛 `BadRequestException`（配合 `ApiError` 响应）；异步线程内异常必须自行捕获记日志（调用方收不到）。
