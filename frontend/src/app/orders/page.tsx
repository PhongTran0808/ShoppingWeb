"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ShieldCheck, ChevronLeft, Package, CheckCircle2, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function OrdersPage() {
  const [orders, setOrders] = useState<any[]>([]);

  useEffect(() => {
    const savedOrders = localStorage.getItem("vault_orders");
    if (savedOrders) {
      setOrders(JSON.parse(savedOrders).reverse()); // Hiện đơn mới nhất lên đầu
    }
  }, []);

  return (
    <div className="min-h-screen bg-[#05050A] text-white">
      <main className="container mx-auto px-6 pt-32 pb-12 max-w-4xl">
        <div className="flex items-center gap-4 mb-8">
          <div className="w-16 h-16 bg-indigo-500/10 rounded-2xl flex items-center justify-center border border-indigo-500/20">
            <Package className="w-8 h-8 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-3xl font-extrabold tracking-tight">Đơn hàng của tôi</h1>
            <p className="text-zinc-400 text-sm mt-1">
              Dữ liệu đơn hàng được bảo vệ bằng Field-Level Encryption.
            </p>
          </div>
        </div>

        {orders.length === 0 ? (
          <div className="text-center py-20 bg-white/[0.02] border border-white/5 rounded-3xl">
            <Package className="w-16 h-16 text-zinc-600 mx-auto mb-4" />
            <h2 className="text-xl font-bold mb-2">Bạn chưa có đơn hàng nào</h2>
            <p className="text-zinc-500 mb-6">Hãy khám phá các sản phẩm tuyệt vời của chúng tôi.</p>
            <Link href="/catalog">
              <Button className="bg-indigo-600 hover:bg-indigo-500 rounded-xl text-white px-8">
                Bắt đầu mua sắm
              </Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-6">
            {orders.map((order: any, idx: number) => (
              <div key={idx} className="bg-white/[0.03] border border-white/5 rounded-2xl p-6 shadow-xl">
                <div className="flex flex-wrap justify-between items-center border-b border-white/5 pb-4 mb-4 gap-4">
                  <div>
                    <span className="text-indigo-400 font-mono font-bold text-lg">{order.id}</span>
                    <span className="text-zinc-500 text-sm ml-4">{new Date(order.date).toLocaleString('vi-VN')}</span>
                  </div>
                  <div className="flex items-center gap-2 px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 rounded-full text-sm font-medium">
                    <CheckCircle2 className="w-4 h-4" /> {order.status || "Hoàn tất"}
                  </div>
                </div>

                <div className="space-y-4">
                  {order.items.map((item: any, i: number) => (
                    <div key={i} className="flex items-center gap-4">
                      <div className="w-16 h-16 bg-white/5 rounded-xl p-2 shrink-0">
                        <img src="/picture.png" alt={item.name} className="w-full h-full object-contain" />
                      </div>
                      <div className="flex-1">
                        <h4 className="font-semibold text-white">{item.name}</h4>
                        <p className="text-sm text-zinc-500">Số lượng: {item.quantity}</p>
                      </div>
                      <div className="text-emerald-400 font-medium">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.price * item.quantity)}
                      </div>
                    </div>
                  ))}
                </div>

                <div className="mt-6 pt-4 border-t border-white/5 flex flex-wrap justify-between items-start gap-4">
                  <div className="text-sm text-zinc-400">
                    <p><strong className="text-zinc-300">Người nhận:</strong> {order.shippingInfo?.fullName}</p>
                    <p><strong className="text-zinc-300">SĐT:</strong> {order.shippingInfo?.phone}</p>
                    <p><strong className="text-zinc-300">Địa chỉ:</strong> {order.shippingInfo?.address}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-zinc-400 mb-1">Tổng cộng</p>
                    <p className="text-2xl font-bold text-indigo-400">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.total)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
