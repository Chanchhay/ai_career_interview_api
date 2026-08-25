package co.istad.ai_interview_app.shared.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static co.istad.ai_interview_app.shared.util.TextUtils.hasText;

/**
 * Encrypts the secrets an administrator saves through the console, so a
 * database dump or a stray backup does not hand out a live provider key.
 *
 * <p>AES-GCM with a 256-bit key supplied as {@code AI_SETTINGS_ENCRYPTION_KEY}
 * (base64, 32 bytes). The key lives with the deployment, never in the database
 * it protects — storing both together would encrypt nothing.
 *
 * <p>Ciphertext is stored as {@code v1:base64(iv || ciphertext)}. The version
 * prefix is what makes a future key rotation or algorithm change readable
 * rather than a guessing game about what a given row holds.
 *
 * <p>When no key is configured the cipher reports itself unavailable and
 * refuses to encrypt, rather than quietly storing a secret in the clear.
 */
@Slf4j
@Component
public class SecretCipher {

    private static final String PREFIX = "v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public SecretCipher(
            @Value("${app.security.settings-encryption-key:}") String encodedKey
    ) {
        this.key = parseKey(encodedKey);

        if (this.key == null) {
            log.warn(
                    "No settings encryption key configured; secrets cannot be saved "
                            + "from the admin console. Set AI_SETTINGS_ENCRYPTION_KEY to a "
                            + "base64-encoded 32-byte value to enable it."
            );
        }
    }

    /** Whether secrets can be stored at all. Surfaced to the console so it can explain why not. */
    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        if (key == null) {
            throw new IllegalStateException("No settings encryption key is configured");
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not encrypt the secret", ex);
        }
    }

    /**
     * Returns null for a value this deployment cannot read — no key configured,
     * the wrong key, or a corrupted row. Callers fall back to their configured
     * default, which keeps a bad row from taking the platform's AI offline.
     */
    public String decrypt(String stored) {
        if (!hasText(stored) || key == null) {
            return null;
        }

        if (!stored.startsWith(PREFIX)) {
            log.warn("Stored secret is not in the expected format; ignoring it");
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH)
            );

            byte[] plaintext = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("Could not decrypt a stored secret; falling back to the configured default", ex);
            return null;
        }
    }

    /** `sk-1234…cdef` → `••••••cdef`. Enough to tell two keys apart, useless to a thief. */
    public static String mask(String secret) {
        if (!hasText(secret)) {
            return null;
        }

        String trimmed = secret.trim();
        String tail = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);

        return "•".repeat(6) + tail;
    }

    private SecretKeySpec parseKey(String encodedKey) {
        if (!hasText(encodedKey)) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey.trim());
            if (decoded.length != KEY_LENGTH_BYTES) {
                log.error(
                        "Settings encryption key must decode to {} bytes but was {}; secrets are disabled",
                        KEY_LENGTH_BYTES, decoded.length
                );
                return null;
            }
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException ex) {
            log.error("Settings encryption key is not valid base64; secrets are disabled");
            return null;
        }
    }
}
