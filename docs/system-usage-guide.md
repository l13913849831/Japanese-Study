# 日语学习系统使用说明

## 1. 文档目的

这份文档只说明当前仓库已经落地的能力、运行方式和主使用路径。

当前代码状态以 `2026-04-24` 为准，已经不是最早的“词库准备工具”形态，而是一个单机版的日语学习 MVP，包含两条学习线：

- 单词学习：词库、学习计划、今日卡片、复习记录、学习工作台
- 知识点学习：知识点 CRUD、Markdown 导入预览、知识点复习、知识点仪表盘

仍然不包含：

- 多用户
- 登录鉴权
- `.apkg` 完整包导出
- 独立的后端聚合型总工作台接口

## 2. 仓库结构

- `backend/`: Spring Boot 3 + Java 21 + PostgreSQL + Flyway
- `frontend/`: React 18 + TypeScript + Vite + Ant Design
- `docs/`: 使用说明、接口文档、路线文档
- `.trellis/`: 工作流、规范、任务记录

## 3. 当前模块

后端模块：

- `wordset`: 词库、词条、导入预览与导入
- `studyplan`: 学习计划与生命周期动作
- `card`: 今日卡片、日历查询、复习提交、复习历史
- `dashboard`: 单词学习聚合视图
- `note`: 知识点、Markdown 导入预览、FSRS 复习、知识点仪表盘
- `template`: Anki / Markdown 模板列表、创建、更新、预览
- `exportjob`: 导出任务创建、列表、下载
- `shared`: 统一响应、异常、配置、通用支持

前端页面：

- `/dashboard`
- `/word-sets`
- `/study-plans`
- `/cards`
- `/notes`
- `/notes/review`
- `/notes/dashboard`
- `/templates`
- `/export-jobs`

## 4. 运行前准备

后端运行条件：

- JDK 21
- Maven 3.9+
- PostgreSQL

前端运行条件：

- Node.js 22+
- npm 10+

数据库环境变量：

- `JP_DB_URL`
- `JP_DB_USERNAME`
- `JP_DB_PASSWORD`
- `APP_EXPORT_BASE_DIR`

默认配置见 `backend/src/main/resources/application.yml`。

## 5. 启动方式

### 5.1 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

启动时会：

- 连接 PostgreSQL
- 执行 Flyway `V1` 到 `V7`
- 初始化词库、计划、卡片、模板、导出、知识点相关表
- 暴露 `/api` 接口

健康检查：

```text
GET http://localhost:8080/api/health
```

### 5.2 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 会把 `/api` 代理到后端 `http://localhost:8080`。

## 6. 当前已经落地的核心能力

### 6.1 单词学习线

- 创建词库
- 浏览词库列表
- 词条列表分页与关键词 / 等级 / 标签过滤
- 单词条创建、更新、删除
- CSV / `.apkg` 导入预览
- 导入确认后正式写入词条
- 创建、查看、更新学习计划
- 学习计划 `activate / pause / archive`
- 查询某计划某天的今日卡片
- 查询学习日历
- 为卡片提交 `AGAIN / HARD / GOOD / EASY` 评分
- 查看卡片复习历史
- 查看单词学习仪表盘

### 6.2 知识点学习线

- 手动创建、编辑、删除知识点
- Markdown 上传后先做拆分预览
- 预览阶段可编辑标题、内容、标签并删除草稿项
- 支持公共标签预填
- 导入后生成独立知识点，不保留源文件概念
- 查询当日知识点复习队列
- 按 FSRS 规则提交知识点评分
- 查看知识点复习历史
- 查看知识点仪表盘

### 6.3 模板与导出

- 查看 Anki 模板
- 查看 Markdown 模板
- 创建与更新两类模板
- 对模板做草稿预览
- 创建导出任务
- 查看导出任务列表
- 下载导出结果

### 6.4 学习入口

- `/` 会跳到 `/dashboard`
- `/dashboard` 是统一今日学习工作台
- 工作台前端组合了单词学习仪表盘和知识点仪表盘
- 工作台可直接跳到 `/cards`、`/notes`、`/notes/review`、`/notes/dashboard`

## 7. 页面与主用途

### 7.1 `/dashboard`

- 今日总览
- 单词学习摘要
- 知识点学习摘要
- 快速进入单词复习或知识点复习

### 7.2 `/word-sets`

- 管理词库
- 维护词条
- 上传 CSV / `.apkg`
- 先预览再确认导入

### 7.3 `/study-plans`

- 创建和编辑学习计划
- 选择模板
- 切换计划生命周期状态

### 7.4 `/cards`

- 先选计划和日期
- 进入连续单词复习会话
- 聚焦当前卡片、进度、队列、历史

### 7.5 `/notes`

- 管理知识点
- 上传 Markdown
- 调整导入预览草稿
- 确认导入

### 7.6 `/notes/review`

- 进入连续知识点复习会话
- 先看标题主动回忆
- 按需显示内容
- 评分后自动继续下一条

### 7.7 `/notes/dashboard`

- 查看知识点总量、到期量、掌握度分布、最近趋势、最近新增

### 7.8 `/templates`

- 创建、编辑、预览 Anki / Markdown 模板

### 7.9 `/export-jobs`

- 创建导出任务
- 查看导出状态
- 下载文件

## 8. 推荐使用顺序

### 8.1 单词学习

1. 在 `/word-sets` 创建词库并导入词条
2. 在 `/study-plans` 创建学习计划并激活
3. 在 `/dashboard` 查看今日任务
4. 进入 `/cards` 做连续复习
5. 需要导出时去 `/export-jobs`

### 8.2 知识点学习

1. 在 `/notes` 手动创建知识点，或上传 Markdown 做预览导入
2. 在 `/notes/dashboard` 查看当日积压和趋势
3. 进入 `/notes/review` 做连续复习

## 9. 当前限制

- 单机单用户使用，不含账号体系
- 工作台是前端组合现有接口，不是新的后端聚合接口
- 卡片复习仍是基础评分写日志，不做完整动态卡片重排
- 导出页创建任务时还不能显式选择模板
- 没有全局“默认模板中心”
- 长周期统计能力还比较薄

## 10. 事实来源

如果本文件和代码不一致，以当前代码与 `.trellis/spec/` 为准，再回写文档。
