import { useQuery } from "@tanstack/react-query";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getMe } from "@/features/auth/api";
import { ApiClientError } from "@/shared/api/errors";
import { StatusState } from "@/shared/components/StatusState";

export function RequireAdmin() {
  const location = useLocation();
  const currentUserQuery = useQuery({
    queryKey: ["me"],
    queryFn: getMe,
    retry: false
  });

  if (currentUserQuery.isLoading) {
    return <StatusState mode="loading" />;
  }

  if (currentUserQuery.isError) {
    const error = currentUserQuery.error as ApiClientError;
    if (error.status === 401) {
      return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
    }
    return <StatusState mode="error" description={error.message} />;
  }

  const currentUser = currentUserQuery.data;
  if (!currentUser) {
    return <StatusState mode="error" title="会话状态异常" description="无法读取当前登录用户。" />;
  }

  if (!currentUser.roles.includes("ADMIN")) {
    return <StatusState mode="error" title="无权访问" description="当前账号没有管理员权限。" />;
  }

  return <Outlet />;
}
