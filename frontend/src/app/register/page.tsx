"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { ShieldCheck, Lock, User, ChevronLeft, ArrowRight, CheckCircle2, Mail } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { generateSecureSessionToken, sha256 } from "@/utils/crypto";

export default function RegisterPage() {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [authStatus, setAuthStatus] = useState<"idle" | "authenticating" | "success">("idle");
  const [errorMsg, setErrorMsg] = useState("");
  const router = useRouter();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !email || !password) {
      setErrorMsg("Vui lòng điền đầy đủ thông tin!");
      return;
    }
    setAuthStatus("authenticating");
    setErrorMsg("");

    try {
      // 1. Lấy thông tin user hiện tại từ LocalStorage (Giả lập Database)
      const existingUsers = JSON.parse(localStorage.getItem("vault_db_users") || "[]");
      
      if (existingUsers.find((u: any) => u.username === username)) {
        setErrorMsg("Tên đăng nhập đã tồn tại!");
        setAuthStatus("idle");
        return;
      }

      // 2. Tạo Session Token BẰNG MẬT MÃ HỌC SHA-256 (Kế hoạch Mới)
      const sessionToken = await generateSecureSessionToken(username, password);

      // 3. Mã hóa Master Password (lưu Hash vào "DB")
      const passwordHash = await sha256(password);

      // 4. Lưu User vào "Database"
      const newUser = {
        username,
        email,
        passwordHash,
        token: sessionToken,
        role: "ROLE_USER"
      };
      
      existingUsers.push(newUser);
      localStorage.setItem("vault_db_users", JSON.stringify(existingUsers));
      localStorage.setItem("vault_token", sessionToken);
      localStorage.setItem("vault_user", JSON.stringify({ username, role: "ROLE_USER" }));

      // 5. Chuyển hướng
      setAuthStatus("success");
      setTimeout(() => {
        router.push("/catalog");
      }, 1000);
    } catch (error) {
      console.error("Lỗi mật mã học:", error);
      setErrorMsg("Trình duyệt không hỗ trợ Web Crypto API!");
      setAuthStatus("idle");
    }
  };

  return (
    <div className="min-h-screen bg-[#020205] text-white flex flex-col relative overflow-hidden">
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-indigo-600/20 rounded-full blur-[120px] pointer-events-none" />

      <nav className="p-6 relative z-10">
        <Link href="/login" className="flex items-center gap-2 text-zinc-400 hover:text-white transition-colors w-fit">
          <ChevronLeft className="w-5 h-5" /> Trở về Đăng nhập
        </Link>
      </nav>

      <main className="flex-1 flex flex-col items-center justify-center p-6 relative z-10">
        <div className="w-full max-w-md bg-white/[0.02] border border-white/5 rounded-3xl p-8 backdrop-blur-xl flex flex-col items-center shadow-2xl">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-indigo-500 to-emerald-500 p-[1px] mb-8">
            <div className="w-full h-full bg-black rounded-2xl flex items-center justify-center">
              <ShieldCheck className="w-8 h-8 text-emerald-400" />
            </div>
          </div>

          <h1 className="text-3xl font-bold tracking-tight mb-2 text-center">Tạo Tài khoản</h1>
          <p className="text-zinc-400 mb-8 text-center text-sm">
            Tất cả thông tin được bảo vệ bởi Mật mã ứng dụng.
          </p>

          <div className="w-full mt-6">
            {authStatus === "idle" && (
              <div className="w-full">
                {errorMsg && (
                  <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm text-center font-medium shadow-lg animate-pulse">
                    {errorMsg}
                  </div>
                )}
                <form onSubmit={handleRegister} className="space-y-4">
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-zinc-400 ml-1">Tên đăng nhập (Username)</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                        <User className="w-5 h-5 text-zinc-500" />
                      </div>
                      <input
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
                        placeholder="Nhập tên đăng nhập"
                        required
                        onKeyDown={(e) => { if (e.key === 'Enter') handleRegister(e); }}
                      />
                    </div>
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-zinc-400 ml-1">Email</label>
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                        <Mail className="w-5 h-5 text-zinc-500" />
                      </div>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
                        placeholder="email@example.com"
                        required
                        onKeyDown={(e) => { if (e.key === 'Enter') handleRegister(e); }}
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
                        className="w-full bg-black/50 border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
                        placeholder="••••••••"
                        required
                        minLength={6}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleRegister(e); }}
                      />
                    </div>
                  </div>

                  <button 
                    type="submit"
                    className="w-full h-12 bg-indigo-600 hover:bg-indigo-500 text-md text-white font-medium rounded-xl mt-6 shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all flex items-center justify-center gap-2"
                  >
                    Đăng ký <ArrowRight className="w-4 h-4" />
                  </button>
                </form>
              </div>
            )}

            {authStatus === "authenticating" && (
              <div className="flex flex-col items-center py-8">
                <div className="w-12 h-12 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin mb-4" />
                <p className="text-indigo-400 font-medium animate-pulse">
                  Đang khởi tạo tài khoản bảo mật...
                </p>
              </div>
            )}

            {authStatus === "success" && (
              <div className="flex flex-col items-center py-8">
                <div className="w-20 h-20 rounded-full bg-emerald-500/20 flex items-center justify-center mb-6">
                  <CheckCircle2 className="w-10 h-10 text-emerald-400" />
                </div>
                <p className="text-emerald-400 font-bold text-xl">
                  Đăng ký thành công!
                </p>
              </div>
            )}
          </div>

          {authStatus === "idle" && (
            <>
              <div className="mt-8 flex items-center justify-between text-sm text-zinc-500 w-full">
                <div className="h-[1px] flex-1 bg-white/10" />
                <span className="px-4">Đã có tài khoản?</span>
                <div className="h-[1px] flex-1 bg-white/10" />
              </div>
              
              <Link href="/login" className="w-full">
                <button type="button" className="w-full h-12 mt-4 text-zinc-400 hover:text-white bg-white/5 hover:bg-white/10 rounded-xl font-medium transition-colors">
                  Đăng nhập hệ thống
                </button>
              </Link>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
