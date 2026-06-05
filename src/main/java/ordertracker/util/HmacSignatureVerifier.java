package ordertracker.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
public class HmacSignatureVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";

    public boolean verify(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || secret == null) return false;

        try {
            String expected = "sha256=" + computeHmac(payload, secret);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.warn("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String computeHmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}