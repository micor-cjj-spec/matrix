package single.cjj.scheduler.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SchedulerImNotificationServiceTest {

    @Test
    void usesSameCallbackCanonicalSignatureContract() {
        String signature = SchedulerImNotificationService.sign(
                "callback-secret",
                "https://example.com/api/scheduler/im/callbacks",
                "1784720000000",
                "nonce-001",
                "{\"eventId\":\"evt-1\",\"status\":\"FAILED\"}"
        );

        assertEquals(
                "4ddb13800966b1538eab1755e2ce4763a6510f871be1d394e0e18e54d98efb2a",
                signature
        );
    }
}
