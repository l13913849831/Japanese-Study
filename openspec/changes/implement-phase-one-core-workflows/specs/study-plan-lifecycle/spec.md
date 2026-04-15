## ADDED Requirements

### Requirement: System SHALL create study plans with validated core fields
系统 MUST 支持创建学习计划，并校验词库存在性、开始日期、每日新词数、复习间隔、模板绑定和计划状态。

#### Scenario: Successful study plan creation
- **WHEN** 用户提交合法的学习计划创建请求
- **THEN** 系统保存学习计划并返回完整计划详情

#### Scenario: Invalid review offsets are rejected
- **WHEN** 用户提交未升序排列、缺少 `0` 或存在非法值的 `reviewOffsets`
- **THEN** 系统拒绝该请求并返回字段级校验错误

### Requirement: System SHALL allow updating study plans under defined rules
系统 MUST 支持更新学习计划的名称、开始日期、每日新词数、模板绑定和状态，并明确关键字段变更后的处理规则。

#### Scenario: User updates a draft or paused study plan
- **WHEN** 用户修改允许编辑的学习计划字段
- **THEN** 系统保存更新后的计划并返回最新详情

### Requirement: System SHALL expose study plan details and list views
系统 MUST 支持查询学习计划列表和单个计划详情，以便前端展示和后续操作。

#### Scenario: User views a specific study plan
- **WHEN** 用户请求某个学习计划详情
- **THEN** 系统返回该计划的完整配置和关联信息
