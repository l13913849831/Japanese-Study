# 日语单词记忆系统数据库建模文档

## 1. 文档目标

本文件定义第一阶段数据库模型，作为以下内容的统一依据：

- Flyway 迁移脚本
- JPA 实体设计
- DTO 字段约束
- 接口入参与返回结构

第一阶段设计目标是支持：

- 词库管理
- 学习计划管理
- 固定间隔卡片生成
- 模板管理
- 导出记录
- 复习日志记录

## 2. 数据库选型

- 数据库：PostgreSQL
- 主键类型：`bigserial`
- 时间字段：`timestamp with time zone`
- 日期字段：`date`
- 灵活字段：`jsonb`

说明：

- `created_at` 和 `updated_at` 使用 `timestamptz`
- 计划日期、到期日期使用 `date`
- `review_offsets`、`tags` 等字段使用 `jsonb`

## 3. 实体关系总览

核心关系如下：

- 一个 `word_set` 包含多个 `word_entry`
- 一个 `study_plan` 绑定一个 `word_set`
- 一个 `study_plan` 可绑定一个 `anki_template` 和一个 `md_template`
- 一个 `study_plan` 生成多个 `card_instance`
- 一个 `card_instance` 对应多个 `review_log`
- 一个 `study_plan` 可产生多个 `export_job`

可以抽象为：

```text
word_set 1 --- n word_entry
word_set 1 --- n study_plan
study_plan 1 --- n card_instance
card_instance 1 --- n review_log
study_plan n --- 1 anki_template
study_plan n --- 1 md_template
study_plan 1 --- n export_job
```

## 4. 枚举值约定

第一阶段不单独创建 PostgreSQL enum type，统一使用 `varchar + check constraint`。

## 4.1 `study_plan.status`

可选值：

- `DRAFT`
- `ACTIVE`
- `PAUSED`
- `ARCHIVED`

## 4.2 `card_instance.card_type`

可选值：

- `NEW`
- `REVIEW`

说明：

- 第一阶段先按学习任务维度区分新词和复习卡
- 如果后续需要识别卡、回忆卡双卡模式，可再扩展为 `RECOGNITION` 和 `RECALL`

## 4.3 `card_instance.status`

可选值：

- `PENDING`
- `DONE`
- `SKIPPED`

## 4.4 `review_log.rating`

可选值：

- `AGAIN`
- `HARD`
- `GOOD`
- `EASY`

## 4.5 `export_job.export_type`

可选值：

- `ANKI_CSV`
- `ANKI_TSV`
- `MARKDOWN`

## 4.6 `export_job.status`

可选值：

- `PENDING`
- `SUCCESS`
- `FAILED`

## 5. 表设计

## 5.1 `word_set`

### 用途

词库集合。

### 字段

- `id`
- `name`
- `description`
- `created_at`
- `updated_at`

### 约束

- `name` 必填
- `name` 建议全局唯一

## 5.2 `word_entry`

### 用途

词库内的单词条目。

### 字段

- `id`
- `word_set_id`
- `expression`
- `reading`
- `meaning`
- `part_of_speech`
- `example_jp`
- `example_zh`
- `level`
- `tags`
- `source_order`
- `created_at`
- `updated_at`

### 设计说明

- `source_order` 用于维持原始导入顺序
- `tags` 存数组，如 `["N4","工作"]`
- `expression` 是单词原文
- `reading` 是假名读音

### 约束建议

- 同一词库下，`expression + reading` 唯一
- 如果 `reading` 为空，应用层做额外兜底去重

## 5.3 `study_plan`

### 用途

描述一套学习计划及其生成规则。

### 字段

- `id`
- `name`
- `word_set_id`
- `start_date`
- `daily_new_count`
- `review_offsets`
- `anki_template_id`
- `md_template_id`
- `status`
- `created_at`
- `updated_at`

### 设计说明

- `review_offsets` 存放整数数组，如 `[0,1,3,7,14,30]`
- 第一阶段一条计划只绑定一个词库
- 模板绑定为可空，允许先建计划再配模板

### 约束建议

- `daily_new_count > 0`
- `review_offsets` 必须是 JSON 数组
- `status` 必须在白名单内

## 5.4 `card_instance`

### 用途

学习计划拆解出的实际卡片任务实例。

### 字段

- `id`
- `plan_id`
- `word_entry_id`
- `card_type`
- `sequence_no`
- `stage_no`
- `due_date`
- `status`
- `created_at`
- `updated_at`

### 设计说明

- `sequence_no` 表示该词属于计划中的第几批新词
- `stage_no` 对应 `review_offsets` 的索引位置
- `due_date` 表示应学习日期
- 对同一个 `plan_id + word_entry_id + stage_no` 必须唯一

### 示例

假设：

- `daily_new_count = 20`
- 词条 `source_order = 1`
- `review_offsets = [0,1,3,7,14,30]`

则：

- 该词 `sequence_no = 1`
- 第一次卡片 `stage_no = 0`, `due_date = start_date`
- 第二次卡片 `stage_no = 1`, `due_date = start_date + 1`

## 5.5 `review_log`

### 用途

记录每次复习行为。

### 字段

- `id`
- `card_instance_id`
- `reviewed_at`
- `rating`
- `response_time_ms`
- `note`
- `created_at`

### 设计说明

- 第一阶段允许一张卡多次记录
- 后续如果要做最终完成态，可以再补汇总表

## 5.6 `anki_template`

### 用途

保存系统内部定义的 Anki 风格模板。

### 字段

- `id`
- `name`
- `description`
- `field_mapping`
- `front_template`
- `back_template`
- `css_template`
- `created_at`
- `updated_at`

### 设计说明

- `field_mapping` 为 JSON 结构
- 第一阶段模板只做字段映射和内容模板，不直接兼容 `.apkg`

## 5.7 `md_template`

### 用途

保存 Markdown 导出模板。

### 字段

- `id`
- `name`
- `description`
- `template_content`
- `created_at`
- `updated_at`

## 5.8 `export_job`

### 用途

记录导出任务和下载文件信息。

### 字段

- `id`
- `plan_id`
- `export_type`
- `target_date`
- `file_name`
- `file_path`
- `status`
- `created_at`
- `updated_at`

### 设计说明

- 第一阶段支持导出单日数据或整计划数据
- `target_date` 可为空，表示整计划导出

## 6. 索引设计

## 6.1 `word_entry`

建议索引：

- `(word_set_id)`
- `(word_set_id, source_order)`
- `(expression)`
- 唯一索引 `(word_set_id, expression, reading)`

## 6.2 `study_plan`

建议索引：

- `(word_set_id)`
- `(status)`
- `(start_date)`

## 6.3 `card_instance`

建议索引：

- `(plan_id, due_date)`
- `(plan_id, sequence_no)`
- `(word_entry_id)`
- 唯一索引 `(plan_id, word_entry_id, stage_no)`

## 6.4 `review_log`

建议索引：

- `(card_instance_id, reviewed_at)`

## 6.5 `export_job`

建议索引：

- `(plan_id, created_at)`
- `(status)`

## 7. 删除策略

第一阶段建议采用以下外键删除策略：

- 删除 `word_set` 时级联删除 `word_entry`
- 删除 `study_plan` 时级联删除 `card_instance` 和 `export_job`
- 删除 `card_instance` 时级联删除 `review_log`
- 删除模板时，不联动删除计划，计划中的模板外键置空

原因：

- 词库和卡片属于强从属关系
- 模板属于可替换配置，不应阻塞历史计划

## 8. 约束策略

第一阶段建议 SQL 层直接收紧以下约束：

- 必填字段使用 `not null`
- 合法范围使用 `check constraint`
- JSON 类型使用 `jsonb_typeof(...)`
- 唯一性由唯一索引保证

不建议第一阶段放到数据库层的规则：

- `review_offsets` 必须升序
- `review_offsets` 必须全部为整数且非负
- 导入词条时 `source_order` 连续性

这些规则建议由应用层校验，原因是数据库层实现会明显增加复杂度。

## 9. 推荐实体映射

JPA 层建议关系：

- `WordSet` -> `@OneToMany` `WordEntry`
- `StudyPlan` -> `@ManyToOne` `WordSet`
- `StudyPlan` -> `@ManyToOne(optional = true)` `AnkiTemplate`
- `StudyPlan` -> `@ManyToOne(optional = true)` `MarkdownTemplate`
- `CardInstance` -> `@ManyToOne` `StudyPlan`
- `CardInstance` -> `@ManyToOne` `WordEntry`
- `ReviewLog` -> `@ManyToOne` `CardInstance`
- `ExportJob` -> `@ManyToOne` `StudyPlan`

`review_offsets` 和 `tags` 可以先通过 `AttributeConverter` 映射为 JSON 字符串，或直接接入 Hibernate JSON 支持。

## 10. Flyway 迁移规划

建议按以下顺序：

1. `V1__init_word_set.sql`
2. `V2__init_study_plan.sql`
3. `V3__init_card_instance.sql`
4. `V4__init_templates_seed.sql`
5. `V5__init_export_job.sql`

这样的顺序能保证外键依赖正确：

- `study_plan` 依赖 `word_set`
- 模板表在 `V2` 中先于 `study_plan` 创建，以满足模板外键
- `card_instance` 依赖 `study_plan` 和 `word_entry`
- `export_job` 依赖 `study_plan`

## 11. 下一步建议

数据库层现在可以直接进入两个动作：

1. 基于本文件生成 JPA 实体
2. 以 Flyway SQL 为准开始搭建后端工程

如果继续推进实现，下一步最合适的是：

- 生成 Spring Boot 基础工程
- 把这些表结构接进实体和 repository
- 先打通词库导入和学习计划创建
