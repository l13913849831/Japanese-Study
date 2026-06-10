# 微信小程序迁移说明

## 目标

第一阶段只迁移学习端主路径。管理端继续通过 Web 使用。

## 技术选择

使用 Taro + React + TypeScript。

原因：

- 现有 Web 是 React + TypeScript，迁移成本低。
- 学习端页面可以保留 React 组件思维和 DTO 组织方式。
- 小程序端仍需要单独处理路由、UI 组件、请求层和认证。

## 账号边界

小程序端采用微信登录优先，但系统内部用户主体不变。

```text
微信用户
  -> wx.login code
  -> 后端换 openid
  -> user_identity 绑定
  -> user_account 作为学习数据主体
```

不要把 `openid` 当系统用户 ID。`openid` 和 `unionid` 只属于外部身份层。

## 三期任务

### Phase 1

- Taro 小程序工程。
- 微信登录入口。
- mobile token 请求层。
- 今日学习工作台。
- 单词复习。
- 知识卡复习。
- 弱项。
- 账号页。

### Phase 2

- 已有 Web 本地账号绑定微信。
- 手机号授权。
- unionid 支持。
- 解绑微信身份。

### Phase 3

- 设备会话管理。
- token 撤销。
- 账号合并。
- 异常登录提醒。
- 管理端查看绑定状态和会话。

## 后端契约

第一阶段小程序端按下面契约调用。后端已补齐实现。

```text
POST /api/mobile/auth/wechat-login
POST /api/mobile/auth/logout
GET  /api/mobile/me
```

`POST /api/mobile/auth/wechat-login` 请求：

```json
{
  "code": "wx.login 返回的 code"
}
```

响应：

```json
{
  "token": "mobile bearer token",
  "expiresAt": "2026-06-10T12:00:00+09:00",
  "user": {
    "id": 1,
    "username": "wx_xxx",
    "displayName": "微信用户",
    "preferredLearningOrder": "WORD_FIRST",
    "roles": ["USER"]
  }
}
```

学习端已有 API 后续允许 mobile token 访问。管理端 API 仍只允许 Web 管理端 session 访问。

## 环境要求

- 微信开发者工具。
- 小程序 AppID。
- 后端可访问的 HTTPS 域名。
- 微信公众平台配置合法请求域名。

本机当前没有微信小程序运行环境，本轮只做代码和静态契约。需要回到有微信开发者工具的环境后验证构建产物。

## 启动命令

```bash
cd miniapp
npm install
npm run dev:weapp
```

微信开发者工具打开 `miniapp/`，构建产物目录是 `miniapp/dist/`。

本地联调可临时关闭微信开发者工具的域名校验，或使用可访问的内网 HTTPS 代理。正式环境必须配置合法 HTTPS 域名。

## 当前验证结果

已通过：

```bash
cd miniapp
npm run typecheck
npm run build:weapp
```

未验证：

- 微信开发者工具打开项目。
- `wx.login` 真正返回 code。
- 后端 `code -> openid -> mobile token` 已落地。
- 合法 HTTPS 域名下的接口访问。
