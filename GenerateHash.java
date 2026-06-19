import java.security.MessageDigest;
import java.util.HexFormat;

public class GenerateHash {
    public static void main(String[] args) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA3-512");
        String[] passwords = {
            "AdminPass@1", "AdminPass@2", "AdminPass@3",
            "UserPass@1", "UserPass@2", "UserPass@3", "UserPass@4",
            "UserPass@5", "UserPass@6", "UserPass@7", "UserPass@8",
            "UserPass@9", "UserPass@10"
        };
        for (String p : passwords) {
            byte[] hash = digest.digest(p.getBytes("UTF-8"));
            System.out.println(p + ":" + HexFormat.of().formatHex(hash));
        }
    }
}
