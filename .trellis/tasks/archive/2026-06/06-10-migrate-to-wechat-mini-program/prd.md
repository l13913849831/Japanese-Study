# 微信小程序迁移

## Goal

把现有 Web 学习端主路径迁移出一套微信小程序端。小程序端用 Taro + React + TypeScript，管理端继续留在 Web。

## Current Decisions

- 技术栈：Taro + React + TypeScript。
- 迁移范围：只迁学习端主路径。
- 不迁移范围：管理端、模板管理、大批量导入、导出、备份。
- 账号方向：微信登录优先，系统内部仍统一使用 `user_account.id`。
- 小程序会话：走 mobile token，不复用浏览器 cookie + CSRF。
- Web 管理端：继续使用 session cookie + CSRF。

## Account Boundary

小程序不能把 `openid` 当系统用户主键。系统内部用户主体仍是 `user_account`，微信身份只作为外部登录凭证绑定到内部用户。

```text
user_account
  -> owns learning data
  -> has roles and profile

user_identity
  -> provider = WECHAT_MINIAPP
  -> provider_user_id = openid
  -> unionid optional

mobile_session
  -> bearer token
  -> expires/revoked metadata
  -> belongs to user_account
```

## Phase 1 MVP

- 新增 `miniapp/` Taro 工程。
- 小程序端页面：
  - 微信登录
  - 今日学习工作台
  - 单词复习
  - 知识卡复习
  - 弱项
  - 账号
- 小程序端 API 层：
  - 使用 `Taro.request`
  - 使用 Bearer Token
  - 不使用 cookie + CSRF
- 后端契约：
  - `POST /api/mobile/auth/wechat-login`
  - `POST /api/mobile/auth/logout`
  - `GET /api/mobile/me`
  - 学习端已有 API 后续允许 mobile token 访问
- 微信登录链路：
  - `wx.login` 获取 code
  - 后端用 code 换 openid
  - 查找或创建内部 learner 账号
  - 绑定 `WECHAT_MINIAPP` 身份
  - 返回 mobile token 和当前用户

## Phase 2

- 绑定已有本地账号。
- 已有 Web 用户可在小程序内绑定微信。
- 支持手机号授权登录或补充手机号。
- 支持 unionid，便于公众号、App 或其他微信入口合并身份。
- 完善账号解除绑定流程。

## Phase 3

- 设备会话管理。
- token 撤销和踢下线。
- 账号合并治理。
- 异常登录提醒。
- 管理端查看小程序绑定状态和设备会话。
- 更细的移动端安全审计。

## MVP Scope

### In Scope

- 生成可由微信开发者工具打开的 Taro 小程序工程。
- 迁移学习端主路径的页面壳、API DTO、请求层和基础交互。
- 明确 mobile auth 契约和环境变量。
- 文档记录本机无法验证的小程序运行项。

### Out of Scope

- 第一阶段不实现 Web 管理端迁移。
- 第一阶段不迁移模板、导入、导出、备份。
- 第一阶段不实现手机号、unionid、账号合并和设备管理。
- 第一阶段不伪造微信登录。

## Acceptance Criteria

- [x] `miniapp/` 存在 Taro + React + TypeScript 工程骨架。
- [x] 小程序端使用 `Taro.login` 调起微信登录。
- [x] 小程序端 token 存储在微信本地存储。
- [x] 小程序端请求层统一加 `Authorization: Bearer <token>`。
- [x] 学习端主路径页面能按 API 契约编译。
- [x] 文档说明微信 AppID、后端域名、mobile auth 配置。
- [x] Trellis 任务记录三期账号边界。
- [x] 后端实现 mobile auth endpoints。
- [ ] 微信开发者工具打开 `miniapp/` 并完成真机或模拟器验证。

## Technical Notes

- 当前 Web 请求层在 `frontend/src/shared/api/http.ts`，依赖 `fetch + credentials: include + CSRF`。
- 小程序请求层必须单独维护，不能复用 Web 请求层。
- 当前后端认证在 `backend/src/main/java/com/jp/vocab/shared/auth/SecurityConfig.java`。
- 当前后端已具备 mobile token、微信 code 换 openid、设备会话。

## Verification

- `npm run typecheck`
- `npm run build:weapp`

以上命令已在 `miniapp/` 下通过。微信开发者工具和真实微信登录未在本机验证。
