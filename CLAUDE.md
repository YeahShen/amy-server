# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库工作时提供指引。代码注释均为中文，本文件同样使用中文。

## 项目简介

amy-server 是 AMY 站点的后端服务（艺术家/视频元数据管理、视频分片上传与 FFmpeg 转码、SSE 推送、JWT + Redis 在线会话认证、CI 镜像发布与 Webhook 自动部署）。

**当前处于多模块 Maven 重构中途**（develop 分支）：旧单模块实现（`src/` 整体，含艺术家/视频/上传/转码/SSE 全部业务代码）已在 `cdcbfac` 提交中移除，仅存于 git 历史；新五模块骨架已搭好，security 模块部分实现，业务模块待按新结构回迁。改代码前先分清"已迁移 / 骨架 / 空壳 / 仅存历史"。

## 模块结构

```
amy-server（根 pom，packaging=pom）
├── boot             # 唯一可执行模块：Booter 启动类 + 全部 YAML 配置（boot/src/main/resources/config/*.yml）
├── common           # 零依赖公共库：注解、枚举、异常、静态工具（Constants/RedisUtils/RsaUtils/SecurityUtils…）
├── model            # 实体模块（目前仅 AppUser 空骨架）
├── infrastructure   # 基础设施：@ConfigurationProperties 配置组、Redis/Web/CORS/异步 配置、dao Mapper
└── system           # 业务模块：modules/security（认证，部分实现）、modules/hugefileupload（占位）
```

- 依赖方向：boot → system/infrastructure/model → common；所有模块共享根包 `site.ashenstation`，`Booter` 位于根包使 `@SpringBootApplication` 组件扫描天然覆盖全部模块。
- **全部第三方依赖声明在根 pom 的 `<dependencies>`**（非 dependencyManagement），各模块直接继承，新增依赖改根 pom 即可。
- 各模块 pom 里还有 Maven 原型遗留的 junit 3.8.1 `AppTest`，无实际测试。

## Build & Run

```bash
# 开发环境启动（dev profile，连远程开发库）
./mvnw spring-boot:run -pl boot -Dspring-boot.run.profiles=dev

# 运行测试（pom 默认 skipTests=true，需测试请加 -DskipTests=false）
./mvnw test -DskipTests=false

# 构建 JAR（默认跳过测试；多模块下产物在 boot/target/boot-0.0.1-SNAPSHOT.jar，不再在根 target/）
./mvnw package
```

- 应用监听 **9999** 端口（`application.yml` 的 `server.port`，非 8080）。`Booter` 启动时经 `ApplicationPidFileWriter` 生成 pid 文件并打印本地访问地址；`@MapperScan("site.ashenstation.infrastructure.dao")`、`@EnableAsync`、`@EnableScheduling`；另为 `@RestController`：`GET /` 匿名探活返回 `"AMY STATION @"`。
- **⚠️ `.mvn/wrapper/maven-wrapper.properties` 已在重构提交中删除，`mvnw`/`mvnw.cmd` 目前完全无法启动**（报 "Cannot start maven from wrapper"），本机亦未安装系统 mvn。修复方式：从任一 Spring Initializr 工程拷回该文件，或安装 Maven 后 `mvn wrapper:wrapper`。CI 同样会挂（见部署节）。
- `mvnw` 以 100644 入库，Linux（含 CI runner）上执行前需 `chmod +x`。

## 技术栈

| 关注点 | 技术 |
|---|---|
| Web 框架 | Spring Boot 4.1.0 MVC（`spring-boot-starter-webmvc`，Boot 4 命名），JDK 21 |
| ORM | MyBatis-Flex 1.11.8（注解 `BaseMapper` + APT 生成 TableDef；目前仅 AppUserMapper 骨架） |
| 数据库 | MariaDB |
| 缓存 | Redis（Jackson 序列化 + `@class` 白名单，缓存默认 TTL 2 小时） |
| 认证 | Spring Security 6 + JJWT 0.12.6（HS512）签发；Hutool JWT 解析 claim；会话有效性由 Redis 在线表决定 |
| 转码 | net.bramp.ffmpeg 0.8.0（依赖在，`FFmpegUtils` 为空壳，业务待回迁） |
| 模板 | FreeMarker 依赖保留，`TemplateRenderUtils` 已迁移，但 `templates/` 目录尚未回迁（当前无模板文件） |
| 异步 | `@Async("AmyTaskExecutor")`（核心 5 / 最大 10 / 队列 100，前缀 `Async-`），`AsyncConfig` 在 infrastructure |
| 工具 | Hutool 5.8.46、Guava 33.6、Commons Codec（MurmurHash3）、mica-ip2region |

## 包结构（含迁移状态）

```
common/src/main/java/site/ashenstation/
├── annotation/Log.java                # 空骨架（操作日志注解，待实现切面）
├── annotation/rest/AnonymousXxx       # ✅ 匿名访问注解（Access + Get/Post/Put/Patch/Delete Mapping）
├── enums/                             # ✅ LoginPlatform(CLIENT/BROWSER/MOBILE，find 未命中默认 CLIENT)、
│                                      #    RequestMethodEnum、UploadTaskType(仅 VIDEO)、VideoStatus
├── exception/BadRequestException.java # ✅ 业务异常（配合 infrastructure 的 ApiError）
└── utils/
    ├── Constants.java                 # ✅ JWT claim 常量（见"认证"节，claim 名与旧版不同！）
    ├── RedisObjectMapper.java         # ✅ 静态 ObjectMapper.INSTANCE，多态白名单（禁止注册为 Bean）
    ├── RedisUtils.java                # ✅ Redis 访问入口（set 重试 3 次、过期、hash/list/set、key 扫描）
    ├── AnonTagUtils.java              # ✅ 扫描 @AnonymousAccess 登记 URL（供 SecurityConfig 放行）
    ├── SecurityUtils.java             # ✅ 静态工具（历史惯例 @Value 注入 header，见代码规范）
    ├── RsaUtils.java / AesUtils.java  # ✅ RSA 私钥解密 / AES-128-CBC（随机 IV 前置拼接，Base64）
    ├── FileUtils.java                 # ✅ 继承 Hutool FileUtil，mergeFileChunk 分片合并（进度仅计算未上报）
    ├── TemplateRenderUtils.java       # ✅ FreeMarker 渲染包装（模板文件待回迁）
    ├── FFmpegUtils.java               # 🚧 空壳
    ├── IpAddrUtils / RequestHolder / SpringBeanHolder / ThrowableUtils  # ✅
infrastructure/src/main/java/site/ashenstation/infrastructure/
├── config/ApiError.java               # ✅ 错误响应体（旧 config/exception/ 移入）
├── config/AsyncConfig.java            # ✅ AmyTaskExecutor
├── config/redis/                      # ✅ RedisConfiguration、JacksonRedisSerializer
├── config/web/                        # ✅ WebConfig（@EnableWebMvc + file: 静态映射）、CorsFilterConfig
├── dao/AppUserMapper.java             # 🚧 BaseMapper<AppUser> 骨架
└── property/                          # ✅ Security/Rsa/Login/StaticResourceDirectory/FFmpeg 五个配置组
model/src/main/java/site/ashenstation/model/entity/
└── AppUser.java                       # 🚧 空骨架（无 @Table，表名未定；旧实体 User/sys_user 已删）
system/src/main/java/site/ashenstation/modules/
├── security/
│   ├── config/TokenProvider.java      # ✅ JJWT 签发/解析/loginKey/续期
│   ├── config/TokenFilter.java        # ✅ 请求鉴权过滤器（写 SecurityContext）
│   ├── config/TokenConfigurer.java    # ✅ 将 TokenFilter 插到 UsernamePasswordAuthenticationFilter 之前
│   ├── config/JwtAuthenticationEntryPoint.java / JwtAccessDeniedHandler.java  # ✅ 401/403 JSON 响应
│   ├── config/SpringSecurityConfig.java   # 🚧 空壳（HttpSecurity 主配置未实现，见已知问题）
│   ├── service/OnlineUserService.java     # ✅ 在线会话写删查 + kickOut
│   ├── service/UserDetailService.java     # 🚧 空壳（按用户名查库 + BCrypt 校验，登录流核心）
│   ├── service/UserCacheService.java      # 🚧 空壳（用户信息缓存，LoginProperties.userCacheIdleTime 待用）
│   ├── service/SseService.java            # 🚧 空壳
│   ├── dto/AuthDto.java                   # 🚧 {username, password} 骨架（RSA 解密待接）
│   ├── dto/JwtAppUserDto.java             # 🚧 UserDetails 空实现（旧 JwtUserDto）
│   ├── dto/OnlineUserDto.java             # ✅ 在线会话值对象
│   ├── rest/AuthController.java           # 🚧 空壳（登录/info/logout 接口）
│   └── vo/UserInfoVo.java / NotificationVO.java  # 🚧 空壳
└── hugefileupload/                    # 🚧 rest/ 与 service/ 仅 .gitkeep 占位（视频分片上传待回迁）
```

## 功能模块现状

### 1. 认证与安全（部分实现）

**已实现**：

- **TokenProvider**（`system/.../security/config/`）：
  - 两种签发：`createToken(subject, claims)` **不带过期**（注释注明时效转由 Redis 维护）与 `createToken(subject, claims, expired)` 带过期毫秒；登录时用哪个待 AuthController 定夺。HS512，secret 来自 `jwt.base64-secret`（≥88 位 Base64）。
  - `getClaims`（JJWT 解析）、`isTokenExpired`、`checkRenewal`（到期前 `detect` 范围内续 `renew`，**仍未接入任何调用链**）。
  - `loginKey(token)` → `onlineKey + sub + ":" + uid`；`loginKey(token, platform)` → `onlineKey + platform + ":" + sub + ":" + uid`。onlineKey = `app_online_token:`。
  - `resolveToken`：请求头 `Authorization`，`Bearer ` 前缀剥离；另有 ServerHttpRequest 重载（WebFlux/SSE 场景）。
- **TokenFilter**（`@Component`，经 TokenConfigurer 注入过滤链）：resolveToken → 解析 claims → 按 `platform` claim 拼 loginKey → `OnlineUserService.getOne` 命中（会话仍在线）才写入 `SecurityContext`（principal 为无权限 Spring `User`，authorities 恒空，credentials 存原 token）。**有效性完全由 Redis 在线表决定**，token 本身过期与否不作第一道判断。
- **OnlineUserService**：`save`（3 参/4 参带平台）、`logout`（同）、`getOne`、`kickOutForUsername(AndPlatform)`（`scanDel` 模糊键）。会话值 `OnlineUserDto{userName, ip, address, key, loginTime, loginPlatform}`，Redis TTL = `jwt.token-validity-in-seconds`。
- **JwtAuthenticationEntryPoint / JwtAccessDeniedHandler**：401（"登录状态已过期"）/ 403（"禁止访问"）写 `ApiError` JSON。

**JWT claim 常量**（`common` 的 `Constants`，旧版 `AmyConstants` 仅类改名，**claim 值完全一致**）：`username`、`userId`、`uid`（会话 UUID）、`platform`、`type`、`tokenId`（创建者 id）、`permission`（预留权限 claim）。

**⚠️ 已知陷阱**：`OnlineUserService.save(username, token, request)`（3 参）用**不带平台**的 `loginKey(token)` 写入，而 TokenFilter 按**带平台**键读取——两者键结构不一致。实现登录时必须调 4 参 `save`（写入 `loginKey(token, platform)`），或统一两处键结构，否则登录后请求永远 401。

**待实现**（均为空壳）：`SpringSecurityConfig`（HttpSecurity 主配置：应用 TokenConfigurer、放行 `AnonTagUtils` 扫出的匿名 URL + `/` + `/resource/**` 等固定前缀、挂 EntryPoint/AccessDeniedHandler、CSRF 等约定）、`AuthController`（`POST /api/auth/login-by-username-password`、`GET /api/auth/info`、`DELETE /api/auth/logout`；登录头 `X_PLATFORM`）、`UserDetailService`（查库 + BCrypt(12)）、`UserCacheService`（`LoginProperties.cacheKey = "user_login_cache:"` + `userCacheIdleTime`）。单点登录：`login.single-login: true`（prod 已配）应踢同用户名同平台旧会话——旧版在 save 前踢，新版 `kickOutForUsernameAndPlatform` 已具备但**未接线**。

- `SecurityUtils`（静态）：`getCurrentUserId()`/`getTokenUid()`/`getCurrentUsername()` 用 **Hutool JWTUtil** 从请求头 token 取 claim；`getCurrentUser()` 经 `SpringBeanHolder` 拿 `UserDetailsService` 查库。供 SSE 会话键、资源创建者 id 使用。

### 2. Redis / 缓存（已迁移完成）

- `RedisConfiguration`（infrastructure）：`RedisTemplate` key 用 String、value 用 `JacksonRedisSerializer`；`RedisCacheManager` 默认 TTL 2h；缓存 Key 由 `KeyGenerator` 生成——(class、method、package、params) 序列化 JSON 后做 **MurmurHash3**（commons-codec `hash32x86`）取十六进制；`CacheErrorHandler` 读写异常只记日志不抛。
- **`RedisObjectMapper` 是静态持有者（common/utils），禁止注册为 Spring Bean**——否则会被 Spring MVC HTTP 消息转换器拾取，把 `@class` 字段泄漏进响应体。白名单仅 `java.util`、`java.time`、`site.ashenstation`（`BasicPolymorphicTypeValidator`），并忽略未知字段。
- `RedisUtils`：主要访问入口，set 带重试（3 次）、过期时间、hash/list/set、`scanDel` 等扫描删除。

### 3. 静态资源与目录约定（已迁移完成）

`StaticResourceDirectoryProperties`（前缀 `static-resource-directory-properties`，infrastructure）管理全部落盘目录与访问前缀；`WebConfig`（`@EnableWebMvc`）把 user-avatar / artist-avatar / poster 单目录与**全部 video-roots** 以 `file:` URI（Windows 反斜杠转正斜杠）映射到各自 path-prefix。video-roots 为列表（`{name, path}`），`enable-video-root` 指定当前启用根（写入端逻辑随视频模块回迁）。`CorsFilterConfig` 独立提供 `CorsFilter`（allowCredentials + originPattern `*`）。

### 4. 巨量上传 / 视频 / SSE（未迁移）

`system/modules/hugefileupload/` 仅 .gitkeep 占位。旧实现（分片上传 + FFmpeg 单命令自适应 HLS 转码 + SSE 进度推送）整体待回迁，关键设计见下"旧实现迁移参考"。

## 配置文件

YAML 位于 **`boot/src/main/resources/config/`**（注意：随重构从根 `src/main/resources/config/` 移入 boot，且扩展名由 `.yaml` 改为 **`.yml`**），共三个：

| 文件 | 内容 |
|---|---|
| `application.yml` | 公共：端口 9999、`jwt.*` 公共键（`client-header: X_PLATFORM`、`header`、`token-start-with`、`online-key`、`resource-permission-key`）、`rsa.private_key`。**旧版公共 yaml 里的 multipart 上限、FreeMarker、`login.*` 已不在公共层** |
| `application-dev.yml` | dev：远程库/Redis 凭据（明文，47.112.7.167）、`jwt.base64-secret`/时效、静态目录（本机 `C:\Users\ayuan\...`）、ffmpeg 本机绝对路径 + `h264_amf` |
| `application-prod.yml` | prod：DB/Redis 全走环境变量（`DATASOURCE_HOST/PORT/NAME/USERNAME/PASSWORD`、`REDIS_HOST/PORT/DB/PASSWORD`）、静态目录 `/amy/*`、ffmpeg 走 PATH + **`libx264` 软编**（`h264_qsv` 已弃用）、`login.single-login: true`（仅 prod 配置） |

**单位陷阱**：`jwt.token-validity-in-seconds` 名为秒、值实为**毫秒**（`7200000` = 2h），`detect`/`renew` 同为毫秒，沿用旧名勿按秒填。

配置组（infrastructure 的 `@ConfigurationProperties`）：

| 类 | 前缀 | 说明 |
|---|---|---|
| `SecurityProperties` | `jwt.*` | header/token 前缀（getter 自动拼尾空格）/在线 key/有效期/detect/renew/**`resourcePermissionKey`（权限缓存键，待用）** |
| `RsaProperties` | `rsa.*` | `private_key`（Base64） |
| `LoginProperties` | `login.*` | `singleLogin`、`userCacheIdleTime`（待用）、常量 `cacheKey = "user_login_cache:"` |
| `StaticResourceDirectoryProperties` | `static-resource-directory-properties.*` | 见上节 |
| `FFmpegProperties` | `ffmpeg.*` | **仅 4 字段**：ffmpegExecutorPath / ffprobeExecutorPath / videoEncoder / audioEncoder（旧 `conversion-to-mp4-args`/`conversion-to-ts-args` 已删，回迁转码时再定参数外置形态） |

## 数据库层

- 现状：仅 `model/entity/AppUser`（无 `@Table` 注解的空骨架）+ `infrastructure/dao/AppUserMapper`。旧 `User(sys_user)` 与全部 `mda_*` 实体/Mapper/服务已在重构中删除。
- 枚举仍保留于 common：**`VideoStatus`**（NORMAL/DELETE/CONVERSION，`@EnumValue` 存小写 `normal/delete/conversion`，`find` 未命中默认 NORMAL）、`UploadTaskType`（仅 VIDEO）。
- MyBatis-Flex 纯注解映射，无 XML；联表优先 `QueryChain` + APT TableDef。

## 部署（Docker + GitHub Actions）

**Dockerfile**：基础镜像 `registry.cn-hangzhou.aliyuncs.com/ashen_station/ffmpeg-openjdk21:latest`（含 ffmpeg），`COPY target/*.jar /app.jar`，`prod` profile 启动，jdwp 调试端口 5006，预建 `/amy/{poster,artist-avatar,archive,video_1,user-avatar,temp}`。⚠️ 两个问题：`EXPOSE 8080` 与实际端口 **9999** 不一致（遗留）；**`COPY target/*.jar` 与多模块产物路径不匹配**——重构后 jar 在 `boot/target/*.jar`，需改 Dockerfile（或 CI 构建上下文），当前 CI 的 Docker build 阶段必挂。

**CI/CD**（[.github/workflows/deploy.yml](.github/workflows/deploy.yml)）：**仅推送 `v*` tag 时触发**（可用 `/release`），同一 ref 只保留最新一次运行。流程：checkout → JDK 21 + Maven 缓存 → `mvn versions:set -DnewVersion=${GITHUB_REF_NAME#v}`（jar 版本去 v，须用 `github.ref_name`）→ `chmod +x ./mvnw && ./mvnw -B -DskipTests package`（**当前会因 .mvn wrapper 文件缺失直接失败**）→ 校验 ACR 凭据（secrets 不能进 `if:` 表达式，须先映射 env）→ buildx（`type=gha` 缓存）→ 登录 ACR → build-push-action 推 `ashen_station/amy-server:latest` + `:<tag 带 v>`（**`provenance: false`/`sbom: false` 必须保留**，ACR 不支持 OCI artifact 制品）→ GET Webhook `https://hooks.ashen-station.top/hooks/deploy-my-app`（头 `X-Webhook-Secret`）→ `success()`/`failure()` 发 163 SMTP 部署成功/失败邮件。

Secrets：`ALIYUN_ACR_USERNAME/PASSWORD`、`WEBHOOK_SECRET`、`MAIL_USERNAME/PASSWORD`。

**Qodana**（[.github/workflows/qodana_code_quality.yml](.github/workflows/qodana_code_quality.yml)）：master 推送 / PR / 手动触发静态扫描（`QODANA_TOKEN`），全量分析非 pr-mode。

分支：**develop 为日常开发分支，master 为主分支（PR 目标）**。

## 旧实现迁移参考

旧单模块完整实现保留在 git 历史，**最后含旧代码的提交是 `5d83927`**（`cdcbfac` 将其删除）。查看旧代码：`git show 5d83927:src/main/java/site/ashenstation/amyserver/<路径>`。

| 旧（site.ashenstation.amyserver） | 新 |
|---|---|
| `utils/AmyConstants` | common `utils/Constants`（类改名，claim 值不变） |
| `utils/TokenProvider`、`config/security/*` | system `modules/security/config/*` |
| `service/OnlineUserService` | 同名迁移；踢人逻辑与旧版一致：由登录方在 `save` 前单独调 `kickOutForUsernameAndPlatform`（见旧 AuthService） |
| `dto/JwtUserDto` | `modules/security/dto/JwtAppUserDto` |
| `entity/User`、`mapper/*` | model `entity/AppUser`、infrastructure `dao/*`（骨架） |
| `config/*`（redis/web/aspect/exception） | infrastructure `config/*`（exception 并入 config） |
| `property/*` | infrastructure `property/*` |
| `controller/*`、`service/Artist|Video|Upload|Sse` | system `modules/*/rest|service`（待回迁） |

回迁视频/上传流水线时必须保留的非显而易见设计（否则会踩坑）：

- 流程：`createUploadTask` 在 `upload-temp-directory/<id>/` 写 `config`（外壳 `UploadTaskDto{id, type, data}`，读回用 `UploadTaskDto<JsonNode>` 防泛型擦除）→ `POST /api/upload/chunk` 写 `chunk_<index>`（1 起）→ `GET /api/upload/next-step` 经 `@UploadProcess` 注解 + `@AfterReturning` 切面异步（AmyTaskExecutor）触发处理，接口可重复调用（临时目录已清理则 warn 跳过）。
- 合并直接发生在**启用视频根** `<taskId>/` 下（临时目录只存分片与 config）；`mda_video.id` 为字符串 UUID 主键非自增。
- 单命令自适应 HLS（`-var_stream_map` + `filter_complex` 多路 scale/pad）：档位目录 `v4k/v2k/v1080p/v720p/v480p`（= stream name，`%v` 替换）**执行前须预建**（ffmpeg 不自动建）；480p 档宽度取 **848** 规避硬编奇数宽；主播放列表由 `-master_pl_name index.m3u8` 生成（FreeMarker 模板已弃用该用途）。
- 状态机：入库 status=CONVERSION，主列表就绪后局部更新 NORMAL；**失败无回退**（DB 停 CONVERSION、临时目录与中间产物保留，可重放 next-step）。
- 旧三处 insert 无事务（`TransactionTemplate` 注入未用）；SSE 进度推送被摘除后 `FileUtils.mergeFileChunk` 里进度仅计算未上报，SSE 回迁时需接回。
- 旧匿名放行清单（SpringSecurityConfig 回迁参考）：`AnonTagUtils` 扫出的注解接口按 HTTP 方法放行 + `/`、`/resource/**`、`/ws/**`、swagger、`/api/version/amy/publish`，其余 `anyRequest().authenticated()`。

## 已知问题 / 待办（2026-09-11 现状）

- **构建链断裂**：`.mvn/wrapper/maven-wrapper.properties` 被删，`mvnw` 本地/CI 均无法启动（本机无系统 mvn）。恢复后首查：根 pom `<build><plugins>` 的 `spring-boot-maven-plugin` 被全部子模块继承，普通库模块若被 repackage（类进 BOOT-INF）将导致依赖方编译失败——多模块下该插件通常只应声明在 boot 模块。
- Dockerfile `COPY target/*.jar` 与多模块产物 `boot/target/*.jar` 不匹配；`EXPOSE 8080` vs 实际 9999。
- security：`SpringSecurityConfig`/`AuthController`/`UserDetailService`/`UserCacheService`/`SseService`/`JwtAppUserDto` 等空壳待实现；`save` 3 参/4 参 loginKey 键结构不一致（见认证节陷阱）；`checkRenewal` 未接线；`single-login` 踢人未接线；authorities 恒空无接口粒度权限（`resource-permission-key`/`permission` claim 预留）。
- 业务模块（艺术家/视频/上传/SSE）全部待回迁，`hugefileupload` 仅占位；model/dao 仅骨架。
- 旧 `check-chunk` 本就是占位实现，回迁时一并补。
- CI 仅 `v*` tag 触发（验证构建需本地执行或临时 tag）。

## 代码规范

- 全类 Lombok（`@Data`、`@Slf4j`、`@RequiredArgsConstructor` 构造器注入）；类型写法与既有代码保持一致；注释一律中文。
- 提交信息 `feat/fix/docs/chore` + 中文描述（可用 `/gitpush`；发版打 tag 用 `/release`）。
- JWT claim 一律引用 `common` 的 `Constants` 常量，勿硬编码字符串（claim 值与旧 `AmyConstants` 一致，前端无需配合修改）。
- MyBatis-Flex 纯注解，**不写 XML mapper**；新实体 `@Table/@Id/@Column` 放 model 模块，Mapper 放 infrastructure `dao`。
- 配置分组一律走 `@ConfigurationProperties`（infrastructure `property` 包），不要散落 `@Value`（`SecurityUtils` 静态注入属历史惯例，新增请避免）。
- 根 pom 的 `maven.compiler.proc=full` 是 Lombok 生效前提（maven-compiler-plugin 3.15+ 默认关闭 classpath 注解处理），勿删；pom 默认 `skipTests=true`。
- 异常统一抛 `BadRequestException`（配合 `ApiError` 响应）；异步线程内异常必须自行捕获记日志（调用方收不到）。
