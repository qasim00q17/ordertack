package ordertracker;

import ordertracker.util.HmacSignatureVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignatureVerifierTest {

    private final HmacSignatureVerifier verifier = new HmacSignatureVerifier();

    @Test
    void verify_correctSignature_returnsTrue() throws Exception {
        String payload = "{\"eventType\":\"payment.succeeded\"}";
        String secret  = "my-test-secret";

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) hex.append(String.format("%02x", b));
        String signature = "sha256=" + hex;

        assertThat(verifier.verify(payload, signature, secret)).isTrue();
    }

    @Test
    void verify_wrongSignature_returnsFalse() {
        assertThat(verifier.verify("payload", "sha256=wrongsig", "secret")).isFalse();
    }

    @Test
    void verify_nullSignature_returnsFalse() {
        assertThat(verifier.verify("payload", null, "secret")).isFalse();
    }
}