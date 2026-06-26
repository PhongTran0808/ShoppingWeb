"use client";

import { useState, useEffect, useRef } from "react";
import { Send, User, ShieldCheck, MessageSquare } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function AdminChatPage() {
  const [messages, setMessages] = useState<{text: string, isBot: boolean, user?: string}[]>([]);
  const [input, setInput] = useState("");
  const [selectedUser, setSelectedUser] = useState<string | null>(null);
  const channelRef = useRef<BroadcastChannel | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const history = localStorage.getItem("vault_chat_history");
    if (history) {
      setMessages(JSON.parse(history));
    }

    const bc = new BroadcastChannel("vault_chat");
    channelRef.current = bc;

    bc.onmessage = (event) => {
      if (event.data.type === "client_message") {
        setMessages(prev => {
          const newMsgs = [...prev, { text: event.data.text, isBot: false, user: event.data.user || "Khách ẩn danh" }];
          localStorage.setItem("vault_chat_history", JSON.stringify(newMsgs));
          return newMsgs;
        });
      }
    };

    return () => bc.close();
  }, []);

  // Cuộn xuống tin nhắn mới nhất
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, selectedUser]);

  const handleReply = () => {
    if (!input.trim() || !selectedUser) return;

    const newMsgs = [...messages, { text: input, isBot: true, user: selectedUser }];
    setMessages(newMsgs);
    setInput("");
    localStorage.setItem("vault_chat_history", JSON.stringify(newMsgs));

    if (channelRef.current) {
      // Gửi targetUser để client biết
      channelRef.current.postMessage({ type: "admin_reply", text: input, targetUser: selectedUser });
    }
  };

  // Lấy danh sách user duy nhất
  const usersList = Array.from(new Set(messages.filter(m => !m.isBot).map(m => m.user || "Khách ẩn danh")));
  if (usersList.length > 0 && !selectedUser) {
    setSelectedUser(usersList[0]);
  }

  const currentMessages = messages.filter(m => m.user === selectedUser);

  return (
    <div className="space-y-4 h-[calc(100vh-110px)] flex flex-col">
      <div className="flex justify-between items-center shrink-0">
        <div>
          <h2 className="text-2xl font-bold">Live Support (E2EE)</h2>
          <p className="text-zinc-400">Giám sát và phản hồi khách hàng theo thời gian thực.</p>
        </div>
        <div className="flex items-center gap-2 bg-emerald-500/10 text-emerald-400 px-3 py-1 rounded-full text-sm border border-emerald-500/20">
          <ShieldCheck className="w-4 h-4" /> Real-time Sync Active
        </div>
      </div>

      <div className="bg-white/[0.03] border border-white/5 rounded-2xl flex-1 flex overflow-hidden backdrop-blur-sm shadow-xl">
        {/* Sidebar - Users List */}
        <div className="w-1/3 max-w-[300px] border-r border-white/5 bg-black/20 flex flex-col">
          <div className="p-4 border-b border-white/5 font-semibold text-white flex items-center gap-2">
            <MessageSquare className="w-5 h-5 text-indigo-400" /> Tin nhắn gần đây
          </div>
          <div className="flex-1 overflow-y-auto">
            {usersList.length === 0 ? (
              <div className="p-4 text-center text-zinc-500 text-sm">Chưa có khách hàng</div>
            ) : (
              usersList.map((usr, idx) => (
                <div 
                  key={idx} 
                  onClick={() => setSelectedUser(usr)}
                  className={`p-4 border-b border-white/5 cursor-pointer flex items-center gap-3 transition-colors ${selectedUser === usr ? 'bg-white/10 border-l-4 border-l-indigo-500' : 'hover:bg-white/5 border-l-4 border-l-transparent'}`}
                >
                  <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center shrink-0">
                    <User className="w-5 h-5 text-indigo-400" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="text-white text-sm font-semibold truncate">{usr}</h4>
                    <p className="text-xs text-zinc-400 truncate">
                      {messages.filter(m => m.user === usr).slice(-1)[0]?.text || "..."}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Main Chat Area */}
        <div className="flex-1 flex flex-col bg-black/10">
          {selectedUser ? (
            <>
              {/* Chat Header */}
              <div className="p-4 border-b border-white/5 bg-white/5 flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-indigo-500/20 flex items-center justify-center">
                  <User className="w-5 h-5 text-indigo-400" />
                </div>
                <div>
                  <h4 className="text-white font-semibold">{selectedUser}</h4>
                  <span className="text-xs text-emerald-400 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 block"></span> Đang trực tuyến
                  </span>
                </div>
              </div>

              {/* Messages Area */}
              <div className="flex-1 overflow-y-auto p-6 space-y-4">
                {currentMessages.length === 0 ? (
                  <div className="h-full flex items-center justify-center text-zinc-500">
                    Bắt đầu cuộc trò chuyện với {selectedUser}
                  </div>
                ) : (
                  currentMessages.map((msg, idx) => (
                    <div key={idx} className={`flex ${msg.isBot ? 'justify-end' : 'justify-start'}`}>
                      <div className={`max-w-[70%] rounded-2xl px-5 py-3 text-sm ${
                        msg.isBot 
                          ? 'bg-emerald-600 text-white rounded-tr-none shadow-lg' 
                          : 'bg-white/5 text-zinc-200 rounded-tl-none border border-white/5'
                      }`}>
                        {msg.text}
                      </div>
                    </div>
                  ))
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Reply Area */}
              <div className="p-4 bg-black/40 border-t border-white/5 flex items-center gap-3 shrink-0">
                <input 
                  type="text" 
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleReply()}
                  placeholder={`Nhập tin nhắn cho ${selectedUser}...`}
                  className="flex-1 bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white focus:outline-none focus:border-emerald-500"
                />
                <Button 
                  onClick={handleReply}
                  className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-medium"
                >
                  <Send className="w-4 h-4" />
                </Button>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-zinc-500 flex-col gap-4">
              <MessageSquare className="w-12 h-12 opacity-20" />
              <p>Chọn một khách hàng để bắt đầu chat</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
