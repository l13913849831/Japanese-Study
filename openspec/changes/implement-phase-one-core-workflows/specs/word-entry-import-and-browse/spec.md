## ADDED Requirements

### Requirement: System SHALL import word entries from CSV into an existing word set
系统 MUST 支持向现有词库上传 CSV 文件并批量导入词条，且在导入时校验必填字段、保留原始顺序并执行基础去重。

#### Scenario: Successful CSV import
- **WHEN** 用户向某个已存在的词库提交格式正确的 CSV 文件
- **THEN** 系统创建对应词条记录、按文件顺序写入 `source_order`，并返回导入结果摘要

#### Scenario: Duplicate word entry is skipped
- **WHEN** 导入文件中的词条与同一词库内已有 `expression + reading` 组合重复
- **THEN** 系统跳过重复词条并在导入结果中标记该行被忽略

### Requirement: System SHALL return line-level import errors
系统 MUST 在导入失败时返回具体行号、字段和失败原因，便于用户修正输入文件。

#### Scenario: Invalid row appears in CSV
- **WHEN** 上传的 CSV 某一行缺少必填字段或字段格式非法
- **THEN** 系统返回包含该行位置和错误原因的导入错误明细

### Requirement: System SHALL provide paginated word entry browsing for a word set
系统 MUST 支持按词库维度分页查询词条列表，并返回词条核心字段与顺序信息。

#### Scenario: User browses word entries for a word set
- **WHEN** 用户请求某个词库的词条列表并提供分页参数
- **THEN** 系统返回对应页的词条数据、分页信息和总数
