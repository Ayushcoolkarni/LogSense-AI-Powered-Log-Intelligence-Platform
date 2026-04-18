package com.logplatform.alert.channel;

import com.logplatform.alert.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SlackNotificationChannel implements NotificationChannel {

    private final WebClient webClient;

    @Value("${alert.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${alert.slack.enabled:false}")
    private boolean enabled;

    @Override
    public void send(Alert alert) {
        if (!enabled || webhookUrl.isBlank()) {
            log.debug("Slack notifications disabled or webhook not configured");
            return;
        }

        String emoji = switch (alert.getSeverity()) {
            case CRITICAL -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
        };

        String text = String.format(
                "%s *[%s] Alert: %s*\n*Service:* %s\n*Type:* %s\n*Message:* %s\n*Alert ID:* %s",
                emoji,
                alert.getSeverity(),
                alert.getAnomalyType(),
                alert.getServiceName(),
                alert.getAnomalyType(),
                alert.getMessage(),
                alert.getId()
        );

        Map<String, Object> payload = Map.of("text", text);

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.info("Slack alert sent for alertId={}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send Slack alert for alertId={}: {}", alert.getId(), e.getMessage());
            throw new RuntimeException("Slack send failed", e);
        }
    }

    @Override
    public String channelName() {
        return "SLACK";
    }

    @Override
    public boolean supports(Alert.AlertSeverity severity) {
        // Slack gets all severity levels
        return true;
    }
}
