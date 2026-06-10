import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  App,
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Typography,
  type TablePaginationConfig
} from "antd";
import dayjs from "dayjs";
import { useState } from "react";
import { ApiClientError } from "@/shared/api/errors";
import { PageHeader } from "@/shared/components/PageHeader";
import { PageSection } from "@/shared/components/PageSection";
import { StatusState } from "@/shared/components/StatusState";
import {
  disableAdminUser,
  enableAdminUser,
  getAdminUserDetail,
  listSecurityAuditEvents,
  listSecurityAlerts,
  listAdminUsers,
  resetAdminUserPassword,
  type AdminUserDetail,
  type AdminUserFilters,
  type AdminUserListItem,
  type AdminUserRole,
  type AdminUserStatus,
  type SecurityAlert,
  type SecurityAlertSeverity,
  type SecurityAuditEvent,
  type SecurityAuditEventType,
  type SecurityAuditFilters,
  type SecurityAuditOutcome
} from "@/features/admin/api";

interface AdminUserFilterFormValues {
  keyword?: string;
  status?: AdminUserStatus;
  role?: AdminUserRole;
}

interface AdminPasswordResetFormValues {
  newPassword?: string;
}

interface SecurityAuditFilterFormValues {
  eventType?: SecurityAuditEventType;
  outcome?: SecurityAuditOutcome;
  username?: string;
}

const USER_STATUS_COLORS: Record<AdminUserStatus, string> = {
  ACTIVE: "green",
  DISABLED: "red"
};

const USER_ROLE_COLORS: Record<AdminUserRole, string> = {
  USER: "blue",
  ADMIN: "volcano"
};

const SECURITY_AUDIT_EVENT_OPTIONS: Array<{ label: string; value: SecurityAuditEventType }> = [
  { label: "LOGIN_SUCCESS", value: "LOGIN_SUCCESS" },
  { label: "LOGIN_FAILURE", value: "LOGIN_FAILURE" },
  { label: "LOGIN_LOCKED", value: "LOGIN_LOCKED" },
  { label: "LOGIN_DISABLED", value: "LOGIN_DISABLED" },
  { label: "LOGOUT", value: "LOGOUT" },
  { label: "ADMIN_USER_DETAIL_VIEW", value: "ADMIN_USER_DETAIL_VIEW" },
  { label: "ADMIN_USER_STATUS_CHANGE", value: "ADMIN_USER_STATUS_CHANGE" },
  { label: "ADMIN_USER_PASSWORD_RESET", value: "ADMIN_USER_PASSWORD_RESET" }
];

const SECURITY_AUDIT_OUTCOME_COLORS: Record<SecurityAuditOutcome, string> = {
  SUCCESS: "green",
  FAILURE: "red",
  BLOCKED: "orange"
};

const SECURITY_ALERT_SEVERITY_COLORS: Record<SecurityAlertSeverity, string> = {
  HIGH: "red",
  MEDIUM: "orange"
};

function formatDate(value: string) {
  return dayjs(value).format("YYYY-MM-DD HH:mm");
}

function userLabel(user: Pick<AdminUserListItem, "id" | "username" | "displayName">) {
  return user.username ? `${user.displayName} (${user.username})` : `${user.displayName} (#${user.id})`;
}

function renderStatusTag(status: AdminUserStatus) {
  return <Tag color={USER_STATUS_COLORS[status]}>{status}</Tag>;
}

function renderRoleTag(role: AdminUserRole) {
  return <Tag color={USER_ROLE_COLORS[role]}>{role}</Tag>;
}

function renderAuditOutcomeTag(outcome: SecurityAuditOutcome) {
  return <Tag color={SECURITY_AUDIT_OUTCOME_COLORS[outcome]}>{outcome}</Tag>;
}

function renderAlertSeverityTag(severity: SecurityAlertSeverity) {
  return <Tag color={SECURITY_ALERT_SEVERITY_COLORS[severity]}>{severity}</Tag>;
}

export function AdminHomePage() {
  const [filterForm] = Form.useForm<AdminUserFilterFormValues>();
  const [passwordResetForm] = Form.useForm<AdminPasswordResetFormValues>();
  const [auditFilterForm] = Form.useForm<SecurityAuditFilterFormValues>();
  const { message } = App.useApp();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<AdminUserFilters>({ page: 1, pageSize: 20 });
  const [auditFilters, setAuditFilters] = useState<SecurityAuditFilters>({ page: 1, pageSize: 20 });
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [passwordResetTarget, setPasswordResetTarget] = useState<AdminUserListItem | null>(null);

  const usersQuery = useQuery({
    queryKey: [
      "adminUsers",
      filters.page ?? 1,
      filters.pageSize ?? 20,
      filters.keyword ?? "",
      filters.status ?? "",
      filters.role ?? ""
    ],
    queryFn: () => listAdminUsers(filters)
  });

  const detailQuery = useQuery({
    queryKey: ["adminUserDetail", selectedUserId],
    queryFn: () => getAdminUserDetail(selectedUserId!),
    enabled: selectedUserId !== null
  });

  const auditEventsQuery = useQuery({
    queryKey: [
      "securityAuditEvents",
      auditFilters.page ?? 1,
      auditFilters.pageSize ?? 20,
      auditFilters.eventType ?? "",
      auditFilters.outcome ?? "",
      auditFilters.username ?? ""
    ],
    queryFn: () => listSecurityAuditEvents(auditFilters)
  });

  const securityAlertsQuery = useQuery({
    queryKey: ["securityAlerts"],
    queryFn: listSecurityAlerts
  });

  const refreshAdminUsers = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["adminUsers"] }),
      queryClient.invalidateQueries({ queryKey: ["adminUserDetail"] }),
      queryClient.invalidateQueries({ queryKey: ["securityAuditEvents"] }),
      queryClient.invalidateQueries({ queryKey: ["securityAlerts"] })
    ]);
  };

  const disableMutation = useMutation({
    mutationFn: disableAdminUser,
    onSuccess: async () => {
      message.success("User disabled.");
      await refreshAdminUsers();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const enableMutation = useMutation({
    mutationFn: enableAdminUser,
    onSuccess: async () => {
      message.success("User enabled.");
      await refreshAdminUsers();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const resetPasswordMutation = useMutation({
    mutationFn: ({ userId, newPassword }: { userId: number; newPassword: string }) =>
      resetAdminUserPassword(userId, { newPassword }),
    onSuccess: async () => {
      message.success("Password reset.");
      setPasswordResetTarget(null);
      passwordResetForm.resetFields();
      await refreshAdminUsers();
    },
    onError: (error) => message.error((error as ApiClientError).message)
  });

  const handleApplyFilters = (values: AdminUserFilterFormValues) => {
    setFilters((current) => ({
      page: 1,
      pageSize: current.pageSize ?? 20,
      keyword: values.keyword?.trim() || undefined,
      status: values.status,
      role: values.role
    }));
  };

  const handleResetFilters = () => {
    filterForm.resetFields();
    setFilters((current) => ({ page: 1, pageSize: current.pageSize ?? 20 }));
  };

  const handleApplyAuditFilters = (values: SecurityAuditFilterFormValues) => {
    setAuditFilters((current) => ({
      page: 1,
      pageSize: current.pageSize ?? 20,
      eventType: values.eventType,
      outcome: values.outcome,
      username: values.username?.trim() || undefined
    }));
  };

  const handleResetAuditFilters = () => {
    auditFilterForm.resetFields();
    setAuditFilters((current) => ({ page: 1, pageSize: current.pageSize ?? 20 }));
  };

  const handleTableChange = (pagination: TablePaginationConfig) => {
    setFilters((current) => ({
      ...current,
      page: pagination.current ?? 1,
      pageSize: pagination.pageSize ?? 20
    }));
  };

  const handleAuditTableChange = (pagination: TablePaginationConfig) => {
    setAuditFilters((current) => ({
      ...current,
      page: pagination.current ?? 1,
      pageSize: pagination.pageSize ?? 20
    }));
  };

  const handleOpenPasswordReset = (user: AdminUserListItem) => {
    setPasswordResetTarget(user);
    passwordResetForm.resetFields();
  };

  const handleSubmitPasswordReset = (values: AdminPasswordResetFormValues) => {
    if (!passwordResetTarget || !values.newPassword) {
      return;
    }
    resetPasswordMutation.mutate({
      userId: passwordResetTarget.id,
      newPassword: values.newPassword
    });
  };

  const users = usersQuery.data?.items ?? [];
  const securityAlerts = securityAlertsQuery.data ?? [];
  const visibleActiveCount = users.filter((user) => user.status === "ACTIVE").length;
  const visibleDisabledCount = users.filter((user) => user.status === "DISABLED").length;
  const highSeverityAlertCount = securityAlerts.filter((alert) => alert.severity === "HIGH").length;
  const actionPending = disableMutation.isPending || enableMutation.isPending;

  return (
    <div className="page-stack">
      <PageHeader
        title="Admin Console"
        description="Operate the first user governance slice: search users, inspect account detail, and change account status."
        extra={<Tag color="volcano">ADMIN</Tag>}
      />

      <Alert
        type="info"
        showIcon
        message="Governance boundary"
        description="This MVP exposes account status and asset counts only. It does not expose passwords, raw learning content, or direct data-edit tools."
      />

      <PageSection title="Overview">
        <div className="dashboard-overview-grid">
          <Statistic title="Matched Users" value={usersQuery.data?.total ?? 0} />
          <Statistic title="Visible Active" value={visibleActiveCount} />
          <Statistic title="Visible Disabled" value={visibleDisabledCount} />
          <Statistic title="High Severity Alerts" value={highSeverityAlertCount} />
        </div>
      </PageSection>

      <PageSection title="Filters">
        <Form<AdminUserFilterFormValues> form={filterForm} layout="inline" onFinish={handleApplyFilters}>
          <Form.Item label="Keyword" name="keyword">
            <Input allowClear placeholder="username / display name" />
          </Form.Item>
          <Form.Item label="Status" name="status">
            <Select
              allowClear
              placeholder="Any status"
              style={{ width: 140 }}
              options={[
                { label: "ACTIVE", value: "ACTIVE" },
                { label: "DISABLED", value: "DISABLED" }
              ]}
            />
          </Form.Item>
          <Form.Item label="Role" name="role">
            <Select
              allowClear
              placeholder="Any role"
              style={{ width: 140 }}
              options={[
                { label: "USER", value: "USER" },
                { label: "ADMIN", value: "ADMIN" }
              ]}
            />
          </Form.Item>
          <Space wrap>
            <Button type="primary" htmlType="submit">
              Apply
            </Button>
            <Button onClick={handleResetFilters}>Reset</Button>
          </Space>
        </Form>
      </PageSection>

      <PageSection title="Users">
        {usersQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : usersQuery.isError ? (
          <StatusState mode="error" description={(usersQuery.error as Error).message} />
        ) : (
          <Table<AdminUserListItem>
            rowKey="id"
            dataSource={users}
            onChange={handleTableChange}
            pagination={{
              current: usersQuery.data?.page,
              pageSize: usersQuery.data?.pageSize,
              total: usersQuery.data?.total,
              showSizeChanger: true
            }}
            columns={[
              {
                title: "User",
                render: (_, user) => (
                  <Space direction="vertical" size={2}>
                    <Typography.Text strong>{user.displayName}</Typography.Text>
                    <Typography.Text type="secondary">{user.username ?? "No local username"}</Typography.Text>
                  </Space>
                )
              },
              {
                title: "Status",
                dataIndex: "status",
                render: (status: AdminUserStatus) => renderStatusTag(status)
              },
              {
                title: "Role",
                dataIndex: "role",
                render: (role: AdminUserRole) => renderRoleTag(role)
              },
              {
                title: "Created",
                dataIndex: "createdAt",
                render: (value: string) => formatDate(value)
              },
              {
                title: "Updated",
                dataIndex: "updatedAt",
                render: (value: string) => formatDate(value)
              },
              {
                title: "Action",
                render: (_, user) => (
                  <Space wrap>
                    <Button type="link" onClick={() => setSelectedUserId(user.id)}>
                      Detail
                    </Button>
                    <Button type="link" onClick={() => handleOpenPasswordReset(user)}>
                      Reset Password
                    </Button>
                    {user.status === "ACTIVE" ? (
                      <Popconfirm
                        title={`Disable ${userLabel(user)}?`}
                        onConfirm={() => disableMutation.mutate(user.id)}
                      >
                        <Button type="link" danger loading={actionPending}>
                          Disable
                        </Button>
                      </Popconfirm>
                    ) : (
                      <Popconfirm
                        title={`Enable ${userLabel(user)}?`}
                        onConfirm={() => enableMutation.mutate(user.id)}
                      >
                        <Button type="link" loading={actionPending}>
                          Enable
                        </Button>
                      </Popconfirm>
                    )}
                  </Space>
                )
              }
            ]}
            locale={{ emptyText: "No users found." }}
          />
        )}
      </PageSection>

      <PageSection title="Security Alerts">
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Alert
            type="warning"
            showIcon
            message="Alert strategy"
            description="Alerts are derived from the last 24 hours of audit events: repeated login failures, account locks, and disabled-account login attempts."
          />

          {securityAlertsQuery.isLoading ? (
            <StatusState mode="loading" />
          ) : securityAlertsQuery.isError ? (
            <StatusState mode="error" description={(securityAlertsQuery.error as Error).message} />
          ) : (
            <Table<SecurityAlert>
              rowKey="id"
              dataSource={securityAlerts}
              pagination={false}
              columns={[
                {
                  title: "Severity",
                  dataIndex: "severity",
                  render: (severity: SecurityAlertSeverity) => renderAlertSeverityTag(severity)
                },
                {
                  title: "Alert",
                  render: (_, alert) => (
                    <Space direction="vertical" size={2}>
                      <Typography.Text strong>{alert.title}</Typography.Text>
                      <Typography.Text type="secondary">{alert.alertType}</Typography.Text>
                    </Space>
                  )
                },
                {
                  title: "Target",
                  render: (_, alert) => alert.username ?? "Unknown username"
                },
                {
                  title: "IP",
                  dataIndex: "ipAddress",
                  render: (value?: string | null) => value ?? "-"
                },
                {
                  title: "Count",
                  dataIndex: "eventCount"
                },
                {
                  title: "Last Seen",
                  dataIndex: "lastSeenAt",
                  render: (value: string) => formatDate(value)
                },
                {
                  title: "Description",
                  dataIndex: "description"
                }
              ]}
              locale={{ emptyText: "No active security alerts." }}
            />
          )}
        </Space>
      </PageSection>

      <PageSection title="Security Audit Events">
        <Space direction="vertical" size={16} style={{ width: "100%" }}>
          <Form<SecurityAuditFilterFormValues> form={auditFilterForm} layout="inline" onFinish={handleApplyAuditFilters}>
            <Form.Item label="Event" name="eventType">
              <Select
                allowClear
                showSearch
                placeholder="Any event"
                style={{ width: 240 }}
                options={SECURITY_AUDIT_EVENT_OPTIONS}
              />
            </Form.Item>
            <Form.Item label="Outcome" name="outcome">
              <Select
                allowClear
                placeholder="Any outcome"
                style={{ width: 140 }}
                options={[
                  { label: "SUCCESS", value: "SUCCESS" },
                  { label: "FAILURE", value: "FAILURE" },
                  { label: "BLOCKED", value: "BLOCKED" }
                ]}
              />
            </Form.Item>
            <Form.Item label="Username" name="username">
              <Input allowClear placeholder="username" />
            </Form.Item>
            <Space wrap>
              <Button type="primary" htmlType="submit">
                Apply
              </Button>
              <Button onClick={handleResetAuditFilters}>Reset</Button>
            </Space>
          </Form>

          {auditEventsQuery.isLoading ? (
            <StatusState mode="loading" />
          ) : auditEventsQuery.isError ? (
            <StatusState mode="error" description={(auditEventsQuery.error as Error).message} />
          ) : (
            <Table<SecurityAuditEvent>
              rowKey="id"
              dataSource={auditEventsQuery.data?.items ?? []}
              onChange={handleAuditTableChange}
              pagination={{
                current: auditEventsQuery.data?.page,
                pageSize: auditEventsQuery.data?.pageSize,
                total: auditEventsQuery.data?.total,
                showSizeChanger: true
              }}
              columns={[
                {
                  title: "Time",
                  dataIndex: "createdAt",
                  render: (value: string) => formatDate(value)
                },
                {
                  title: "Event",
                  dataIndex: "eventType"
                },
                {
                  title: "Outcome",
                  dataIndex: "outcome",
                  render: (outcome: SecurityAuditOutcome) => renderAuditOutcomeTag(outcome)
                },
                {
                  title: "Actor",
                  render: (_, event) => (
                    <Space direction="vertical" size={2}>
                      <Typography.Text>{event.username ?? "-"}</Typography.Text>
                      <Typography.Text type="secondary">{event.userId ? `#${event.userId}` : "No user id"}</Typography.Text>
                    </Space>
                  )
                },
                {
                  title: "IP",
                  dataIndex: "ipAddress",
                  render: (value?: string | null) => value ?? "-"
                },
                {
                  title: "Message",
                  dataIndex: "message",
                  render: (value?: string | null) => value ?? "-"
                }
              ]}
              locale={{ emptyText: "No audit events found." }}
            />
          )}
        </Space>
      </PageSection>

      <Modal
        title={detailQuery.data ? `User Detail #${detailQuery.data.id}` : "User Detail"}
        open={selectedUserId !== null}
        width={720}
        footer={null}
        onCancel={() => setSelectedUserId(null)}
      >
        {detailQuery.isLoading ? (
          <StatusState mode="loading" />
        ) : detailQuery.isError ? (
          <StatusState mode="error" description={(detailQuery.error as Error).message} />
        ) : detailQuery.data ? (
          <UserDetailContent user={detailQuery.data} />
        ) : (
          <StatusState mode="empty" description="Select a user first." />
        )}
      </Modal>

      <Modal
        title={passwordResetTarget ? `Reset Password: ${userLabel(passwordResetTarget)}` : "Reset Password"}
        open={passwordResetTarget !== null}
        confirmLoading={resetPasswordMutation.isPending}
        onCancel={() => {
          setPasswordResetTarget(null);
          passwordResetForm.resetFields();
        }}
        onOk={() => passwordResetForm.submit()}
      >
        <Alert
          type="warning"
          showIcon
          message="Password reset is audited"
          description="Use this only for account recovery or security governance. The target user's existing login lock state will be cleared."
          style={{ marginBottom: 16 }}
        />
        <Form<AdminPasswordResetFormValues>
          form={passwordResetForm}
          layout="vertical"
          onFinish={handleSubmitPasswordReset}
        >
          <Form.Item
            label="New Password"
            name="newPassword"
            rules={[
              { required: true, message: "Enter a new password." },
              { min: 8, message: "Password must be at least 8 characters." },
              { max: 72, message: "Password must be at most 72 characters." }
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}

function UserDetailContent({ user }: { user: AdminUserDetail }) {
  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }}>
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="ID">{user.id}</Descriptions.Item>
        <Descriptions.Item label="Username">{user.username ?? "No local username"}</Descriptions.Item>
        <Descriptions.Item label="Display Name">{user.displayName}</Descriptions.Item>
        <Descriptions.Item label="Status">{renderStatusTag(user.status)}</Descriptions.Item>
        <Descriptions.Item label="Role">{renderRoleTag(user.role)}</Descriptions.Item>
        <Descriptions.Item label="Created">{formatDate(user.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="Updated">{formatDate(user.updatedAt)}</Descriptions.Item>
      </Descriptions>

      <div className="dashboard-overview-grid">
        <Statistic title="Word Sets" value={user.assetSummary.wordSetCount} />
        <Statistic title="Study Plans" value={user.assetSummary.studyPlanCount} />
        <Statistic title="Notes" value={user.assetSummary.noteCount} />
      </div>
    </Space>
  );
}
