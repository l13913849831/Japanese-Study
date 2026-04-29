import { lazy, Suspense } from "react";
import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppShellLayout } from "@/app/shell/AppShellLayout";
import { RequireAuth } from "@/features/auth/RequireAuth";
import { StatusState } from "@/shared/components/StatusState";

const StudyDashboardPage = lazy(async () => {
  const module = await import("@/features/dashboard/StudyDashboardPage");
  return { default: module.StudyDashboardPage };
});

const WordSetPage = lazy(async () => {
  const module = await import("@/features/word-sets/WordSetPage");
  return { default: module.WordSetPage };
});

const StudyPlanPage = lazy(async () => {
  const module = await import("@/features/study-plans/StudyPlanPage");
  return { default: module.StudyPlanPage };
});

const TodayCardsPage = lazy(async () => {
  const module = await import("@/features/cards/TodayCardsPage");
  return { default: module.TodayCardsPage };
});

const NoteDashboardPage = lazy(async () => {
  const module = await import("@/features/notes/NoteDashboardPage");
  return { default: module.NoteDashboardPage };
});

const NotesPage = lazy(async () => {
  const module = await import("@/features/notes/NotesPage");
  return { default: module.NotesPage };
});

const NoteReviewPage = lazy(async () => {
  const module = await import("@/features/notes/NoteReviewPage");
  return { default: module.NoteReviewPage };
});

const TemplatePage = lazy(async () => {
  const module = await import("@/features/templates/TemplatePage");
  return { default: module.TemplatePage };
});

const ExportJobPage = lazy(async () => {
  const module = await import("@/features/export-jobs/ExportJobPage");
  return { default: module.ExportJobPage };
});

const BackupPage = lazy(async () => {
  const module = await import("@/features/backups/BackupPage");
  return { default: module.BackupPage };
});

const WeakItemsPage = lazy(async () => {
  const module = await import("@/features/weak-items/WeakItemsPage");
  return { default: module.WeakItemsPage };
});

const LoginPage = lazy(async () => {
  const module = await import("@/features/auth/LoginPage");
  return { default: module.LoginPage };
});

const AccountPage = lazy(async () => {
  const module = await import("@/features/auth/AccountPage");
  return { default: module.AccountPage };
});

function withPageFallback(element: React.JSX.Element) {
  return (
    <Suspense fallback={<StatusState mode="loading" />}>
      {element}
    </Suspense>
  );
}

export const router = createBrowserRouter([
  {
    path: "/login",
    element: withPageFallback(<LoginPage />)
  },
  {
    element: <RequireAuth />,
    children: [
      {
        path: "/",
        element: <AppShellLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: "dashboard", element: withPageFallback(<StudyDashboardPage />) },
          { path: "word-sets", element: withPageFallback(<WordSetPage />) },
          { path: "study-plans", element: withPageFallback(<StudyPlanPage />) },
          { path: "cards", element: withPageFallback(<TodayCardsPage />) },
          { path: "notes/dashboard", element: withPageFallback(<NoteDashboardPage />) },
          { path: "notes", element: withPageFallback(<NotesPage />) },
          { path: "notes/review", element: withPageFallback(<NoteReviewPage />) },
          { path: "weak-items", element: withPageFallback(<WeakItemsPage />) },
          { path: "templates", element: withPageFallback(<TemplatePage />) },
          { path: "backups", element: withPageFallback(<BackupPage />) },
          { path: "export-jobs", element: withPageFallback(<ExportJobPage />) },
          { path: "account", element: withPageFallback(<AccountPage />) }
        ]
      }
    ]
  }
]);
