# brainstorm: split export into learning action and management history

## Goal

把当前单一的 `/export-jobs` 管理页，拆成两层语义：学习模式里的“导出阶段成果/复盘材料”动作，以及管理模式里的“历史导出任务 / 模板与规则相关管理”入口。

## What I already know

* 当前导出页 `frontend/src/features/export-jobs/ExportJobPage.tsx` 是管理型页面。
* 当前页面能做：
  * 选择学习计划
  * 选择导出类型
  * 选择目标日期
  * 创建导出任务
  * 查看任务列表
  * 下载成功文件
* 当前推荐路线把导出工作流补全列为 P1。
* 用户认为导出更像：
  * 学习或复习一段时间后的阶段回顾
  * 中期或长期成果带走
  * 学习闭环收尾动作
* 用户不希望导出只作为后台管理工具存在。

## Assumptions (temporary)

* “立即导出阶段成果”和“查看历史导出任务”应分开表达。
* 学习模式里的导出入口更偏动作型。
* 管理模式里的导出入口更偏记录与配置型。

## Open Questions

* 学习模式里的导出动作，是更适合挂在 `/dashboard`，还是挂在某条学习线完成后的收尾区域？

## Requirements (evolving)

* 导出入口应体现“学习闭环的一部分”。
* 学习模式中应有一个轻量、直达的导出动作。
* 历史任务列表与复杂管理信息不应抢学习主路径。
* 模板管理继续留在管理模式，不和学习导出动作混为一体。

## Acceptance Criteria (evolving)

* [ ] 学习模式里能直接触发“导出阶段成果”。
* [ ] 历史导出任务与下载记录仍可访问，但放到更深一层。
* [ ] 页面命名和文案不再把导出仅表达成后台任务管理。
* [ ] 新结构能和未来的学习模式 / 管理模式分层对齐。

## Definition of Done (team quality bar)

* 导出入口语义清楚
* 前端构建通过
* 文档与前端契约同步

## Out of Scope (explicit)

* 这轮不直接定义新的导出文件格式
* 这轮不直接重做模板系统
* 这轮不直接做长期统计分析

## Technical Notes

* 当前导出页：
  * `frontend/src/features/export-jobs/ExportJobPage.tsx`
* 当前导出 API：
  * `frontend/src/features/export-jobs/api.ts`
* 当前学习入口：
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/notes/NoteReviewPage.tsx`
* 当前系统说明：
  * `docs/recommended-next-features.md`
  * `docs/system-usage-guide.md`

