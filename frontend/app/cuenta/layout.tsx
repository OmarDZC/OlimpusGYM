import { RequireRole } from "@/components/auth/RequireRole";

export default function CuentaLayout({ children }: { children: React.ReactNode }) {
  return <RequireRole roles={["MEMBER"]}>{children}</RequireRole>;
}
