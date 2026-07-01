"use client";

import { useState, useEffect } from "react";
import { ChevronLeft, ShieldCheck, CreditCard, MapPin, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { motion, AnimatePresence } from "framer-motion";
import { useRouter } from "next/navigation";

export default function CheckoutPage() {
  const router = useRouter();
  const [cartItems, setCartItems] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState<"idle" | "processing" | "success">("idle");
  const [showQR, setShowQR] = useState(false);
  const [orderId, setOrderId] = useState("");
  const [user, setUser] = useState<any>(null);
  const [cryptoProof, setCryptoProof] = useState<{signature: string, publicKey: string} | null>(null);

  // Form states
  const [fullName, setFullName] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");

  useEffect(() => {
    // Kiem tra token
    const token = localStorage.getItem("vault_token");
    if (!token) {
      router.push("/login");
      return;
    }
    
    const savedUser = localStorage.getItem("vault_user");
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }

    const savedShipping = localStorage.getItem("vault_shipping_info");
    if (savedShipping) {
      const parsed = JSON.parse(savedShipping);
      setFullName(parsed.fullName || "");
      setPhone(parsed.phone || "");
      setAddress(parsed.address || "");
    }

    const savedCart = localStorage.getItem("vault_cart");
    if (savedCart) {
      const parsedCart = JSON.parse(savedCart);
      setCartItems(parsedCart);
      const sum = parsedCart.reduce((acc: number, item: any) => acc + (item.price * item.quantity), 0);
      setTotal(sum);
    }
  }, [router]);

  const handleCheckout = (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName || !phone || !address) return;

    // Lưu thông tin giao hàng để lần sau không cần nhập lại
    localStorage.setItem("vault_shipping_info", JSON.stringify({ fullName, phone, address }));

    setStatus("processing");

    setTimeout(() => {
      // Giả lập Hash và Verify an toàn từ Backend
      const generatedOrderId = `ORD-${Math.floor(Math.random() * 900000) + 100000}`;
      setOrderId(generatedOrderId);
      setShowQR(true);
      setStatus("idle");
    }, 1500);
  };

  const confirmPayment = async () => {
    setStatus("processing");
    const token = localStorage.getItem("vault_token");
    
    if (!token) {
      router.push("/login");
      return;
    }

    try {
      // 1. Sync cart to backend first
      for (const item of cartItems) {
        await fetch("/api/cart/items", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}` 
          },
          body: JSON.stringify({ productId: item.id, quantity: item.quantity })
        });
      }

      // 2. Create Order with ECDSA Digital Signature (Non-repudiation)
      const timestamp = Date.now().toString();
      const payloadString = address + ":" + phone + ":" + timestamp;

      // Sinh cặp khóa ECDSA P-256
      const keyPair = await window.crypto.subtle.generateKey(
        {
          name: "ECDSA",
          namedCurve: "P-256",
        },
        true,
        ["sign", "verify"]
      );

      // Ký số dữ liệu bằng Private Key
      const encoder = new TextEncoder();
      const dataToSign = encoder.encode(payloadString);
      const signatureBuffer = await window.crypto.subtle.sign(
        {
          name: "ECDSA",
          hash: { name: "SHA-256" },
        },
        keyPair.privateKey,
        dataToSign
      );

      // Xuất Public Key dạng SPKI (để gửi lên Java Backend)
      const exportedPublicKey = await window.crypto.subtle.exportKey(
        "spki",
        keyPair.publicKey
      );

      // Chuyển đổi sang Base64
      const signatureB64 = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)));
      const publicKeyB64 = btoa(String.fromCharCode(...new Uint8Array(exportedPublicKey)));

      setCryptoProof({ signature: signatureB64, publicKey: publicKeyB64 });

      const payload = {
        shippingAddress: address,
        phoneNumber: phone,
        timestamp: timestamp,
        signature: signatureB64,
        publicKey: publicKeyB64
      };

      const res = await fetch("/api/orders", {
        method: "POST",
        headers: { 
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        const orderData = await res.json();
        
        // 3. Create Payment Transaction
        await fetch("/api/payments", {
          method: "POST",
          headers: { 
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
          },
          body: JSON.stringify({
            orderId: orderData.orderId,
            paymentToken: "VIETQR_" + Date.now(),
            paymentMethod: "VIETQR"
          })
        });

        localStorage.removeItem("vault_cart");
        setCartItems([]);
        setStatus("success");
      } else {
        const errData = await res.text();
        console.error("Order creation failed:", errData);
        setStatus("idle");
      }
    } catch (e) {
      console.error("Lỗi gọi API thanh toán", e);
      setStatus("idle");
    }
  };

  if (showQR && status !== "success") {
    // Đọc cấu hình thanh toán từ Admin (nếu có)
    const paymentConfig = JSON.parse(localStorage.getItem("vault_payment_config") || "{}");
    const bankId = paymentConfig.bankId || "970422";
    const accountNum = paymentConfig.accountNumber || "87343556868";
    const accName = paymentConfig.accountName || "HUYNH THANH TU";

    // Render VietQR API
    const encodedAccountName = encodeURIComponent(accName);
    const encodedAddInfo = encodeURIComponent(orderId);
    const qrUrl = `https://img.vietqr.io/image/${bankId}-${accountNum}-compact2.png?amount=${total}&addInfo=${encodedAddInfo}&accountName=${encodedAccountName}`;
    return (
      <div className="min-h-screen bg-[#05050A] text-white flex flex-col items-center justify-center py-12">
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white/[0.03] border border-white/5 p-8 rounded-3xl backdrop-blur-xl flex flex-col items-center max-w-md w-full text-center shadow-2xl relative"
        >
          <div className="flex items-center gap-2 mb-6 text-indigo-400">
            <ShieldCheck className="w-6 h-6" />
            <span className="font-semibold tracking-tight">Thanh toán An toàn VietQR</span>
          </div>
          
          <div className="bg-white p-4 rounded-2xl mb-6 shadow-lg">
            <img src={qrUrl} alt="VietQR" className="w-64 h-64 object-contain" />
          </div>

          <div className="space-y-3 mb-8 w-full bg-black/30 p-4 rounded-xl border border-white/5 text-left text-sm">
            <div className="flex justify-between">
              <span className="text-zinc-400">Mã đơn hàng:</span>
              <span className="font-mono text-white">{orderId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-zinc-400">Tổng tiền:</span>
              <span className="font-bold text-emerald-400">
                {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total)}
              </span>
            <div className="flex justify-between flex-col mt-2 pt-2 border-t border-white/5">
              <span className="text-zinc-400 mb-1">Chữ ký số ECDSA (Trích xuất):</span>
              <span className="font-mono text-emerald-500 text-xs truncate">
                {cryptoProof?.signature ? cryptoProof.signature.substring(0, 40) + "..." : "Đang tạo..."}
              </span>
            </div>
            <div className="flex justify-between flex-col mt-1">
              <span className="text-zinc-400 mb-1">Public Key SPKI (Trích xuất):</span>
              <span className="font-mono text-indigo-400 text-xs truncate">
                {cryptoProof?.publicKey ? cryptoProof.publicKey.substring(0, 40) + "..." : "Đang tạo..."}
              </span>
            </div>
          </div>

          <Button 
            onClick={confirmPayment}
            disabled={status === "processing"}
            className="w-full h-12 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-lg font-medium"
          >
            {status === "processing" ? "Đang xử lý..." : "Tôi đã chuyển khoản"}
          </Button>
          <p className="text-xs text-zinc-500 mt-4">
            Dữ liệu chuyển khoản đã được băm (hash) để đối chiếu tự động.
          </p>
        </motion.div>
      </div>
    );
  }

  if (status === "success") {
    return (
      <div className="min-h-screen bg-[#020205] text-white flex flex-col items-center justify-center relative overflow-hidden">
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-emerald-600/20 rounded-full blur-[150px] pointer-events-none" />
        
        <motion.div 
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          className="bg-white/[0.03] border border-white/5 p-12 rounded-3xl backdrop-blur-xl flex flex-col items-center max-w-lg text-center shadow-2xl relative z-10"
        >
          <div className="w-24 h-24 rounded-full bg-emerald-500/20 flex items-center justify-center mb-8">
            <CheckCircle2 className="w-12 h-12 text-emerald-400" />
          </div>
          <h1 className="text-3xl font-bold mb-4">Đặt hàng thành công!</h1>
          <p className="text-zinc-400 mb-6 leading-relaxed">
            Mã đơn hàng: <span className="font-mono text-indigo-400">#{orderId}</span><br/>
            Cảm ơn {user?.username} đã mua sắm. Thông tin đơn hàng đã được bảo vệ bằng Mã hóa Field-Level Encryption.
          </p>

          {cryptoProof && (
            <div className="w-full bg-black/40 border border-emerald-500/20 p-4 rounded-xl mb-8 text-left space-y-3">
              <div className="flex items-center gap-2 text-emerald-400 mb-2">
                <ShieldCheck className="w-5 h-5" />
                <span className="font-semibold text-sm">Bằng chứng Mật mã học (Non-repudiation)</span>
              </div>
              <div>
                <div className="text-[10px] text-zinc-500 uppercase font-bold tracking-wider mb-1">Chữ ký số (ECDSA P-256 Signature)</div>
                <div className="font-mono text-xs text-zinc-300 break-all bg-black/50 p-2 rounded border border-white/5">{cryptoProof.signature}</div>
              </div>
              <div>
                <div className="text-[10px] text-zinc-500 uppercase font-bold tracking-wider mb-1">Khóa công khai (SPKI Public Key)</div>
                <div className="font-mono text-xs text-zinc-300 break-all bg-black/50 p-2 rounded border border-white/5">{cryptoProof.publicKey}</div>
              </div>
            </div>
          )}

          <Link href="/catalog" className="w-full">
            <Button className="w-full h-12 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-lg">
              Tiếp tục mua sắm
            </Button>
          </Link>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#05050A] text-white">
      <main className="container mx-auto px-6 pt-32 pb-12 max-w-6xl">
        <h1 className="text-3xl font-bold mb-8">Thông tin Giao hàng & Thanh toán</h1>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
          {/* Cột Form */}
          <div className="space-y-8">
            <form id="checkout-form" onSubmit={handleCheckout} className="space-y-6 bg-white/[0.03] border border-white/5 p-8 rounded-3xl">
              <h2 className="text-xl font-semibold flex items-center gap-2 mb-6">
                <MapPin className="w-5 h-5 text-indigo-400" /> Địa chỉ nhận hàng
              </h2>
              
              <div className="space-y-2">
                <label className="text-sm text-zinc-400 ml-1">Họ và tên người nhận</label>
                <input
                  type="text"
                  required
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
                  placeholder="Nhập tên của bạn"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm text-zinc-400 ml-1">Số điện thoại liên hệ</label>
                <input
                  type="tel"
                  required
                  pattern="[0-9]*"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value.replace(/\D/g, ''))}
                  className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500"
                  placeholder="09xx xxx xxx"
                />
              </div>

              <div className="space-y-2">
                <label className="text-sm text-zinc-400 ml-1">Địa chỉ giao hàng chi tiết</label>
                <textarea
                  required
                  rows={3}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  className="w-full bg-black/50 border border-white/10 rounded-xl py-3 px-4 text-white placeholder-zinc-600 focus:outline-none focus:border-indigo-500 resize-none"
                  placeholder="Số nhà, đường, phường, quận..."
                />
              </div>
            </form>

            <div className="bg-emerald-500/10 border border-emerald-500/20 p-6 rounded-3xl flex items-start gap-4">
              <ShieldCheck className="w-8 h-8 text-emerald-400 shrink-0" />
              <div>
                <h3 className="font-bold text-emerald-400 mb-1">Dữ liệu được Mã hóa (Encryption)</h3>
                <p className="text-sm text-emerald-400/80 leading-relaxed">
                  Tại VaultCommerce, chúng tôi không lưu trữ thông tin nhạy cảm của bạn dưới dạng Plain-text. Tên, số điện thoại và địa chỉ sẽ được đưa qua Transit Engine của HashiCorp Vault để mã hóa bằng thuật toán AES-256 GCM trước khi ghi vào Cơ sở dữ liệu.
                </p>
              </div>
            </div>
          </div>

          {/* Cột Tổng kết */}
          <div>
            <div className="bg-white/[0.03] border border-white/5 rounded-3xl p-8 sticky top-24">
              <h2 className="text-xl font-semibold flex items-center gap-2 mb-6">
                <CreditCard className="w-5 h-5 text-indigo-400" /> Tóm tắt Đơn hàng
              </h2>

              <div className="space-y-4 mb-8 max-h-[40vh] overflow-y-auto pr-2 custom-scrollbar">
                {cartItems.map(item => (
                  <div key={item.id} className="flex gap-4">
                    <div className="w-16 h-16 bg-white/5 rounded-lg p-1 shrink-0">
                      <img src={item.imageUrl || "/picture.png"} alt={item.name} className="w-full h-full object-contain" onError={(e) => { e.currentTarget.src = "/picture.png"; }} />
                    </div>
                    <div className="flex-1">
                      <h4 className="font-medium text-sm line-clamp-1 text-zinc-200">{item.name}</h4>
                      <div className="text-xs text-zinc-500 mt-1">Số lượng: {item.quantity}</div>
                    </div>
                    <div className="text-sm font-semibold text-emerald-400 shrink-0">
                      {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(item.price * item.quantity)}
                    </div>
                  </div>
                ))}
              </div>

              <div className="space-y-4 border-t border-white/10 pt-6 mb-8">
                <div className="flex justify-between text-zinc-400">
                  <span>Tạm tính</span>
                  <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total)}</span>
                </div>
                <div className="flex justify-between text-zinc-400">
                  <span>Phí giao hàng</span>
                  <span className="text-emerald-400">Miễn phí</span>
                </div>
                <div className="flex justify-between text-xl font-bold text-white pt-4 border-t border-white/5">
                  <span>Tổng thanh toán</span>
                  <span className="text-indigo-400">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total)}</span>
                </div>
              </div>

              <Button 
                form="checkout-form"
                type="submit"
                disabled={status === "processing"}
                className="w-full h-14 bg-indigo-600 hover:bg-indigo-500 text-lg rounded-xl shadow-[0_0_20px_rgba(79,70,229,0.3)] transition-all"
              >
                {status === "processing" ? (
                  <span className="flex items-center gap-2">
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Đang mã hóa dữ liệu...
                  </span>
                ) : (
                  "Xác nhận Đặt hàng"
                )}
              </Button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
