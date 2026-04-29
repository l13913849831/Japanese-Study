# P0-b-1: define backup package format and manifest

## Goal

为 `P0-b` 第一版账号整体备份定义稳定的压缩包协议，明确 `zip` 包内目录结构、`manifest.json` 字段、备份类型语义，以及后续接入 S3 / VIP 托管时不需要重做的格式边界。

## What I already know

* 父任务 `P0-b` 已确认第一版只面向免费用户：
  * 创建备份后直接下载
  * 上传本地备份文件恢复
  * 恢复前先下载 `SAFETY_SNAPSHOT`
* 第一版不保留免费用户的服务端长期备份历史。
* 备份包已经确认做成单文件压缩包，例如 `zip`。
* 后续需要为 `BackupStorage` 抽象和 S3/VIP 托管预留接口。

## Requirements

* 需要定义第一版 `zip` 包目录结构。
* 需要定义 `manifest.json` 的稳定字段。
* 需要明确哪些学习资产文件属于第一版备份范围。
* 需要明确 `MANUAL_BACKUP` 与 `SAFETY_SNAPSHOT` 的协议语义。
* 需要让后端代码里存在可复用的 manifest / layout 类型，而不是只停留在文档。

## Acceptance Criteria

* [ ] 仓库中存在一份可读的备份包格式文档。
* [ ] 后端存在 `backup` 域的 manifest / layout 基础类型。
* [ ] manifest 能表达：
  * `formatVersion`
  * `packageType`
  * `backupType`
  * `createdAt`
  * 备份文件清单
* [ ] 文件清单覆盖当前确认的第一版学习资产范围。
* [ ] 有测试保证 manifest 序列化和文件布局约定稳定。

## Out of Scope

* 这轮不实现真正的备份导出接口
* 这轮不实现真正的恢复接口
* 这轮不实现数据库表结构
* 这轮不实现 S3 接入

## Technical Notes

* 当前实现优先落在 `backend/src/main/java/com/jp/vocab/backup/`。
* manifest 类型需要服务后续导出和恢复，因此应避免和 HTTP DTO 强耦合。
* 当前阶段不需要把 record count、checksum 等字段做成强制必填，但格式应为后续扩展留位。
