# P0-c-2: introduce admin role foundation and access control

## Goal

为后续管理员后台补齐最小角色与访问控制基础，明确普通用户与管理员的权限边界，并让后台路由/API 能被正确隔离。

## What I already know

* 当前 `AppUserPrincipal#getAuthorities()` 返回空集合。
* 当前系统只有“已登录用户”这一层，没有 `ADMIN` 角色。
* 当前还没有后台独立 API 命名空间和前端壳层守卫。

## Requirements

* 定义最小角色模型，至少包含 `USER` 与 `ADMIN`。
* 明确哪些接口属于普通用户，哪些接口属于管理员。
* 为管理员后台路由与 API 提供统一访问控制入口。
* 为后续审计与用户治理操作预留权限边界。

## Acceptance Criteria

* [ ] 已确定最小角色模型。
* [ ] 已确定管理员 API / 路由隔离策略。
* [ ] 已明确后续用户治理后台实现所依赖的权限基础。

## Out of Scope

* 这轮不做复杂 RBAC 配置界面
* 这轮不做多租户/组织权限
* 这轮不做 impersonation
