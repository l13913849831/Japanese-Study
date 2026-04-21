import { createBrowserRouter, Navigate } from "react-router-dom";
import { AppShellLayout } from "@/app/shell/AppShellLayout";
import { StudyDashboardPage } from "@/features/dashboard/StudyDashboardPage";
import { WordSetPage } from "@/features/word-sets/WordSetPage";
import { StudyPlanPage } from "@/features/study-plans/StudyPlanPage";
import { TodayCardsPage } from "@/features/cards/TodayCardsPage";
import { TemplatePage } from "@/features/templates/TemplatePage";
import { ExportJobPage } from "@/features/export-jobs/ExportJobPage";

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppShellLayout />,
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <StudyDashboardPage /> },
      { path: "word-sets", element: <WordSetPage /> },
      { path: "study-plans", element: <StudyPlanPage /> },
      { path: "cards", element: <TodayCardsPage /> },
      { path: "templates", element: <TemplatePage /> },
      { path: "export-jobs", element: <ExportJobPage /> }
    ]
  }
]);
