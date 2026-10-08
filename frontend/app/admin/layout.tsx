import { RequireRole } from "@/components/auth/RequireRole";
import { AdminNav } from "@/components/admin/AdminNav";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireRole roles={["OWNER", "STAFF"]}>
      <AdminNav />
      {children}
    </RequireRole>
  );
}
