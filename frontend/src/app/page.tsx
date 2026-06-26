"use client";

import { ShieldCheck, Lock, CreditCard, ChevronRight } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function Home() {
  return (
    <div className="min-h-screen bg-[#05050A] text-white selection:bg-indigo-500/30 overflow-hidden relative">
      {/* Background gradients */}
      <div className="fixed inset-0 z-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-[20%] -left-[10%] w-[50%] h-[50%] rounded-full bg-indigo-900/40 blur-[120px]" />
        <div className="absolute top-[60%] -right-[10%] w-[40%] h-[60%] rounded-full bg-emerald-900/20 blur-[120px]" />
      </div>

      {/* Hero Section */}
      <main className="relative z-10 pt-40 pb-20 container mx-auto px-6">
        <div className="max-w-5xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm font-medium mb-8">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            Hệ thống Mật mã ứng dụng đã kích hoạt 100%
          </div>

          <h1 className="text-6xl md:text-8xl font-extrabold tracking-tight mb-8">
            Trải nghiệm Mua sắm <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-purple-400 to-emerald-400">
              Tuyệt đối Bảo mật.
            </span>
          </h1>

          <p className="text-lg md:text-xl text-zinc-400 mb-12 max-w-2xl mx-auto leading-relaxed">
            Nền tảng E-commerce đầu tiên tích hợp HashiCorp Vault. Dữ liệu cá nhân, địa chỉ và thông tin thanh toán của bạn được mã hóa cấp độ quân sự.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link href="/catalog">
              <Button size="lg" className="h-14 px-8 text-lg bg-indigo-600 hover:bg-indigo-500 text-white rounded-full transition-all hover:scale-105 shadow-[0_0_30px_rgba(79,70,229,0.4)]">
                Khám phá Sản phẩm <ChevronRight className="ml-2 w-5 h-5" />
              </Button>
            </Link>
            <Link href="/security">
              <Button size="lg" variant="outline" className="h-14 px-8 text-lg rounded-full border-white/10 hover:bg-white/5 bg-white/5 backdrop-blur-sm text-white transition-all">
                Tìm hiểu Kiến trúc
              </Button>
            </Link>
          </div>
        </div>

        {/* Feature Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-32">
          {/* Card 1 */}
          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-xl hover:bg-white/[0.06] transition-all duration-300 group hover:-translate-y-2">
            <div className="w-14 h-14 rounded-2xl bg-indigo-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
              <Lock className="w-7 h-7 text-indigo-400" />
            </div>
            <h3 className="text-xl font-bold mb-3 text-zinc-100">Mã hóa Cấp độ Trường (FLE)</h3>
            <p className="text-zinc-400 leading-relaxed">
              Toàn bộ địa chỉ và số điện thoại được mã hóa trực tiếp trên cơ sở dữ liệu bằng thuật toán AES-256 GCM cực kỳ an toàn.
            </p>
          </div>

          {/* Card 2 */}
          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-xl hover:bg-white/[0.06] transition-all duration-300 group hover:-translate-y-2">
            <div className="w-14 h-14 rounded-2xl bg-emerald-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
              <ShieldCheck className="w-7 h-7 text-emerald-400" />
            </div>
            <h3 className="text-xl font-bold mb-3 text-zinc-100">HashiCorp Vault Transit</h3>
            <p className="text-zinc-400 leading-relaxed">
              Quản lý vòng đời khóa mã hóa tập trung bằng Vault. Không có bất kỳ chìa khóa nào được lưu trữ trong mã nguồn hay RAM lâu dài.
            </p>
          </div>

          {/* Card 3 */}
          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-xl hover:bg-white/[0.06] transition-all duration-300 group hover:-translate-y-2">
            <div className="w-14 h-14 rounded-2xl bg-rose-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform duration-300">
              <CreditCard className="w-7 h-7 text-rose-400" />
            </div>
            <h3 className="text-xl font-bold mb-3 text-zinc-100">Chữ ký số HMAC</h3>
            <p className="text-zinc-400 leading-relaxed">
              Mọi giao dịch thanh toán đều được ký bằng thuật toán HMAC SHA-256 để chống giả mạo dữ liệu trên đường truyền mạng.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
