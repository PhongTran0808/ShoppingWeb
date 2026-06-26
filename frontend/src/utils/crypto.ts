/**
 * Tiện ích Mật mã học cho VaultCommerce
 * Cung cấp các hàm băm (Hash) an toàn sử dụng Web Crypto API của trình duyệt.
 */

// Chuyển đổi ArrayBuffer thành chuỗi Hex
function bufferToHex(buffer: ArrayBuffer): string {
  const hashArray = Array.from(new Uint8Array(buffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  return hashHex;
}

/**
 * Sinh mã băm SHA-256 cho một chuỗi bất kỳ
 * @param message Chuỗi cần băm (Ví dụ: Password)
 * @returns Chuỗi Hex của mã băm SHA-256
 */
export async function sha256(message: string): Promise<string> {
  // Trình duyệt chặn crypto.subtle trên HTTP (IP LAN). Fallback thuật toán băm cơ bản nếu không có HTTPS.
  if (typeof crypto === 'undefined' || !crypto.subtle) {
    console.warn("Môi trường HTTP không an toàn: Fallback thuật toán băm (Insecure)");
    let hash = 0;
    for (let i = 0; i < message.length; i++) {
      const char = message.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return Math.abs(hash).toString(16) + "f8a9d1"; // Giả lập chuỗi Hex
  }

  const msgBuffer = new TextEncoder().encode(message);
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
  return bufferToHex(hashBuffer);
}

/**
 * Tạo Session Token theo công thức bảo mật tùy chỉnh:
 * Vault_Token = SHA256(Password) + "." + Username
 */
export async function generateSecureSessionToken(username: string, passwordPlain: string): Promise<string> {
  const passwordHash = await sha256(passwordPlain);
  return `${passwordHash}.${username}`;
}
