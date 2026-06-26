"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { LayoutDashboard, Package, Users, Settings, LogOut, ShieldCheck, MessageSquare } from "lucide-react";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [adminName, setAdminName] = useState("");

  useEffect(() => {
    const savedUser = localStorage.getItem("vault_user");
    if (!savedUser) {
      router.push("/login");
      return;
    }
    const parsedUser = JSON.parse(savedUser);
    if (parsedUser.role !== "ROLE_ADMIN") {
      router.push("/login");
      return;
    }
    setAdminName(parsedUser.username);
  }, [router]);

  const navItems = [
    { name: "Tổng quan", href: "/admin", icon: LayoutDashboard },
    { name: "Đơn hàng", href: "/admin/orders", icon: Package },
    { name: "Quản lý Sản phẩm", href: "/admin/products", icon: Package },
    { name: "Người dùng", href: "/admin/users", icon: Users },
    { name: "Cấu hình Bảo mật KMS", href: "/admin/settings", icon: Settings },
    { name: "Live Support", href: "/admin/chat", icon: MessageSquare },
  ];

  return (
    <div className="flex min-h-screen bg-[#020205] text-white">
      {/* Sidebar */}
      <aside className="w-64 border-r border-white/5 bg-[#05050A] hidden md:flex flex-col">
        <div className="h-20 flex items-center px-6 border-b border-white/5">
          <ShieldCheck className="w-6 h-6 text-indigo-400 mr-2" />
          <span className="text-xl font-bold tracking-tight text-white">Admin Panel</span>
        </div>
        
        <div className="flex-1 px-4 py-6 space-y-2">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link key={item.name} href={item.href}>
                <div className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
                  isActive 
                    ? "bg-indigo-500/10 text-indigo-400 font-medium" 
                    : "text-zinc-400 hover:text-white hover:bg-white/5"
                }`}>
                  <item.icon className="w-5 h-5" />
                  {item.name}
                </div>
              </Link>
            );
          })}
        </div>

        <div className="p-4 border-t border-white/5">
          <Link href="/">
            <div className="flex items-center gap-3 px-4 py-3 rounded-xl text-zinc-400 hover:text-white hover:bg-white/5 transition-all">
              <LogOut className="w-5 h-5" />
              Thoát (Về Cửa hàng)
            </div>
          </Link>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-h-screen relative overflow-hidden">
        {/* Background glow effects for admin */}
        <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-indigo-900/20 rounded-full blur-[100px] pointer-events-none" />
        <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-emerald-900/10 rounded-full blur-[100px] pointer-events-none" />
        
        {/* Header */}
        <header className="h-20 border-b border-white/5 bg-black/20 backdrop-blur-md flex items-center justify-between px-8 relative z-10">
          <h2 className="text-xl font-semibold">
            {navItems.find(item => item.href === pathname)?.name || "Dashboard"}
          </h2>
          <div className="flex items-center gap-4">
            <div className="flex flex-col items-end mr-2">
              <span className="text-sm font-bold text-white">{adminName || "Loading..."}</span>
              <span className="text-[10px] text-emerald-400 font-medium">Administrator</span>
            </div>
            <div className="w-10 h-10 rounded-full bg-gradient-to-r from-indigo-500 to-emerald-500 flex items-center justify-center font-bold uppercase">
              {adminName ? adminName.substring(0, 2) : "AD"}
            </div>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto p-8 relative z-10">
          {children}
        </main>
      </div>
    </div>
  );
}
