## ADDED Requirements

### Requirement: System SHALL import word entries from an Anki `.apkg` package into an existing word set
系统 MUST 接受上传到既有词库导入接口的 `.apkg` 文件，读取包内 Anki note 数据，并将可识别字段映射为系统词条后写入目标词库。

#### Scenario: Successful `.apkg` import
- **WHEN** 用户向 `POST /api/word-sets/{wordSetId}/import` 上传一个包含可读取集合数据库的 `.apkg` 文件
- **THEN** 系统 SHALL 从包中提取 note 字段并导入 `word_entry`

#### Scenario: Package contains duplicate entries
- **WHEN** `.apkg` 中某条记录与目标词库现有 `expression + reading` 组合重复
- **THEN** 系统 SHALL 跳过该记录并在导入结果中累计 `skippedCount`

### Requirement: System SHALL derive import fields from Anki note models
系统 MUST 基于包内 note type 的字段定义识别 `expression`、`reading`、`meaning` 以及可选扩展字段，并对提取内容做基础文本清洗后再入库。

#### Scenario: Known field names are present
- **WHEN** note type 中存在可识别的字段名，例如 `Expression`、`Reading`、`Meaning`
- **THEN** 系统 SHALL 使用字段名映射对应词条字段并保留字段顺序生成 `source_order`

#### Scenario: Unsupported field layout appears
- **WHEN** 某条 note 无法识别出必填的 `expression` 或 `meaning`
- **THEN** 系统 SHALL 在导入结果中返回该条记录的错误信息而不终止整个文件导入

### Requirement: System SHALL keep the existing import endpoint compatible with CSV uploads
系统 MUST 保持现有词条导入接口继续支持 CSV 上传，并根据上传文件类型在 CSV 与 `.apkg` 导入流程之间切换。

#### Scenario: CSV upload is submitted after `.apkg` support is added
- **WHEN** 用户继续上传 CSV 文件到相同导入接口
- **THEN** 系统 SHALL 按既有 CSV 规则处理并返回与此前一致的导入结果结构
