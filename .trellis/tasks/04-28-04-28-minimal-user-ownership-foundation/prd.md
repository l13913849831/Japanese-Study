# brainstorm: minimal user ownership foundation

## Goal

为当前系统补上最小用户归属底座，使产品从单机单用户工具过渡到“每个用户有自己的学习进度、自己的资料、自己的偏好，同时能使用系统默认资料”的形态。

## What I already know

* 当前没有用户表、登录页、鉴权链路。
* 当前核心学习表普遍没有 `user_id` / `owner` 字段。
* 单词线的数据结构相对更容易后补用户归属：
  * `word_set / word_entry` 更像内容层
  * `study_plan / card_instance / review_log` 更像进度层
* 知识点线当前 `note` 表同时承载：
  * 内容本身
  * 复习进度
  * 掌握状态
  这会提高后补“默认资料 + 用户独立进度”的改造成本。
* `P1-A` 已确定为“按用户偏好决定默认顺序”，因此用户偏好应进入用户设置，而不应长期停留在本地状态。

## Requirements (evolving)

* 需要明确最小用户归属底座的范围，不把同步一起塞进来。
* 需要明确哪些表必须进入用户归属改造。
* 需要明确“系统默认资料”和“用户私有资料”的并存规则。
* 需要明确哪些设置属于用户级设置。

## Acceptance Criteria (evolving)

* [x] 能明确 `P0-a` 与 `P0-b` 的边界。
* [x] 能明确最小用户模型要覆盖哪些数据。
* [x] 能提出“默认资料 + 用户私有资料”并存的基本策略。
* [x] 能说明单词线与知识点线改造难度的差异。

## Out of Scope

* 不在这轮展开多端同步
* 不在这轮展开复杂权限体系
* 不在这轮展开团队/共享/社交能力

## Initial Split

### 1. 用户身份层

* 最小用户表
* 最基本登录能力
* 用户级设置

### 2. 数据归属层

* 哪些数据必须有 `user_id`
* 哪些数据应允许“系统默认”与“用户私有”并存

### 3. 单词线改造层

* 默认词库与用户词库
* 用户自己的学习计划与复习进度

### 4. 知识点线改造层

* 默认知识点资料是否要和用户进度拆层
* 当前 `note` 表是否需要后续拆成“内容层 + 用户进度层”

## Detailed Direction

### 1. Design Principle

第一版可以让“认证实现”保持简单，但“数据模型”必须为后续扩展预留边界。

具体原则：

* 业务永远只认内部 `user_id`，不直接依赖某种登录方式。
* 登录方式和内部用户分层，避免把“本地账号”写死成唯一身份模型。
* 用户偏好直接进入后端用户设置，不长期停留在前端本地。
* 资料归属从一开始就区分：
  * 系统默认资料
  * 用户私有资料

### 2. Recommended Minimal Model

#### 2.1 `user_account`

内部用户主表，只表达“这个系统里的用户是谁”。

建议字段：

* `id`
* `display_name`
* `status`
* `created_at`
* `updated_at`

说明：

* 不把密码直接塞进这里。
* 这个表是所有学习数据的最终归属锚点。

#### 2.2 `user_identity`

登录身份表，表达“这个内部用户如何登录进来”。

建议字段：

* `id`
* `user_id`
* `provider`
  * `LOCAL`
  * `KEYCLOAK`
  * `WECHAT_MP`
* `provider_subject`
  * 本地登录时可先用用户名或邮箱
  * Keycloak 时用 OIDC `sub`
  * 微信小程序优先用 `unionid`，没有时可先用 `openid`
* `password_hash`
  * 仅 `LOCAL` provider 使用
* `created_at`
* `updated_at`

说明：

* 第一版本地登录时，只落 `LOCAL` provider。
* 后续接 Keycloak 或微信，只是新增 provider，不会改业务归属。

#### 2.3 `user_setting`

用户设置表，先承接 P1-A 的主路径偏好。

建议字段：

* `user_id`
* `preferred_learning_order`
  * `WORD_FIRST`
  * `NOTE_FIRST`
* `created_at`
* `updated_at`

说明：

* `P1-A` 后续应该直接读取这里，而不是依赖 `localStorage`。

### 3. Authentication Strategy For V1

第一版建议：

* Web 端先做最基本本地登录
* 后端使用简单 session / cookie 认证
* 优先 `HttpOnly` cookie

原因：

* 比 JWT 刷新链路简单，适合先把用户底座补起来。
* 以后接 Keycloak 或微信时，认证入口可以替换，但内部 `user_id` 不变。

不要做的事：

* 不要把“密码校验逻辑”散落到业务服务里。
* 不要让学习模块直接依赖“本地登录”假设。

### 4. Content Ownership Strategy

第一版就要定义“系统默认资料”和“用户私有资料”怎么并存。

推荐统一规则：

* `scope = SYSTEM | USER`
* `owner_user_id`
  * `SYSTEM` 资料为空
  * `USER` 资料指向对应用户

这样后续自然支持：

* 使用系统默认资料
* 上传自己的资料
* 两类资料并存

### 5. Word Line Migration Plan

单词线当前分层相对更健康，适合第一批进入用户归属改造。

#### 5.1 内容层

建议改造：

* `word_set`
  * 增加 `scope`
  * 增加 `owner_user_id`
* `word_entry`
  * 继续挂在 `word_set_id` 下

说明：

* 这样默认词库可以作为 `SYSTEM` 词库存在。
* 用户上传的词库则是 `USER` 词库。

#### 5.2 进度层

这些表必须明确用户归属：

* `study_plan`
  * 增加 `user_id`
* `card_instance`
  * 推荐通过 `plan_id` 间接归属用户
  * 第一版可不强行再冗余 `user_id`
* `review_log`
  * 继续通过 `card_instance -> plan -> user` 追溯

原因：

* 单词线的进度本来就是围绕 `study_plan` 建立的。
* 把 `study_plan` 归到用户，单词线大部分用户边界就成立了。

### 6. Note Line Migration Plan

知识点线是 `P0-a` 的真正难点。

当前 `note` 表同时承载：

* 内容
* 复习状态
* FSRS 数据
* 弱项状态

如果未来要支持“系统默认知识点资料 + 每个用户独立进度”，推荐不要只给 `note` 粗暴加一个 `user_id` 就结束。

#### 6.1 推荐拆层

建议逐步演进为：

* `note_source`
  * 内容本体
  * `title`
  * `content`
  * `tags`
  * `scope`
  * `owner_user_id`
* `user_note_state`
  * `user_id`
  * `note_source_id`
  * `review_count`
  * `mastery_status`
  * `due_at`
  * `last_reviewed_at`
  * `fsrs_card_json`
  * `weak_flag`
  * `weak_marked_at`
  * `last_review_rating`
* `user_note_review_log`
  * `user_note_state_id`
  * `reviewed_at`
  * `rating`
  * `response_time_ms`
  * `note_text`
  * `fsrs_review_log_json`

#### 6.2 为什么不建议只给 `note` 加 `user_id`

如果只加 `user_id`：

* 默认资料就很难共享
* 用户想基于同一份系统默认知识点产生自己的进度会很别扭

## Current Implementation Snapshot (2026-04-28)

### 已完成

* 后端新增 `user_account / user_identity / user_setting`，并通过 `V10__init_user_ownership_foundation.sql` 回填现有单词线与模板线数据。
* 第一版认证采用 Spring Security + 本地账号 + `HttpOnly` session cookie。
* 已提供接口：
  * `POST /api/auth/login`
  * `POST /api/auth/logout`
  * `GET /api/me`
  * `PUT /api/me/settings`
* 已把这些现有主路径收口到当前用户上下文：
  * `word_set / word_entry`
  * `study_plan`
  * `anki_template / md_template`
  * `export_job`
  * 单词学习 dashboard
  * 单词弱项查询与移除
* 前端已补：
  * 登录页
  * 路由鉴权守卫
  * `me` 初始化查询
  * Header 学习顺序偏好切换
  * 退出登录
  * 系统资料只读 / 我的资料可编辑的前端呈现

### 当前仍未完成

* 知识点线仍是全局 `note` 模型，尚未拆到“内容层 + 用户进度层”。
* 还没有注册、用户管理、切换账号、多账号运营工具。
* 身份模型已预留 `KEYCLOAK / WECHAT_MP` provider，但当前只实现了 `LOCAL`。
* 本轮完成了编译与测试校验，但还没有对真实数据库和浏览器会话做人工端到端冒烟。
* 后面会更难拆出“内容层”和“用户进度层”

所以我的建议是：

* 单词线可以先做“渐进改造”
* 知识点线最好尽早按“内容 / 用户状态”分层思路设计

### 7. P1-A Dependency

`P1-A` 应明确依赖 `P0-a` 的这个结果：

* 用户已登录
* 用户有自己的 `preferred_learning_order`
* 工作台可以按用户偏好决定默认先单词还是先知识点

这样后续无论：

* 改为 Keycloak
* 接入微信小程序
* 同一用户多端登录

P1-A 的规则都不用重写，只是认证入口变化。

### 8. V1 Scope Recommendation

如果要做“先简单、但后续好扩展”，我建议第一版范围收敛成：

1. `user_account`
2. `user_identity`（先只支持 `LOCAL`）
3. `user_setting`
4. 单词线用户归属改造
5. 知识点线按“内容 / 用户状态”分层重构设计

这个范围的特点是：

* 认证实现简单
* 业务归属模型正确
* 后续接 Keycloak / 微信不会推翻业务层

## Table-Level Change Plan

### Must add in V1

#### New tables

1. `user_account`
2. `user_identity`
3. `user_setting`

#### Existing tables that should change in V1

1. `word_set`
   * add `scope`
   * add `owner_user_id`
2. `study_plan`
   * add `user_id`
3. `anki_template`
   * add `scope`
   * add `owner_user_id`
4. `md_template`
   * add `scope`
   * add `owner_user_id`
5. `export_job`
   * keep `plan_id` as the main ownership chain
   * first version can avoid duplicating `user_id`
   * ownership can be derived through `plan -> user`

### Should be redesigned in V1 discussion, but can land in staged implementation

1. `note`
   * do not rush into only adding `user_id`
   * first finalize whether to split into source/state layers
2. `note_review_log`
   * should follow the chosen note-state model
3. `card_instance`
   * first version can remain plan-owned
   * later evaluate whether direct `user_id` is worth adding for query simplicity
4. `review_log`
   * first version can remain card-owned
   * later evaluate whether direct `user_id` is worth adding

### Can defer after V1

1. extra auth-provider metadata fields for advanced SSO flows
2. device/session management tables
3. sync-related snapshot/version tables

## Ownership Rules

### Content-like resources

These should support both system-default and user-private ownership:

* `word_set`
* `anki_template`
* `md_template`
* future `note_source`

Rule:

* `scope = SYSTEM` -> `owner_user_id = null`
* `scope = USER` -> `owner_user_id != null`

### Progress-like resources

These should always be user-private:

* `study_plan`
* `card_instance` (directly or indirectly)
* `review_log` (directly or indirectly)
* future `user_note_state`
* future `user_note_review_log`
* `user_setting`

## Migration Order Recommendation

### Step 1: User foundation

Add:

* `user_account`
* `user_identity`
* `user_setting`

At this stage:

* create one bootstrap local user
* seed one default `user_setting`
* keep existing learning features still pointing to old single-user assumptions until data backfill is ready

### Step 2: Word-line ownership migration

1. add `scope` + `owner_user_id` to `word_set`
2. mark existing word sets as either:
   * `SYSTEM`
   * or belonging to the bootstrap user
3. add `user_id` to `study_plan`
4. backfill all existing plans to the bootstrap user
5. let plan-owned card/review data continue to derive ownership through plan

Reason:

* this gets one learning line user-ready quickly
* this also gives `export_job` a stable ownership chain through `plan_id`

### Step 3: Template ownership migration

1. add `scope` + `owner_user_id` to `anki_template`
2. add `scope` + `owner_user_id` to `md_template`
3. mark shipped templates as `SYSTEM`
4. allow future user-created templates to be `USER`

Reason:

* templates are part of “default resources vs user resources”
* doing this early prevents later ambiguity around who owns export behavior

### Step 4: Authentication wiring

1. add local login endpoint
2. create authenticated session
3. load `user_setting`
4. expose current-user endpoint for frontend bootstrapping

Reason:

* at this point P1-A can safely depend on real user preference

### Step 5: Note-line structural migration

This should be the most carefully designed step.

Preferred direction:

1. introduce `note_source`
2. introduce `user_note_state`
3. introduce `user_note_review_log`
4. migrate existing `note` + `note_review_log` data into the new split model
5. retire the old single-layer note model

Reason:

* this is where “system default note content + user-specific progress” becomes truly possible
* postponing this design too long increases rework cost

## API/Backend Boundary Suggestion

### V1 auth surface

Keep the first version minimal:

* `POST /api/auth/login`
* `POST /api/auth/logout`
* `GET /api/me`
* `PUT /api/me/settings`

### Service boundary

Recommended internal separation:

* auth / identity service
* current-user resolver
* user-setting service

Avoid:

* letting learning services parse login credentials directly
* coupling business services to `LOCAL`-only assumptions

## Key Extension Points For Future Keycloak/WeChat

To keep V1 easy to extend later, reserve these assumptions now:

1. one `user_account` may have many `user_identity` rows
2. provider-specific subject is opaque text, not a typed local username field
3. user settings belong to `user_account`, not to a specific identity provider
4. business tables never reference `user_identity`, only `user_account`
