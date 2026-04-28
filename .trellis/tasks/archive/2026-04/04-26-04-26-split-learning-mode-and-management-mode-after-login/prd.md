# brainstorm: split learning mode and management mode after login

## Goal

把当前统一暴露所有功能的应用壳层，调整成“进入系统后先进入学习模式，管理能力下沉到次级入口”的双模式结构，并预留未来真实登录后的模式切换位置。

## What I already know

* 当前前端没有真实登录鉴权。
* 当前 `/` 默认跳到 `/dashboard`。
* 当前左侧菜单直接暴露：
  * `/word-sets`
  * `/study-plans`
  * `/notes`
  * `/templates`
  * `/export-jobs`
* 当前更适合作为学习主路径的入口已经有：
  * `/dashboard`
  * `/cards`
  * `/notes/review`
* 用户希望：
  * 登录后先看到学习流程
  * 不默认暴露导入或管理界面
  * 管理界面通过某个界面再进入
* 当前“登录”阶段只需要前端占位，不接真实后端鉴权。

## Assumptions (temporary)

* 这轮先做前端壳层与导航结构，不做真正账户体系。
* “学习模式”和“管理模式”更像信息架构分层，不是权限边界。
* 导入、计划、模板、词库维护仍然保留，只是不再放第一层。

## Open Questions

* 学习模式里是否只保留 `/dashboard`、`/cards`、`/notes/review` 三个主入口，还是要把 `/weak-items` 也算学习模式一级入口？

## Requirements (evolving)

* 进入系统后默认进入学习模式。
* 学习模式优先展示：
  * 今日工作台
  * 单词复习
  * 知识点复习
* 管理模式入口可见，但不抢默认主路径。
* 后续如果接真实登录，不需要重做整套路由语义。

## Acceptance Criteria (evolving)

* [ ] 用户进入系统后先看到学习入口而不是管理菜单。
* [ ] 管理页面仍可访问，但默认不会抢占首屏。
* [ ] 新壳层能明确区分学习模式与管理模式。
* [ ] 登录占位页能承接未来真实鉴权替换。

## Definition of Done (team quality bar)

* 路由与壳层行为说明清楚
* 前端构建通过
* 文档与前端契约同步

## Out of Scope (explicit)

* 这轮不做后端用户表
* 这轮不做 token / session / 权限模型
* 这轮不做多角色权限控制

## Technical Notes

* 路由入口：
  * `frontend/src/app/router.tsx`
* 应用壳层：
  * `frontend/src/app/shell/AppShellLayout.tsx`
* 当前状态存储：
  * `frontend/src/shared/store/useUiStore.ts`
* 当前系统说明：
  * `docs/system-usage-guide.md`

