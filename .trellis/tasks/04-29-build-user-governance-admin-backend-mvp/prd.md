# P0-c-3: build user governance admin backend MVP

## Goal

实现管理员后台第一期的“用户治理”能力，让内部管理员可以查看用户、管理账号状态，并处理最基本的安全治理动作。

## What I already know

* 用户已明确第一阶段只优先做“用户治理后台”。
* 后续还可能有内容治理、VIP 治理、托管备份治理，但不在第一期。
* 当前更需要的是运营治理和安全治理，而不是复杂 IAM 平台。

## Requirements

* 提供管理员后台独立入口与基础壳层。
* 提供用户列表、搜索、详情。
* 支持禁用/启用账号。
* 支持基础密码治理能力。
* 查看用户资产摘要，辅助排障与治理。
* 对关键后台操作做审计记录。

## Acceptance Criteria

* [x] 已明确用户治理后台 MVP 的页面和接口范围。
* [x] 已明确哪些用户数据允许管理员查看，哪些不应暴露。
* [x] 已明确账号状态操作和审计的最小闭环。

## Current Delivered Slice

* `/api/admin/users` 已支持用户列表、搜索、状态过滤、角色过滤。
* `/api/admin/users/{userId}` 已支持用户详情和资产摘要。
* `/api/admin/users/{userId}/disable` 与 `/enable` 已支持账号禁用 / 启用。
* `/api/admin/users/{userId}/reset-password` 已支持后台重置本地密码，并清理登录失败状态。
* `/api/admin/audit-events` 已支持安全审计事件查询。
* `/api/admin/security-alerts` 已支持从审计事件派生安全告警。
* 前端 `/admin` 已展示用户治理、审计事件和安全告警。
* 管理员查看用户详情、启用 / 禁用、重置密码都会写入审计事件。

## Remaining Work

* 无

## Out of Scope

* 这轮不做内容/模板治理后台
* 这轮不做客服工单系统
* 这轮不做后台直接编辑学习数据
* 这轮不做复杂统计看板
