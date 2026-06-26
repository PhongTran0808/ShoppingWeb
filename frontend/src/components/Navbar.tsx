"use client";

import { ShieldCheck, User } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useEffect, useState } from "react";

export default function Navbar() {
  const pathname = usePathname();
  const [user, setUser] = useState<{username: string, role: string} | null>(null);

  useEffect(() => {
    const savedUser = localStorage.getItem("vault_user");
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("vault_token");
    localStorage.removeItem("vault_user");
    setUser(null);
    window.location.href = "/";
  };

  // Helper function để render link với hiệu ứng phát sáng
  const NavLink = ({ href, children }: { href: string, children: React.ReactNode }) => {
    const isActive = pathname === href || pathname.startsWith(href + "/");
    return (
      <Link 
        href={href} 
        className={`transition-all duration-300 ${
          isActive 
            ? "text-emerald-400 drop-shadow-[0_0_10px_rgba(52,211,153,0.8)] font-bold scale-105" 
            : "hover:text-emerald-300 hover:drop-shadow-[0_0_5px_rgba(52,211,153,0.5)] text-zinc-400"
        }`}
      >
        {children}
      </Link>
    );
  };

  // Ẩn Navbar trên trang Admin vì Admin đã có Sidebar riêng
  if (pathname.startsWith("/admin")) return null;

  return (
    <nav className="fixed top-0 w-full z-50 border-b border-white/5 bg-black/40 backdrop-blur-xl">
      <div className="container mx-auto px-6 h-20 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link href="/" className="flex items-center gap-3 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-emerald-500 p-[1px] group-hover:shadow-[0_0_15px_rgba(52,211,153,0.5)] transition-all">
              <div className="w-full h-full bg-black rounded-xl flex items-center justify-center">
                <ShieldCheck className="w-6 h-6 text-emerald-400" />
              </div>
            </div>
            <span className="text-xl font-bold tracking-tight text-white group-hover:text-emerald-400 transition-colors">
              VaultCommerce
            </span>
          </Link>
        </div>
        <div className="hidden md:flex items-center gap-8 text-sm font-medium">
          <NavLink href="/catalog">Sản phẩm</NavLink>
          <NavLink href="/cart">Giỏ hàng</NavLink>
          <NavLink href="/orders">Đơn hàng của tôi</NavLink>
          <NavLink href="/security">Kiến trúc Mật mã</NavLink>
        </div>
        <div className="flex items-center gap-3">
          {user ? (
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 bg-white/5 border border-white/10 px-4 py-2 rounded-full">
                <User className="w-4 h-4 text-emerald-400" />
                <span className="text-sm font-medium">{user.username}</span>
              </div>
              {user.role === "ROLE_ADMIN" && (
                <Link href="/admin">
                  <Button variant="ghost" className="text-indigo-400 hover:text-indigo-300 hover:bg-indigo-500/10">
                    Dashboard
                  </Button>
                </Link>
              )}
              <Button variant="ghost" onClick={handleLogout} className="text-rose-400 hover:text-rose-300 hover:bg-rose-500/10">
                Đăng xuất
              </Button>
            </div>
          ) : (
            <>
              <Link href="/login">
                <Button variant="ghost" className="text-zinc-300 hover:text-white hover:bg-white/10 hidden sm:flex">
                  Đăng nhập
                </Button>
              </Link>
              <Link href="/register">
                <Button variant="outline" className="border-white/20 hover:bg-white/10 text-white bg-transparent hidden sm:flex">
                  Đăng ký
                </Button>
              </Link>
            </>
          )}
          <Link href="/catalog">
            <Button className="bg-indigo-600 hover:bg-indigo-500 text-white border-0 shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all ml-2">
              Mua sắm ngay
            </Button>
          </Link>
        </div>
      </div>
    </nav>
  );
}
