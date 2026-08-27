package single.cjj.botp.rule;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import single.cjj.botp.domain.BotpContracts.FieldMapping;
import single.cjj.botp.domain.BotpContracts.MappingSourceType;
import single.cjj.botp.domain.BotpContracts.WritebackMapping;

import java.util.List;

@Component
@ConditionalOnProperty(name = "botp.persistence.mode", havingValue = "mysql", matchIfMissing = true)
public class BotpBuiltInRuleInitializer implements ApplicationRunner {

    private final BotpRuleRepository repository;

    public BotpBuiltInRuleInitializer(BotpRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureProcurementOrderToReceiptRule();
        ensureProcurementReceiptToAcceptanceRule();
        ensureProcurementAcceptanceToInboundRule();
        ensureLegacyApRule();
        ensureFormalApRule();
        ensurePaymentApplicationToOrderRule();
    }

    private void ensureProcurementOrderToReceiptRule() {
        if (repository.findPublishedByCode("PURCHASE_ORDER_TO_RECEIPT").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "PURCHASE_ORDER_TO_RECEIPT",
                "采购订单下推收货单",
                "MATRIX",
                "ERP_PURCHASE_ORDER",
                "MATRIX",
                "ERP_PURCHASE_RECEIPT",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "date", "date", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerId", "businessPartnerId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerCode", "businessPartnerCode", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerName", "businessPartnerName", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "currencyCode", "currencyCode", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "warehouseId", "warehouseId", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "supplierDeliveryNo", "supplierDeliveryNo", null, false)
                ),
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "entryId", "purchaseOrderEntryId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "availableQuantity", "quantity", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "warehouseId", "warehouseId", null, false)
                ),
                List.of()
        ));
        repository.publish("PURCHASE_ORDER_TO_RECEIPT");
    }

    private void ensureProcurementReceiptToAcceptanceRule() {
        if (repository.findPublishedByCode("PURCHASE_RECEIPT_TO_ACCEPTANCE").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "PURCHASE_RECEIPT_TO_ACCEPTANCE",
                "采购收货下推验收单",
                "MATRIX",
                "ERP_PURCHASE_RECEIPT",
                "MATRIX",
                "ERP_PURCHASE_ACCEPTANCE",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "date", "date", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "purchaseReceiptId", "purchaseReceiptId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerId", "businessPartnerId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerCode", "businessPartnerCode", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerName", "businessPartnerName", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "currencyCode", "currencyCode", null, true)
                ),
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "entryId", "purchaseReceiptEntryId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "availableQuantity", "inspectionQuantity", null, true)
                ),
                List.of()
        ));
        repository.publish("PURCHASE_RECEIPT_TO_ACCEPTANCE");
    }

    private void ensureProcurementAcceptanceToInboundRule() {
        if (repository.findPublishedByCode("PURCHASE_ACCEPTANCE_TO_INBOUND").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "PURCHASE_ACCEPTANCE_TO_INBOUND",
                "采购验收下推入库单",
                "MATRIX",
                "ERP_PURCHASE_ACCEPTANCE",
                "MATRIX",
                "ERP_PURCHASE_INBOUND",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "date", "date", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "purchaseAcceptanceId", "purchaseAcceptanceId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerId", "businessPartnerId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerCode", "businessPartnerCode", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerName", "businessPartnerName", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "currencyCode", "currencyCode", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "warehouseId", "warehouseId", null, false)
                ),
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "entryId", "purchaseAcceptanceEntryId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "availableQuantity", "quantity", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "batchNo", "batchNo", null, false)
                ),
                List.of()
        ));
        repository.publish("PURCHASE_ACCEPTANCE_TO_INBOUND");
    }

    private void ensureLegacyApRule() {
        if (repository.findPublishedByCode("AP_TO_PAYMENT_APPLICATION").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "AP_TO_PAYMENT_APPLICATION",
                "应付单下推付款申请单",
                "MATRIX",
                "FI_AP_DOC",
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "counterparty", "counterparty", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payMethod", "payMethod", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operator", "operator", null, false),
                        new FieldMapping(MappingSourceType.CONSTANT, null, "docType", "AP_PAYMENT_APPLY", true)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "appliedAmount", "RECOMPUTE"))
        ));
        repository.publish("AP_TO_PAYMENT_APPLICATION");
    }

    private void ensureFormalApRule() {
        if (repository.findPublishedByCode("FORMAL_AP_TO_PAYMENT_APPLICATION").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "FORMAL_AP_TO_PAYMENT_APPLICATION",
                "正式应付下推付款申请",
                "MATRIX",
                "FI_AP_PAYABLE",
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "businessPartnerId", "counterparty", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payMethod", "payMethod", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "operatorId", null, false)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "reservedAmount", "RECOMPUTE"))
        ));
        repository.publish("FORMAL_AP_TO_PAYMENT_APPLICATION");
    }
    private void ensurePaymentApplicationToOrderRule() {
        if (repository.findPublishedByCode("PAYMENT_APPLICATION_TO_PAYMENT_ORDER").isPresent()) {
            return;
        }
        repository.saveDraft(new RuleSaveRequest(
                "PAYMENT_APPLICATION_TO_PAYMENT_ORDER",
                "付款申请下推付款单",
                "MATRIX",
                "FI_PAYMENT_APPLICATION",
                "MATRIX",
                "FI_PAYMENT_ORDER",
                List.of(
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "number", "sourceBillNo", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "orgId", "orgId", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "paymentMethod", "paymentMethod", null, true),
                        new FieldMapping(MappingSourceType.SOURCE_FIELD, "plannedPayDate", "plannedPayDate", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "tenantId", "tenantId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceSystemCode", "sourceSystem", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentType", "sourceDocumentType", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "sourceDocumentId", "sourceDocumentId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "executionId", "sourceExecutionId", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "pushAmount", "amount", null, true),
                        new FieldMapping(MappingSourceType.CONTEXT, "payerBankAccountId", "payerBankAccountId", null, false),
                        new FieldMapping(MappingSourceType.CONTEXT, "operatorId", "operatorId", null, false)
                ),
                List.of(),
                List.of(new WritebackMapping("allocatedAmount", "orderedAmount", "RECOMPUTE"))
        ));
        repository.publish("PAYMENT_APPLICATION_TO_PAYMENT_ORDER");
    }
}