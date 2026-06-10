# P0-c-1: harden session auth and cookie security

## Goal

在不切换到 JWT 的前提下，先把现有基于 session/cookie 的登录体系补到更安全、可上线和更适合后续管理员后台的状态。

## What I already know

* 当前使用 Spring Security + `HttpSession`。
* 当前密码使用 `BCrypt` 哈希，不是明文。
* 当前 `csrf` 被关闭。
* 当前还没有显式的 session cookie 安全配置。
* 当前存在 bootstrap 本地账号能力，生产环境策略需要更谨慎。

## Requirements

* 明确 session/cookie 的目标安全基线。
* 补齐 CSRF 防护方案。
* 显式配置 cookie 安全属性。
* 明确开发/生产环境 bootstrap 账号策略。
* 明确登录失败治理和后续限流入口。

## Acceptance Criteria

* [x] 已明确当前 session 方案的安全整改范围。
* [x] 已明确 cookie、CSRF、bootstrap 账号、登录失败治理的落地任务边界。
* [x] 后续可以直接进入实现，而无需再重新讨论“要不要先换 JWT”。

## Current Delivered Slice

* 已开启基于 session cookie 的 CSRF 防护
* 已新增 `GET /api/auth/csrf` 给前端取 token
* 前端写请求已统一自动携带 CSRF token
* bootstrap 本地账号已改为默认关闭、显式开启
* session cookie 已补显式配置入口
* 后端安全回归已补首批用例并通过：CSRF endpoint、缺 token 拒绝、带 token 登录、bootstrap 默认关闭与空密码保护
* 登录失败限流 / 锁定策略已落地：默认 5 次失败锁定 15 分钟，成功登录清零失败状态
* 认证安全审计事件已落地：登录成功、登录失败、账号锁定、账号禁用、登出

## Remaining Work

* 安全告警策略
* 审计事件后台查询 / 管理端展示

## Out of Scope

* 这轮不直接切 JWT
* 这轮不直接接 OAuth / Keycloak / 微信
* 这轮不直接做设备会话管理 UI
