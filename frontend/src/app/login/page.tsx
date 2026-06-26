"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ShieldCheck, Lock, User, ChevronLeft, ArrowRight, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { generateSecureSessionToken, sha256 } from "@/utils/crypto";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authStatus, setAuthStatus] = useState<"idle" | "authenticating" | "success">("idle");
  const [errorMsg, setErrorMsg] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setErrorMsg("Vui lòng nhập tài khoản và mật khẩu!");
      return;
    }

    try {
      setAuthStatus("authenticating");
      setErrorMsg("");

      const res = await fetch("http://localhost:8081/api/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });

      const data = await res.json();

      if (res.ok && data.success) {
        localStorage.setItem("vault_token", data.token);
        localStorage.setItem("vault_user", JSON.stringify(data.user));
        
        setAuthStatus("success");
        
        setTimeout(() => {
          const role = data.user.role || "";
          window.location.href = role === "ROLE_ADMIN" ? "/admin" : "/catalog";
        }, 1500);
      } else {
        setErrorMsg(data.message || "Đăng nhập thất bại!");
        setAuthStatus("idle");
      }
    } catch (err: any) {
      console.error("Lỗi đăng nhập:", err);
      setErrorMsg("Lỗi kết nối đến Backend Server!");
      setAuthStatus("idle");
    }
  };

  return (
    <div className="min-h-screen bg-[#020205] text-white flex flex-col relative overflow-hidden">
      {/* Background glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-indigo-600/20 rounded-full blur-[120px] pointer-events-none" />

      <nav className="p-6 relative z-10">
        <Link href="/" className="flex items-center gap-2 text-zinc-400 hover:text-white transition-colors w-fit">
          <ChevronLeft className="w-5 h-5" /> Trở về
        </Link>
      </nav>

      <main className="flex-1 flex flex-col items-center justify-center p-6 relative z-10">
        <div className="w-full max-w-md bg-white/[0.02] border border-white/5 rounded-3xl p-8 backdrop-blur-xl flex flex-col items-center shadow-2xl">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-emerald-500 p-[1px] mb-8">
            <div className="w-full h-full bg-black rounded-2xl flex items-center justify-center">
              <ShieldCheck className="w-8 h-8 text-emerald-400" />
            </div>
          </div>

          <h1 className="text-3xl font-bold tracking-tight mb-2 text-center">Đăng nhập Quản trị & Người dùng</h1>
          <p className="text-zinc-400 mb-8 text-center text-sm">
            Kết nối an toàn qua giao thức mã hóa AES-256.
          </p>

          <div className="w-full mt-6">
            {authStatus === "idle" && (
              <div className="w-full">
                {errorMsg && (
                  <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm text-center font-medium shadow-lg animate-pulse">
                    {errorMsg}
                  </div>
                )}
                <form onSubmit={handleLogin} className="space-y-5">
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-zinc-400 ml-1">Tài khoản</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                        <User className="w-5 h-5 text-zinc-500" />
                      </div>
                      <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                        placeholder="Nhập tên đăng nhập"
                        required
                        onKeyDown={(e) => { if (e.key === 'Enter') handleLogin(e); }}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-zinc-400 ml-1">Mật khẩu</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                        <Lock className="w-5 h-5 text-zinc-500" />
                      </div>
                      <input
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                        placeholder="••••••••"
                        required
                        onKeyDown={(e) => { if (e.key === 'Enter') handleLogin(e); }}
                      />
                    </div>
                  </div>

                  <button 
                    type="submit"
                    className="w-full h-12 bg-indigo-600 hover:bg-indigo-500 text-md text-white font-medium rounded-xl mt-4 shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all flex items-center justify-center gap-2"
                  >
                    Đăng nhập <ArrowRight className="w-4 h-4" />
                  </button>
                  
                  <div className="mt-6 flex items-center justify-between text-sm text-zinc-500 w-full">
                    <div className="h-[1px] flex-1 bg-white/10" />
                    <span className="px-4">Chưa có tài khoản?</span>
                    <div className="h-[1px] flex-1 bg-white/10" />
                  </div>
                  
                  <Link href="/register" className="w-full">
                    <button type="button" className="w-full h-12 mt-4 text-zinc-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-xl font-medium transition-colors">
                      Tạo tài khoản mới
                    </button>
                  </Link>
                </form>
              </div>
            )}

            {authStatus === "authenticating" && (
              <div className="flex flex-col items-center py-8">
                <div className="w-12 h-12 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin mb-4" />
                <p className="text-indigo-400 font-medium animate-pulse">
                  Đang xác thực thông tin...
                </p>
              </div>
            )}

            {authStatus === "success" && (
              <div className="flex flex-col items-center py-8">
                <div className="w-20 h-20 rounded-full bg-emerald-500/20 flex items-center justify-center mb-6">
                  <CheckCircle2 className="w-10 h-10 text-emerald-400" />
                </div>
                <p className="text-emerald-400 font-bold text-xl">
                  Đăng nhập thành công!
                </p>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
