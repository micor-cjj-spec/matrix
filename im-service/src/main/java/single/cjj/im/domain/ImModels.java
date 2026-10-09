package single.cjj.im.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ImModels {

    private ImModels() {
    }

    public static final class ChannelType {
        public static final String LOCAL = "LOCAL";
        public static final String EMAIL = "EMAIL";

        private ChannelType() {
        }
    }

    public static final class MessageStatus {
        public static final String ACCEPTED = "ACCEPTED";
        public static final String PROCESSING = "PROCESSING";
        public static final String SUCCESS = "SUCCESS";
        public static final String PARTIAL_SUCCESS = "PARTIAL_SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String UNKNOWN = "UNKNOWN";
        public static final String CANCELLED = "CANCELLED";
        public static final String EXPIRED = "EXPIRED";

        private MessageStatus() {
        }
    }

    public static final class ChannelStatus {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILED = "FAILED";
        public static final String RETRYING = "RETRYING";
        public static final String DEAD = "DEAD";
        public static final String UNKNOWN = "UNKNOWN";
        public static final String CANCELLED = "CANCELLED";
        public static final String EXPIRED = "EXPIRED";

        private ChannelStatus() {
        }
    }

    public static final class OutboxStatus {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String PUBLISHED = "PUBLISHED";
        public static final String RETRYING = "RETRYING";
        public static final String DEAD = "DEAD";

        private OutboxStatus() {
        }
    }

    public static final class ReadStatus {
        public static final String UNREAD = "UNREAD";
        public static final String READ = "READ";

        private ReadStatus() {
        }
    }

    public record SendMessageRequest(
            @NotBlank @Size(max = 128) String requestId,
            @Size(max = 64) String messageType,
            @Size(max = 64) String templateCode,
            @Size(max = 255) String title,
            String content,
            Set<String> channels,
            Map<String, Object> templateParams,
            @NotEmpty List<@Valid RecipientRequest> recipients,
            @Valid BusinessRef business,
            LocalDateTime scheduledTime,
            LocalDateTime expireTime,
            @Size(max = 1000) String callbackUrl,
            @Size(max = 64) String tenantId,
            @Size(max = 16) String priority
    ) {
    }

    public record RecipientRequest(
            @Size(max = 128) String userId,
            @Size(max = 128) String receiverName,
            @Email @Size(max = 255) String email
    ) {
    }

    public record BusinessRef(
            @Size(max = 64) String type,
            @Size(max = 128) String id,
            @Size(max = 1000) String actionUrl
    ) {
    }

    public record AcceptedResponse(
            String messageNo,
            String requestId,
            String status,
            boolean idempotentReplay
    ) {
    }

    public record MessageStatusResponse(
            String messageNo,
            String requestId,
            String status,
            int totalChannels,
            int successChannels,
            int failedChannels,
            List<ChannelStatusResponse> channels,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }

    public record ChannelStatusResponse(
            String channelTaskId,
            String recipientId,
            String channel,
            String status,
            int retryCount,
            String providerMessageId,
            String errorCode,
            String errorMessage,
            LocalDateTime sentTime,
            LocalDateTime deliveredTime
    ) {
    }

    public record TemplateUpsertRequest(
            @NotBlank @Size(max = 64) String templateCode,
            @NotBlank @Size(max = 128) String templateName,
            @NotBlank @Size(max = 64) String messageType,
            String localTitleTemplate,
            String localBodyTemplate,
            String emailSubjectTemplate,
            String emailBodyTemplate,
            @NotEmpty Set<String> defaultChannels,
            @NotNull @Positive Integer version,
            String status
    ) {
    }

    public record MessageRecord(
            String id,
            String messageNo,
            String tenantId,
            String appCode,
            String requestId,
            String messageType,
            String templateCode,
            String title,
            String content,
            String priority,
            LocalDateTime scheduledTime,
            LocalDateTime expireTime,
            String businessType,
            String businessId,
            String actionUrl,
            String status,
            int totalChannels,
            int successChannels,
            int failedChannels,
            String callbackUrl,
            String callbackStatus,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }

    public record RecipientRecord(
            String id,
            String messageId,
            String receiverType,
            String receiverId,
            String receiverName,
            String email,
            String readStatus,
            LocalDateTime readTime,
            LocalDateTime createdTime
    ) {
    }

    public record ChannelTaskRecord(
            String id,
            String messageId,
            String recipientId,
            String channelType,
            String subject,
            String content,
            String status,
            int retryCount,
            int maxRetryCount,
            LocalDateTime nextRetryTime,
            String providerCode,
            String providerMessageId,
            String lastErrorCode,
            String lastErrorMessage,
            LocalDateTime sentTime,
            LocalDateTime deliveredTime,
            LocalDateTime processingStartedTime,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }

    public record OutboxRecord(
            String id,
            String eventId,
            String aggregateType,
            String aggregateId,
            String eventType,
            String payloadJson,
            String status,
            int retryCount,
            LocalDateTime nextRetryTime,
            String lastError,
            LocalDateTime createdTime,
            LocalDateTime publishedTime
    ) {
    }

    public record TemplateRecord(
            String id,
            String templateCode,
            String templateName,
            String messageType,
            String localTitleTemplate,
            String localBodyTemplate,
            String emailSubjectTemplate,
            String emailBodyTemplate,
            String defaultChannels,
            int version,
            String status,
            LocalDateTime createdTime,
            LocalDateTime updatedTime
    ) {
    }

    public record LocalNotificationRecord(
            String id,
            String messageId,
            String recipientId,
            String userId,
            String title,
            String content,
            String messageType,
            String businessType,
            String businessId,
            String actionUrl,
            String pushStatus,
            String readStatus,
            LocalDateTime readTime,
            LocalDateTime createdTime
    ) {
    }

    public record PagedResult<T>(
            long total,
            int page,
            int size,
            List<T> records
    ) {
    }

    public record ChannelSendResult(
            boolean success,
            boolean retryable,
            String providerMessageId,
            String errorCode,
            String errorMessage
    ) {
        public static ChannelSendResult success(String providerMessageId) {
            return new ChannelSendResult(true, false, providerMessageId, null, null);
        }

        public static ChannelSendResult retryable(String errorCode, String errorMessage) {
            return new ChannelSendResult(false, true, null, errorCode, errorMessage);
        }

        public static ChannelSendResult permanentFailure(String errorCode, String errorMessage) {
            return new ChannelSendResult(false, false, null, errorCode, errorMessage);
        }
    }
}
