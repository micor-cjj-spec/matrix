package single.cjj.bizfi.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class AlertNotifier {

    @Value("${alert.webhook-url:${ALERT_WEBHOOK_URL:}}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void notifyError(String title, String detail) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "text");

            Map<String, String> text = new HashMap<>();
            text.put("text", "[matrix告警] " + title + "\n" + detail);
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("send alert webhook failed: {}", e.getMessage());
        }
    }
}
