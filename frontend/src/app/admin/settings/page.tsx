"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { CheckCircle2, ShieldCheck, CreditCard } from "lucide-react";

export default function AdminSettingsPage() {
  const [bankId, setBankId] = useState("970422");
  const [accountNumber, setAccountNumber] = useState("87343556868");
  const [accountName, setAccountName] = useState("HUYNH THANH TU");
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    const config = localStorage.getItem("vault_payment_config");
    if (config) {
      const parsed = JSON.parse(config);
      if (parsed.bankId) setBankId(parsed.bankId);
      if (parsed.accountNumber) setAccountNumber(parsed.accountNumber);
      if (parsed.accountName) setAccountName(parsed.accountName);
    }
  }, []);

  const handleSave = () => {
    localStorage.setItem("vault_payment_config", JSON.stringify({
      bankId, accountNumber, accountName
    }));
    setSaved(true);
    setTimeout(() => setSaved(false), 2000);
  };

  return (
    <div className="space-y-8 relative">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold flex items-center gap-2">
            <ShieldCheck className="w-6 h-6 text-indigo-400" /> Cấu hình Hệ thống
          </h2>
          <p className="text-zinc-400">Quản trị kết nối HashiCorp Vault và thiết lập hệ thống.</p>
        </div>
      </div>

      {/* KMS Security Config (Existing) */}
      <div className="bg-white/[0.03] border border-white/5 rounded-2xl p-8 backdrop-blur-sm space-y-6">
        <h3 className="text-xl font-bold text-zinc-300 mb-2">Bảo mật KMS (Vault)</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="p-4 rounded-xl border border-white/10 bg-black/20">
            <span className="block text-zinc-500 text-sm mb-1">Vault Address</span>
            <span className="font-mono text-emerald-400">http://127.0.0.1:8200</span>
          </div>
          <div className="p-4 rounded-xl border border-white/10 bg-black/20">
            <span className="block text-zinc-500 text-sm mb-1">Active Transit Engine</span>
            <span className="font-mono text-emerald-400">ecommerce-orders-key</span>
          </div>
          <div className="p-4 rounded-xl border border-white/10 bg-black/20 md:col-span-2">
            <span className="block text-zinc-500 text-sm mb-1">Trạng thái Database</span>
            <span className="font-mono text-emerald-400">Connected (Aiven MySQL)</span>
          </div>
        </div>
      </div>

      {/* Payment Settings Config (New) */}
      <div className="bg-white/[0.03] border border-white/5 rounded-2xl p-8 backdrop-blur-sm space-y-6 relative">
        <h3 className="text-xl font-bold text-zinc-300 mb-2 flex items-center gap-2">
          <CreditCard className="w-5 h-5 text-indigo-400" /> Thiết lập Thanh toán VietQR
        </h3>
        <p className="text-sm text-zinc-400 mb-4">
          Cấu hình tài khoản ngân hàng dùng để tạo mã QR thanh toán tự động cho đơn hàng.
        </p>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <label className="text-sm text-zinc-400">Mã Ngân hàng (BIN / Bank ID)</label>
            <input
              type="text"
              value={bankId}
              onChange={(e) => setBankId(e.target.value)}
              className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
              placeholder="VD: 970422 (MB Bank)"
            />
          </div>
          <div className="space-y-2">
            <label className="text-sm text-zinc-400">Số tài khoản</label>
            <input
              type="text"
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
              placeholder="VD: 87343556868"
            />
          </div>
          <div className="space-y-2 md:col-span-2">
            <label className="text-sm text-zinc-400">Tên chủ tài khoản (Không dấu)</label>
            <input
              type="text"
              value={accountName}
              onChange={(e) => setAccountName(e.target.value)}
              className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500 uppercase"
              placeholder="VD: HUYNH THANH TU"
            />
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <Button 
            onClick={handleSave}
            className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl px-8"
          >
            {saved ? (
              <span className="flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5" /> Đã lưu
              </span>
            ) : "Lưu Cấu hình"}
          </Button>
        </div>
      </div>
    </div>
  );
}
