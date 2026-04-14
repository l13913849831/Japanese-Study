# 日语单词记忆系统提案

## 1. 项目目标

构建一个面向日语单词记忆的 Web 系统，围绕“固定起始日期 + 间隔复习 + 每日卡片生成”这一主线，支持：

- 维护和导入日语词库
- 创建学习计划并设置起始日期
- 根据计划自动生成每日新词卡和复习卡
- 支持导出可导入 Anki 的卡片数据
- 支持自定义 Markdown 模板导出
- 记录复习结果，为后续智能调度预留基础

该系统第一阶段定位为单用户、自托管、桌面浏览器优先的学习管理工具。

## 2. 设计原则

- 先做稳定的学习计划生成，不一开始做复杂记忆算法
- 先保证“能稳定导入 Anki”，再考虑深度兼容 `.apkg`
- 模板系统独立设计，避免导出逻辑和业务逻辑耦合
- 以后可扩展到错题强化、动态间隔、多计划并行
- 后端先提供清晰的领域模型和 REST API，前端独立演进

## 3. 目标用户与使用场景

### 3.1 目标用户

- 使用日语单词书或自建词库进行长期记忆的人
- 希望结合遗忘曲线和固定复习节奏的人
- 需要导出到 Anki 或 Markdown 笔记系统的人

### 3.2 典型使用流程

1. 导入一批日语单词
2. 创建学习计划，设置起始日期、每日新词数、复习间隔
3. 系统根据日期生成当日应学习的卡片
4. 用户在系统内查看卡片、记录复习结果
5. 用户导出 Anki CSV 或 Markdown 学习文档

## 4. MVP 范围

### 4.1 必做功能

- 单词词库管理
- 词库批量导入
- 学习计划管理
- 起始日期设置
- 固定间隔复习配置
- 每日卡片生成
- 今日学习列表展示
- 复习记录打分
- Anki CSV/TSV 导出
- 自定义 Markdown 模板导出
- 模板预览

### 4.2 暂不纳入第一版

- 多用户账号与权限
- 音频抓取与发音资源管理
- 自动接入第三方词典 API
- `.apkg` 完整包生成
- 移动端 App
- 复杂自适应算法，例如完整 SM-2 或 FSRS

## 5. 技术选型

## 5.1 后端

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL

### 选择理由

- Java + Spring Boot 对管理后台、任务调度、模板渲染、导出文件这类需求成熟稳定
- JPA 足够支撑第一版领域建模和 CRUD
- Flyway 适合明确管理数据库结构演进
- PostgreSQL 对 JSONB、全文索引、统计查询支持较好

## 5.2 前端

- React 18
- TypeScript
- Vite
- Ant Design
- TanStack Query
- Zustand
- React Router

### 选择理由

- React + Vite 是当前主流组合，开发反馈快
- Ant Design 适合表单、表格、后台配置页面
- TanStack Query 适合服务端状态管理
- Zustand 足够处理页面级 UI 状态，无需引入更重的 Redux

## 5.3 模板与导出

- 模板引擎：FreeMarker
- 导出格式：
  - Anki：CSV 或 TSV
  - 文档：Markdown

### 模板策略

- Anki 模板按“字段映射 + 前后模板 + CSS”建模
- Markdown 模板按“自定义占位符文本模板”建模
- 首版不解析 `.apkg`，只支持导入系统定义的 Anki 风格模板

## 6. 核心业务设计

## 6.1 词库模型

词库是学习计划的基础数据来源。每个词条建议包含：

- 单词原文
- 假名读音
- 中文释义
- 词性
- 日文例句
- 中文例句释义
- 标签
- 难度等级

### 词条示例

```text
expression: 準備
reading: じゅんび
meaning: 准备
partOfSpeech: 名词/サ变
exampleJp: 明日の会議を準備する。
exampleZh: 为明天的会议做准备。
tags: N4,工作
```

## 6.2 学习计划模型

学习计划负责定义学习节奏和时间基线。

### 核心配置项

- 计划名称
- 起始日期
- 每日新词数量
- 固定复习间隔
- 关联词库
- 卡片模板
- 导出模板
- 状态

### 默认复习间隔

建议默认值：

```text
[0, 1, 3, 7, 14, 30]
```

解释：

- `0` 表示学习当天首次学习
- `1` 表示 1 天后复习
- `3` 表示 3 天后复习
- `7` 表示 7 天后复习
- `14` 表示 14 天后复习
- `30` 表示 30 天后复习

## 6.3 卡片生成逻辑

系统按学习计划和起始日期推算卡片：

- 按词库顺序或排序规则切分每日新词
- 每个新词生成一个或多个卡片实例
- 根据复习间隔推导每张卡片的应复习日期
- 当查询某一天的学习任务时，汇总该日所有应学习和应复习卡片

### 例子

计划参数：

- 起始日期：`2026-04-20`
- 每日新词数：`20`
- 间隔：`[0,1,3,7,14,30]`

则：

- `2026-04-20` 生成第 1 组 20 个新词
- `2026-04-21` 生成：
  - 第 1 组的 `+1 天复习`
  - 第 2 组 20 个新词
- `2026-04-23` 生成：
  - 第 1 组的 `+3 天复习`
  - 第 3 组新词

### 第一版处理原则

- 先按固定间隔生成，不因为用户打分动态调整学习日程
- 复习打分先作为记录和统计项存储
- 第二版再接入动态调度策略

## 7. 模板设计

## 7.1 Anki 模板

Anki 模板不直接等于 `.apkg` 模板，而是系统内部可配置的“字段输出定义”。

### 支持的字段

- `expression`
- `reading`
- `meaning`
- `partOfSpeech`
- `exampleJp`
- `exampleZh`
- `tags`
- `planName`
- `dueDate`

### 模板结构

- 模板名称
- 字段映射
- Front Template
- Back Template
- CSS Template

### 示例

Front:

```html
{{expression}}
```

Back:

```html
<div>{{reading}}</div>
<div>{{meaning}}</div>
<div>{{exampleJp}}</div>
<div>{{exampleZh}}</div>
```

CSS:

```css
.card {
  font-family: Arial, sans-serif;
  font-size: 20px;
}
```

### 第一版导出策略

- 将模板渲染后的字段导出为 CSV/TSV
- 保证可被 Anki 标准导入功能读取
- 不生成 Anki note type 包结构

## 7.2 Markdown 模板

Markdown 模板用于每日学习清单、复习文档或打印资料。

### 支持的上下文变量

- `date`
- `planName`
- `cards`
- `newCards`
- `reviewCards`
- 单卡字段：
  - `expression`
  - `reading`
  - `meaning`
  - `exampleJp`
  - `exampleZh`

### 示例模板

```md
# {{date}} 今日单词

## 新词
{{#newCards}}
- **{{expression}}**（{{reading}}）：{{meaning}}
  - 例句：{{exampleJp}}
{{/newCards}}

## 复习
{{#reviewCards}}
- **{{expression}}**（{{reading}}）：{{meaning}}
{{/reviewCards}}
```

### 第一版限制

- 以文本模板渲染为主
- 不支持嵌套模板继承
- 不支持模板脚本执行

## 8. 数据库设计

以下为建议表结构，正式建表时可根据命名规范调整。

## 8.1 `word_set`

```sql
id bigint primary key
name varchar(128) not null
description varchar(512)
created_at timestamp not null
updated_at timestamp not null
```

## 8.2 `word_entry`

```sql
id bigint primary key
word_set_id bigint not null
expression varchar(255) not null
reading varchar(255)
meaning text not null
part_of_speech varchar(64)
example_jp text
example_zh text
level varchar(32)
tags jsonb
source_order int
created_at timestamp not null
updated_at timestamp not null
```

说明：

- `source_order` 用于维持导入顺序，便于按词书顺序切片学习
- `tags` 建议使用 `jsonb`

## 8.3 `study_plan`

```sql
id bigint primary key
name varchar(128) not null
word_set_id bigint not null
start_date date not null
daily_new_count int not null
review_offsets jsonb not null
anki_template_id bigint
md_template_id bigint
status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

## 8.4 `card_instance`

```sql
id bigint primary key
plan_id bigint not null
word_entry_id bigint not null
card_type varchar(32) not null
sequence_no int not null
stage_no int not null
due_date date not null
status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

说明：

- `card_type` 可区分 `NEW`, `REVIEW`, `RECOGNITION`, `RECALL`
- `sequence_no` 表示它属于计划中的第几批新词
- `stage_no` 表示当前是第几个间隔阶段

## 8.5 `review_log`

```sql
id bigint primary key
card_instance_id bigint not null
reviewed_at timestamp not null
rating varchar(16) not null
response_time_ms bigint
note text
created_at timestamp not null
```

评分建议：

- `AGAIN`
- `HARD`
- `GOOD`
- `EASY`

## 8.6 `anki_template`

```sql
id bigint primary key
name varchar(128) not null
description varchar(512)
field_mapping jsonb not null
front_template text not null
back_template text not null
css_template text
created_at timestamp not null
updated_at timestamp not null
```

## 8.7 `md_template`

```sql
id bigint primary key
name varchar(128) not null
description varchar(512)
template_content text not null
created_at timestamp not null
updated_at timestamp not null
```

## 8.8 `export_job`

```sql
id bigint primary key
plan_id bigint not null
export_type varchar(32) not null
target_date date
file_name varchar(255)
file_path varchar(1024)
status varchar(32) not null
created_at timestamp not null
updated_at timestamp not null
```

## 9. API 设计草案

## 9.1 词库管理

- `POST /api/word-sets`
- `GET /api/word-sets`
- `GET /api/word-sets/{id}`
- `POST /api/word-sets/{id}/import`
- `GET /api/word-sets/{id}/words`
- `POST /api/words`
- `PUT /api/words/{id}`
- `DELETE /api/words/{id}`

## 9.2 学习计划

- `POST /api/study-plans`
- `GET /api/study-plans`
- `GET /api/study-plans/{id}`
- `PUT /api/study-plans/{id}`
- `POST /api/study-plans/{id}/activate`
- `POST /api/study-plans/{id}/pause`

## 9.3 今日卡片与复习

- `GET /api/study-plans/{id}/cards/today?date=2026-04-20`
- `GET /api/study-plans/{id}/cards/calendar?start=2026-04-20&end=2026-04-30`
- `POST /api/cards/{id}/review`
- `GET /api/cards/{id}/reviews`

### 今日卡片接口返回建议

```json
{
  "date": "2026-04-20",
  "planId": 1,
  "newCount": 20,
  "reviewCount": 35,
  "cards": [
    {
      "cardId": 101,
      "wordId": 9001,
      "cardType": "NEW",
      "expression": "準備",
      "reading": "じゅんび",
      "meaning": "准备",
      "exampleJp": "明日の会議を準備する。",
      "dueDate": "2026-04-20"
    }
  ]
}
```

## 9.4 模板管理

- `POST /api/templates/anki`
- `GET /api/templates/anki`
- `GET /api/templates/anki/{id}`
- `PUT /api/templates/anki/{id}`
- `POST /api/templates/anki/{id}/preview`
- `POST /api/templates/md`
- `GET /api/templates/md`
- `GET /api/templates/md/{id}`
- `PUT /api/templates/md/{id}`
- `POST /api/templates/md/{id}/preview`

## 9.5 导出管理

- `POST /api/exports/anki`
- `POST /api/exports/md`
- `GET /api/exports`
- `GET /api/exports/{id}/download`

## 10. 前端页面设计

## 10.1 仪表盘

用途：

- 查看今日学习概览
- 快速进入当前计划

模块：

- 今日新词数
- 今日复习数
- 最近 7 天完成情况
- 当前活跃计划

## 10.2 词库管理页

用途：

- 维护词库
- 批量导入

模块：

- 词库列表
- 新建词库弹窗
- 导入文件入口
- 单词表格
- 筛选条件：标签、等级、词性

## 10.3 学习计划页

用途：

- 创建和管理学习计划

表单字段：

- 计划名称
- 词库
- 起始日期
- 每日新词数
- 复习间隔
- Anki 模板
- Markdown 模板

页面内容：

- 计划列表
- 新建计划
- 计划详情
- 未来 30 天学习量预览

## 10.4 今日卡片页

用途：

- 展示指定日期的学习任务
- 提交复习结果

模块：

- 日期切换
- 新词卡片区
- 复习卡片区
- 单卡详情弹层
- 打分操作：Again / Hard / Good / Easy

## 10.5 模板管理页

用途：

- 管理导出模板
- 进行预览

模块：

- Anki 模板列表
- Markdown 模板列表
- 编辑器
- 示例数据预览

## 10.6 导出中心

用途：

- 发起导出
- 下载结果文件

模块：

- 导出按钮
- 导出参数
- 历史导出记录
- 下载入口

## 11. 项目目录结构建议

## 11.1 后端目录

```text
backend/
  src/main/java/com/example/jpvocab/
    common/
    config/
    wordset/
      controller/
      service/
      repository/
      domain/
      dto/
    studyplan/
      controller/
      service/
      repository/
      domain/
      dto/
    card/
      controller/
      service/
      repository/
      domain/
      dto/
    review/
      controller/
      service/
      repository/
      domain/
      dto/
    template/
      controller/
      service/
      repository/
      domain/
      dto/
    export/
      controller/
      service/
      repository/
      domain/
      dto/
  src/main/resources/
    db/migration/
    templates/
    application.yml
```

## 11.2 前端目录

```text
frontend/
  src/
    app/
    api/
    components/
    features/
      dashboard/
      wordsets/
      study-plans/
      cards/
      templates/
      exports/
    pages/
    routes/
    stores/
    types/
    utils/
    styles/
```

## 12. 核心领域对象建议

第一版建议明确以下领域对象：

- `WordSet`
- `WordEntry`
- `StudyPlan`
- `CardInstance`
- `ReviewLog`
- `AnkiTemplate`
- `MarkdownTemplate`
- `ExportJob`

后续如果要做动态调度，再引入：

- `CardSchedulePolicy`
- `ReviewSummary`
- `LearningStatistics`

## 13. 开发阶段建议

## 13.1 第一阶段：最小可运行版本

目标：

- 可以导入词库
- 可以创建计划
- 可以根据日期生成当日卡片
- 可以导出 Markdown 和 Anki CSV

交付内容：

- 后端基础工程
- PostgreSQL 表结构
- 词库和计划 CRUD
- 卡片生成逻辑
- 模板管理
- 导出功能
- 基础前端页面

## 13.2 第二阶段：学习闭环

目标：

- 在系统内完成每日学习打分
- 形成复习记录和统计

交付内容：

- 今日卡片交互页
- 复习打分接口
- 复习日志查询
- 仪表盘统计

## 13.3 第三阶段：增强模板与智能调度

目标：

- 提高模板能力
- 开始引入动态复习逻辑

交付内容：

- 模板导入增强
- 模板变量校验
- 错词加练
- 基于评分的动态间隔

## 14. 第一阶段详细开发计划

### 任务 1：工程初始化

- 创建 `backend` Spring Boot 工程
- 创建 `frontend` React + Vite 工程
- 配置 PostgreSQL 连接
- 配置 Flyway
- 配置跨域和基础异常处理

### 任务 2：词库模块

- 建表：`word_set`, `word_entry`
- 实现词库 CRUD
- 实现 CSV 导入
- 实现基础去重规则

### 任务 3：学习计划模块

- 建表：`study_plan`
- 实现创建计划
- 实现起始日期和复习间隔配置
- 实现未来学习量预览

### 任务 4：卡片生成模块

- 建表：`card_instance`
- 实现固定间隔卡片生成逻辑
- 实现按日期查询今日卡片

### 任务 5：模板与导出模块

- 建表：`anki_template`, `md_template`, `export_job`
- 实现模板 CRUD
- 实现模板预览
- 实现 CSV/TSV 导出
- 实现 Markdown 导出

### 任务 6：前端页面

- 仪表盘
- 词库管理页
- 学习计划页
- 今日卡片页基础展示
- 模板管理页
- 导出中心

## 15. 风险与技术决策点

## 15.1 Anki 模板兼容边界

风险：

- 如果用户希望“直接导入 Anki 模板文件”，需求边界容易扩大

建议：

- 第一版仅支持系统内部定义的 Anki 风格模板
- 导出 CSV/TSV，确保可被 Anki 导入
- `.apkg` 支持作为第二阶段或第三阶段评估项

## 15.2 复习算法复杂度

风险：

- 如果第一版就接入动态算法，系统复杂度会明显上升

建议：

- 第一版固定间隔
- 评分仅记录，不影响计划主链路
- 后续单独抽象调度策略接口

## 15.3 模板安全

风险：

- 如果模板系统允许任意脚本执行，会带来安全和稳定性问题

建议：

- 仅允许占位符替换和受控循环
- 禁止脚本执行

## 16. 建议的下一步

如果确认进入实现阶段，建议紧接着输出以下文档或产物：

1. `第一阶段任务拆分清单`
2. `数据库建表 SQL / Flyway 脚本`
3. `后端模块骨架`
4. `前端页面骨架`
5. `卡片生成核心算法说明`

## 17. 结论

该方案适合先快速做出一个可用的日语单词学习系统，核心优势在于：

- 围绕起始日期和固定间隔构建，贴近实际背词节奏
- Anki 和 Markdown 两条导出链路都兼顾
- 技术栈成熟，适合稳定落地
- 第一版边界清楚，后续演进空间足够

建议按“固定间隔学习计划系统”作为第一目标实现，不要在第一版引入过多兼容性和智能算法要求。
