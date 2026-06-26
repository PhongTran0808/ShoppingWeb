"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ChevronLeft, ShieldCheck, ShoppingCart, CheckCircle2, Truck, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";
import { MOCK_PRODUCTS } from "@/lib/mockData";
import { motion, AnimatePresence } from "framer-motion";

export default function ProductDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  
  const [product, setProduct] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [cartCount, setCartCount] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [toast, setToast] = useState<{ message: string; show: boolean }>({ message: "", show: false });

  useEffect(() => {
    // Đọc số lượng giỏ hàng
    const savedCart = localStorage.getItem("vault_cart");
    if (savedCart) {
      setCartCount(JSON.parse(savedCart).length);
    }

    // Lấy thông tin sản phẩm
    fetch(`http://localhost:8081/api/products/${id}`)
      .then(res => res.json())
      .then(data => {
        if (data && data.id) {
          setProduct(data);
        } else {
          // Fallback to MOCK
          const found = MOCK_PRODUCTS.find(p => p.id === Number(id));
          if (found) setProduct(found);
          else router.push("/catalog");
        }
        setLoading(false);
      })
      .catch(() => {
        // Fallback to MOCK
        const found = MOCK_PRODUCTS.find(p => p.id === Number(id));
        if (found) setProduct(found);
        else router.push("/catalog");
        setLoading(false);
      });
  }, [id, router]);

  const addToCart = () => {
    if (!product) return;
    
    const savedCart = localStorage.getItem("vault_cart");
    let currentCart = savedCart ? JSON.parse(savedCart) : [];
    
    const existing = currentCart.find((item: any) => item.id === product.id);
    if (existing) {
      existing.quantity += quantity;
    } else {
      currentCart.push({ ...product, quantity: quantity });
    }
    
    localStorage.setItem("vault_cart", JSON.stringify(currentCart));
    setCartCount(currentCart.length);
    
    setToast({ message: `Đã thêm ${quantity} sản phẩm vào giỏ hàng!`, show: true });
    setTimeout(() => {
      setToast({ message: "", show: false });
    }, 3000);
  };

  if (loading) return <div className="min-h-screen bg-[#05050A] text-white flex items-center justify-center">Đang tải...</div>;
  if (!product) return null;

  return (
    <div className="min-h-screen bg-[#05050A] text-white relative">
      
      {/* Toast Notification */}
      <AnimatePresence>
        {toast.show && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9, x: "-50%" }}
            animate={{ opacity: 1, y: 0, scale: 1, x: "-50%" }}
            exit={{ opacity: 0, y: 20, scale: 0.9, x: "-50%" }}
            className="fixed bottom-10 left-1/2 z-[100] bg-emerald-500/20 backdrop-blur-md border border-emerald-500/30 text-emerald-400 px-6 py-4 rounded-full shadow-[0_0_30px_rgba(16,185,129,0.3)] flex items-center gap-3 font-semibold whitespace-nowrap"
          >
            <CheckCircle2 className="w-5 h-5" />
            {toast.message}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Content */}
      <main className="container mx-auto px-6 pt-32 pb-12 max-w-6xl">
        <div className="flex flex-col lg:flex-row gap-12">
          
          {/* Image Gallery */}
          <div className="flex-1">
            <div className="bg-white/5 border border-white/10 rounded-3xl p-12 aspect-square flex items-center justify-center relative overflow-hidden">
              <img 
                src="/picture.png" 
                alt={product.name} 
                className="w-full h-full object-contain hover:scale-110 transition-transform duration-700" 
              />
              <div className="absolute top-6 right-6 bg-emerald-500/20 text-emerald-400 text-sm font-bold px-4 py-2 rounded-full border border-emerald-500/20 backdrop-blur-md flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4" /> Còn hàng (In Stock)
              </div>
            </div>
          </div>

          {/* Product Details */}
          <div className="flex-1 flex flex-col">
            <div className="mb-2">
              <span className="text-indigo-400 font-semibold text-sm tracking-wider uppercase">
                {product.category}
              </span>
            </div>
            
            <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-6">
              {product.name}
            </h1>
            
            <div className="text-3xl font-bold text-white mb-8 border-b border-white/10 pb-8">
              {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
            </div>

            <div className="prose prose-invert prose-p:text-zinc-400 mb-8 max-w-none">
              <p className="text-lg leading-relaxed">
                {product.description || "Đây là sản phẩm demo công nghệ. Dữ liệu sản phẩm được mã hóa toàn diện bảo vệ khỏi các cuộc tấn công đánh cắp cơ sở dữ liệu."}
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4 mb-10">
              <div className="bg-white/[0.03] border border-white/5 p-4 rounded-2xl flex items-start gap-3">
                <ShieldCheck className="w-6 h-6 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-bold text-sm text-zinc-200">Giao dịch An toàn</h4>
                  <p className="text-xs text-zinc-500 mt-1">Bảo vệ bởi AES-256 GCM.</p>
                </div>
              </div>
              <div className="bg-white/[0.03] border border-white/5 p-4 rounded-2xl flex items-start gap-3">
                <Truck className="w-6 h-6 text-indigo-400 shrink-0 mt-0.5" />
                <div>
                  <h4 className="font-bold text-sm text-zinc-200">Giao hàng Cấp tốc</h4>
                  <p className="text-xs text-zinc-500 mt-1">Miễn phí toàn quốc.</p>
                </div>
              </div>
            </div>

            <div className="mt-auto flex gap-4">
              {/* Box chỉnh số lượng */}
              <div className="flex items-center bg-white/5 border border-white/10 rounded-2xl p-2 h-14">
                <button 
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="w-10 h-full flex items-center justify-center text-zinc-400 hover:text-white hover:bg-white/10 rounded-xl transition-colors"
                >
                  -
                </button>
                <div className="w-12 text-center font-bold text-lg">{quantity}</div>
                <button 
                  onClick={() => setQuantity(quantity + 1)}
                  className="w-10 h-full flex items-center justify-center text-zinc-400 hover:text-white hover:bg-white/10 rounded-xl transition-colors"
                >
                  +
                </button>
              </div>

              {/* Nút thêm giỏ hàng */}
              <Button 
                onClick={addToCart}
                className="flex-1 h-14 bg-indigo-600 hover:bg-indigo-500 text-lg rounded-2xl shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all flex items-center justify-center gap-3"
              >
                Thêm vào Giỏ hàng <ShoppingCart className="w-5 h-5" />
              </Button>
            </div>

            <div className="mt-6 flex items-start gap-3 text-zinc-500 text-xs text-center justify-center">
              <ShieldAlert className="w-4 h-4 text-zinc-400 shrink-0" />
              <p>Mọi giao dịch và thông tin thanh toán sẽ được trung chuyển qua <br/> HashiCorp Vault Transit Engine.</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
