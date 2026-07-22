package single.cjj.im.config;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import single.cjj.im.domain.ImModels.ChannelSendResult;
import single.cjj.im.domain.ImModels.ChannelTaskRecord;
import single.cjj.im.domain.ImModels.ChannelType;
import single.cjj.im.domain.ImModels.LocalNotificationRecord;
import single.cjj.im.domain.ImModels.MessageRecord;
import single.cjj.im.domain.ImModels.ReadStatus;
import single.cjj.im.domain.ImModels.RecipientRecord;
import single.cjj.im.repository.ImMessageRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
public class ChannelDeliveryConfiguration {

    @Bean
    ChannelHandler localChannelHandler(ImMessageRepository repository) {
        return new LocalChannelHandler(repository);
    }

    @Bean
    ChannelHandler emailChannelHandler(JavaMailSender mailSender,
                                       @Value("${im.email.from:}") String from) {
        return new EmailChannelHandler(mailSender, from);
    }

    public interface ChannelHandler {
        boolean supports(String channelType);

        ChannelSendResult send(MessageRecord message,
                               RecipientRecord recipient,
                               ChannelTaskRecord channelTask);
    }
}

class LocalChannelHandler implements ChannelDeliveryConfiguration.ChannelHandler {

    private final ImMessageRepository repository;

    LocalChannelHandler(ImMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(String channelType) {
        return ChannelType.LOCAL.equals(channelType);
    }

    @Override
    public ChannelSendResult send(MessageRecord message,
                                  RecipientRecord recipient,
                                  ChannelTaskRecord channelTask) {
        if (!StringUtils.hasText(recipient.receiverId()) || !"USER".equals(recipient.receiverType())) {
            return ChannelSendResult.permanentFailure("LOCAL_USER_MISSING", "本地提醒缺少 Matrix 用户ID");
        }
        LocalDateTime now = LocalDateTime.now();
        repository.insertLocalNotificationIfAbsent(new LocalNotificationRecord(
                UUID.randomUUID().toString().replace("-", ""),
                message.id(),
                recipient.id(),
                recipient.receiverId(),
                channelTask.subject(),
                channelTask.content(),
                message.messageType(),
                message.businessType(),
                message.businessId(),
                message.actionUrl(),
                "CREATED",
                ReadStatus.UNREAD,
                null,
                now
        ));
        return ChannelSendResult.success("local:" + channelTask.id());
    }
}

class EmailChannelHandler implements ChannelDeliveryConfiguration.ChannelHandler {

    private final JavaMailSender mailSender;
    private final String from;

    EmailChannelHandler(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public boolean supports(String channelType) {
        return ChannelType.EMAIL.equals(channelType);
    }

    @Override
    public ChannelSendResult send(MessageRecord message,
                                  RecipientRecord recipient,
                                  ChannelTaskRecord channelTask) {
        if (!StringUtils.hasText(recipient.email())) {
            return ChannelSendResult.permanentFailure("EMAIL_MISSING", "邮件渠道缺少收件人邮箱");
        }
        if (!StringUtils.hasText(from)) {
            return ChannelSendResult.permanentFailure("EMAIL_FROM_MISSING", "未配置 im.email.from");
        }
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient.email());
            helper.setSubject(channelTask.subject());
            helper.setText(channelTask.content(), true);
            mailSender.send(mimeMessage);
            return ChannelSendResult.success("smtp:" + channelTask.id());
        } catch (MailParseException e) {
            return ChannelSendResult.permanentFailure("EMAIL_CONTENT_INVALID", safeMessage(e));
        } catch (MailException | MessagingException e) {
            return ChannelSendResult.retryable("EMAIL_SEND_FAILED", safeMessage(e));
        }
    }

    private String safeMessage(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
    }
}
