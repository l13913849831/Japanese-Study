# Brainstorm: Note Taking And Markdown Review

## Goal

在现有“背单词 + 学习计划 + 卡片复习”系统上增加“笔记/知识点”能力，既支持直接写笔记，也支持导入 Markdown 文档；导入时按大标题拆分为可复习的知识点，并让这些知识点进入后续复习链路。

## What I already know

* 用户希望新增笔记功能，不再只支持单词记忆。
* 用户希望支持两种入口：
  * 直接创建笔记
  * 导入 Markdown
* 用户希望 Markdown 按大标题拆分成知识点。
* 用户希望知识点能做后续复习。
* 用户明确要求笔记线和单词线是两套独立路线。
* 用户明确要求直接创建时，每次创建一个知识点。
* 用户确认 MVP 以知识点优先，不保留独立“原文档”实体作为主模型。
* 用户确认笔记复习也要做成正式记忆系统，而不是仅记录一次复习。
* 用户确认笔记复习使用独立评分集合，不沿用单词系统的评分文案。
* 用户希望 Markdown 拆分层级在导入时作为可选参数，而不是写死。
* 用户确认 Markdown 导入默认按一级标题 `#` 拆分。
* 用户确认 Markdown 导入的拆分层级参数先固定为三档，而不是任意层级。
* 用户确认笔记线在 MVP 里也要有自己的 dashboard/统计首页。
* 用户确认笔记复习评分先做成四档。
* 用户确认四档评分文案为：`不会 / 吃力 / 还行 / 熟悉`。
* 用户确认笔记 dashboard 采用标准版，而不是精简版。
* 用户确认排期算法采用折中方案：MVP 先用阶段制，数据模型预留后续切 FSRS。
* 用户确认直接创建知识点的字段先收敛为：`标题 + 内容 + 标签`。
* 用户确认 Markdown 导入后的知识点也只落 `标题 + 内容 + 标签`，不自动补复杂标签。
* 用户确认 Markdown 导入采用“先预览再导入”流程。
* 用户确认 dashboard 的掌握度分布按学习状态分组：`未开始 / 学习中 / 巩固中 / 已掌握`。
* 当前系统已有完整的学习链路：
  * `study_plan` 管计划
  * `card_instance` 管预生成复习卡
  * `review_log` 管复习记录
* 当前系统首页、今日卡片、复习记录和 dashboard 都围绕 `study_plan -> card_instance -> review_log` 展开。
* 当前前端路由只有：
  * `/dashboard`
  * `/word-sets`
  * `/study-plans`
  * `/cards`
  * `/templates`
  * `/export-jobs`
* 当前没有“笔记”“知识点”“文档导入”相关模块或页面。

## Assumptions (temporary)

* “按大标题拆分” 暂时指按 Markdown 一级标题 `# ` 拆分，而不是同时支持 `##` / `###` 多级切分。
* 笔记线和单词线是两条独立主线，不强行复用现有 `study_plan / card_instance / review_log`。
* Markdown 导入的目标是提取可复习的知识点单元，MVP 不保留独立文档容器。
* MVP 先不做富文本编辑器，直接用纯文本 / Markdown 输入即可。
* “直接创建知识点” 会成为笔记系统的主输入方式之一，不依赖先建文档容器。

## Requirements (evolving)

* 新增独立于单词系统的笔记/知识点域模型，支持新建、查询、编辑。
* 直接创建入口按“单个知识点”建模。
* 直接创建知识点先只包含 `标题 + 内容 + 标签` 三类字段。
* 支持导入 Markdown 文档。
* Markdown 导入流程采用“上传 -> 预览 -> 确认导入”。
* 导入时把 Markdown 拆成知识点，导入结果最终仍以知识点为主，而不是文档为主。
* Markdown 导入需要支持可配置的拆分层级参数。
* Markdown 导入默认按一级标题 `#` 拆分。
* Markdown 导入的拆分层级参数先固定为三档：
  * 只按 `#`
  * 按 `# + ##`
  * 按全部标题
* Markdown 导入后的知识点字段先只映射到 `标题 + 内容 + 标签`。
* 知识点需要能进入独立于单词系统的正式记忆复习流程。
* 复习流程需要有独立四档评分/阶段流转，而不是单纯打勾完成。
* 评分文案先使用四档：
  * `不会`
  * `吃力`
  * `还行`
  * `熟悉`
* 复习排期算法在 MVP 中先使用固定阶段/间隔规则，但数据模型要预留未来切换到 FSRS 的空间。
* 需要有前端页面承载笔记录入、导入、查看和复习入口。
* 笔记系统的列表、详情、复习进度和复习记录与单词系统分开。
* 笔记系统在 MVP 中需要独立 dashboard，展示笔记复习相关统计。
* 笔记 dashboard 先包含：
  * 今日待复习数
  * 总知识点数
  * 已复习数
  * 掌握度分布（按 `未开始 / 学习中 / 巩固中 / 已掌握` 分组）
  * 最近 7 天复习趋势
  * 最近导入/新增知识点
  * 开始复习入口

## Acceptance Criteria (evolving)

* [ ] 用户可以直接创建至少一条知识点。
* [ ] 直接创建知识点时只要求填写标题、内容和标签。
* [ ] 用户可以上传一个 Markdown 文件并看到拆分结果。
* [ ] Markdown 导入在真正入库前可先预览拆分结果。
* [ ] Markdown 导入后的知识点字段与手动创建字段保持一致。
* [ ] Markdown 中的大标题会被拆成独立知识点。
* [ ] Markdown 导入时可以选择拆分层级参数。
* [ ] 拆出的知识点可进入独立笔记复习流程。
* [ ] 笔记复习支持独立四档评分与后续排期。
* [ ] 前端存在明确的笔记入口，不依赖手工调用接口。
* [ ] 前端存在独立笔记 dashboard，可查看笔记复习概览。
* [ ] 笔记 dashboard 展示标准版统计块，并可跳转到待复习页。

## Definition of Done (team quality bar)

* Tests added/updated (unit/integration where appropriate)
* Lint / typecheck / CI green
* Docs/notes updated if behavior changes
* Rollout/rollback considered if risky

## Out of Scope (explicit)

* 富文本协同编辑
* 全文搜索
* 图像 OCR 导入
* AI 自动摘要或自动生成题目
* 权限、多用户共享、评论协作
* 独立文档树、文档详情页、文档级管理工作流
* 先不要求和单词系统共享复习算法或共用表结构

## Technical Notes

* 现有后端复习链路：
  * `backend/src/main/java/com/jp/vocab/studyplan/service/StudyPlanService.java`
  * `backend/src/main/java/com/jp/vocab/card/service/CardGenerationService.java`
  * `backend/src/main/java/com/jp/vocab/card/service/CardReviewService.java`
* 现有前端复习入口：
  * `frontend/src/features/cards/TodayCardsPage.tsx`
  * `frontend/src/features/dashboard/StudyDashboardPage.tsx`
* 现有系统没有 notes / knowledge / markdown-import 模块。
* 用户已明确偏好：笔记系统与单词系统分线，不在 MVP 中强行并轨。
* 这意味着后端需要新增独立的 notes / knowledge review 模型；前端需要新增独立 route、页面和复习入口，而不是只扩展现有 `cards` 页。
* 当前又确认了 MVP 以知识点为主模型，所以导入链路更像“Markdown -> 多个知识点”，而不是“文档 -> 文档下的知识点树”。
* 当前又确认了复习要做成正式记忆系统，因此除了知识点主表，还需要独立的复习排期/复习记录模型。

## Research Notes

### What similar tools do

* SuperMemo 的 SM-2 是最经典的间隔重复算法之一，核心是评分后更新间隔与难度因子。
* Anki 当前文档明确把 FSRS 作为 SM-2 的替代调度器，强调它会更准确地估计遗忘概率。
* Open Spaced Repetition 提供了 Java 版 `java-fsrs`，可直接在 Java 系统中接入 FSRS 调度，但其 README 也明确说明 Java 版当前不支持参数优化。

### Constraints from our repo/project

* 后端是 Java 21 + Spring Boot 3.3。
* 当前单词系统的复习实现非常简单，只是预生成卡片并在复习后标记 `DONE`，没有真正的间隔重复算法可复用。
* 用户要求笔记线与单词线独立，因此可以单独建模，不需要兼容当前 `card_instance` 表。
* MVP 需要独立 dashboard，这要求我们至少能稳定给出 `due / reviewed / mastery` 这类统计。

### Feasible approaches here

**Approach A: 简化版自研间隔重复**（Recommended）

* How it works:
  * 我们自定义四档评分和固定阶段/间隔规则。
  * 每次复习后更新知识点的当前阶段、下次复习时间和掌握度分类。
* Pros:
  * 实现可控，和你的评分文案天然一致。
  * 更容易解释和调试。
  * 适合 MVP 快速落地。
* Cons:
  * 不如 FSRS 智能。
  * 后续如果想升级到更先进算法，需要做一次迁移。

**Approach B: 直接接入 FSRS**

* How it works:
  * 后端引入 `java-fsrs`，把四档评分映射到 Again/Hard/Good/Easy。
  * 存储 FSRS 卡片状态和 review log。
* Pros:
  * 算法成熟，后续可扩展。
  * 正式记忆系统味道最足。
* Cons:
  * 你的笔记评分文案要额外映射。
  * Java 版当前不支持参数优化，MVP 只能先吃默认参数。
  * 数据模型和调试复杂度更高。

**Approach C: 先做阶段制，预留后续切 FSRS**

* How it works:
  * MVP 用固定阶段/间隔规则。
  * 数据模型提前保留可迁移字段，方便后续切到 FSRS。
* Pros:
  * 首版稳。
  * 后续升级成本比 Approach A 低。
* Cons:
  * 比纯简化版多一点设计成本。
  * 首版仍不是最先进算法。

## Decision (ADR-lite)

**Context**: 笔记系统需要正式记忆能力，但当前项目没有现成的间隔重复算法实现；同时用户希望 MVP 先稳定落地，再考虑后续算法升级。

**Decision**: MVP 先使用固定阶段/间隔规则，评分采用独立四档；数据模型预留后续切换到 FSRS 的扩展空间。

**Consequences**:

* 首版实现与调试成本可控。
* 评分文案可以完全按笔记语义设计。
* 后续若切 FSRS，需要做一次调度状态迁移，但当前建模会尽量降低迁移成本。
