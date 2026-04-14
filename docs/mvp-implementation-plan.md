# 日语单词记忆系统第一阶段实施文档

## 1. 文档目标

本文件用于把提案进一步压实为可执行的第一阶段开发方案，覆盖：

- 第一阶段交付边界
- 后端任务拆分
- 前端任务拆分
- 数据库落地方案
- 核心接口优先级
- 建议开发顺序

默认前提：

- 单用户系统
- 前后端分离
- 后端使用 Java 21 + Spring Boot 3
- 数据库使用 PostgreSQL
- 前端使用 React + TypeScript + Vite

## 2. 第一阶段目标

第一阶段只解决一个问题：

“用户能导入单词，创建计划，设置起始日期和复习间隔，并在指定日期生成今日卡片，同时导出 Anki CSV 和 Markdown。”

## 3. 第一阶段交付范围

## 3.1 必须完成

- 基础工程初始化
- PostgreSQL 表结构和迁移脚本
- 词库管理
- 单词导入
- 学习计划创建与查询
- 起始日期设置
- 固定间隔配置
- 今日卡片生成
- Anki CSV 导出
- Markdown 模板导出
- 模板管理和模板预览

## 3.2 可以延后

- 卡片打分交互优化
- 仪表盘统计图
- 批量编辑词条
- 高级筛选
- 错词本

## 3.3 不进入本阶段

- 多用户
- 登录鉴权
- `.apkg` 直接生成
- 动态记忆算法
- 音频资源
- 移动端适配优化

## 4. 第一阶段交付清单

第一阶段完成后，系统应具备以下可演示能力：

1. 创建词库并导入一批日语单词
2. 创建学习计划并设置起始日期
3. 查看某一天应学习的新词和复习词
4. 预览 Anki 模板和 Markdown 模板
5. 导出某个计划在某一天的卡片数据

## 5. 开发顺序建议

建议按以下顺序推进，避免前端等待后端，或模板系统反向阻塞主链路：

1. 数据库和后端领域模型
2. 词库模块
3. 学习计划模块
4. 卡片生成模块
5. 模板模块
6. 导出模块
7. 前端页面串联

## 6. 后端任务拆分

## 6.1 工程初始化

### 目标

建立可运行的 Spring Boot 项目骨架。

### 任务

- 创建 `backend` 工程
- 引入依赖：
  - spring-boot-starter-web
  - spring-boot-starter-validation
  - spring-boot-starter-data-jpa
  - postgresql
  - flyway-core
- 配置 `application.yml`
- 配置统一返回结构
- 配置统一异常处理
- 配置 CORS

### 交付

- 后端服务可启动
- 可以连接 PostgreSQL
- Flyway 能执行第一版迁移

## 6.2 词库模块

### 目标

支持创建词库、查看词库、导入词条、查询词条。

### 任务

- 建立 `word_set`、`word_entry` 实体和表
- 实现词库 CRUD
- 实现词条 CRUD
- 实现 CSV 导入
- 实现基础去重规则

### 去重规则建议

- 同一词库内，以 `expression + reading` 作为默认去重键
- 如读取不到 `reading`，则退化为 `expression`

### 接口优先级

优先实现：

- `POST /api/word-sets`
- `GET /api/word-sets`
- `GET /api/word-sets/{id}/words`
- `POST /api/word-sets/{id}/import`

### 导入文件格式建议

第一版 CSV 字段：

```text
expression,reading,meaning,partOfSpeech,exampleJp,exampleZh,level,tags
```

### 验收标准

- 能导入 1000 条以内词条
- 导入异常可返回具体行号和原因
- 可查询词库内词条列表

## 6.3 学习计划模块

### 目标

支持创建学习计划，并绑定起始日期、词库和模板。

### 任务

- 建立 `study_plan` 表和实体
- 创建计划 DTO
- 校验参数合法性
- 支持激活和暂停计划

### 关键字段校验

- `name` 非空
- `wordSetId` 必须存在
- `startDate` 不可为空
- `dailyNewCount` 必须大于 0
- `reviewOffsets` 必须为升序数组且包含 `0`

### 接口优先级

- `POST /api/study-plans`
- `GET /api/study-plans`
- `GET /api/study-plans/{id}`
- `PUT /api/study-plans/{id}`

### 验收标准

- 可创建计划
- 可修改起始日期和每日新词数
- 可绑定模板
- 可查看计划详情

## 6.4 卡片生成模块

### 目标

根据学习计划和查询日期，生成并返回当日卡片。

### 实现策略

第一阶段建议采用“预生成 + 查询”的方式。

原因：

- 查询逻辑更简单
- 便于后续记录复习进度
- 有利于导出和统计复用

### 任务

- 建立 `card_instance` 表和实体
- 编写计划初始化卡片生成逻辑
- 编写按日期查询卡片逻辑
- 区分新词卡和复习卡

### 推荐生成时机

- 创建学习计划后，立即按当前词库和复习间隔批量生成卡片实例

### 生成规则

以词条导入顺序作为默认学习顺序：

- 第 1 天取前 `dailyNewCount` 个词条
- 第 2 天取下一批
- 每个词条根据 `reviewOffsets` 生成多个 `dueDate`

### 接口优先级

- `GET /api/study-plans/{id}/cards/today?date=yyyy-MM-dd`
- `GET /api/study-plans/{id}/cards/calendar?start=...&end=...`

### 验收标准

- 指定日期能返回当日学习卡片
- 新词和复习卡统计正确
- 对 3000 词以内词库生成可在合理时间完成

## 6.5 模板模块

### 目标

支持配置 Anki 模板和 Markdown 模板，并支持预览。

### 任务

- 建立 `anki_template`、`md_template` 表和实体
- 实现模板 CRUD
- 实现模板变量替换
- 实现模板预览接口

### 模板变量白名单

建议第一阶段只开放以下变量：

- `expression`
- `reading`
- `meaning`
- `partOfSpeech`
- `exampleJp`
- `exampleZh`
- `tags`
- `date`
- `planName`

### 接口优先级

- `POST /api/templates/anki`
- `GET /api/templates/anki`
- `POST /api/templates/anki/{id}/preview`
- `POST /api/templates/md`
- `GET /api/templates/md`
- `POST /api/templates/md/{id}/preview`

### 验收标准

- 可保存模板
- 可对示例卡片进行渲染预览
- 模板变量缺失时给出可读错误

## 6.6 导出模块

### 目标

支持导出指定计划、指定日期的卡片文件。

### 任务

- 建立 `export_job` 表和实体
- 实现 CSV/TSV 导出
- 实现 Markdown 导出
- 提供文件下载接口

### 导出范围建议

第一阶段支持两种模式：

- 导出某一天的卡片
- 导出某个计划的全部卡片

### 接口优先级

- `POST /api/exports/anki`
- `POST /api/exports/md`
- `GET /api/exports`
- `GET /api/exports/{id}/download`

### 验收标准

- 导出文件可下载
- Anki CSV 能被 Anki 正常导入
- Markdown 输出符合模板结构

## 7. 前端任务拆分

## 7.1 前端工程初始化

### 任务

- 创建 `frontend` 工程
- 配置 React Router
- 配置 Ant Design
- 配置请求层
- 配置全局错误提示

### 页面骨架

- `/`
- `/word-sets`
- `/word-sets/:id`
- `/study-plans`
- `/study-plans/:id`
- `/templates`
- `/exports`

## 7.2 词库管理页

### 功能

- 词库列表
- 新建词库
- 单词列表
- 导入 CSV

### 组件

- `WordSetTable`
- `WordEntryTable`
- `ImportModal`
- `WordFilterForm`

## 7.3 学习计划页

### 功能

- 计划列表
- 新建计划
- 查看计划详情
- 预览未来学习量

### 组件

- `StudyPlanTable`
- `StudyPlanForm`
- `SchedulePreview`

## 7.4 今日卡片页

### 功能

- 指定日期查看卡片
- 区分新词和复习词
- 展示词条详情

### 组件

- `DailyCardList`
- `CardDetailDrawer`
- `DateSwitcher`

### 第一阶段限制

- 不需要复杂翻卡动画
- 先以列表和详情弹层实现

## 7.5 模板管理页

### 功能

- 模板列表
- 编辑模板
- 预览模板效果

### 组件

- `AnkiTemplateEditor`
- `MarkdownTemplateEditor`
- `TemplatePreviewPanel`

## 7.6 导出中心

### 功能

- 选择导出类型
- 选择计划和日期
- 触发导出
- 下载历史文件

### 组件

- `ExportForm`
- `ExportHistoryTable`

## 8. 数据库落地建议

## 8.1 主键策略

建议第一阶段使用：

- PostgreSQL `bigint`
- 配合 identity 或 sequence

不建议第一阶段引入 UUID 作为主键，原因是：

- 管理和调试 SQL 时不够直观
- 当前没有分布式主键需求

## 8.2 索引建议

建议至少建立以下索引：

### `word_entry`

- `(word_set_id)`
- `(word_set_id, source_order)`
- `(expression)`

### `study_plan`

- `(word_set_id)`
- `(status)`

### `card_instance`

- `(plan_id, due_date)`
- `(plan_id, sequence_no)`
- `(word_entry_id)`

### `review_log`

- `(card_instance_id, reviewed_at)`

## 8.3 Flyway 迁移规划

建议第一阶段至少准备以下脚本：

- `V1__init_word_set.sql`
- `V2__init_study_plan.sql`
- `V3__init_card_instance.sql`
- `V4__init_templates_seed.sql`
- `V5__init_export_job.sql`

## 9. 核心接口优先级清单

为了尽快打通主链路，建议接口开发按以下顺序推进：

1. `POST /api/word-sets`
2. `POST /api/word-sets/{id}/import`
3. `POST /api/study-plans`
4. `GET /api/study-plans/{id}`
5. `GET /api/study-plans/{id}/cards/today`
6. `POST /api/templates/anki`
7. `POST /api/templates/md`
8. `POST /api/exports/anki`
9. `POST /api/exports/md`

## 10. 第一阶段接口入参与返回建议

## 10.1 创建学习计划

请求：

```json
{
  "name": "N4 核心词汇",
  "wordSetId": 1,
  "startDate": "2026-04-20",
  "dailyNewCount": 20,
  "reviewOffsets": [0, 1, 3, 7, 14, 30],
  "ankiTemplateId": 1,
  "mdTemplateId": 2
}
```

返回：

```json
{
  "id": 1,
  "name": "N4 核心词汇",
  "status": "ACTIVE"
}
```

## 10.2 获取今日卡片

返回：

```json
{
  "date": "2026-04-21",
  "planId": 1,
  "newCount": 20,
  "reviewCount": 20,
  "cards": [
    {
      "cardId": 1001,
      "cardType": "NEW",
      "expression": "安心",
      "reading": "あんしん",
      "meaning": "放心，安心",
      "exampleJp": "それを聞いて安心した。",
      "dueDate": "2026-04-21"
    }
  ]
}
```

## 10.3 导出 Markdown

请求：

```json
{
  "planId": 1,
  "targetDate": "2026-04-21",
  "templateId": 2
}
```

返回：

```json
{
  "exportJobId": 10,
  "status": "SUCCESS",
  "downloadUrl": "/api/exports/10/download"
}
```

## 11. 第一阶段测试建议

## 11.1 后端测试

至少覆盖：

- 词库导入成功
- 词库导入重复词处理
- 学习计划创建校验
- 卡片生成数量校验
- 指定日期卡片查询
- Markdown 模板渲染
- CSV 导出内容结构

## 11.2 前端测试

第一阶段可以先轻量处理：

- 核心表单交互测试
- 页面渲染 smoke test
- 模板预览流程测试

## 11.3 手工验收脚本

建议准备一份手工验收清单：

1. 导入一个 50 词的小词库
2. 建立起始日期为今天的计划
3. 查看今天卡片
4. 查看三天后的卡片
5. 导出 CSV
6. 导出 Markdown
7. 校验 Anki 导入结果

## 12. 工期建议

按单人开发估算，第一阶段可以拆为：

- 工程初始化：0.5 天
- 数据库与后端基础模块：2 到 3 天
- 卡片生成与导出：2 天
- 前端基础页面：2 到 3 天
- 联调与修正：1 到 2 天

总计约：

- `7 到 10 个工作日`

如果并行推进前后端，可以进一步压缩。

## 13. 建议的下一步产物

本文件之后，最值得继续落地的两个文件是：

1. `数据库建模文档`
2. `第一版接口定义文档`

如果进入实现阶段，也可以直接继续生成：

- Spring Boot 工程骨架
- React 前端工程骨架
- 第一批 Flyway SQL

## 14. 结论

第一阶段不应追求“做一个完整 Anki 替代品”，而应聚焦“把固定起始日期的日语单词学习计划跑通”。

只要完成以下三条，这一版就是成功的：

- 学习计划能生成
- 每日卡片能查看
- 导出文件能使用

在这个基础上，再继续补复习评分、智能调度和更复杂的模板兼容，风险会更低，演进也更清晰。
