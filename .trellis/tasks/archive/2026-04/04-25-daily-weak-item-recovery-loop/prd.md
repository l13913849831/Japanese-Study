# brainstorm: daily weak item recovery loop

## Goal

为单词线和知识点线增加“当天补救 + 易错项沉淀”的学习闭环，让 `Again` 不再只影响长期排期，而能在当日会话里形成回捞、加练和易错项提醒。

## What I already know

* 单词线当前有：
  * 今日卡片查询
  * 复习评分
  * 复习日志
  * 基础 dashboard
* 知识点线当前有：
  * FSRS 调度
  * 连续复习会话
  * 复习日志
  * 知识点 dashboard
* 两条线当前都没有：
  * 当天回队尾 / 回捞
  * `WEAK` 状态沉淀
  * 易错项列表页
  * 今日收尾的“薄弱项再练一轮”
* 已确认的产品决定：
  * 增加独立页面入口 `/weak-items`
  * 前台文案分开：
    * 单词线：易错词
    * 知识点线：易错知识点 / 薄弱知识点
  * 底层状态统一概念：`WEAK`
  * 两条线各自存储，不混表

## Requirements

* 单词线需要支持：
  * 第一次 `Again` 回今日队尾
  * 第二次 `Again` 标记 `WEAK`
  * 会话结束时可选“薄弱项再练一轮”
* 知识点线需要支持同一理念，但不照搬单词线：
  * 第一次 `Again` 进入今日待回顾子队列
  * 第二次 `Again` 标记 `WEAK`
  * 会话结束时可选“薄弱知识点再练一轮”
* 需要新增统一入口页 `/weak-items`
* `/weak-items` 需要至少包含两个标签页：
  * 易错词
  * 易错知识点
* 后续正式复习中：
  * `Good` / `Easy` 自动让项目从易错项中毕业
  * `Hard` 不自动毕业

## Acceptance Criteria

* [x] 设计能清楚区分单词线和知识点线的共同点与差异。
* [x] `/weak-items` 的入口和页面结构明确。
* [x] `WEAK` 的生命周期明确：
  * 进入
  * 展示
  * 加练
  * 自动毕业
* [x] 可以直接进入下一步实现前设计或开发任务。

## Out of Scope

* 本轮不实现复杂错误率模型
* 本轮不做独立专项训练模式
* 本轮不做跨端同步或通知

## Technical Approach

第一阶段先做 MVP：

* 单词线强调“回队尾”
* 知识点线强调“主队列结束后的薄弱项回顾”
* 两条线都落 `WEAK` 状态
* 页面统一收敛到 `/weak-items`

## Decision (ADR-lite)

**Context**:

当前系统已有记忆引擎和复习日志，但缺少会话内补救和薄弱项沉淀。用户希望这套能力自然融入每日学习流程，而不是只多一个孤立错题本。

**Decision**:

采用“当天补救 + `WEAK` 状态 + 易错项页面”三层组合：

* 单词线：
  * 第一次 `Again` 回队尾
  * 第二次 `Again` 标记 `WEAK`
* 知识点线：
  * 第一次 `Again` 进入待回顾子队列
  * 第二次 `Again` 标记 `WEAK`
* 两条线：
  * dashboard 给出摘要入口
  * `/weak-items` 聚合展示
  * `Good/Easy` 自动毕业

**Consequences**:

* 学习闭环更完整
* 前台表达更自然
* 未来可以继续扩专项训练和更复杂判定

## Technical Notes

* 单词线参考：
  * `frontend/src/features/cards/*`
  * `backend/src/main/java/com/jp/vocab/card/*`
* 知识点线参考：
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `backend/src/main/java/com/jp/vocab/note/service/NoteReviewService.java`
  * `backend/src/main/java/com/jp/vocab/note/service/NoteFsrsScheduler.java`

## Pre-Implementation Design

### 1. Design principles

* 不重做现有长期排期引擎。
* 当天补救和长期排期分层处理。
* `WEAK` 是长期态。
* 今日回捞是会话态。
* 单词线和知识点线共享概念，不强行共享表结构。

### 2. Data model design

#### 2.1 Word line

当前单词线的 `card_instance` 只有：

* `status`
* `due_date`
* `stage_no`

它不够表达 `WEAK` 和易错项生命周期。

建议在 `card_instance` 增加：

* `weak_flag boolean not null default false`
* `weak_marked_at timestamptz null`
* `weak_review_count integer not null default 0`
* `last_review_rating varchar(16) null`

说明：

* `weak_flag`
  * 当前是否属于易错词
* `weak_marked_at`
  * 进入易错词时间
* `weak_review_count`
  * 进入 `WEAK` 后累计被再次正式复习的次数
* `last_review_rating`
  * 便于弱项列表和 dashboard 摘要直接展示最近表现

会话态不建议先落库到表字段，第一版先由前端会话状态管理：

* `sessionAgainCountByCardId`
* `sessionWeakQueueCardIds`
* `sessionRequeuedCardIds`

原因：

* 当前单词复习是单用户、单会话前端驱动
* 今日回队尾是短生命周期状态
* 先不为短态引入额外表

#### 2.2 Note line

当前知识点线的长期状态在 `note` 表中。

建议在 `note` 增加：

* `weak_flag boolean not null default false`
* `weak_marked_at timestamptz null`
* `last_review_rating varchar(16) null`

不建议增加 `weak_review_count` 到 MVP 必需项，先靠 review log 和最近评分即可。

知识点线的会话态也先放前端会话状态：

* `sessionAgainCountByNoteId`
* `todayWeakNoteIds`
* `todayRecoveryQueueNoteIds`

### 3. Backend API design

#### 3.1 Word line review response

当前 `POST /api/cards/{cardId}/review` 返回值过于简单。

建议扩成：

```text
reviewId
cardId
rating
cardStatus
reviewedAt
weak
weakMarkedAt
todayAction
```

其中：

* `weak`
  * 当前是否已进入 `WEAK`
* `weakMarkedAt`
  * 首次进入 `WEAK` 的时间
* `todayAction`
  * `DONE`
  * `REQUEUE_TODAY`
  * `MOVE_TO_WEAK_ROUND`

说明：

* 第一版单词线的“第一次 Again 回队尾 / 第二次 Again 标记 WEAK”主要由前端会话态判断。
* 但后端仍要在正式 review 保存后更新长期 `WEAK` 状态。

#### 3.2 Note line review response

当前 `POST /api/notes/{noteId}/reviews` 建议扩成：

```text
reviewId
noteId
rating
masteryStatus
reviewedAt
dueAt
weak
weakMarkedAt
todayAction
```

`todayAction` 取值建议：

* `DONE`
* `MOVE_TO_RECOVERY_QUEUE`
* `MOVE_TO_WEAK_ROUND`

#### 3.3 Weak-item listing API

新增聚合入口：

* `GET /api/weak-items/summary`
* `GET /api/weak-items/words?page=&pageSize=`
* `GET /api/weak-items/notes?page=&pageSize=`
* `POST /api/weak-items/words/{cardId}/dismiss`
* `POST /api/weak-items/notes/{noteId}/dismiss`

说明：

* `dismiss`
  * 用户手动移出易错项列表
  * 只是清 `weak_flag`
  * 不改 review log

#### 3.4 Dashboard enhancement

在现有 dashboard / note dashboard 聚合里增加：

* `weakWordCount`
* `weakNoteCount`

用于统一工作台和 `/weak-items` 入口卡片展示。

### 4. Frontend design

#### 4.1 Route and navigation

新增路由：

* `/weak-items`

导航增加：

* `Weak Items`

页面结构：

* 顶部摘要卡：
  * 易错词数量
  * 易错知识点数量
* 两个标签页：
  * `易错词`
  * `易错知识点`

列表字段建议：

##### 易错词

* expression
* reading
* meaning
* 所属计划 / dueDate
* 最近评分
* 标记时间
* 操作：
  * 查看详情
  * 手动移出

##### 易错知识点

* title
* tags
* masteryStatus
* 最近评分
* 标记时间
* 操作：
  * 进入复习
  * 手动移出

#### 4.2 Word review session behavior

单词页会话态新增：

* `againCountByCardId`
* `weakRoundCardIds`
* `requeuedCardIds`
* `showWeakRoundPrompt`

处理规则：

* 第一次 `Again`
  * 调 review API
  * 本地把当前 card 放到队尾
* 第二次 `Again`
  * 调 review API
  * 本地放入 `weakRoundCardIds`
  * 当前主队列不再继续重复塞入多次
* 主队列完成后
  * 如果 `weakRoundCardIds` 非空
  * 弹“再练一轮薄弱项”提示

#### 4.3 Note review session behavior

知识点页会话态新增：

* `againCountByNoteId`
* `recoveryQueueNoteIds`
* `weakRoundNoteIds`
* `showWeakRoundPrompt`

处理规则：

* 第一次 `Again`
  * 不立刻插回主队列当前位置后面
  * 放入今日待回顾子队列
* 第二次 `Again`
  * 放入 `weakRoundNoteIds`
  * 标记长期 `WEAK`
* 主队列完成后
  * 提示是否进入薄弱知识点再练一轮

### 5. Lifecycle rules

#### 5.1 Enter WEAK

MVP 规则：

* 同一日同一会话内第二次 `Again` 时进入 `WEAK`

#### 5.2 Leave WEAK

自动毕业：

* 后续正式复习出现 `Good` 或 `Easy`

手动毕业：

* 用户在 `/weak-items` 页面点击移出

不自动毕业：

* `Hard`
* `Again`

### 6. Migration plan

新增迁移建议：

* `V8__add_weak_state_to_card_instance.sql`
* `V9__add_weak_state_to_note.sql`

如果实现时想收敛，也可以合并成一个迁移版本。

### 7. Testing plan

#### Backend

* `CardReviewServiceTest`
  * `Again` 不同次数下的 `WEAK` 更新行为
  * `Good/Easy` 自动清理 `WEAK`
* `NoteReviewServiceTest`
  * `Again` 下的 `WEAK` 更新行为
  * `Good/Easy` 自动清理 `WEAK`
* 新增 weak-item query service/controller test
  * summary
  * words list
  * notes list
  * dismiss

#### Frontend

* `/cards`
  * 第一次 `Again` 回队尾
  * 第二次 `Again` 进入薄弱轮
  * 主队列结束后出现加练提示
* `/notes/review`
  * `Again` 进入待回顾子队列
  * 主队列结束后出现薄弱知识点加练提示
* `/weak-items`
  * 入口渲染
  * 标签页切换
  * 列表展示
  * 手动移出

### 8. Implementation order

建议按这个顺序开工：

1. 数据迁移和后端字段
2. 后端 review 更新逻辑与 weak-item API
3. dashboard 摘要补充
4. 前端 `/weak-items` 页面和导航入口
5. 单词线会话内回队尾
6. 知识点线待回顾子队列
7. 回归测试和文档同步

### 9. Risk notes

* 单词线当前 review 是“打一次就 DONE”，实现当天回队尾时要避免和现有 `status` 语义冲突。
* MVP 应把“今日补救”优先视为前端会话队列行为，不急着把临时队列塞进数据库。
* 知识点线接了 FSRS，不能让“今天回捞一次”篡改长期排期语义；今天补救是会话态，不是再次调用多次正式调度。
