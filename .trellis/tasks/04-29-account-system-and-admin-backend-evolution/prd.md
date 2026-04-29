# brainstorm: account system and admin backend evolution

## Goal

为当前“最小本地账号 + session 登录”的基础版系统，规划下一阶段更完整的账号体系与管理员后台边界，明确哪些能力属于用户自助账号域，哪些能力属于平台运营/管理域，以及这些能力应如何拆分为可落地的后续任务。

## What I already know

* 当前仓库已经完成 `P0-a` 最小用户归属底座，不再是匿名单用户工具。
* 当前后端已经有：
  * `user_account`
  * `user_identity`
  * `user_setting`
  * `POST /api/auth/login`
  * `POST /api/auth/register`
  * `POST /api/auth/logout`
  * `GET /api/me`
  * `/api/me/profile`
  * `/api/me/settings`
  * `/api/me/password`
* 当前认证实现是 Spring Security + `HttpSession`：
  * 不是 JWT
  * 没有 refresh token
  * 没有设备会话管理
  * 没有 API token / personal access token
* 当前 `AppUserPrincipal#getAuthorities()` 返回空集合，说明还没有角色/权限模型。
* 当前前端已有 `/login`、`/account`、鉴权守卫，但没有管理员后台入口。
* 当前系统已经开始按“当前登录用户”收口业务数据，但还没有：
  * 管理员角色
  * 用户管理后台
  * 审计日志
  * 封禁/解封/重置密码后台流程
  * 多身份登录（Keycloak / 微信）
  * 多设备会话治理
* 当前已完成 `P0-b` 的本地备份/恢复 MVP，这意味着后续账号系统规划需要把“谁能恢复什么、后台能看到什么、是否允许代操作”边界一起考虑进去。

## Assumptions (temporary)

* 近期仍以 Web 端为主，不是移动 App / 第三方开放 API 优先。
* 管理员后台第一阶段更像内部运营后台，而不是面向普通用户开放的复杂租户控制台。
* 账号系统的下一阶段目标应优先补“治理能力”和“权限边界”，而不是立刻追求 SSO / OAuth 平台化。

## Requirements (evolving)

* 需要明确“账号系统增强”与“管理员后台系统”不是一个单点任务，而是一个能力容器。
* 需要把账号能力至少拆成：
  * 认证方式
  * 账号资料
  * 安全治理
  * 权限模型
  * 后台管理
* 需要明确普通用户自助能力和管理员能力的边界。
* 需要明确哪些能力值得先做，哪些应延后，避免过早引入复杂 IAM。
* 需要考虑后续：
  * VIP 功能
  * 云端备份托管
  * 多端同步
  * 外部身份接入
  的兼容性。

## Acceptance Criteria (evolving)

* [ ] 能说明当前账号系统已经覆盖了什么、还缺什么。
* [ ] 能明确“用户账号域”和“管理员域”的职责边界。
* [ ] 能给出 2-3 个可行的演进路线，并说明推荐方案。
* [ ] 能拆出一组可继续排期的父任务 / 子任务。
* [ ] 能明确第一阶段管理员后台的 MVP 范围。

## Definition of Done (team quality bar)

* 后续进入实现前，任务边界清楚，不再把账号、权限、后台、同步混成一个大包。
* 每个子任务都能说明：
  * 目标
  * 边界
  * 风险
  * 明确不做什么
* 文档与任务拆分可直接作为后续实现输入。

## Out of Scope (explicit)

* 这轮不直接实现新的鉴权机制
* 这轮不直接上 Keycloak / OAuth2 / 微信登录
* 这轮不直接做完整多租户
* 这轮不直接做客服工单系统
* 这轮不直接做 BI / 数据分析平台

## Technical Notes

* 当前安全配置见：
  * `backend/src/main/java/com/jp/vocab/shared/auth/SecurityConfig.java`
* 当前登录/注册/登出实现见：
  * `backend/src/main/java/com/jp/vocab/user/service/AuthService.java`
  * `backend/src/main/java/com/jp/vocab/user/controller/AuthController.java`
* 当前用户主体没有 authorities：
  * `backend/src/main/java/com/jp/vocab/shared/auth/AppUserPrincipal.java`
* 当前前端账号入口见：
  * `frontend/src/features/auth/LoginPage.tsx`
  * `frontend/src/features/auth/AccountPage.tsx`
* 当前存在 bootstrap 本地用户初始化：
  * `backend/src/main/java/com/jp/vocab/user/service/BootstrapLocalUserInitializer.java`
* 这说明系统目前是“先能登录、能隔离当前用户数据”，但还不是“可运营、可审计、可授权管理”的账号系统。

## Research Notes

### What mature products usually separate

成熟产品通常会把下面几层拆开，而不是塞进一个“用户模块”：

* **Identity / Authentication**
  * 用户怎么登录
  * 支持哪些身份源
  * session / token / SSO / MFA
* **Account / Profile**
  * 用户资料
  * 偏好设置
  * 账号状态
* **Authorization**
  * 角色
  * 权限
  * 后台操作范围
* **Administration / Operations**
  * 用户搜索
  * 封禁/解封
  * 密码重置
  * 审计日志
  * 内容治理

### Constraints from our repo/project

* 当前是单仓、单后端、单前端，不适合一下子引入过重 IAM 平台。
* 当前 Web 端用 session/cookie 已经工作，短期没有必须切 JWT 的硬需求。
* 当前还没有后台独立前端壳层，也没有管理员 API 命名空间。
* 后续可能接 VIP / 云端备份 / 多端同步，因此“角色、权限、可见范围、代操作边界”要尽早定义。

### Feasible approaches here

**Approach A: 先补本地账号治理 + 轻量管理员后台** (Recommended)

* How it works:
  * 保留当前 session/cookie
  * 补角色模型、管理员 API、后台前端壳层
  * 先完成内部运营必需能力
* Pros:
  * 与现有代码衔接最顺
  * 风险低
  * 能最快补上“可运营、可治理、可封禁、可排障”的能力
* Cons:
  * 外部身份接入与 SSO 仍需后续再做
  * API token / 多端设备能力仍要后补

**Approach B: 先做统一权限模型，再同时推进后台与多身份接入**

* How it works:
  * 先引入角色/权限/资源范围模型
  * 再补后台管理和更多登录方式
* Pros:
  * 架构更完整
  * 后续扩展更顺
* Cons:
  * 抽象过早，容易超前设计
  * 第一阶段交付慢

**Approach C: 直接外接成熟身份平台，再围绕它做后台**

* How it works:
  * 先把登录与身份源交给外部 IdP
  * 本系统主要保留业务归属与后台运营功能
* Pros:
  * 长期看认证能力最强
  * SSO / MFA / 联邦登录更规范
* Cons:
  * 当前阶段明显过重
  * 无法优先解决“现在就缺的后台治理”

## Current Recommendation

推荐采用 **Approach A**：

* 第一阶段先承认现实：
  * 当前系统已经有可用 session 登录
  * 真正缺的是治理能力与后台能力，不是 JWT 本身
* 因此优先做：
  * 角色/权限基础
  * 管理员后台壳层
  * 用户管理与审计 MVP
  * 管理侧内容治理边界
* 外部身份接入、MFA、JWT/API token 进入后续阶段，而不是现在一起捆绑。

## User Decision

### Chosen First Admin Scope

用户已选择：

* **管理员后台第一阶段先只做“用户治理后台”**

含义：

* 第一阶段不优先做内容/模板治理后台
* 第一阶段不优先做客服工单式后台
* 第一阶段重点是账号治理、安全治理、用户状态治理

### Derived Priority Order

结合当前仓库状态，当前推荐优先级调整为：

1. `P0-c-1`：session 鉴权与 cookie 安全加固
2. `P0-c-2`：管理员角色基础与后台访问控制
3. `P0-c-3`：用户治理后台 MVP
4. `P0-b` 后续延伸：云端托管备份、VIP、设备会话、更重的产品化管理能力

原因：

* `P0-b` 当前本地备份/恢复 MVP 已经打通
* 现在更高风险、更影响后续可运营性的短板是：
  * session 安全治理
  * 管理员权限边界
  * 用户治理后台
* JWT/API token 不是此时的第一优先级，先补治理与访问控制更合理

## Proposed Capability Split

### 1. 用户账号域

面向普通登录用户的自助能力：

* 注册
* 登录 / 登出
* 修改资料
* 修改密码
* 查看当前账号状态
* 管理个人备份与恢复
* 后续可扩：
  * 邮箱验证
  * 找回密码
  * 设备会话管理
  * 绑定外部身份

### 2. 平台权限域

平台内部控制能力：

* 角色模型
* 权限点
* 后台路由访问控制
* 管理 API 访问控制
* 资源级可见范围定义

### 3. 管理员后台域

面向内部运营/管理员：

* 用户搜索与详情查看
* 账号状态操作
* 重置密码或触发密码重置流程
* 查看关键用户资产摘要
* 后续可扩：
  * 查看备份托管状态
  * VIP 状态管理
  * 内容审核/模板治理
  * 审计日志筛查

## Candidate Task Breakdown

### P0-c-1: harden session auth and cookie security

* 显式配置 session cookie 安全策略
* 补 CSRF 方案
* 处理 bootstrap 默认账号策略
* 增加登录限流/失败治理的设计入口

### P0-c-2: introduce role and permission foundation

* 增加最小角色模型
* 至少区分：
  * `USER`
  * `ADMIN`
* 为后续后台 API 做访问控制

### P0-c-3: build user governance admin backend MVP

* 用户列表
* 用户详情
* 禁用/启用账号
* 管理员后台壳层与路由隔离
* 查看基础资料与最近活动摘要
* 后台重置密码与关键操作审计

### Deferred Track: future auth evolution

* 会话管理
* 设备管理
* API token / JWT / 外部 IdP 评估
* 这一条延后，不强绑第一期

## Admin MVP Boundary Proposal

第一阶段管理员后台只建议做这些：

* 登录后识别管理员角色
* 后台独立入口与路由守卫
* 用户列表 / 搜索 / 详情
* 禁用 / 启用账号
* 查看用户拥有的数据摘要
  * 词库数
  * 学习计划数
  * 知识点数
  * 备份恢复相关摘要（若后续有托管）
* 审计以下操作：
  * 管理员查看用户详情
  * 管理员禁用/启用账号
  * 管理员重置密码

不建议第一阶段就做：

* 完整 RBAC 配置界面
* 复杂客服 impersonation
* 多租户组织/团队
* 后台直接编辑用户学习数据
