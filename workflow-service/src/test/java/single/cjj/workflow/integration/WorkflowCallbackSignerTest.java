package single.cjj.workflow.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowCallbackSignerTest {

    @Test
    void signatureIsStableAndPrefixed() {
        WorkflowCallbackSigner signer = new WorkflowCallbackSigner("secret");
        String first = signer.sign(1000L, "{\"eventId\":\"evt1\"}");
        String second = signer.sign(1000L, "{\"eventId\":\"evt1\"}");

        assertEquals(first, second);
        assertTrue(first.startsWith("sha256="));
        assertEquals(71, first.length());
    }
}
