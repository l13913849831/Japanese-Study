# P3: link words notes and knowledge points

## Goal

建立单词、例句、语法点和知识卡之间的关联，让用户能从复习中沉淀知识点，也能从知识点回看相关词条和例句。

## What I already know

* 当前系统已有单词学习线和知识点学习线。
* 两条线已有独立复习、看板和长期指标。
* 母任务把“单词和知识点联动”列为 P3 候选，优先级低于复习体验 P1/P2。
* 本任务不应影响现有复习主路径的稳定性。

## Requirements

* 支持从单词、例句或复习过程创建 / 关联知识点。
* 支持在知识点详情或复习视图中查看关联词条和例句。
* 支持在单词相关页面查看已关联知识点。
* 关联关系需要区分来源，至少能表达“从复习沉淀”和“手动关联”。
* 关联能力不改变单词和知识点各自的 FSRS 调度规则。
* Web 和小程序都应提供可见入口；如某端因交互限制延后，必须在验收中说明。

## Acceptance Criteria

* [ ] 用户能把一个词条或例句关联到一个知识点。
* [ ] 用户能从复习流程中沉淀新的知识点或关联到已有知识点。
* [ ] 知识点侧能看到关联词条和例句。
* [ ] 单词侧能看到相关知识点。
* [ ] Web 端覆盖单词侧和知识点侧入口。
* [ ] 小程序端覆盖单词复习和知识点复习入口。
* [ ] 删除或取消关联不会删除原始词条、例句或知识点。
* [ ] 文档说明两条复习线仍保持独立调度。

## Definition of Done

* 数据模型、接口和前端入口有清晰边界。
* 后端测试覆盖创建关联、查询关联、取消关联。
* 前端 build 通过。
* 小程序 typecheck 和 build:weapp 通过。
* 行为变化同步更新 `.trellis/spec/` 和 `docs/system-usage-guide.md`。

## Out of Scope

* 不合并单词和知识点的复习调度。
* 不做自动语义匹配或机器学习推荐关联。
* 不做课程体系或全文知识图谱。
* 不影响 P1/P2 复习体验排期。

## Technical Notes

* Word review anchors:
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `miniapp/src/pages/cards/index.tsx`
  * `backend/src/main/java/com/jp/vocab/card/service/CardReviewService.java`
* Note anchors:
  * `frontend/src/features/notes/NoteReviewPage.tsx`
  * `miniapp/src/pages/notes-review/index.tsx`
  * `backend/src/main/java/com/jp/vocab/note/service/NoteReviewService.java`
* Existing docs:
  * `docs/system-usage-guide.md`
  * `.trellis/spec/backend/current-feature-contracts.md`
  * `.trellis/spec/frontend/current-feature-workflows.md`

## Decision

P3 阶段先做显式手动关联和复习中沉淀，并同步评估 Web 与小程序入口。不引入自动知识图谱。
