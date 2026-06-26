"use client";

import { useState, useEffect } from "react";
import { Users, Ban, CheckCircle2, ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AdminUsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const [actionUser, setActionUser] = useState<{id: string, isActive: boolean} | null>(null);
  const [errorMsg, setErrorMsg] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("vault_token") || "";
    // Gọi API thật từ Spring Boot (identity/catalog service)
    fetch("/api/catalog/users", {
      headers: { "Authorization": `Bearer ${token}` }
    })
      .then(async res => {
        if (!res.ok) {
          if (res.status === 401) {
            throw new Error("Phiên đăng nhập hết hạn hoặc bạn chưa đăng nhập (Lỗi 401). Vui lòng đăng xuất và đăng nhập lại!");
          }
          const text = await res.text();
          throw new Error(`HTTP ${res.status}: ${text}`);
        }
        return res.json();
      })
      .then(data => {
        setUsers(data);
        setLoading(false);
      })
      .catch(err => {
        setErrorMsg("Lỗi khi tải danh sách từ cơ sở dữ liệu: " + err.message);
        setUsers([]);
        setLoading(false);
      });
  }, []);

  const confirmAction = async () => {
    if (!actionUser) return;
    setErrorMsg("");
    try {
      const token = localStorage.getItem("vault_token") || "";
      // Soft Delete qua API
      await fetch(`/api/catalog/users/${actionUser.id}`, { 
        method: "DELETE",
        headers: { "Authorization": `Bearer ${token}` }
      });
      
      // Update local state
      setUsers(users.map(u => u.id === actionUser.id ? { ...u, isActive: !actionUser.isActive } : u));
      setActionUser(null);
    } catch (err) {
      setErrorMsg("Lỗi khi kết nối tới Server Backend!");
      setActionUser(null);
    }
  };

  if (loading) return <div className="text-white p-8 animate-pulse">Đang đồng bộ dữ liệu Users từ Database...</div>;

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-2xl font-bold flex items-center gap-2">
            <Users className="w-6 h-6 text-indigo-400" /> Quản lý Người dùng
          </h2>
          <p className="text-zinc-400 mt-1">Quản trị tài khoản, phân quyền và Soft Delete an toàn.</p>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center gap-2">
          <ShieldAlert className="w-5 h-5 text-rose-400 shrink-0" />
          <p className="text-sm text-rose-400">{errorMsg}</p>
        </div>
      )}

      <div className="bg-white/[0.03] border border-white/5 rounded-2xl overflow-hidden shadow-xl">
        <table className="w-full text-left text-sm text-zinc-300">
          <thead className="bg-white/5 text-zinc-400">
            <tr>
              <th className="px-6 py-4 font-medium">Tên đăng nhập</th>
              <th className="px-6 py-4 font-medium">Email</th>
              <th className="px-6 py-4 font-medium">Vai trò</th>
              <th className="px-6 py-4 font-medium">Trạng thái</th>
              <th className="px-6 py-4 font-medium text-right">Thao tác an toàn</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-white/5">
            {users.map((user) => (
              <tr key={user.id} className="hover:bg-white/[0.02] transition-colors">
                <td className="px-6 py-4 font-medium text-white flex items-center gap-2">
                  {user.role === 'ROLE_ADMIN' ? <ShieldAlert className="w-4 h-4 text-rose-400" /> : <Users className="w-4 h-4 text-indigo-400" />}
                  {user.username}
                </td>
                <td className="px-6 py-4 text-zinc-400">{user.email}</td>
                <td className="px-6 py-4">
                  <span className={`px-2 py-1 rounded-md text-xs font-medium ${user.role === 'ROLE_ADMIN' ? 'bg-rose-500/10 text-rose-400' : 'bg-indigo-500/10 text-indigo-400'}`}>
                    {user.role}
                  </span>
                </td>
                <td className="px-6 py-4">
                  {user.isActive ? (
                    <span className="flex items-center gap-1 text-emerald-400 text-xs"><CheckCircle2 className="w-3 h-3"/> Active</span>
                  ) : (
                    <span className="flex items-center gap-1 text-rose-400 text-xs"><Ban className="w-3 h-3"/> Banned (Soft)</span>
                  )}
                </td>
                <td className="px-6 py-4 text-right">
                  <Button 
                    onClick={() => setActionUser({id: user.id, isActive: user.isActive})}
                    variant="ghost" 
                    size="sm" 
                    className={`h-8 px-3 ${user.isActive ? 'text-rose-400 hover:text-rose-300 hover:bg-rose-500/10' : 'text-emerald-400 hover:text-emerald-300 hover:bg-emerald-500/10'}`}
                  >
                    {user.isActive ? "Khóa (Ban)" : "Mở khóa"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Confirmation Modal */}
      {actionUser !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
          <div className="bg-zinc-900 border border-white/10 rounded-3xl p-6 w-full max-w-sm shadow-2xl">
            <div className={`w-12 h-12 rounded-full flex items-center justify-center mb-4 mx-auto ${actionUser.isActive ? 'bg-rose-500/10' : 'bg-emerald-500/10'}`}>
              {actionUser.isActive ? <Ban className="w-6 h-6 text-rose-400" /> : <CheckCircle2 className="w-6 h-6 text-emerald-400" />}
            </div>
            <h3 className="text-xl font-bold text-white text-center mb-2">
              {actionUser.isActive ? "Khóa người dùng?" : "Mở khóa người dùng?"}
            </h3>
            <p className="text-sm text-zinc-400 text-center mb-6">
              {actionUser.isActive 
                ? "Tài khoản bị khóa sẽ không thể đăng nhập, nhưng dữ liệu lịch sử vẫn được giữ nguyên (Soft Delete)." 
                : "Tài khoản sẽ được khôi phục quyền truy cập hệ thống."}
            </p>
            <div className="flex justify-center gap-3">
              <Button variant="ghost" onClick={() => setActionUser(null)} className="text-zinc-400 hover:text-white bg-white/5">
                Hủy bỏ
              </Button>
              <Button 
                onClick={confirmAction} 
                className={actionUser.isActive ? "bg-rose-600 hover:bg-rose-500 text-white" : "bg-emerald-600 hover:bg-emerald-500 text-white"}
              >
                Xác nhận
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
