# brainstorm: migrate word review scheduling to fsrs

## Goal

把单词线从当前“固定 `reviewOffsets` + 预生成 `card_instance`”的静态调度模型，升级为和知识点线一致方向的动态 FSRS 调度模型，让单词复习评分真正影响后续复习时间。

## What I already know

* 当前单词线不是 FSRS。
* 当前单词线核心逻辑：
  * `study_plan.dailyNewCount`
  * `study_plan.reviewOffsets`
  * 创建/更新计划时批量预生成全部 `card_instance`
  * 复习提交时只写 `review_log` 并把当前卡标成 `DONE`
* 当前知识点线已经有：
  * `fsrsCardJson`
  * `NoteFsrsScheduler`
  * 动态 `dueAt`
* 当前单词线如果切 FSRS，会牵动：
  * `study_plan`
  * `card_instance`
  * `review_log`
  * `CardGenerationService`
  * `CardReviewService`
  * dashboard 查询
  * 文档和接口契约

## Requirements

* 需要明确单词线是否继续保留 `card_instance`。
* 需要明确 `reviewOffsets` 在 FSRS 时代的去留：
  * 废弃
  * 兼容保留
  * 改成仅控制新词引入节奏
* 需要明确单词线如何落 `fsrsCardJson` 或等价状态。
* 需要明确旧数据迁移策略。
* 需要把这次改造和当前“学习闭环 / WEAK”任务拆开。

## Acceptance Criteria

* [ ] 能说明当前单词线和目标 FSRS 模型的核心差异。
* [ ] 能给出单词线切 FSRS 的推荐落地方案。
* [ ] 能明确数据迁移和兼容策略。
* [ ] 能作为独立子任务推进，不与当前学习闭环任务混在一起。

## Out of Scope

* 这轮不直接实现 FSRS 迁移
* 这轮不重构知识点线
* 这轮不做单词线和知识点线的完全统一建模

## Technical Approach

### Option A: 保留 `study_plan`，弱化 `reviewOffsets`

* `study_plan` 继续负责：
  * 词库绑定
  * 新词引入节奏
  * 生命周期状态
* `reviewOffsets` 不再驱动长期复习排期
* 单词长期排期交给 FSRS

### Option B: 继续保留 `card_instance` 作为运行时表

* 每个词不再预生成整套固定阶段卡
* 改成每个词维护一个运行时复习状态
* `card_instance` 更像“单词复习卡状态表”

### Option C: 新建独立单词复习状态表

* 不再复用 `card_instance`
* 为单词线单独引入：
  * `word_review_state`
  * `word_review_log`
* 代价更大，但语义最干净

## Recommendation

推荐先走 **Option A + Option B**：

* 保留 `study_plan`
* 保留 `card_instance`，但改变它的语义
* `dailyNewCount` 继续负责“新词引入”
* `reviewOffsets` 逐步降级为兼容字段，最终考虑废弃
* `card_instance` 增加或替换为：
  * `fsrsCardJson`
  * `dueAt` / `dueDate`
  * `reviewCount`
  * `lastReviewedAt`

这样改造成本低于重做一整套表，但仍能完成从静态计划到动态调度的切换。

## Migration Notes

* 需要评估现有 `card_instance` 的历史数据是否保留。
* 需要决定旧计划上的 `reviewOffsets` 如何迁移：
  * 新计划禁用
  * 旧计划兼容只读
  * 自动映射成初始学习阶段
* 需要补文档：
  * `docs/api-specification.md`
  * `docs/system-usage-guide.md`
  * `.trellis/spec/backend/current-feature-contracts.md`

## Decision (ADR-lite)

**Context**:

当前单词线的记忆逻辑明显弱于知识点线，也不符合更成熟的间隔重复研究。用户明确希望后续升级到 FSRS，但当前正在推进“WEAK / 易错项 / 今日补救”，两类改动不宜混在一轮里。

**Decision**:

把“单词线升级到 FSRS”单独拆成总任务下的独立子任务，先完成当前学习闭环增强，再单独推进单词调度模型切换。

**Consequences**:

* 当前学习闭环任务范围更稳
* 后续切 FSRS 时不会和今日补救逻辑互相污染
* 未来仍可让 `WEAK` 机制复用在 FSRS 化后的单词线
