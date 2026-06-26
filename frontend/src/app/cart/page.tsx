"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ShieldCheck, ShoppingCart, Trash2, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function CartPage() {
  const [cartItems, setCartItems] = useState<any[]>([]);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    // Đọc giỏ hàng từ localStorage
    const savedCart = localStorage.getItem("vault_cart");
    if (savedCart) {
      const parsedCart = JSON.parse(savedCart);
      setCartItems(parsedCart);
      
      // Tính tổng tiền
      const sum = parsedCart.reduce((acc: number, item: any) => acc + (item.price * item.quantity), 0);
      setTotal(sum);
    }
  }, []);

  const removeItem = (id: number) => {
    const newCart = cartItems.filter(item => item.id !== id);
    setCartItems(newCart);
    localStorage.setItem("vault_cart", JSON.stringify(newCart));
    
    const sum = newCart.reduce((acc: number, item: any) => acc + (item.price * item.quantity), 0);
    setTotal(sum);
  };

  const updateQuantity = (id: number, newQty: number) => {
    if (newQty < 1) return; // Không cho giảm xuống âm hoặc 0
    const newCart = cartItems.map(item => 
      item.id === id ? { ...item, quantity: newQty } : item
    );
    setCartItems(newCart);
    localStorage.setItem("vault_cart", JSON.stringify(newCart));
    
    const sum = newCart.reduce((acc: number, item: any) => acc + (item.price * item.quantity), 0);
    setTotal(sum);
  };

  return (
    <div className="min-h-screen bg-[#05050A] text-white">
      <main className="container mx-auto px-6 pt-32 pb-12 max-w-5xl">
        <h1 className="text-4xl font-extrabold tracking-tight mb-8">Giỏ hàng của bạn</h1>

        {cartItems.length === 0 ? (
          <div className="bg-white/[0.02] border border-white/5 rounded-3xl p-12 text-center">
            <div className="w-24 h-24 bg-white/5 rounded-full flex items-center justify-center mx-auto mb-6">
              <ShoppingCart className="w-12 h-12 text-zinc-500" />
            </div>
            <h2 className="text-2xl font-bold mb-4">Giỏ hàng trống</h2>
            <p className="text-zinc-400 mb-8 max-w-md mx-auto">
              Bạn chưa chọn sản phẩm nào. Hãy quay lại cửa hàng để khám phá các sản phẩm công nghệ tuyệt vời được bảo vệ bởi Mật mã ứng dụng.
            </p>
            <Link href="/catalog">
              <Button className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl h-12 px-8">
                Khám phá Sản phẩm
              </Button>
            </Link>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Cột danh sách sản phẩm */}
            <div className="lg:col-span-2 space-y-4">
              {cartItems.map((item) => (
                <div key={item.id} className="bg-white/[0.03] border border-white/5 rounded-2xl p-4 flex gap-4 items-center">
                  <div className="w-24 h-24 bg-white/5 rounded-xl flex items-center justify-center shrink-0 p-2">
                    <img 
                      src={item.imageUrl || "/picture.png"} 
                      alt={item.name} 
                      className="w-full h-full object-contain"
                    />
                  </div>
                  <div className="flex-1">
                    <h3 className="font-bold text-lg">{item.name}</h3>
                    <p className="text-sm text-zinc-500">{item.category}</p>
                    <div className="text-emerald-400 font-medium mt-1">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.price)}
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="flex items-center bg-black/40 rounded-lg border border-white/5 h-10 overflow-hidden">
                      <button 
                        onClick={() => updateQuantity(item.id, item.quantity - 1)}
                        className="px-3 hover:bg-white/10 h-full flex items-center justify-center transition-colors text-zinc-400 hover:text-white"
                      >
                        -
                      </button>
                      <div className="px-4 text-sm font-medium w-12 text-center">
                        {item.quantity}
                      </div>
                      <button 
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        className="px-3 hover:bg-white/10 h-full flex items-center justify-center transition-colors text-zinc-400 hover:text-white"
                      >
                        +
                      </button>
                    </div>
                    <Button 
                      variant="ghost" 
                      size="icon" 
                      onClick={() => removeItem(item.id)}
                      className="text-red-400 hover:text-red-300 hover:bg-red-400/10"
                    >
                      <Trash2 className="w-5 h-5" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>

            {/* Cột Thanh toán */}
            <div className="bg-white/[0.03] border border-white/5 rounded-2xl p-6 h-fit sticky top-24">
              <h3 className="text-xl font-bold mb-6">Tóm tắt đơn hàng</h3>
              
              <div className="space-y-4 mb-6">
                <div className="flex justify-between text-zinc-400">
                  <span>Tạm tính ({cartItems.length} sản phẩm)</span>
                  <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total)}</span>
                </div>
                <div className="flex justify-between text-zinc-400">
                  <span>Phí vận chuyển</span>
                  <span>Miễn phí</span>
                </div>
                <div className="h-px bg-white/10 w-full my-4" />
                <div className="flex justify-between text-xl font-bold text-white">
                  <span>Tổng cộng</span>
                  <span className="text-indigo-400">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total)}</span>
                </div>
              </div>

              <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-4 mb-6 flex items-start gap-3">
                <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                <p className="text-xs text-emerald-400/90 leading-relaxed">
                  Thông tin thanh toán và địa chỉ của bạn sẽ được mã hóa Field-Level Encryption bằng AES-256 GCM trước khi lưu vào Database.
                </p>
              </div>

              {/* Logic: Kiểm tra Token để bắt buộc đăng nhập */}
              <Button 
                onClick={() => {
                  const token = localStorage.getItem("vault_token");
                  if (token) {
                    window.location.href = "/checkout"; // Trang thanh toán (sẽ làm ở Phase sau)
                  } else {
                    window.location.href = "/login"; // Chưa đăng nhập thì bắt đăng nhập
                  }
                }}
                className="w-full h-14 bg-indigo-600 hover:bg-indigo-500 text-lg rounded-xl shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all"
              >
                Tiến hành Thanh toán <ArrowRight className="w-5 h-5 ml-2" />
              </Button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
