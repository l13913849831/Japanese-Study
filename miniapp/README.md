# Japanese Study Miniapp

Taro + React + TypeScript 微信小程序端。

## Scope

Phase 1 只覆盖学习端主路径：

- 微信登录
- 今日学习工作台
- 单词复习
- 知识卡复习
- 弱项
- 账号

管理端、模板、导入、导出、备份继续留在 Web。

## Commands

```bash
npm install
npm run dev:weapp
npm run build:weapp
npm run typecheck
```

## Environment

默认 API 地址：

```text
http://localhost:8080/api
```

可在构建时覆盖：

```bash
TARO_APP_API_BASE_URL=https://example.com/api npm run build:weapp
```

微信正式环境必须配置 HTTPS 合法请求域名。
