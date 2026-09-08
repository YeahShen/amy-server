---
name: release
description: 自动完成 amy-server 发布流程。当用户输入 `/release` 时触发此技能。功能包括：检查分支与远端同步、确定发布版本号、打 v* tag 并推送以触发 GitHub Actions CI（自动打包、推镜像到阿里云 ACR、Webhook 触发服务器部署、发送成功/失败邮件）。支持参数直接指定版本，如 `/release v1.2.3`；未给参数时自动建议下一个版本并请求用户确认。
  disable-model-invocation:true
---

# Release 发布技能

自动完成 amy-server 发版流程：确定版本 → 打 tag → 推送 tag → 触发 GitHub Actions 全自动构建部署。

## 背景（必读）

本仓库 GitHub Actions（[.github/workflows/deploy.yml](.github/workflows/deploy.yml)）**只在推送 `v*` tag 时触发**：

- 推送如 `v1.0` 的 tag → CI 执行 `versions:set`（jar 版本取 tag 名）→ 打包 → 构建镜像推 `registry.cn-hangzhou.aliyuncs.com/ashen_station/amy-server:latest` + `:<tag>` → 请求部署 Webhook → 按成败发邮件。
- **master 常规推送、PR 均不触发 CI**，因此发布 = 打 `v*` tag 并推送远端，仅此一步。

## 执行步骤

### 1. 确认版本号

- 用户可直接带参：`/release v1.2.3`（须以 `v` 开头，否则提醒并补全）。
- 未带参时给出建议值：执行 `git tag --list 'v*' --sort=-v:refname | head -1` 取当前最高 tag（如 `v1.2.3`），建议**补丁位 +1**（→ `v1.2.4`）；**无任何 tag 时建议 `v0.1.0`**。
  - 若近期改动含新特性/破坏性变更，提示用户可考虑 minor / major。
- 与用户最终确认版本号（如无歧义可跳过再次询问，但要明示将发布的版本）。

### 2. 前置检查

1. 当前分支为 `master`：`git branch --show-current`；不是则提醒先切换或确认是否要对非 master 打 tag（不推荐）。
2. 与远端同步：`git fetch origin` 后比较 `git rev-parse master` 与 `git rev-parse origin/master`；存在**未推送提交**时提醒先 `/gitpush` 或确认——发布内容 = tag 指向的提交。
3. 工作区有无未提交改动（`git status --short`）：仅提示，不强制（tag 打在当前 HEAD，不影响未提交内容）。
4. 确认 tag 不存在：`git rev-parse -q --verify refs/tags/<版本>` 有输出则报错（tag 已存在，禁止覆盖，需换号或先删除）。

### 3. 打 tag 并推送

```bash
git tag -a <版本> -m "release: <版本>"
git push origin <版本>
```

- 推送只推 tag（`git push origin <tag>`），不要 `git push origin master`（tag 推送即触发 CI，master 推送无效果）。
- 推送失败时展示具体错误（网络/权限/非快进等），tag 若已推送成功但后续失败，说明远端已存在，勿重复推送。

### 4. 报告

输出格式：

### 成功时

✓ 发布已触发！

- 版本号: v1.2.3
- 指向提交: [short sha]（附完整提交信息首行）
- Tag 推送: 已完成
- CI 触发: GitHub Actions 已开始执行（可前往 https://github.com/<owner>/<repo>/actions 跟踪；若本机已装 gh 并登录，可 `gh run watch`）
- 后续自动化: 打包 → ACR `latest` + `<tag>` 镜像 → Webhook 服务器部署 → 结果邮件

### 失败时

✗ 发布失败
原因: [具体错误信息]（如：分支未同步、tag 已存在、推送被拒等）

### 无需发布时

✓ 已取消/无可发布内容

## 注意事项

1. **版本号必须带 `v` 前缀**（workflow 的 tag 过滤器为 `v*`）。
2. **不要修改 pom.xml 版本号**：pom 固定 `0.0.1-SNAPSHOT` 基线，CI 中由 `versions:set` 按 tag 覆盖，本地无需也不应改动。
3. tag 推送会立即触发线上部署（Webhook + 邮件），**确认无误后再推**；远端删除 tag 需 force，操作前想清楚。
4. 首次发布前确认仓库已配置 secrets：`ALIYUN_ACR_USERNAME` / `ALIYUN_ACR_PASSWORD` / `WEBHOOK_SECRET` / `MAIL_USERNAME` / `MAIL_PASSWORD`（缺失时 CI 相应步骤会失败）。
5. 推送 tag 前先 `git fetch`，避免基于过期的远端状态发布。
