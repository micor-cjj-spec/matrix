package single.cjj.im.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.im.application.ImApplicationService;
import single.cjj.im.application.ImApplicationService.AuthenticatedApplication;
import single.cjj.im.domain.ImModels.AcceptedResponse;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.SendMessageRequest;
import single.cjj.im.domain.ImModels.TemplateRecord;
import single.cjj.im.repository.ImMessageRepository;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReliableMessageFacade {

    private final MessageCommandService commandService;
    private final ImMessageRepository repository;
    private final ImApplicationService applicationService;

    public ReliableMessageFacade(MessageCommandService commandService,
                                 ImMessageRepository repository,
                                 ImApplicationService applicationService) {
        this.commandService = commandService;
        this.repository = repository;
        this.applicationService = applicationService;
    }

    public AcceptedResponse acceptDirect(String appCode, SendMessageRequest request) {
        SendMessageRequest prepared = prepare(appCode, request, false);
        return executeIdempotently(appCode, prepared, false);
    }

    public AcceptedResponse acceptTemplate(String appCode, SendMessageRequest request) {
        SendMessageRequest prepared = prepare(appCode, request, true);
        return executeIdempotently(appCode, prepared, true);
    }

    private SendMessageRequest prepare(String appCode, SendMessageRequest request, boolean templateMode) {
        AuthenticatedApplication application = applicationService.requireEnabled(appCode);
        String tenantId = StringUtils.hasText(request.tenantId()) ? request.tenantId().trim() : application.tenantId();
        if (!application.tenantId().equals(tenantId)) {
            throw new ImBusinessException("请求 tenantId 与调用应用所属租户不一致");
        }

        Set<String> channels = resolveChannels(request, templateMode);
        if (!application.allowedChannels().containsAll(channels)) {
            Set<String> denied = new LinkedHashSet<>(channels);
            denied.removeAll(application.allowedChannels());
            throw new ImBusinessException("调用应用无权使用渠道: " + String.join(",", denied));
        }

        String callbackUrl = resolveCallbackUrl(application, request.callbackUrl());
        return new SendMessageRequest(
                request.requestId(),
                request.messageType(),
                request.templateCode(),
                request.title(),
                request.content(),
                request.channels(),
                request.templateParams(),
                request.recipients(),
                request.business(),
                request.scheduledTime(),
                request.expireTime(),
                callbackUrl,
                tenantId,
                request.priority()
        );
    }

    private Set<String> resolveChannels(SendMessageRequest request, boolean templateMode) {
        if (request.channels() != null && !request.channels().isEmpty()) {
            return normalize(request.channels());
        }
        if (!templateMode || !StringUtils.hasText(request.templateCode())) {
            return Set.of();
        }
        TemplateRecord template = repository.findLatestEnabledTemplate(request.templateCode().trim())
                .orElseThrow(() -> new ImBusinessException("消息模板不存在或未启用"));
        return Arrays.stream(template.defaultChannels().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveCallbackUrl(AuthenticatedApplication application, String requestedCallbackUrl) {
        if (StringUtils.hasText(requestedCallbackUrl)) {
            if (!StringUtils.hasText(application.callbackUrl())
                    || !application.callbackUrl().equals(requestedCallbackUrl.trim())) {
                throw new ImBusinessException("callbackUrl 必须使用应用登记的回调地址");
            }
            return requestedCallbackUrl.trim();
        }
        return application.callbackUrl();
    }

    private AcceptedResponse executeIdempotently(String appCode,
                                                 SendMessageRequest request,
                                                 boolean templateMode) {
        try {
            return templateMode
                    ? commandService.acceptTemplate(appCode, request)
                    : commandService.acceptDirect(appCode, request);
        } catch (DuplicateKeyException duplicate) {
            MessageRecord existing = awaitExisting(appCode, request.requestId());
            if (existing == null) {
                throw duplicate;
            }
            return new AcceptedResponse(existing.messageNo(), existing.requestId(), existing.status(), true);
        }
    }

    private MessageRecord awaitExisting(String appCode, String requestId) {
        for (int attempt = 0; attempt < 8; attempt++) {
            MessageRecord existing = repository.findMessageByAppRequest(appCode, requestId).orElse(null);
            if (existing != null) {
                return existing;
            }
            try {
                Thread.sleep(25L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private Set<String> normalize(Set<String> channels) {
        return channels.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
