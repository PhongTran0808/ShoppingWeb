"use client";

import { useEffect, useState } from "react";
import { Package, Search, CheckCircle2, ChevronRight } from "lucide-react";
import { motion } from "framer-motion";

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<any[]>([]);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    // Lấy dữ liệu đơn hàng từ "Database" (LocalStorage)
    const savedOrders = localStorage.getItem("vault_orders");
    if (savedOrders) {
      setOrders(JSON.parse(savedOrders).reverse());
    }
  }, []);

  const filteredOrders = orders.filter((order) => {
    const term = searchTerm.toLowerCase();
    return (
      order.id.toLowerCase().includes(term) ||
      order.shippingInfo?.fullName?.toLowerCase().includes(term) ||
      order.shippingInfo?.phone?.includes(term)
    );
  });

  const handleVerifyOrder = (orderId: string) => {
    const updatedOrders = orders.map(order => {
      if (order.id === orderId) {
        return {
          ...order,
          status: "SUCCESS",
          admin_verified: true
        };
      }
      return order;
    });
    setOrders(updatedOrders);
    // Lưu ngược lại mảng theo thứ tự cũ (vì display đang reverse)
    localStorage.setItem("vault_orders", JSON.stringify([...updatedOrders].reverse()));
  };

  const totalRevenue = orders.reduce((sum, order) => sum + order.total, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold flex items-center gap-2 text-white">
            <Package className="w-6 h-6 text-indigo-400" /> Quản lý Đơn hàng
          </h2>
          <p className="text-zinc-400 mt-1 text-sm">
            Theo dõi, tra cứu và xử lý các đơn hàng được mã hóa.
          </p>
        </div>

        {/* Thống kê nhanh */}
        <div className="flex items-center gap-6 bg-white/[0.03] border border-white/5 rounded-2xl px-6 py-3">
          <div>
            <p className="text-xs text-zinc-500 mb-1">Tổng đơn</p>
            <p className="text-xl font-bold text-white">{orders.length}</p>
          </div>
          <div className="w-px h-8 bg-white/10" />
          <div>
            <p className="text-xs text-zinc-500 mb-1">Doanh thu tạm tính</p>
            <p className="text-xl font-bold text-emerald-400">
              {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(totalRevenue)}
            </p>
          </div>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative max-w-md">
        <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
          <Search className="w-5 h-5 text-zinc-500" />
        </div>
        <input 
          type="text" 
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full bg-white/[0.03] border border-white/10 rounded-xl py-3 pl-12 pr-4 text-white placeholder-zinc-500 focus:outline-none focus:border-indigo-500"
          placeholder="Tra cứu theo Mã đơn, Tên hoặc SĐT..."
        />
      </div>

      {/* Orders List */}
      <div className="space-y-4">
        {filteredOrders.length === 0 ? (
          <div className="text-center py-12 text-zinc-500 bg-white/[0.02] border border-white/5 rounded-2xl">
            Không tìm thấy đơn hàng nào phù hợp với "{searchTerm}".
          </div>
        ) : (
          filteredOrders.map((order, idx) => (
            <motion.div 
              key={order.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.05 }}
              className="bg-white/[0.03] border border-white/5 rounded-2xl p-6 hover:bg-white/[0.05] transition-colors group cursor-pointer"
            >
              <div className="flex flex-col lg:flex-row justify-between gap-6">
                
                {/* Info Left */}
                <div className="flex-1 space-y-4">
                  <div className="flex items-center gap-3">
                    <span className="text-lg font-bold text-indigo-400 font-mono">{order.id}</span>
                    <span className={`px-3 py-1 border rounded-full text-xs font-semibold flex items-center gap-1 ${
                      order.status === "SUCCESS" 
                        ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                        : "bg-amber-500/10 text-amber-400 border-amber-500/20"
                    }`}>
                      <CheckCircle2 className="w-3 h-3" /> {order.status === "SUCCESS" ? "Đã xác nhận" : "Chờ xác nhận"}
                    </span>
                    <span className="text-sm text-zinc-500">
                      {new Date(order.date).toLocaleString('vi-VN')}
                    </span>
                  </div>

                  <div className="bg-black/20 rounded-xl p-4 border border-white/5">
                    <h4 className="text-sm font-semibold text-zinc-300 mb-2">Thông tin giao hàng (Decrypted)</h4>
                    <p className="text-sm text-zinc-400"><strong className="text-zinc-200">Người nhận:</strong> {order.shippingInfo?.fullName}</p>
                    <p className="text-sm text-zinc-400"><strong className="text-zinc-200">Điện thoại:</strong> {order.shippingInfo?.phone}</p>
                    <p className="text-sm text-zinc-400"><strong className="text-zinc-200">Địa chỉ:</strong> {order.shippingInfo?.address}</p>
                  </div>
                </div>

                {/* Items Right */}
                <div className="flex-1 lg:max-w-md flex flex-col justify-between">
                  <div className="space-y-3 mb-4">
                    <h4 className="text-sm font-semibold text-zinc-300">Sản phẩm ({order.items.length})</h4>
                    <div className="max-h-32 overflow-y-auto pr-2 custom-scrollbar space-y-2">
                      {order.items.map((item: any, i: number) => (
                        <div key={i} className="flex justify-between items-center text-sm">
                          <span className="text-zinc-400 line-clamp-1 flex-1 pr-4">{item.quantity}x {item.name}</span>
                          <span className="text-zinc-300 whitespace-nowrap">
                            {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.price * item.quantity)}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="flex flex-col gap-4 pt-4 border-t border-white/5 mt-auto">
                    <div className="flex items-center justify-between">
                      <span className="text-zinc-400 text-sm">Tổng thanh toán:</span>
                      <span className="text-xl font-bold text-white">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(order.total)}
                      </span>
                    </div>
                    {order.status === "PENDING_VERIFICATION" && (
                      <button 
                        onClick={() => handleVerifyOrder(order.id)}
                        className="w-full py-2 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-lg flex items-center justify-center gap-2 transition-colors"
                      >
                        <CheckCircle2 className="w-4 h-4" /> Đối soát VietQR thành công
                      </button>
                    )}
                  </div>
                </div>
              </div>
            </motion.div>
          ))
        )}
      </div>
    </div>
  );
}
