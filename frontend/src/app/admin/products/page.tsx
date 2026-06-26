"use client";

import { useState, useEffect } from "react";
import { Plus, Edit2, Trash2, Search, PackageOpen } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<any[]>([]);

  useEffect(() => {
    // Luôn ưu tiên đọc từ MySQL Backend API
    fetch("http://localhost:8081/api/products")
      .then(res => {
        if (!res.ok) throw new Error("Backend not available");
        return res.json();
      })
      .then(data => {
        setProducts(data);
        localStorage.setItem("vault_admin_products", JSON.stringify(data));
      })
      .catch(err => {
        console.error("Lỗi lấy sản phẩm từ Backend, dùng Mock:", err);
        const stored = localStorage.getItem("vault_admin_products");
        if (stored) {
          setProducts(JSON.parse(stored));
        } else {
          // Mock data fallback
          const initial = [
            { id: 1, name: "iPhone 15 Pro Max", price: 29000000, category: "Smartphone", stock: 45 },
            { id: 2, name: "MacBook Pro M3", price: 49000000, category: "Laptop", stock: 12 },
            { id: 3, name: "Sony WH-1000XM5", price: 7990000, category: "Audio", stock: 89 },
          ];
          setProducts(initial);
          localStorage.setItem("vault_admin_products", JSON.stringify(initial));
        }
      });
  }, []);

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [newProductName, setNewProductName] = useState("");
  const [newProductPrice, setNewProductPrice] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [editId, setEditId] = useState<number | null>(null);

  const filteredProducts = products.filter(p => p.name.toLowerCase().includes(searchTerm.toLowerCase()));

  const openEditModal = (product: any) => {
    setEditId(product.id);
    setNewProductName(product.name);
    setNewProductPrice(product.price.toString());
    setIsAddModalOpen(true);
  };

  const handleAddSubmit = async () => {
    if (!newProductName.trim()) return;
    const price = parseInt(newProductPrice) || 0;
    
    const token = localStorage.getItem("vault_token") || "";
    const productPayload = {
      name: newProductName,
      price,
      category: "Mới",
      stock: 100,
      isActive: true
    };

    try {
      if (editId !== null) {
        // Cập nhật (PUT) vào MySQL
        const res = await fetch(`http://localhost:8081/api/products/${editId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
          body: JSON.stringify(productPayload)
        });
        if (res.ok) {
          const updatedProduct = await res.json();
          const newProducts = products.map(p => p.id === editId ? updatedProduct : p);
          setProducts(newProducts);
          localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
        } else {
          // Fallback lưu local nếu API lỗi
          const newProducts = products.map(p => p.id === editId ? { ...p, name: newProductName, price } : p);
          setProducts(newProducts);
          localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
        }
      } else {
        // Thêm mới (POST) vào MySQL
        const res = await fetch("http://localhost:8081/api/products", {
          method: "POST",
          headers: { "Content-Type": "application/json", "Authorization": `Bearer ${token}` },
          body: JSON.stringify(productPayload)
        });
        if (res.ok) {
          const newProduct = await res.json();
          const newProducts = [...products, newProduct];
          setProducts(newProducts);
          localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
        } else {
          // Fallback lưu local nếu API lỗi
          const newProductFallback = { id: Date.now(), ...productPayload };
          const newProducts = [...products, newProductFallback];
          setProducts(newProducts);
          localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
        }
      }
    } catch (e) {
      console.error("Lỗi API, lưu tạm vào Local Storage", e);
      // Fallback
      let newProducts;
      if (editId !== null) {
        newProducts = products.map(p => p.id === editId ? { ...p, name: newProductName, price } : p);
      } else {
        newProducts = [...products, { id: Date.now(), ...productPayload }];
      }
      setProducts(newProducts);
      localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
    }
    
    setIsAddModalOpen(false);
    setNewProductName("");
    setNewProductPrice("");
    setEditId(null);
  };

  const confirmDelete = async () => {
    if (deleteConfirmId === null) return;
    const token = localStorage.getItem("vault_token") || "";

    try {
      // Xóa mềm (DELETE) trên MySQL
      const res = await fetch(`http://localhost:8081/api/products/${deleteConfirmId}`, {
        method: "DELETE",
        headers: { "Authorization": `Bearer ${token}` }
      });
      // Bất kể thành công hay không, cũng cập nhật UI
    } catch (e) {
      console.error("Lỗi API xóa", e);
    }

    const newProducts = products.filter(p => p.id !== deleteConfirmId);
    setProducts(newProducts);
    localStorage.setItem("vault_admin_products", JSON.stringify(newProducts));
    setDeleteConfirmId(null);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold flex items-center gap-2">
            <PackageOpen className="w-6 h-6 text-indigo-400" /> Quản lý Sản phẩm
          </h2>
          <p className="text-zinc-400 mt-1">Thêm, sửa, xóa danh mục hàng hóa an toàn.</p>
        </div>
        <Button onClick={() => {
          setEditId(null);
          setNewProductName("");
          setNewProductPrice("");
          setIsAddModalOpen(true);
        }} className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl shadow-lg">
          <Plus className="w-4 h-4 mr-2" /> Thêm Sản phẩm
        </Button>
      </div>

      {/* Table Section */}
      <div className="bg-white/[0.03] border border-white/5 rounded-2xl overflow-hidden shadow-xl">
        <div className="p-4 border-b border-white/5 flex items-center justify-between bg-black/20">
          <div className="relative w-64">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-500" />
            <input 
              type="text" 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm mã hóa..." 
              className="w-full bg-black/50 border border-white/10 rounded-lg pl-9 pr-4 py-2 text-sm text-white focus:border-indigo-500 focus:outline-none"
            />
          </div>
        </div>
        
        <table className="w-full text-left text-sm text-zinc-300">
          <thead className="bg-white/5 text-zinc-400">
            <tr>
              <th className="px-6 py-4 font-medium">ID</th>
              <th className="px-6 py-4 font-medium">Tên Sản phẩm</th>
              <th className="px-6 py-4 font-medium">Danh mục</th>
              <th className="px-6 py-4 font-medium">Giá</th>
              <th className="px-6 py-4 font-medium">Tồn kho</th>
              <th className="px-6 py-4 font-medium text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {filteredProducts.map((product) => (
              <tr key={product.id} className="hover:bg-white/[0.02] transition-colors">
                <td className="px-6 py-4">#{product.id.toString().slice(0, 4)}</td>
                <td className="px-6 py-4 font-medium text-white">{product.name}</td>
                <td className="px-6 py-4">
                  <span className="px-2 py-1 rounded-md bg-white/10 text-xs">{product.category}</span>
                </td>
                <td className="px-6 py-4 text-emerald-400 font-medium">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(product.price)}
                </td>
                <td className="px-6 py-4">{product.stock}</td>
                <td className="px-6 py-4 text-right">
                  <div className="flex justify-end gap-2">
                    <Button 
                      onClick={() => openEditModal(product)}
                      variant="ghost" 
                      size="icon" 
                      className="h-8 w-8 text-indigo-400 hover:text-indigo-300 hover:bg-indigo-500/10"
                    >
                      <Edit2 className="w-4 h-4" />
                    </Button>
                    <Button 
                      onClick={() => setDeleteConfirmId(product.id)}
                      variant="ghost" 
                      size="icon" 
                      className="h-8 w-8 text-rose-400 hover:text-rose-300 hover:bg-rose-500/10"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {filteredProducts.length === 0 && (
              <tr>
                <td colSpan={6} className="px-6 py-8 text-center text-zinc-500">
                  Chưa có sản phẩm nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Add Product Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-white/10 rounded-3xl p-6 w-full max-w-md shadow-2xl">
            <h3 className="text-xl font-bold text-white mb-4">{editId ? "Chỉnh sửa Sản phẩm" : "Thêm Sản phẩm mới"}</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1">Tên sản phẩm</label>
                <input 
                  type="text" 
                  value={newProductName}
                  onChange={(e) => setNewProductName(e.target.value)}
                  className="w-full bg-black/50 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="Ví dụ: iPhone 16 Pro"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-zinc-400 mb-1">Giá sản phẩm (VNĐ)</label>
                <input 
                  type="number" 
                  value={newProductPrice}
                  onChange={(e) => setNewProductPrice(e.target.value)}
                  className="w-full bg-black/50 border border-white/10 rounded-xl px-4 py-3 text-white focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  placeholder="Ví dụ: 30000000"
                />
              </div>
            </div>
            <div className="flex justify-end gap-3 mt-8">
              <Button variant="ghost" onClick={() => setIsAddModalOpen(false)} className="text-zinc-400 hover:text-white">
                Hủy bỏ
              </Button>
              <Button onClick={handleAddSubmit} className="bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl">
                Lưu sản phẩm
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {deleteConfirmId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-rose-500/20 rounded-3xl p-6 w-full max-w-sm shadow-2xl">
            <div className="w-12 h-12 rounded-full bg-rose-500/10 flex items-center justify-center mb-4 mx-auto">
              <Trash2 className="w-6 h-6 text-rose-400" />
            </div>
            <h3 className="text-xl font-bold text-white text-center mb-2">Xóa sản phẩm?</h3>
            <p className="text-sm text-zinc-400 text-center mb-6">
              Bạn có chắc chắn muốn xóa sản phẩm này? Hành động này không thể hoàn tác.
            </p>
            <div className="flex justify-center gap-3">
              <Button variant="ghost" onClick={() => setDeleteConfirmId(null)} className="text-zinc-400 hover:text-white bg-white/5">
                Hủy bỏ
              </Button>
              <Button onClick={confirmDelete} className="bg-rose-600 hover:bg-rose-500 text-white rounded-xl shadow-[0_0_15px_rgba(225,29,72,0.4)]">
                Đồng ý Xóa
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
