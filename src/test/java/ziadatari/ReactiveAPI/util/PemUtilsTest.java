package ziadatari.ReactiveAPI.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PemUtilsTest {

    private static final String HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String FOOTER = "-----END PUBLIC KEY-----";

    @Test
    void testNormalizeProperPem() {
        String input = HEADER + "\nABC\n" + FOOTER;
        String expected = HEADER + "\nABC\n" + FOOTER;
        assertEquals(expected, PemUtils.normalizePem(input, HEADER, FOOTER));
    }

    @Test
    void testNormalizeOneLiner() {
        String input = HEADER + "ABC" + FOOTER;
        String expected = HEADER + "\nABC\n" + FOOTER;
        assertEquals(expected, PemUtils.normalizePem(input, HEADER, FOOTER));
    }

    @Test
    void testNormalizeEscapedNewlines() {
        String input = HEADER + "\\nABC\\n" + FOOTER;
        String expected = HEADER + "\nABC\n" + FOOTER;
        assertEquals(expected, PemUtils.normalizePem(input, HEADER, FOOTER));
    }

    @Test
    void testNormalizeWithWhitespace() {
        String input = HEADER + "  A B C  " + FOOTER;
        String expected = HEADER + "\nABC\n" + FOOTER;
        assertEquals(expected, PemUtils.normalizePem(input, HEADER, FOOTER));
    }

    @Test
    void testNormalizeNull() {
        assertNull(PemUtils.normalizePem(null, HEADER, FOOTER));
    }

    @Test
    void testNormalizeNoHeaders() {
        String input = "JUST_DATA";
        // Should return as is but warn
        assertEquals(input, PemUtils.normalizePem(input, HEADER, FOOTER));
    }
}
