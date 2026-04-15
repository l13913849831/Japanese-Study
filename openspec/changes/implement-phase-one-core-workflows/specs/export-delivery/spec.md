## ADDED Requirements

### Requirement: System SHALL create export jobs for supported output formats
系统 MUST 支持针对学习计划创建导出任务，覆盖 `ANKI_CSV`、`ANKI_TSV` 和 `MARKDOWN` 三种导出类型。

#### Scenario: User creates an export job
- **WHEN** 用户提交合法的导出请求并指定计划、导出类型和目标日期
- **THEN** 系统创建导出任务记录并生成对应文件

### Requirement: System SHALL allow downloading generated export files
系统 MUST 支持下载已经成功生成的导出文件，并返回与导出类型匹配的文件内容。

#### Scenario: User downloads a completed export
- **WHEN** 用户请求下载一个状态为成功的导出任务文件
- **THEN** 系统返回对应文件流和可读文件名

### Requirement: System SHALL expose export job history
系统 MUST 支持分页查询导出任务记录，以便前端查看导出历史和当前状态。

#### Scenario: User views export history
- **WHEN** 用户请求导出任务列表
- **THEN** 系统返回包含导出类型、目标日期、文件名和状态的分页记录
