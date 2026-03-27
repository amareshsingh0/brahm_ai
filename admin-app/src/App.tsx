import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AdminLayout } from "@/components/layout/AdminLayout";
import LoginPage       from "@/pages/LoginPage";
import DashboardPage   from "@/pages/DashboardPage";
import UsersPage       from "@/pages/UsersPage";
import UserDetailPage  from "@/pages/UserDetailPage";
import PaymentsPage    from "@/pages/PaymentsPage";
import ChatsPage       from "@/pages/ChatsPage";
import LogsPage        from "@/pages/LogsPage";
import DeletedAccountsPage from "@/pages/DeletedAccountsPage";
import ApiMonitorPage     from "@/pages/ApiMonitorPage";
import SubscriptionsPage  from "@/pages/SubscriptionsPage";

export default function App() {
  return (
    <BrowserRouter basename="/admin">
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />

        {/* Protected — all wrapped by AdminLayout */}
        <Route element={<AdminLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/users"     element={<UsersPage />} />
          <Route path="/users/:id" element={<UserDetailPage />} />
          <Route path="/payments"  element={<PaymentsPage />} />
          <Route path="/chats"     element={<ChatsPage />} />
          <Route path="/api-monitor"    element={<ApiMonitorPage />} />
          <Route path="/subscriptions"  element={<SubscriptionsPage />} />
          <Route path="/logs"        element={<LogsPage />} />
          <Route path="/deleted-accounts" element={<DeletedAccountsPage />} />
        </Route>

        {/* Catch-all */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
