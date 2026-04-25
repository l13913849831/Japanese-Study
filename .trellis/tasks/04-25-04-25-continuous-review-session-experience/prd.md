# brainstorm: continuous review session experience

## Goal

把单词复习和知识点复习从“能用的管理页”收紧成“顺手的连续学习会话”，让用户从工作台进入后，默认就能开始今天该做的下一项，不再先做表格选择和手工定位。

## What I already know

* 当前统一入口已经是 `/dashboard`。
* 单词复习页是 `frontend/src/features/cards/TodayCardsPage.tsx`。
* 知识点复习页是 `frontend/src/features/notes/NoteReviewPage.tsx`。
* 两条线都已经有：
  * 今日待复习队列
  * 会话内再次回看
  * 弱项轮次
  * 复习历史
* 单词线刚完成两块底座：
  * `daily weak item recovery loop`
  * `word review scheduling to fsrs`
* `/dashboard` 已经能直接跳去 `/cards` 和 `/notes/review`，但复习页仍然保留较重的“设置区 + 表格区 + 明细区”结构。
* 单词页仍有 `Plan ID` / 日期启动动作，知识点页也仍然先走设置区，再进入会话。

## Assumptions (temporary)

* 这轮优先改交互主路径，不重做底层数据模型。
* 单词线和知识点线不强行共用同一页面，但会话骨架应尽量对齐。
* 先收敛 MVP，不把“移动端重设计”“完整动画系统”“全局搜索”一起塞进来。

## Open Questions

* 单词线进入 `/cards` 时，默认策略定为：
  * 有当前计划就直接开这个计划的今天队列
  * 没有当前计划就自动选第一个激活计划
  * 只有都没有时才提示去建计划

## Requirements (evolving)

* `/dashboard` 进入复习时，应尽量直接落到“当前题目”而不是重新找上下文。
* 单词复习页要去掉“先操作表格再评分”的感觉。
* 知识点复习页要继续保留“标题回忆 -> 展开内容 -> 评分”的节奏。
* 两条线都要统一以下概念：
  * 当前项
  * 剩余数量
  * 当前轮次
  * 弱项轮次
  * 上一条复习记录
* 如果当天没有待复习项，要直接给出空态和下一步动作。
* 会话主区域优先呈现：
  * 当前内容
  * 评分动作
  * 会话进度
  * 简短历史
* 表格和管理型信息如果保留，应降级到辅助区，不抢主路径。

## Acceptance Criteria (evolving)

* [ ] `/cards` 默认打开时能直接进入今天的连续复习会话。
* [ ] `/notes/review` 的主路径和 `/cards` 在节奏上保持一致。
* [ ] 评分后会话自动推进，不需要重新选行或切换上下文。
* [ ] 弱项回捞和弱项轮次在新会话布局下仍然成立。
* [ ] 空态、无激活计划、无待复习项这些边界场景有清晰反馈。
* [ ] 相关交互变更有前端回归测试或可重复验证步骤。

## Definition of Done (team quality bar)

* 前端构建通过
* 受影响的后端测试保持通过
* 会话主路径改动同步更新 `.trellis/spec/` 和用户文档
* 单词线与知识点线的会话语义不再明显分裂

## Out of Scope (explicit)

* 这轮不新增第三条学习线
* 这轮不做账户、同步、多端
* 这轮不重写 dashboard 聚合接口
* 这轮不把单词线和知识点线强行合成一个组件体系

## Technical Notes

* 关键页面：
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
* 共用会话工具：
  * `frontend/src/features/review/session.ts`
* 路由入口：
  * `frontend/src/app/router.tsx`
* 当前单词线已基于 `card_instance` + FSRS 动态补下一张待复习卡。
* 当前知识点线已经是 FSRS 复习队列。

## Research Notes

### What the current repo state implies

* 现在真正卡住产品体感的不是排期模型，而是复习入口和复习过程还不够“直进主任务”。
* 单词线和知识点线都已有会话队列雏形，但页面组织还是偏后台工作台。
* `/dashboard` 已经是统一入口，所以这轮可以优先收紧“入口 -> 会话”的链路，而不是再新开首页。

### Constraints from our repo/project

* 现有前端已经有工作台、弱项页、会话工具函数，适合做渐进式改造。
* 单词线会额外受激活计划、当前计划缓存影响。
* 知识点线没有计划维度，进入逻辑会更简单。

### Feasible approaches here

**Approach A: 双页面各自收紧为会话页** (Recommended)

* How it works:
  * 保留 `/cards` 与 `/notes/review` 两个入口
  * 分别重排页面，把设置和表格降级，当前题目区成为视觉主区
* Pros:
  * 改动边界清楚
  * 不动路由结构
  * 风险最低
* Cons:
  * 两页仍会有部分重复布局逻辑

**Approach B: 抽一层共享 ReviewSessionShell**

* How it works:
  * 抽公共会话外壳，单词和知识点只注入内容区和评分动作
* Pros:
  * 长期更统一
* Cons:
  * 这轮就做会拉高改造范围
  * 现在两条线的细节还没完全收敛

## Expansion Sweep

### Future evolution

* 后面如果要补移动端体验，这轮整理出的“会话主区 / 辅助区”结构可以直接复用。
* 如果后面要做更强的统一学习入口，这轮的会话语义会成为公共基线。

### Related scenarios

* `/dashboard` 的快捷启动按钮要和新的默认进入策略保持一致。
* 弱项页后续如果支持“直接开练”，也应复用同样的会话节奏。

### Failure & edge cases

* 没有激活计划时，`/cards` 不能只空白。
* 当前计划已失效或被归档时，要能自动回退到其他可用计划。
* 日期切换后会话状态要正确重建，不能带着旧队列继续走。

## Decision (ADR-lite)

**Context**:

底层复习调度已经补到能支撑真实会话，但前台主路径还没有把这部分能力释放出来。

**Decision**:

把下一步实现任务收敛为“连续复习会话体验改造”，先走 `Approach A`：

1. 收紧 `/cards`
2. 对齐 `/notes/review`
3. 最后回补 `/dashboard` 的直达行为

## Immediate Next Step

下次继续时，先做这三步：

1. 读 `frontend/src/features/cards/TodayCardsPage.tsx`
2. 画出单词复习页现在的主区、辅助区、可删区
3. 先改单词线入口与主布局，再复制节奏到知识点线
