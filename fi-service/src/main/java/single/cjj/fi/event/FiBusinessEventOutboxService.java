package single.cjj.fi.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FiBusinessEventOutboxService {

    private final FiBusinessEventOutboxMapper mapper;
    private final ObjectMapper objectMapper;

    public FiBusinessEventOutboxService(
            FiBusinessEventOutboxMapper mapper,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FiBusinessEventOutboxEntity append(
            String tenantId,
            Long orgId,
            String eventType,
            String domainCode,
            String aggregateType,
            Long aggregateId,
            Long aggregateVersion,
            String documentType,
            String documentNo,
            LocalDate businessDate,
            String correlationId,
            String causationId,
            String traceId,
            Long operatorId,
            String routingKey,
            Object payload
    ) {
        LocalDateTime now = LocalDateTime.now();
        FiBusinessEventOutboxEntity entity = new FiBusinessEventOutboxEntity();
        entity.setFtenantId(tenantId);
        entity.setForgId(orgId);
        entity.setFeventId("BE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase());
        entity.setFeventType(eventType);
        entity.setFeventVersion(1);
        entity.setFproducerService("fi-service");
        entity.setFdomainCode(domainCode);
        entity.setFaggregateType(aggregateType);
        entity.setFaggregateId(String.valueOf(aggregateId));
        entity.setFaggregateVersion(aggregateVersion == null ? 0L : aggregateVersion);
        entity.setFsourceSystemCode("MATRIX");
        entity.setFsourceDocumentType(documentType);
        entity.setFsourceDocumentId(String.valueOf(aggregateId));
        entity.setFsourceDocumentNo(documentNo);
        entity.setFbusinessDate(businessDate);
        entity.setFcorrelationId(correlationId);
        entity.setFcausationId(causationId);
        entity.setFtraceId(traceId);
        entity.setFoperatorId(operatorId);
        entity.setFroutingKey(routingKey);
        entity.setFpayloadJson(toJson(payload));
        entity.setFstatus("PENDING");
        entity.setFretryCount(0);
        entity.setFmaxRetry(10);
        entity.setFcreateBy(operatorId);
        entity.setFcreateTime(now);
        entity.setFmodifyBy(operatorId);
        entity.setFmodifyTime(now);
        entity.setFdeleteFlag(0);
        entity.setFversion(0);
        mapper.insert(entity);
        return entity;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException("FI业务事件 Payload 序列化失败");
        }
    }
}
