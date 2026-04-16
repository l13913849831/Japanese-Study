# 日语单词记忆系统使用说明

## 1. 文档目的

这份文档用于说明当前仓库这套系统怎么启动、怎么访问，以及当前版本已经能做什么。

当前代码状态是：

- 已完成第一阶段前后端工程骨架
- 已完成 PostgreSQL + Flyway 的配置接入
- 已完成词条导入、学习计划创建/编辑、卡片预生成、模板预览、导出创建/下载主链路
- 当前仍属于第一阶段 MVP，不包含多用户、动态记忆算法和 `.apkg` 导出生成

## 2. 仓库结构

### 2.1 目录说明

- `backend/`: Spring Boot 3 + Java 21 后端
- `frontend/`: React 18 + TypeScript + Vite 前端
- `docs/`: 产品方案、数据库建模、接口文档和本说明
- `openspec/`: 当前变更方案与任务记录

### 2.2 当前模块

后端模块：

- `wordset`: 词库与词条
- `studyplan`: 学习计划
- `card`: 今日卡片与日历查询
- `template`: Anki / Markdown 模板
- `exportjob`: 导出任务
- `shared`: 统一响应、异常处理、配置、命名映射

前端模块：

- `/word-sets`
- `/study-plans`
- `/cards`
- `/templates`
- `/export-jobs`

## 3. 运行前准备

### 3.1 后端运行条件

- JDK 21
- Maven 3.9+
- PostgreSQL

### 3.2 前端运行条件

- Node.js 22+
- npm 10+

### 3.3 数据库要求

后端默认连接 PostgreSQL，配置项如下：

- `JP_DB_URL`
- `JP_DB_USERNAME`
- `JP_DB_PASSWORD`
- `APP_EXPORT_BASE_DIR`

代码默认值见 [application.yml](/D:/project/jp/backend/src/main/resources/application.yml)：

- URL: `jdbc:postgresql://localhost:5432/jp_vocab`
- 用户名: `jp`
- 密码: `jp`
- 导出目录: `exports`

如果你本地已经有 PostgreSQL 服务，直接改成本机实际连接信息即可，不需要走 Docker。

建议先自行创建数据库，例如：

```sql
create database jp_vocab;
```

## 4. 后端怎么启动

在仓库根目录打开终端后执行：

```bash
cd backend
mvn spring-boot:run
```

如果你本地数据库不是默认配置，可以先设置环境变量再启动。

PowerShell 示例：

```powershell
$env:JP_DB_URL="jdbc:postgresql://localhost:5432/jp_vocab"
$env:JP_DB_USERNAME="postgres"
$env:JP_DB_PASSWORD="你的密码"
$env:APP_CORS_ALLOWED_ORIGINS="http://localhost:5173"
$env:APP_EXPORT_BASE_DIR="exports"
mvn spring-boot:run
```

后端启动后默认地址：

```text
http://localhost:8080
```

### 4.1 后端启动时会做什么

- 连接 PostgreSQL
- 执行 Flyway `V1` 到 `V6`
- 自动建表 / 建索引 / 插入默认模板种子数据
- 暴露 `/api` 接口

### 4.2 后端健康检查

可访问：

```text
GET http://localhost:8080/api/health
```

正常会返回：

```json
{
  "success": true,
  "data": {
    "status": "UP"
  },
  "error": null,
  "timestamp": "..."
}
```

## 5. 前端怎么启动

在仓库根目录打开终端后执行：

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

Vite 已配置代理，会把前端发往 `/api` 的请求转发到：

```text
http://localhost:8080
```

对应配置见 [vite.config.ts](/D:/project/jp/frontend/vite.config.ts)。

## 6. 系统当前怎么使用

### 6.1 只看前端页面

如果你只是想先看页面结构：

```bash
cd frontend
npm install
npm run dev
```

然后打开：

```text
http://localhost:5173
```

这时你可以看到：

- 左侧导航
- 词库页
- 学习计划页
- 今日卡片页
- 模板页
- 导出任务页

注意：

- 如果后端没启动，页面里的接口请求会失败
- 这属于当前正常现象，不影响先看壳层和路由

### 6.2 前后端一起联调

先启动 PostgreSQL，再启动后端，然后启动前端：

1. 启动 PostgreSQL
2. 启动后端 `mvn spring-boot:run`
3. 启动前端 `npm run dev`
4. 打开 `http://localhost:5173`

这时前端会通过代理访问后端接口。

## 7. 当前已经接好的接口

### 7.1 健康检查

- `GET /api/health`

### 7.2 词库

- `POST /api/word-sets`
- `GET /api/word-sets`
- `GET /api/word-sets/{wordSetId}/words`
- `POST /api/word-sets/{wordSetId}/import`

说明：

- 支持创建词库
- 支持分页查询词库
- 支持词条 CSV / `.apkg` 导入
- 支持查看指定词库下的词条列表
- 导入结果会返回新增数、跳过数和行级错误
- `.apkg` 当前只导入 note 字段内容，不导入复习历史、牌组层级和媒体文件

### 7.3 学习计划

- `POST /api/study-plans`
- `GET /api/study-plans`
- `GET /api/study-plans/{id}`
- `PUT /api/study-plans/{id}`

说明：

- 支持创建学习计划
- 支持读取计划列表和详情
- 支持更新计划
- 更新计划后会重建卡片实例

### 7.4 卡片

- `GET /api/study-plans/{planId}/cards/today`
- `GET /api/study-plans/{planId}/cards/calendar`

说明：

- 学习计划创建或更新后会预生成卡片
- `today` 接口返回真实今日卡片
- `calendar` 接口返回日期区间内的新词/复习聚合统计

### 7.5 模板

- `GET /api/templates/anki`
- `GET /api/templates/md`
- `POST /api/templates/anki/preview`
- `POST /api/templates/md/preview`

说明：

- 这两个接口可以读取 Flyway 初始化进去的默认模板
- 预览接口支持带示例上下文进行渲染

### 7.6 导出任务

- `POST /api/export-jobs`
- `GET /api/export-jobs`
- `GET /api/export-jobs/{id}/download`

说明：

- 支持创建 `ANKI_CSV`、`ANKI_TSV`、`MARKDOWN` 三种导出任务
- 导出后会在 `APP_EXPORT_BASE_DIR` 目录生成文件
- 可通过下载接口直接下载生成结果

## 8. 当前前端页面怎么对应后端

### 8.1 词库页

页面路径：

- `/word-sets`

当前功能：

- 展示词库列表
- 提供创建词库表单
- 支持选中词库查看词条列表
- 支持上传 CSV / `.apkg` 导入词条
- 支持显示导入错误明细

### 8.2 学习计划页

页面路径：

- `/study-plans`

当前功能：

- 展示学习计划列表
- 提供计划创建表单
- 支持选择已有计划后回填并编辑

### 8.3 今日卡片页

页面路径：

- `/cards`

当前功能：

- 输入 `planId`
- 选择日期
- 请求今日卡片接口并展示真实结果

### 8.4 模板页

页面路径：

- `/templates`

当前功能：

- 查看 Anki 模板
- 查看 Markdown 模板
- 对现有模板执行预览

### 8.5 导出任务页

页面路径：

- `/export-jobs`

当前功能：

- 创建导出任务
- 查看导出任务列表
- 下载导出文件

## 9. 当前系统的推荐使用顺序

建议这样使用：

1. 先起后端并连本地 PostgreSQL，确认 Flyway 能建表
2. 打开 `/api/health` 确认服务正常
3. 在前端创建一个词库
4. 上传 CSV 或 `.apkg` 导入词条
5. 创建学习计划
6. 到今日卡片页按计划和日期查询结果
7. 到模板页测试预览
8. 到导出页创建并下载导出文件

## 10. 当前限制

- 这是第一阶段 MVP，不是完整可交付产品
- 尚未实现多用户、登录鉴权、动态记忆算法
- 尚未实现 `.apkg` 完整包生成
- 尚未实现复习打分回写和基于打分的调度

## 11. 下一步建议

优先建议继续实现下面这几块：

1. 复习打分写入 `review_log`
2. 卡片状态流转与已完成/跳过统计
3. 学习计划编辑的更细粒度重建策略
4. 模板编辑与保存能力
5. 导出任务失败状态与错误信息展示
