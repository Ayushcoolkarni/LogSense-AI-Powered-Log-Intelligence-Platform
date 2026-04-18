package com.logplatform.alert.channel;

import com.logplatform.alert.model.Alert;

/**
 * Pluggable notification channel interface.
 * Add new channels (PagerDuty, OpsGenie, SMS) by implementing this interface.
 */
public interface NotificationChannel {
    void send(Alert alert);
    String channelName();
    boolean supports(Alert.AlertSeverity severity);
}
