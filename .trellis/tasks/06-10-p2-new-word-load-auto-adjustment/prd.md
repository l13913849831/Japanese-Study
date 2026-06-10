# P2: new word load auto adjustment

## Goal

根据未来复习负载和当前积压，给出 `dailyNewCount` 调整建议。用户在复习压力过高时能看到明确建议，必要时暂停或降低新词引入节奏。

## What I already know

* `study_plan.dailyNewCount` 仍控制单词线新词引入节奏。
* 长周期学习指标 MVP 已返回未来 7 / 14 / 30 天复习负载预测。
* 母任务把“新词负载自动调节”列为 P2 候选。
* 本任务应给出建议，不直接引入新的学习算法或机器学习推荐。

## Requirements

* 基于未来负载、当前积压和近期完成量，计算新词负载风险。
* 在 Web `/dashboard` 和小程序工作台展示 `dailyNewCount` 调整建议。
* 建议至少覆盖保持、降低、暂停三类动作。
* 建议需要解释原因，例如未来 7 天过载、今日积压未清、近期完成量不足。
* 第一版只给建议，不自动修改学习计划。
* 无激活学习计划或无负载数据时，显示中性提示。

## Acceptance Criteria

* [ ] 未来负载正常时，页面建议保持当前新词节奏。
* [ ] 未来 7 / 14 / 30 天负载偏高时，页面建议降低或暂停新词。
* [ ] 当前有明显积压时，建议优先完成复习而不是引入新词。
* [ ] 建议文案包含可解释原因。
* [ ] 不自动修改 `dailyNewCount`。
* [ ] Web 和小程序工作台展示一致的建议结果。
* [ ] 后端 / 前端对应测试或手工回归覆盖主要分支。

## Definition of Done

* 推荐阈值和解释文案有明确规格记录。
* Web 前端 build 通过。
* 小程序 typecheck 和 build:weapp 通过。
* 需要后端变更时，使用 `D:\apache-maven-3.9.12\bin\mvn.cmd test` 或更小范围测试。
* 行为变化同步更新 `.trellis/spec/` 和 `docs/system-usage-guide.md`。

## Out of Scope

* 不自动修改学习计划参数。
* 不做提醒推送。
* 不做机器学习预测。
* 不做管理员全局负载策略。

## Technical Notes

* Dashboard frontend:
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
  * `frontend/src/features/dashboard/api.ts`
  * `miniapp/src/pages/dashboard/index.tsx`
  * `miniapp/src/features/dashboard/api.ts`
* Long-term metrics:
  * `backend/src/main/java/com/jp/vocab/dashboard/service/LongTermDashboardService.java`
  * `backend/src/main/java/com/jp/vocab/dashboard/dto/LongTermDashboardResponse.java`
* Study plan contract:
  * `.trellis/spec/backend/current-feature-contracts.md`
  * `docs/api-specification.md`
* Related completed task:
  * `.trellis/tasks/06-10-long-term-learning-metrics/prd.md`

## Decision

MVP 先做“建议型自动调节”，并同步展示到 Web 和小程序工作台。它利用现有长期负载预测，不直接改学习计划，降低误伤用户学习节奏的风险。
