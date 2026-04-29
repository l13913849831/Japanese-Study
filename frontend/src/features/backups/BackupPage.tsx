import { DownloadOutlined, InboxOutlined, SafetyCertificateOutlined, SyncOutlined } from "@ant-design/icons";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, App, Button, Descriptions, Popconfirm, Space, Steps, Tag, Typography, Upload, type UploadFile } from "antd";
import { useState } from "react";
import {
  confirmBackupRestore,
  downloadSafetySnapshot,
  exportAccountBackup,
  prepareBackupRestore,
  type BackupRestorePrepareResponse
} from "@/features/backups/api";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";

interface PreparedRestoreState extends BackupRestorePrepareResponse {
  sourceBackupFileName: string;
  safetySnapshotDownloaded: boolean;
}

function triggerBrowserDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function buildManualBackupFileName() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, "-");
  return `manual-backup-${timestamp}.zip`;
}

export function BackupPage() {
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [preparedRestore, setPreparedRestore] = useState<PreparedRestoreState | null>(null);
  const selectedFile = fileList[0]?.originFileObj as File | undefined;
  const currentStep = preparedRestore === null ? 0 : preparedRestore.safetySnapshotDownloaded ? 2 : 1;

  const exportMutation = useMutation({
    mutationFn: exportAccountBackup,
    onSuccess: (blob) => {
      triggerBrowserDownload(blob, buildManualBackupFileName());
      message.success("整体备份已生成并开始下载。");
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const prepareMutation = useMutation({
    mutationFn: prepareBackupRestore,
    onSuccess: (response, file) => {
      setPreparedRestore({
        ...response,
        sourceBackupFileName: file.name,
        safetySnapshotDownloaded: false
      });
      message.success("恢复准备完成。请先下载安全快照，再确认覆盖恢复。");
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const snapshotMutation = useMutation({
    mutationFn: downloadSafetySnapshot,
    onSuccess: (blob) => {
      if (!preparedRestore) {
        return;
      }

      triggerBrowserDownload(blob, preparedRestore.safetySnapshotFileName);
      setPreparedRestore((current) => current ? {
        ...current,
        safetySnapshotDownloaded: true
      } : current);
      message.success("安全快照已下载。现在可以确认恢复。");
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const confirmMutation = useMutation({
    mutationFn: confirmBackupRestore,
    onSuccess: async () => {
      setPreparedRestore(null);
      setFileList([]);
      await queryClient.invalidateQueries();
      message.success("恢复已完成，相关页面数据会在下次进入时按新状态刷新。");
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handlePrepareRestore = () => {
    if (!selectedFile) {
      message.warning("请先选择一个本地备份压缩包。");
      return;
    }

    setPreparedRestore(null);
    prepareMutation.mutate(selectedFile);
  };

  const handleDownloadSnapshot = () => {
    if (!preparedRestore) {
      return;
    }

    snapshotMutation.mutate(preparedRestore.safetySnapshotDownloadPath);
  };

  const handleConfirmRestore = async () => {
    if (!preparedRestore) {
      return;
    }

    await confirmMutation.mutateAsync(preparedRestore.restoreToken);
  };

  return (
    <div className="page-stack">
      <PageHeader
        title="备份与恢复"
        description="免费版当前采用本地压缩包模式：创建备份后直接下载到本机，恢复时再上传本地 zip。"
        extra={<Tag color="blue">local-first</Tag>}
      />

      <PageSection title="当前规则">
        <div className="page-stack">
          <Alert
            type="info"
            showIcon
            message="免费用户不会保留服务端托管备份历史。"
            description="点击创建备份后，系统会生成一份 MANUAL_BACKUP 压缩包并直接下载；如果后续要恢复，需要从本地重新上传。"
          />
          <Alert
            type="warning"
            showIcon
            message="恢复会覆盖当前账号的学习资产。"
            description="本轮覆盖范围包括词库、模板、学习计划、单词进度、知识点与相关复习记录；登录凭据和系统默认资料不在覆盖范围内。"
          />
        </div>
      </PageSection>

      <PageSection title="创建整体备份">
        <Space direction="vertical" size={16} style={{ display: "flex" }}>
          <Typography.Text>
            这里会为当前账号生成一份完整学习资产快照，并立即返回一个可下载的 zip 文件。
          </Typography.Text>
          <Space wrap>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              loading={exportMutation.isPending}
              onClick={() => exportMutation.mutate()}
            >
              创建备份并下载
            </Button>
            <Tag color="processing">MANUAL_BACKUP</Tag>
          </Space>
        </Space>
      </PageSection>

      <PageSection title="恢复当前账号">
        <Space direction="vertical" size={16} style={{ display: "flex" }}>
          <Typography.Text type="secondary">
            恢复流程固定为三步：上传本地备份包、先下载安全快照、再明确确认覆盖恢复。恢复会话是短期的，建议连续完成。
          </Typography.Text>

          <Steps
            current={currentStep}
            items={[
              {
                title: "上传备份",
                description: "选择本地 zip 并创建恢复准备会话"
              },
              {
                title: "下载安全快照",
                description: "系统先导出当前状态，作为回滚保护"
              },
              {
                title: "确认恢复",
                description: "确认后才会覆盖当前账号学习资产"
              }
            ]}
          />

          <Upload.Dragger
            accept=".zip,application/zip"
            beforeUpload={() => false}
            maxCount={1}
            fileList={fileList}
            onChange={({ fileList: nextFileList }) => {
              setFileList(nextFileList.slice(-1));
              setPreparedRestore(null);
            }}
            onRemove={() => {
              setPreparedRestore(null);
              return true;
            }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <Typography.Text strong>上传本地备份压缩包</Typography.Text>
            <br />
            <Typography.Text type="secondary">
              当前只接受整体备份 zip；上传后先校验并创建恢复准备，不会立刻覆盖数据。
            </Typography.Text>
          </Upload.Dragger>

          <Space wrap>
            <Button
              type="primary"
              icon={<SyncOutlined />}
              loading={prepareMutation.isPending}
              disabled={!selectedFile}
              onClick={handlePrepareRestore}
            >
              上传并准备恢复
            </Button>
            <Button
              icon={<SafetyCertificateOutlined />}
              loading={snapshotMutation.isPending}
              disabled={!preparedRestore}
              onClick={handleDownloadSnapshot}
            >
              下载安全快照
            </Button>
            <Popconfirm
              title="确认覆盖恢复？"
              description="这会用上传的备份包替换当前账号现有学习资产。请先确认安全快照已经保存到本地。"
              okText="确认恢复"
              cancelText="取消"
              disabled={!preparedRestore?.safetySnapshotDownloaded}
              onConfirm={handleConfirmRestore}
            >
              <Button
                danger
                type="primary"
                loading={confirmMutation.isPending}
                disabled={!preparedRestore?.safetySnapshotDownloaded}
              >
                确认覆盖恢复
              </Button>
            </Popconfirm>
          </Space>

          {preparedRestore ? (
            <>
              <Descriptions
                bordered
                size="small"
                column={1}
                items={[
                  {
                    key: "sourceBackup",
                    label: "上传备份",
                    children: preparedRestore.sourceBackupFileName
                  },
                  {
                    key: "snapshotFile",
                    label: "安全快照文件",
                    children: preparedRestore.safetySnapshotFileName
                  },
                  {
                    key: "status",
                    label: "当前状态",
                    children: preparedRestore.safetySnapshotDownloaded ? "可确认恢复" : "等待下载安全快照"
                  }
                ]}
              />
              <Alert
                type={preparedRestore.safetySnapshotDownloaded ? "success" : "info"}
                showIcon
                message={
                  preparedRestore.safetySnapshotDownloaded
                    ? "安全快照已就绪，可以继续恢复。"
                    : "请先下载安全快照。"
                }
                description={
                  preparedRestore.safetySnapshotDownloaded
                    ? "如果恢复后发现不符合预期，可以用刚下载的安全快照重新走一次恢复流程。"
                    : "后端会阻止跳过这一步直接恢复；前端也会在下载前保持确认按钮禁用。"
                }
              />
            </>
          ) : (
            <Alert
              type="info"
              showIcon
              message="还没有活动中的恢复会话。"
              description="选择一个本地备份包后，点击“上传并准备恢复”，系统会先生成一份当前状态的安全快照。"
            />
          )}
        </Space>
      </PageSection>
    </div>
  );
}
