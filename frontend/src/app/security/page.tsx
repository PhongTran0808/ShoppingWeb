import Link from "next/link";
import { ShieldCheck, ChevronLeft, Fingerprint, KeyRound, Database } from "lucide-react";

export default function SecurityPage() {
  return (
    <div className="min-h-screen bg-[#05050A] text-white">
      <main className="container mx-auto px-6 pt-32 pb-12 max-w-5xl">
        <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-8 text-center text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 to-indigo-400">
          Kiến trúc Mật mã Ứng dụng
        </h1>
        <p className="text-zinc-400 text-lg text-center mb-16">
          Nền tảng của chúng tôi áp dụng những tiêu chuẩn bảo mật nghiêm ngặt nhất để bảo vệ giao dịch của bạn.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-md">
            <KeyRound className="w-10 h-10 text-indigo-400 mb-6" />
            <h3 className="text-2xl font-bold mb-4">HashiCorp Vault KMS</h3>
            <p className="text-zinc-400 leading-relaxed">
              Sử dụng Transit Engine của Vault để quản lý toàn bộ vòng đời của khóa mã hóa. Hệ thống mã hóa (Encryption-as-a-Service) đảm bảo mã nguồn Spring Boot không bao giờ lưu trữ Plain-text Key.
            </p>
          </div>

          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-md">
            <Database className="w-10 h-10 text-emerald-400 mb-6" />
            <h3 className="text-2xl font-bold mb-4">Field-Level Encryption</h3>
            <p className="text-zinc-400 leading-relaxed">
              Thuật toán AES-256-GCM được sử dụng để mã hóa các trường nhạy cảm như Tên, Số điện thoại và Địa chỉ giao hàng trước khi lưu vào Cơ sở dữ liệu Aiven MySQL.
            </p>
          </div>

          <div className="p-8 rounded-3xl bg-white/[0.03] border border-white/10 backdrop-blur-md md:col-span-2">
            <Fingerprint className="w-10 h-10 text-rose-400 mb-6" />
            <h3 className="text-2xl font-bold mb-4">HMAC Digital Signatures & Blind Index</h3>
            <p className="text-zinc-400 leading-relaxed">
              Mọi gói tin thanh toán từ Gateway tới Payment Service đều được gắn chữ ký số bằng HMAC SHA-256 để chống can thiệp (Tampering). Ngoài ra, cơ sở dữ liệu áp dụng kỹ thuật Blind Index với HMAC SHA3-512 (C++) để cho phép truy vấn dữ liệu đã mã hóa mà không làm lộ thông tin gốc.
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
