package single.cjj.erp.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import single.cjj.bizfi.exception.BizException;
import single.cjj.erp.event.entity.BusinessEventOutboxEntity;
import single.cjj.erp.event.mapper.BusinessEventOutboxMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BusinessEventOutboxService {

    private final BusinessEventOutboxMapper mapper;
    private final ObjectMapper objectMapper;

    public BusinessEventOutboxService(BusinessEventOutboxMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BusinessEventOutboxEntity append(
            String tenantId,
            Long orgId,
            String eventType,
            String aggregateType,
            Long aggregateId,
            Long aggregateVersion,
            String documentType,
            String documentNo,
            LocalDate businessDate,
            Long operatorId,
            Object payload
    ) {
        String eventId = "BE-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        BusinessEventOutboxEntity entity = new BusinessEventOutboxEntity();
        entity.setFtenantId(tenantId);
        entity.setForgId(orgId);
        entity.setFeventId(eventId);
        entity.setFeventType(eventType);
        entity.setFeventVersion(1);
        entity.setFproducerService("erp-service");
        entity.setFdomainCode("PROCUREMENT");
        entity.setFaggregateType(aggregateType);
        entity.setFaggregateId(String.valueOf(aggregateId));
        entity.setFaggregateVersion(aggregateVersion == null ? 0L : aggregateVersion);
        entity.setFsourceSystemCode("MATRIX");
        entity.setFsourceDocumentType(documentType);
        entity.setFsourceDocumentId(String.valueOf(aggregateId));
        entity.setFsourceDocumentNo(documentNo);
        entity.setFbusinessDate(businessDate);
        entity.setFoperatorId(operatorId);
        entity.setFroutingKey(routingKey(eventType));
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

    private String routingKey(String eventType) {
        return switch (eventType) {
            case "PURCHASE_REQUEST_APPROVED" -> "biz.procurement.purchase_request.approved";
            case "PURCHASE_SOURCING_AWARDED" -> "biz.procurement.sourcing.awarded";
            case "PURCHASE_CONTRACT_EFFECTIVE" -> "biz.procurement.purchase_contract.effective";
            case "PURCHASE_RECEIPT_CONFIRMED" -> "biz.procurement.purchase_receipt.confirmed";
            case "PURCHASE_ACCEPTANCE_CONFIRMED" -> "biz.procurement.purchase_acceptance.confirmed";
            case "PURCHASE_INBOUND_CONFIRMED" -> "biz.procurement.purchase_inbound.confirmed";
            case "SUPPLIER_INVOICE_CONFIRMED" -> "biz.procurement.supplier_invoice.confirmed";
            default -> "biz.procurement.event";
        };
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BizException("业务事件 Payload 序列化失败");
        }
    }
}
