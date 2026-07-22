package single.cjj.im.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImSecretCodecTest {

    private static final String MASTER_KEY =
            "bWF0cml4LWltLWRldi1tYXN0ZXIta2V5LTMyYnl0ZSE=";

    @Test
    void encryptsWithRandomIvAndDecrypts() {
        ImSecretCodec codec = new ImSecretCodec(MASTER_KEY);

        String first = codec.encrypt("scheduler-secret");
        String second = codec.encrypt("scheduler-secret");

        assertTrue(first.startsWith("v1:"));
        assertNotEquals(first, second);
        assertEquals("scheduler-secret", codec.decrypt(first));
        assertEquals("scheduler-secret", codec.decrypt(second));
    }
}
