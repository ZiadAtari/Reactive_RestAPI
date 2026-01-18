package ziadatari.ReactiveAPI.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for PEM key normalization.
 */
public class PemUtils {

    private static final Logger logger = LoggerFactory.getLogger(PemUtils.class);

    /**
     * Normalizes a PEM string by handling escaped newlines and ensuring proper
     * PEM headers/footers with internal newlines and stripped whitespace.
     *
     * @param pem    The PEM string to normalize.
     * @param header The expected header (e.g., "-----BEGIN PRIVATE KEY-----").
     * @param footer The expected footer (e.g., "-----END PRIVATE KEY-----").
     * @return The normalized PEM string.
     */
    public static String normalizePem(String pem, String header, String footer) {
        if (pem == null) {
            return null;
        }

        // 1. Convert escaped newlines to actual newlines
        pem = pem.replace("\\n", "\n").replace("\\r", "\r");

        // 2. Ensure headers/footers are on their own lines and content is stripped of
        // whitespace
        if (pem.contains(header) && pem.contains(footer)) {
            int startIdx = pem.indexOf(header);
            int endIdx = pem.indexOf(footer);

            String content = pem.substring(startIdx + header.length(), endIdx).trim();

            // Remove all whitespace from the base64 content
            content = content.replaceAll("\\s+", "");

            // Reconstruct with guaranteed newlines
            return header + "\n" + content + "\n" + footer;
        }

        logger.warn("PEM string does not contain expected header/footer. Returning as is.");
        return pem;
    }
}
