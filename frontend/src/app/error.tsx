"use client";

import { useEffect } from "react";
import { AlertTriangle } from "lucide-react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Ghi log lỗi ra console
    console.error("Global Error Caught:", error);
  }, [error]);

  return (
    <div className="min-h-screen bg-[#05050A] text-white flex flex-col items-center justify-center p-6">
      <div className="bg-red-500/10 border border-red-500/20 rounded-2xl p-8 max-w-2xl w-full text-center shadow-2xl z-[999]">
        <div className="w-20 h-20 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-6">
          <AlertTriangle className="w-10 h-10 text-red-500" />
        </div>
        <h1 className="text-3xl font-bold mb-4 text-white">Oops! Đã xảy ra lỗi nghiêm trọng</h1>
        <div className="bg-black/50 rounded-xl p-4 mb-6 overflow-auto max-h-[300px] text-left border border-white/5">
          <p className="text-red-400 font-mono text-sm mb-2 font-bold">{error.name}: {error.message}</p>
          <pre className="text-zinc-400 font-mono text-xs whitespace-pre-wrap">{error.stack}</pre>
        </div>
        <button
          onClick={() => reset()}
          className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-3 px-8 rounded-xl transition-colors"
        >
          Thử tải lại trang
        </button>
      </div>
    </div>
  );
}
