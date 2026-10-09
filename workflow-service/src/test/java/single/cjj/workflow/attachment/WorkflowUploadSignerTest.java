package single.cjj.workflow.attachment;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowUploadSignerTest {

    private final WorkflowUploadSigner signer = new WorkflowUploadSigner("test-secret");

    @Test
    void shouldVerifyMatchingActionFileAndExpiration() {
        long expires = Instant.now().plusSeconds(60).getEpochSecond();
        String signature = signer.sign("UPLOAD", "file-1", expires);

        assertTrue(signer.verify("UPLOAD", "file-1", expires, signature));
        assertFalse(signer.verify("DOWNLOAD", "file-1", expires, signature));
        assertFalse(signer.verify("UPLOAD", "file-2", expires, signature));
    }

    @Test
    void shouldRejectExpiredSignature() {
        long expires = Instant.now().minusSeconds(1).getEpochSecond();
        String signature = signer.sign("UPLOAD", "file-1", expires);

        assertFalse(signer.verify("UPLOAD", "file-1", expires, signature));
    }
}
