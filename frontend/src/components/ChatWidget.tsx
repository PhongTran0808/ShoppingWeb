"use client";

import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { MessageSquare, X, Send, Lock, AlertTriangle } from "lucide-react";

export default function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<{text: string, isBot: boolean}[]>([]);
  const [input, setInput] = useState("");
  const [spamWarning, setSpamWarning] = useState(false);
  const channelRef = useRef<BroadcastChannel | null>(null);

  // Rate Limiting States (Token Bucket/Sliding Window)
  const [messageTimestamps, setMessageTimestamps] = useState<number[]>([]);
  const MAX_MESSAGES = 60;
  const TIME_WINDOW_MS = 60000;

  useEffect(() => {
    // Tải lịch sử chat
    const history = localStorage.getItem("vault_chat_history");
    if (history) {
      setMessages(JSON.parse(history));
    } else {
      setMessages([{ text: "Xin chào! Tôi có thể giúp gì cho bạn?", isBot: true }]);
    }

    // Kết nối BroadcastChannel
    const bc = new BroadcastChannel("vault_chat");
    channelRef.current = bc;

    bc.onmessage = (event) => {
      // Khi Admin gửi tin nhắn
      if (event.data.type === "admin_reply") {
        setMessages(prev => {
          const newMsgs = [...prev, { text: event.data.text, isBot: true }];
          localStorage.setItem("vault_chat_history", JSON.stringify(newMsgs));
          return newMsgs;
        });
      }
    };

    return () => {
      bc.close();
    };
  }, []);

  const handleSend = () => {
    if (!input.trim()) return;

    const now = Date.now();
    // Lọc các timestamp trong 1 phút qua
    const recentTimestamps = messageTimestamps.filter(ts => now - ts < TIME_WINDOW_MS);
    
    if (recentTimestamps.length >= MAX_MESSAGES) {
      setSpamWarning(true);
      setTimeout(() => setSpamWarning(false), 5000); // Ẩn cảnh báo sau 5s
      return;
    }

    // Cập nhật mảng timestamp
    setMessageTimestamps([...recentTimestamps, now]);

    const newMsgs = [...messages, { text: input, isBot: false }];
    setMessages(newMsgs);
    setInput("");
    localStorage.setItem("vault_chat_history", JSON.stringify(newMsgs));
    
    // Gửi sang kênh Broadcast cho Admin
    if (channelRef.current) {
      channelRef.current.postMessage({ type: "client_message", text: input, user: "Khách hàng" });
    }
  };

  return (
    <>
      <button 
        onClick={() => setIsOpen(true)}
        className={`fixed bottom-6 right-6 w-14 h-14 bg-indigo-600 hover:bg-indigo-500 text-white rounded-full flex items-center justify-center shadow-[0_0_20px_rgba(79,70,229,0.4)] transition-transform hover:scale-110 z-50 ${isOpen ? 'hidden' : 'block'}`}
      >
        <MessageSquare className="w-6 h-6" />
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div 
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            className="fixed bottom-6 right-6 w-[350px] h-[500px] bg-[#0a0a0f] border border-white/10 rounded-2xl shadow-2xl flex flex-col overflow-hidden z-50"
          >
            {/* Header */}
            <div className="h-16 bg-white/[0.02] border-b border-white/5 flex items-center justify-between px-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-indigo-500/20 flex items-center justify-center">
                  <Lock className="w-4 h-4 text-emerald-400" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">E2EE Support</h3>
                  <p className="text-[10px] text-emerald-400">Đã mã hóa đầu cuối</p>
                </div>
              </div>
              <button onClick={() => setIsOpen(false)} className="text-zinc-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Spam Warning */}
            <AnimatePresence>
              {spamWarning && (
                <motion.div 
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  className="bg-red-500/20 border-b border-red-500/30 px-4 py-2 flex items-center gap-2 text-xs text-red-400"
                >
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  <span>Bạn đang thao tác quá nhanh! Vui lòng đợi 1 phút trước khi gửi tiếp (Tối đa 60 tin/phút).</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Messages Area */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 flex flex-col bg-black/40">
              {messages.map((msg, idx) => (
                <div key={idx} className={`flex ${msg.isBot ? 'justify-start' : 'justify-end'}`}>
                  <div className={`max-w-[80%] rounded-2xl px-4 py-2 text-sm ${
                    msg.isBot 
                      ? 'bg-white/5 text-zinc-300 rounded-tl-none border border-white/5' 
                      : 'bg-indigo-600 text-white rounded-tr-none shadow-lg shadow-indigo-600/20'
                  }`}>
                    {msg.text}
                  </div>
                </div>
              ))}
            </div>

            {/* Input Area */}
            <div className="p-4 border-t border-white/5 bg-white/[0.02] flex items-center gap-2">
              <input 
                type="text" 
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                placeholder="Nhập tin nhắn bảo mật..."
                className="flex-1 bg-black border border-white/10 rounded-full px-4 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                disabled={spamWarning}
              />
              <button 
                onClick={handleSend}
                disabled={spamWarning}
                className={`w-10 h-10 rounded-full flex items-center justify-center text-white transition-colors shrink-0 ${spamWarning ? 'bg-zinc-700 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-500'}`}
              >
                <Send className="w-4 h-4 ml-0.5" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
