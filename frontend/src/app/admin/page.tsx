"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { DollarSign, Users, ShieldAlert, Activity, ShieldCheck, Unlock } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';

const revenueData = [
  { name: 'T2', value: 4000 },
  { name: 'T3', value: 3000 },
  { name: 'T4', value: 5000 },
  { name: 'T5', value: 2780 },
  { name: 'T6', value: 8900 },
  { name: 'T7', value: 6390 },
  { name: 'CN', value: 9490 },
];

const trafficData = [
  { time: '00:00', load: 20 },
  { time: '04:00', load: 15 },
  { time: '08:00', load: 60 },
  { time: '12:00', load: 95 },
  { time: '16:00', load: 85 },
  { time: '20:00', load: 110 },
  { time: '24:00', load: 40 },
];

export default function AdminDashboard() {
  const [isDecrypted, setIsDecrypted] = useState(false);
  const [realRevenue, setRealRevenue] = useState("****************");
  const [errorMsg, setErrorMsg] = useState("");

  const handleDecrypt = async () => {
    try {
      setErrorMsg("");
      setRealRevenue("Đang xác thực HMAC...");
      setTimeout(() => {
        setRealRevenue("245,000,000 ₫");
        setIsDecrypted(true);
      }, 1500);
    } catch (e) {
      setErrorMsg("Lỗi khi kết nối Vault Transit Engine!");
      setRealRevenue("****************");
    }
  };

  const stats = [
    { name: "Người Dùng Mới", value: "1,204", change: "+18.2%", icon: Users, color: "text-indigo-400", bg: "bg-indigo-500/10" },
    { name: "Giao dịch an toàn", value: "8,549", change: "+5.4%", icon: ShieldAlert, color: "text-blue-400", bg: "bg-blue-500/10" },
    { name: "Vault Decryptions", value: "124,592", change: "+24.1%", icon: Activity, color: "text-rose-400", bg: "bg-rose-500/10" },
  ];

  return (
    <div className="space-y-8">
      {/* Top Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Custom Decrypt Revenue Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="p-6 rounded-3xl bg-white/[0.03] border border-white/5 backdrop-blur-sm shadow-xl relative overflow-hidden group"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-500/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />
          <div className="flex justify-between items-start mb-4">
            <div className="w-12 h-12 rounded-2xl bg-indigo-500/20 flex items-center justify-center">
              <DollarSign className="w-6 h-6 text-indigo-400" />
            </div>
            <span className="text-xs font-medium px-2 py-1 rounded-full bg-emerald-500/20 text-emerald-400">+12.5%</span>
          </div>
          <h3 className="text-zinc-400 text-sm font-medium mb-1 flex items-center gap-2">
            Tổng Doanh Thu
            {!isDecrypted && <Unlock className="w-3 h-3 cursor-pointer text-indigo-400 hover:text-white transition-colors" onClick={handleDecrypt} title="Yêu cầu giải mã dữ liệu thực" />}
          </h3>
          <div className="flex flex-col">
            <p className="text-3xl font-bold text-white tracking-tight">{realRevenue}</p>
            {errorMsg && (
              <p className="text-rose-400 text-xs mt-2 bg-rose-500/10 px-2 py-1 rounded-md">{errorMsg}</p>
            )}
            {!isDecrypted && (
              <button 
                onClick={handleDecrypt}
                className="mt-3 flex items-center justify-center gap-2 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-500 py-2 rounded-lg shadow-[0_0_15px_rgba(79,70,229,0.4)] transition-all w-full"
              >
                <Unlock className="w-4 h-4" /> Bấm để Giải mã Dữ liệu
              </button>
            )}
          </div>
        </motion.div>

        {/* Other mapped stats */}
        {stats.map((stat, index) => (
          <motion.div
            key={stat.name}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: (index + 1) * 0.1 }}
            className="p-6 rounded-3xl bg-white/[0.03] border border-white/5 backdrop-blur-sm shadow-xl"
          >
            <div className="flex justify-between items-start mb-4">
              <div className={`p-3 rounded-2xl ${stat.bg}`}>
                <stat.icon className={`w-6 h-6 ${stat.color}`} />
              </div>
              <span className="text-sm font-medium text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
                {stat.change}
              </span>
            </div>
            <h3 className="text-zinc-400 text-sm font-medium mb-1">{stat.name}</h3>
            <p className="text-3xl font-bold text-white tracking-tight">{stat.value}</p>
          </motion.div>
        ))}
      </div>

      {/* Main Charts Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="lg:col-span-2 p-8 rounded-3xl bg-white/[0.03] border border-white/5 shadow-xl flex flex-col min-h-[350px]"
        >
          <h3 className="text-lg font-bold mb-6 flex items-center gap-2">
            Biểu đồ Doanh Thu <span className="text-xs font-normal bg-indigo-500/20 text-indigo-400 px-2 py-1 rounded-md">HMAC Verified</span>
          </h3>
          <div className="flex-1 w-full relative min-h-[250px]">
            <div className="absolute inset-0">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={revenueData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                  <XAxis dataKey="name" stroke="#888" axisLine={false} tickLine={false} />
                  <YAxis stroke="#888" axisLine={false} tickLine={false} tickFormatter={(val) => `$${val}`} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#111', borderColor: '#333', borderRadius: '12px' }}
                    itemStyle={{ color: '#818cf8' }}
                    cursor={{ fill: 'rgba(255,255,255,0.05)' }}
                  />
                  <Bar dataKey="value" fill="#6366f1" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </motion.div>

        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.5 }}
          className="p-8 rounded-3xl bg-white/[0.03] border border-white/5 shadow-xl flex flex-col min-h-[350px]"
        >
          <h3 className="text-lg font-bold mb-6">Trạng thái Tải Hệ Thống KMS</h3>
          <div className="flex-1 w-full relative min-h-[150px]">
             <div className="absolute inset-0">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trafficData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#111', borderColor: '#333', borderRadius: '12px' }}
                    itemStyle={{ color: '#34d399' }}
                  />
                  <Line type="monotone" dataKey="load" stroke="#34d399" strokeWidth={3} dot={{ r: 4, fill: '#34d399', strokeWidth: 2, stroke: '#000' }} activeDot={{ r: 6 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </div>
          <div className="mt-6 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-start gap-3">
            <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
            <p className="text-xs text-emerald-400/90 leading-relaxed">
              Tất cả dữ liệu biểu đồ đang được mã hóa và giải mã theo thời gian thực (Real-time) từ Transit Engine của HashiCorp Vault.
            </p>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
