"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ShieldCheck, ShoppingCart, ChevronLeft, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { MOCK_PRODUCTS } from "@/lib/mockData";
import { motion, AnimatePresence } from "framer-motion";

export default function CatalogPage() {
  const [products, setProducts] = useState(MOCK_PRODUCTS);
  const [cartCount, setCartCount] = useState(0);
  const [toast, setToast] = useState<{ message: string; show: boolean }>({ message: "", show: false });
  const [searchTerm, setSearchTerm] = useState("");
  const [filterCategory, setFilterCategory] = useState("All");
  const [filterPrice, setFilterPrice] = useState("All");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const filteredProducts = products.filter((p: any) => {
    const safeName = (p.name || p.productName || p.title || "").toString().toLowerCase();
    const safeCategory = (p.category || p.categoryName || "").toString().toLowerCase();
    const term = searchTerm.toLowerCase();

    const termMatch = safeName.includes(term) || safeCategory.includes(term);
    const categoryMatch = filterCategory === "All" || p.category === filterCategory;
    
    let priceMatch = true;
    const price = p.price || 0;
    if (filterPrice === "Under10") priceMatch = price < 10000000;
    else if (filterPrice === "10To30") priceMatch = price >= 10000000 && price <= 30000000;
    else if (filterPrice === "Over30") priceMatch = price > 30000000;
    
    return termMatch && categoryMatch && priceMatch;
  });

  const totalPages = Math.ceil(filteredProducts.length / itemsPerPage);
  const currentProducts = filteredProducts.slice(
    (currentPage - 1) * itemsPerPage, 
    currentPage * itemsPerPage
  );

  useEffect(() => {
    // Đọc số lượng trong giỏ hàng từ localStorage
    const savedCart = localStorage.getItem("vault_cart");
    if (savedCart) {
      setCartCount(JSON.parse(savedCart).length);
    }

    // Thử gọi API thật, nếu lỗi thì giữ nguyên MOCK_PRODUCTS (50 sản phẩm)
    fetch("/api/catalog/products")
      .then(res => res.json())
      .then(data => {
        let items = [];
        if (data && data.content && Array.isArray(data.content)) {
          items = data.content;
        } else if (Array.isArray(data)) {
          items = data;
        }
        
        if (items.length > 0) {
          // Map categoryId (backend) sang category string (frontend)
          const mappedItems = items.map((p: any) => ({
            ...p,
            category: p.category || (p.categoryId === 1 ? 'Smartphone' : p.categoryId === 2 ? 'Laptop' : 'Accessories')
          }));
          setProducts(mappedItems);
        }
      })
      .catch(() => {
        console.log("Backend chưa sẵn sàng, sử dụng 50 sản phẩm Mock Data.");
      });
  }, []);

  const addToCart = (product: any) => {
    const savedCart = localStorage.getItem("vault_cart");
    let currentCart = savedCart ? JSON.parse(savedCart) : [];
    
    // Kiểm tra xem đã có chưa
    const existing = currentCart.find((item: any) => item.id === product.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      currentCart.push({ ...product, quantity: 1 });
    }
    
    localStorage.setItem("vault_cart", JSON.stringify(currentCart));
    setCartCount(currentCart.length);
    
    // Hiển thị Toast thông báo chuyên nghiệp thay vì alert()
    setToast({ message: `Đã thêm "${product.name}" vào giỏ hàng!`, show: true });
    setTimeout(() => {
      setToast({ message: "", show: false });
    }, 3000);
  };

  // Lấy danh sách category duy nhất và loại bỏ các giá trị undefined/rỗng
  const categories = ["All", ...Array.from(new Set(products.map((p: any) => p.category)))].filter(Boolean);

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
      <main className="container mx-auto px-6 pt-32 pb-12">
        <div className="mb-12">
          <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-4">
            Khám phá {products.length} Sản phẩm
          </h1>
          <p className="text-zinc-400 text-lg max-w-2xl mb-8">
            Tất cả sản phẩm dưới đây được mã hóa và bảo vệ giao dịch bởi nền tảng Mật mã ứng dụng. 
            Guest (Khách) có thể xem và thêm vào giỏ hàng thoải mái.
          </p>

          {/* Search Bar & Filters */}
          <div className="w-full max-w-4xl flex flex-col md:flex-row gap-4">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                <svg className="w-5 h-5 text-zinc-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
              </div>
              <input 
                type="text" 
                value={searchTerm}
                onChange={(e) => {
                  setSearchTerm(e.target.value);
                  setCurrentPage(1); // Reset trang khi tìm kiếm
                }}
                className="w-full bg-white/[0.03] border border-white/10 rounded-2xl py-4 pl-12 pr-4 text-white placeholder-zinc-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 shadow-xl transition-all"
                placeholder="Tìm kiếm theo tên sản phẩm, danh mục, thương hiệu..."
              />
            </div>
            
            <div className="flex gap-4">
              <select 
                value={filterCategory} 
                onChange={e => { setFilterCategory(e.target.value); setCurrentPage(1); }}
                className="bg-white/[0.03] border border-white/10 rounded-2xl px-4 py-4 text-white focus:outline-none focus:border-indigo-500 outline-none cursor-pointer"
              >
                {categories.map((c: any, idx: number) => <option key={`${c}-${idx}`} value={c} className="bg-zinc-900">{c === "All" ? "Tất cả Danh mục" : c}</option>)}
              </select>

              <select 
                value={filterPrice} 
                onChange={e => { setFilterPrice(e.target.value); setCurrentPage(1); }}
                className="bg-white/[0.03] border border-white/10 rounded-2xl px-4 py-4 text-white focus:outline-none focus:border-indigo-500 outline-none cursor-pointer"
              >
                <option value="All" className="bg-zinc-900">Tất cả Giá</option>
                <option value="Under10" className="bg-zinc-900">Dưới 10 Triệu</option>
                <option value="10To30" className="bg-zinc-900">10 - 30 Triệu</option>
                <option value="Over30" className="bg-zinc-900">Trên 30 Triệu</option>
              </select>
            </div>
          </div>
        </div>

        {/* Product Grid */}
        {filteredProducts.length === 0 ? (
          <div className="py-20 text-center text-zinc-500">
            Không tìm thấy sản phẩm nào phù hợp với từ khóa "{searchTerm}".
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6 mb-12">
            {currentProducts.map((product: any) => (
              <Link href={`/catalog/${product.id}`} key={product.id} className="group flex flex-col bg-white/[0.03] border border-white/5 rounded-3xl overflow-hidden hover:bg-white/[0.05] transition-all duration-300 hover:-translate-y-1">
                {/* Product Image Area */}
                <div className="relative aspect-square bg-white/5 p-8 flex items-center justify-center">
                  <img 
                    src={product.imageUrl || "/picture.png"} 
                    alt={product.name}
                    className="w-full h-full object-contain group-hover:scale-110 transition-transform duration-500"
                    onError={(e) => { e.currentTarget.src = "/picture.png"; }}
                  />
                  <div className="absolute top-4 right-4 bg-emerald-500/20 text-emerald-400 text-xs font-bold px-3 py-1 rounded-full border border-emerald-500/20 backdrop-blur-md">
                    In Stock
                  </div>
                </div>

                {/* Product Info Area */}
                <div className="p-6 flex flex-col flex-1">
                  <h3 className="text-lg font-bold text-zinc-100 mb-2 group-hover:text-indigo-400 transition-colors">{product.name}</h3>
                  <p className="text-sm text-emerald-400 mb-1 font-semibold">{product.category}</p>
                  <p className="text-sm text-zinc-500 mb-4 line-clamp-2">{product.description || "Chưa có mô tả chi tiết."}</p>
                  <div className="mt-auto flex items-center justify-between">
                    <span className="text-xl font-extrabold text-indigo-400" suppressHydrationWarning>
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                    </span>
                    <Button 
                      size="sm" 
                      onClick={(e) => {
                        e.preventDefault(); // Ngăn link kích hoạt khi bấm nút "Thêm"
                        addToCart(product);
                      }}
                      className="bg-white/10 hover:bg-indigo-600 text-white rounded-full transition-colors"
                    >
                      Thêm <ShoppingCart className="w-4 h-4 ml-2" />
                    </Button>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex justify-center items-center gap-3 mt-8">
            <button 
              onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
              disabled={currentPage === 1}
              className="w-10 h-10 flex items-center justify-center rounded-xl bg-white/5 border border-white/10 text-white hover:bg-indigo-600 disabled:opacity-30 disabled:hover:bg-white/5 transition-colors"
            >
              <ChevronLeft className="w-5 h-5" />
            </button>
            
            <div className="flex gap-2">
              {Array.from({ length: totalPages }).map((_, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentPage(idx + 1)}
                  className={`w-10 h-10 rounded-xl font-semibold text-sm transition-all ${
                    currentPage === idx + 1 
                      ? 'bg-indigo-600 text-white shadow-[0_0_15px_rgba(79,70,229,0.4)]' 
                      : 'bg-white/5 text-zinc-400 hover:bg-white/10 hover:text-white border border-white/5'
                  }`}
                >
                  {idx + 1}
                </button>
              ))}
            </div>

            <button 
              onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
              disabled={currentPage === totalPages}
              className="w-10 h-10 flex items-center justify-center rounded-xl bg-white/5 border border-white/10 text-white hover:bg-indigo-600 disabled:opacity-30 disabled:hover:bg-white/5 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
