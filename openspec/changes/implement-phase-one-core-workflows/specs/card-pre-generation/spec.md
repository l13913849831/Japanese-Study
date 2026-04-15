## ADDED Requirements

### Requirement: System SHALL pre-generate card instances for a study plan
系统 MUST 在学习计划创建或重建时，依据词条顺序、每日新词数和复习间隔预生成 `card_instance` 记录。

#### Scenario: Plan creation triggers card generation
- **WHEN** 用户成功创建一个学习计划
- **THEN** 系统为该计划下的词条批量生成新词卡和复习卡实例

### Requirement: System SHALL return real today cards for a plan and date
系统 MUST 支持按计划和日期查询真实的今日卡片列表，区分新词卡和复习卡，并返回前端展示所需的词条信息。

#### Scenario: User queries today cards
- **WHEN** 用户请求某个学习计划在指定日期的今日卡片
- **THEN** 系统返回该日期应学习的卡片实例及其关联词条内容

### Requirement: System SHALL provide calendar aggregates from generated card instances
系统 MUST 支持按日期区间返回计划的卡片日历聚合数据，至少包含每日新词数和复习数。

#### Scenario: User queries a calendar range
- **WHEN** 用户请求某个学习计划在一段日期区间内的卡片日历数据
- **THEN** 系统按日期返回聚合后的新词卡和复习卡统计
