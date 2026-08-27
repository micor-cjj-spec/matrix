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
        return append(
                tenantId, orgId, "PROCUREMENT", eventType,
                aggregateType, aggregateId, aggregateVersion,
                documentType, documentNo, businessDate,
                operatorId, payload);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BusinessEventOutboxEntity append(
            String tenantId,
            Long orgId,
            String domainCode,
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
        entity.setFdomainCode(domainCode);
        entity.setFaggregateType(aggregateType);
        entity.setFaggregateId(String.valueOf(aggregateId));
        entity.setFaggregateVersion(aggregateVersion == null ? 0L : aggregateVersion);
        entity.setFsourceSystemCode("MATRIX");
        entity.setFsourceDocumentType(documentType);
        entity.setFsourceDocumentId(String.valueOf(aggregateId));
        entity.setFsourceDocumentNo(documentNo);
        entity.setFbusinessDate(businessDate);
        entity.setFoperatorId(operatorId);
        entity.setFroutingKey(routingKey(domainCode, eventType));
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

    private String routingKey(String domainCode, String eventType) {
        if ("CRM".equalsIgnoreCase(domainCode)) {
            return switch (eventType) {
                case "CRM_LEAD_QUALIFIED" -> "biz.crm.lead.qualified";
                case "CRM_LEAD_CONVERTED" -> "biz.crm.lead.converted";
                case "CRM_OPPORTUNITY_CREATED" -> "biz.crm.opportunity.created";
                case "CRM_OPPORTUNITY_WON" -> "biz.crm.opportunity.won";
                case "CRM_OPPORTUNITY_LOST" -> "biz.crm.opportunity.lost";
                default -> "biz.crm.event";
            };
        }
        if ("SALES".equalsIgnoreCase(domainCode)) {
            return switch (eventType) {
                case "SALES_QUOTATION_ACCEPTED" -> "biz.sales.quotation.accepted";
                case "SALES_CONTRACT_EFFECTIVE" -> "biz.sales.contract.effective";
                default -> "biz.sales.event";
            };
        }
        return switch (eventType) {
            case "PURCHASE_REQUEST_APPROVED" -> "biz.procurement.purchase_request.approved";
            case "PURCHASE_SOURCING_AWARDED" -> "biz.procurement.sourcing.awarded";
            case "PURCHASE_CONTRACT_EFFECTIVE" -> "biz.procurement.purchase_contract.effective";
            case "PURCHASE_DELIVERY_PLAN_PUBLISHED" -> "biz.procurement.delivery_plan.published";
            case "SUPPLIER_DELIVERY_RESPONSE_RECORDED" -> "biz.procurement.delivery_response.recorded";
            case "PURCHASE_DELIVERY_PLAN_CONFIRMED" -> "biz.procurement.delivery_plan.confirmed";
            case "PURCHASE_RETURN_CONFIRMED" -> "biz.procurement.purchase_return.confirmed";
            case "PURCHASE_CLAIM_CONFIRMED" -> "biz.procurement.purchase_claim.confirmed";
            case "PURCHASE_DEDUCTION_CONFIRMED" -> "biz.procurement.purchase_deduction.confirmed";
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