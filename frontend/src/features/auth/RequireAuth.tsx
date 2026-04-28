import { useQuery } from "@tanstack/react-query";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { getMe } from "@/features/auth/api";
import { ApiClientError } from "@/shared/api/errors";
import { StatusState } from "@/shared/components/StatusState";

export function RequireAuth() {
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

  return <Outlet />;
}
