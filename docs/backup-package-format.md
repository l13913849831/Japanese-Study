# 账号备份包格式

## 1. 文档目的

这份文档定义 `P0-b` 第一版账号整体备份的压缩包协议，作为后端导出、上传恢复、后续 S3 托管与 VIP 托管模式的共同基础。

当前约定以 `2026-04-29` 的 `P0-b-1` 任务为准。

## 2. 第一版适用范围

第一版主要服务免费用户：

- 点击“创建备份”后，直接下载一个整体备份 `zip`
- 上传本地 `zip` 后执行覆盖恢复
- 恢复前系统先生成并下载一份 `SAFETY_SNAPSHOT`

第一版不做：

- 免费用户服务端长期备份托管
- S3 / 对象存储正式接入
- 自动备份

## 3. 压缩包顶层结构

```text
backup-<timestamp>.zip
  manifest.json
  data/
    user-profile.json
    user-settings.json
    word-sets.json
    word-entries.json
    anki-templates.json
    markdown-templates.json
    note-sources.json
    study-plans.json
    card-instances.json
    review-logs.json
    notes.json
    note-review-logs.json
```

约定：

- `manifest.json` 放在压缩包根目录
- 业务数据文件统一放在 `data/`
- 第一版每类学习资产对应一个 JSON 文件

## 4. manifest 字段

第一版 `manifest.json` 至少包含：

```json
{
  "formatVersion": "1",
  "packageType": "ACCOUNT_BACKUP",
  "backupType": "MANUAL_BACKUP",
  "scope": "CURRENT_ACCOUNT_LEARNING_ASSETS",
  "createdAt": "2026-04-29T12:00:00Z",
  "files": [
    {
      "path": "data/user-profile.json",
      "payloadType": "USER_PROFILE",
      "required": true,
      "recordCount": 1
    }
  ]
}
```

字段含义：

- `formatVersion`
  - 备份包协议版本
  - 第一版固定为 `"1"`
- `packageType`
  - 当前固定为 `"ACCOUNT_BACKUP"`
- `backupType`
  - `MANUAL_BACKUP`
  - `SAFETY_SNAPSHOT`
- `scope`
  - 当前固定为 `"CURRENT_ACCOUNT_LEARNING_ASSETS"`
  - 表示只覆盖当前账号学习资产，不包含登录凭据和系统默认资料
- `createdAt`
  - 生成时间，ISO 8601
- `files`
  - 包内文件清单

## 5. 备份类型语义

### `MANUAL_BACKUP`

用户主动创建的正式备份。

免费版流程：

- 生成压缩包
- 浏览器直接下载
- 服务端不长期托管

### `SAFETY_SNAPSHOT`

系统在覆盖恢复前自动生成的保护性快照。

免费版流程：

- 系统先生成压缩包
- 浏览器先下载
- 用户确认后才继续恢复
- 服务端不长期托管

## 6. 第一版数据范围

第一版打包这些用户学习资产：

- `user-profile.json`
  - 当前账号可迁移资料字段
- `user-settings.json`
  - 当前账号设置
- `word-sets.json`
  - 用户私有词库
- `word-entries.json`
  - 用户私有词库下的词条
- `anki-templates.json`
  - 用户私有 Anki 模板
- `markdown-templates.json`
  - 用户私有 Markdown 模板
- `note-sources.json`
  - 用户私有知识点内容
- `study-plans.json`
  - 当前账号学习计划
- `card-instances.json`
  - 当前账号卡片进度
- `review-logs.json`
  - 当前账号单词复习日志
- `notes.json`
  - 当前账号知识点进度
- `note-review-logs.json`
  - 当前账号知识点复习日志

第一版不打包：

- `SYSTEM` scope 默认资料
- `user_identity.password_hash`
- 其他用户数据

## 7. 扩展方向

为了后续接入 S3 / VIP 托管，第一版协议需要保持这些稳定前提：

- 备份包格式不依赖本地磁盘路径
- 存储位置和包格式解耦
- `manifest.json` 允许未来增加：
  - checksum
  - app version
  - retention metadata
  - managed storage metadata

也就是说，后续如果切到 S3，变化的是 `BackupStorage` 实现，不是备份包协议本身。
