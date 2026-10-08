import { RequireRole } from "@/components/auth/RequireRole";
import { AccountNav } from "@/components/account/AccountNav";
import { MobileAccountNav } from "@/components/account/MobileAccountNav";

export default function CuentaLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireRole roles={["MEMBER"]}>
      <AccountNav />
      {/* padding inferior en móvil para que la barra fija no tape el contenido */}
      <div className="pb-20 md:pb-0">{children}</div>
      <MobileAccountNav />
    </RequireRole>
  );
}
