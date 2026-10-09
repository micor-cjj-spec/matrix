package single.cjj.im.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import single.cjj.im.domain.ImModels.AcceptedResponse;
import single.cjj.im.domain.ImModels.BusinessRef;
import single.cjj.im.domain.ImModels.ChannelStatus;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.ChannelType;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.MessageStatus;
import single.cjj.im.domain.ImModels.MessageStatusResponse;
import single.cjj.im.domain.ImModels.OutboxRecord;
import single.cjj.im.domain.ImModels.OutboxStatus;
import single.cjj.im.domain.ImModels.PagedResult;
import single.cjj.im.domain.ImModels.ReadStatus;
import single.cjj.im.domain.ImModels.RecipientRecord;
import single.cjj.im.domain.ImModels.RecipientRequest;
import single.cjj.im.domain.ImModels.SendMessageRequest;
import single.cjj.im.domain.ImModels.TemplateRecord;
import single.cjj.im.domain.ImModels.TemplateUpsertRequest;
import single.cjj.im.repository.ImMessageRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageCommandService {

    private static final Set<String> SUPPORTED_CHANNELS = Set.of(ChannelType.LOCAL, ChannelType.EMAIL);

    private final ImMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final int maxRetryCount;

    public MessageCommandService(ImMessageRepository repository,
                                 ObjectMapper objectMapper,
                                 @Value("${im.channel.max-retry-count:5}") int maxRetryCount) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.maxRetryCount = Math.max(1, maxRetryCount);
    }

    @Transactional
    public AcceptedResponse acceptDirect(String appCode, SendMessageRequest request) {
        return accept(appCode, request, false);
    }

    @Transactional
    public AcceptedResponse acceptTemplate(String appCode, SendMessageRequest request) {
        return accept(appCode, request, true);
    }

    public MessageStatusResponse queryStatus(String appCode, String messageNo) {
        MessageRecord message = repository.findMessageByNo(messageNo)
                .orElseThrow(() -> new ImBusinessException("消息不存在"));
        if (!message.appCode().equals(appCode)) {
            throw new ImBusinessException("无权查询该消息");
        }
        return new MessageStatusResponse(
                message.messageNo(),
                message.requestId(),
                message.status(),
                message.totalChannels(),
                message.successChannels(),
                message.failedChannels(),
                repository.findChannelStatusResponses(message.id()),
                message.createdTime(),
                message.updatedTime()
        );
    }

    @Transactional
    public TemplateRecord upsertTemplate(TemplateUpsertRequest request) {
        validateChannels(request.defaultChannels());
        LocalDateTime now = LocalDateTime.now();
        TemplateRecord record = new TemplateRecord(
                id(),
                request.templateCode().trim(),
                request.templateName().trim(),
                request.messageType().trim(),
                request.localTitleTemplate(),
                request.localBodyTemplate(),
                request.emailSubjectTemplate(),
                request.emailBodyTemplate(),
                normalizeChannels(request.defaultChannels()).stream().collect(Collectors.joining(",")),
                request.version(),
                StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase(Locale.ROOT) : "ENABLED",
                now,
                now
        );
        repository.upsertTemplate(record);
        return repository.findLatestEnabledTemplate(record.templateCode()).orElse(record);
    }

    public List<TemplateRecord> listTemplates() {
        return repository.listTemplates();
    }

    public PagedResult<LocalNotificationRecord> listNotifications(String userId,
                                                                   String readStatus,
                                                                   int page,
                                                                   int size) {
        requireUserId(userId);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String normalizedReadStatus = StringUtils.hasText(readStatus)
                ? readStatus.trim().toUpperCase(Locale.ROOT)
                : null;
        long total = repository.countLocalNotifications(userId, normalizedReadStatus);
        List<LocalNotificationRecord> records = repository.listLocalNotifications(
                userId,
                normalizedReadStatus,
                (safePage - 1) * safeSize,
                safeSize
        );
        return new PagedResult<>(total, safePage, safeSize, records);
    }

    public long unreadCount(String userId) {
        requireUserId(userId);
        return repository.countLocalNotifications(userId, ReadStatus.UNREAD);
    }

    @Transactional
    public boolean markRead(String notificationId, String userId) {
        requireUserId(userId);
        return repository.markNotificationRead(notificationId, userId, LocalDateTime.now()) > 0;
    }

    @Transactional
    public int markAllRead(String userId) {
        requireUserId(userId);
        return repository.markAllNotificationsRead(userId, LocalDateTime.now());
    }

    private AcceptedResponse accept(String appCode, SendMessageRequest request, boolean templateMode) {
        if (!StringUtils.hasText(appCode)) {
            throw new ImBusinessException("调用应用不能为空");
        }
        MessageRecord existing = repository.findMessageByAppRequest(appCode, request.requestId()).orElse(null);
        if (existing != null) {
            return new AcceptedResponse(existing.messageNo(), existing.requestId(), existing.status(), true);
        }

        TemplateRecord template = null;
        if (templateMode) {
            if (!StringUtils.hasText(request.templateCode())) {
                throw new ImBusinessException("模板发送时 templateCode 不能为空");
            }
            template = repository.findLatestEnabledTemplate(request.templateCode().trim())
                    .orElseThrow(() -> new ImBusinessException("消息模板不存在或未启用"));
        } else {
            if (!StringUtils.hasText(request.messageType())) {
                throw new ImBusinessException("直接发送时 messageType 不能为空");
            }
            if (!StringUtils.hasText(request.title()) || !StringUtils.hasText(request.content())) {
                throw new ImBusinessException("直接发送时 title 和 content 不能为空");
            }
        }

        Set<String> channels = resolveChannels(request.channels(), template);
        validateRecipients(request.recipients(), channels);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = request.scheduledTime() == null ? now : request.scheduledTime();
        if (request.expireTime() != null && !request.expireTime().isAfter(scheduledTime)) {
            throw new ImBusinessException("expireTime 必须晚于 scheduledTime");
        }

        String messageId = id();
        String messageNo = messageNo();
        BusinessRef business = request.business();
        int totalChannels = request.recipients().size() * channels.size();
        MessageRecord message = new MessageRecord(
                messageId,
                messageNo,
                textOrDefault(request.tenantId(), "default"),
                appCode,
                request.requestId().trim(),
                template == null ? request.messageType().trim() : template.messageType(),
                template == null ? request.templateCode() : template.templateCode(),
                template == null ? request.title() : render(template.localTitleTemplate(), request.templateParams()),
                template == null ? request.content() : render(template.localBodyTemplate(), request.templateParams()),
                textOrDefault(request.priority(), "NORMAL").toUpperCase(Locale.ROOT),
                scheduledTime,
                request.expireTime(),
                business == null ? null : business.type(),
                business == null ? null : business.id(),
                business == null ? null : business.actionUrl(),
                MessageStatus.ACCEPTED,
                totalChannels,
                0,
                0,
                request.callbackUrl(),
                null,
                now,
                now
        );
        repository.insertMessage(message);

        for (RecipientRequest recipientRequest : request.recipients()) {
            RecipientRecord recipient = new RecipientRecord(
                    id(),
                    messageId,
                    StringUtils.hasText(recipientRequest.userId()) ? "USER" : "EMAIL",
                    StringUtils.hasText(recipientRequest.userId()) ? recipientRequest.userId().trim() : recipientRequest.email().trim(),
                    recipientRequest.receiverName(),
                    recipientRequest.email(),
                    ReadStatus.UNREAD,
                    null,
                    now
            );
            repository.insertRecipient(recipient);

            for (String channel : channels) {
                String subject = template == null
                        ? request.title()
                        : render(ChannelType.EMAIL.equals(channel)
                                ? template.emailSubjectTemplate()
                                : template.localTitleTemplate(), request.templateParams());
                String body = template == null
                        ? request.content()
                        : render(ChannelType.EMAIL.equals(channel)
                                ? template.emailBodyTemplate()
                                : template.localBodyTemplate(), request.templateParams());
                if (!StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
                    throw new ImBusinessException("渠道 " + channel + " 的模板标题或正文为空");
                }

                String channelTaskId = id();
                ChannelTaskRecord channelTask = new ChannelTaskRecord(
                        channelTaskId,
                        messageId,
                        recipient.id(),
                        channel,
                        subject,
                        body,
                        ChannelStatus.PENDING,
                        0,
                        maxRetryCount,
                        scheduledTime,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        now,
                        now
                );
                repository.insertChannelTask(channelTask);
                repository.insertOutbox(new OutboxRecord(
                        id(),
                        "channel-task-created:" + channelTaskId,
                        "CHANNEL_TASK",
                        channelTaskId,
                        "CHANNEL_TASK_CREATED",
                        outboxPayload(channelTaskId),
                        OutboxStatus.PENDING,
                        0,
                        scheduledTime,
                        null,
                        now,
                        null
                ));
            }
        }
        return new AcceptedResponse(messageNo, request.requestId(), MessageStatus.ACCEPTED, false);
    }

    private Set<String> resolveChannels(Set<String> requested, TemplateRecord template) {
        if (requested != null && !requested.isEmpty()) {
            return normalizeChannels(requested);
        }
        if (template == null || !StringUtils.hasText(template.defaultChannels())) {
            throw new ImBusinessException("发送渠道不能为空");
        }
        return normalizeChannels(Set.of(template.defaultChannels().split(",")));
    }

    private Set<String> normalizeChannels(Set<String> channels) {
        return channels.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateChannels(Set<String> channels) {
        Set<String> normalized = normalizeChannels(channels);
        if (normalized.isEmpty()) {
            throw new ImBusinessException("发送渠道不能为空");
        }
        List<String> unsupported = normalized.stream().filter(value -> !SUPPORTED_CHANNELS.contains(value)).toList();
        if (!unsupported.isEmpty()) {
            throw new ImBusinessException("暂不支持的渠道: " + String.join(",", unsupported));
        }
    }

    private void validateRecipients(List<RecipientRequest> recipients, Set<String> channels) {
        validateChannels(channels);
        if (recipients == null || recipients.isEmpty()) {
            throw new ImBusinessException("接收人不能为空");
        }
        List<String> errors = new ArrayList<>();
        Set<String> localUsers = new java.util.HashSet<>();
        Set<String> emailAddresses = new java.util.HashSet<>();
        for (int i = 0; i < recipients.size(); i++) {
            RecipientRequest recipient = recipients.get(i);
            if (recipient == null) {
                errors.add("第" + (i + 1) + "个接收人为空");
                continue;
            }
            if (channels.contains(ChannelType.LOCAL)) {
                if (!StringUtils.hasText(recipient.userId())) {
                    errors.add("第" + (i + 1) + "个接收人缺少 userId，无法发送本地提醒");
                } else if (!localUsers.add(recipient.userId().trim())) {
                    errors.add("本地提醒接收人重复: " + recipient.userId().trim());
                }
            }
            if (channels.contains(ChannelType.EMAIL)) {
                if (!StringUtils.hasText(recipient.email())) {
                    errors.add("第" + (i + 1) + "个接收人缺少 email，无法发送邮件");
                } else if (!emailAddresses.add(recipient.email().trim().toLowerCase(Locale.ROOT))) {
                    errors.add("邮件接收人重复: " + recipient.email().trim());
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ImBusinessException(String.join("；", errors));
        }
    }

    private String render(String template, Map<String, Object> params) {
        if (template == null) {
            return null;
        }
        String result = template;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result = result.replace("${" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private String outboxPayload(String channelTaskId) {
        try {
            return objectMapper.writeValueAsString(Map.of("channelTaskId", channelTaskId));
        } catch (JsonProcessingException e) {
            throw new ImBusinessException("消息事件序列化失败", e);
        }
    }

    private void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new ImBusinessException("缺少当前用户身份");
        }
    }

    private String textOrDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String id() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String messageNo() {
        return "MSG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
