package single.cjj.fi.reconciliation.service;

import org.springframework.stereotype.Component;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.Difference;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InboundSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.InvoiceSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.PurchaseOrderSnapshot;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchLine;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class P2pThreeWayMatchPolicy {

    public Evaluation evaluate(ThreeWayMatchRequest request, ThreeWayMatchLine line) {
        List<Difference> differences = new ArrayList<>();
        PurchaseOrderSnapshot po = line == null ? null : line.purchaseOrder();
        InvoiceSnapshot invoice = line == null ? null : line.invoice();
        List<InboundSnapshot> inbounds = line == null || line.inbounds() == null ? List.of() : line.inbounds();

        if (po == null) {
            differences.add(diff("MISSING_DOCUMENT", "purchaseOrder", "PO", null, "BLOCKING", "缺少采购订单分录"));
        }
        if (inbounds.isEmpty()) {
            differences.add(diff("MISSING_DOCUMENT", "purchaseInbound", "至少一条已确认入库", "0", "BLOCKING", "缺少已确认采购入库"));
        }
        if (invoice == null) {
            differences.add(diff("MISSING_DOCUMENT", "supplierInvoice", "Invoice", null, "BLOCKING", "缺少供应商发票分录"));
        }

        BigDecimal inboundTotal = inbounds.stream()
                .map(InboundSnapshot::quantity)
                .map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal alreadyInvoiced = po == null ? BigDecimal.ZERO : nvl(po.invoicedQuantity());
        BigDecimal available = inboundTotal.subtract(alreadyInvoiced).max(BigDecimal.ZERO);

        if (po != null) {
            compareDecimal(differences, "QUANTITY_DIFFERENCE", "purchaseInbound.totalQuantity",
                    po.inboundQuantity(), inboundTotal, "采购订单累计入库数量与已确认入库快照合计不一致");
        }

        if (po != null && invoice != null) {
            compareLong(differences, "PARTNER_DIFFERENCE", "businessPartnerId", po.businessPartnerId(),
                    request.businessPartnerId(), "采购订单供应商与发票供应商不一致");
            compareText(differences, "CURRENCY_DIFFERENCE", "currencyCode", po.currencyCode(),
                    request.currencyCode(), "采购订单币种与发票币种不一致");
            compareLong(differences, "MATERIAL_DIFFERENCE", "materialId", po.materialId(), invoice.materialId(),
                    "采购订单物料与发票物料不一致");
            compareText(differences, "MATERIAL_DIFFERENCE", "materialCode", po.materialCode(), invoice.materialCode(),
                    "采购订单物料编码与发票物料编码不一致");
            compareText(differences, "SPECIFICATION_DIFFERENCE", "specification", po.specification(),
                    invoice.specification(), "采购订单规格与发票规格不一致");

            if (nvl(invoice.quantity()).compareTo(available) > 0) {
                differences.add(diff("QUANTITY_DIFFERENCE", "quantity", available.toPlainString(),
                        nvl(invoice.quantity()).toPlainString(), "BLOCKING", "发票数量超过已入库未开票数量"));
            }

            compareDecimal(differences, "PRICE_DIFFERENCE", "unitPrice", po.unitPrice(), invoice.unitPrice(),
                    "采购订单单价与发票单价不一致");
            BigDecimal expectedNet = nvl(invoice.quantity()).multiply(nvl(po.unitPrice())).setScale(2, RoundingMode.HALF_UP);
            compareDecimal(differences, "AMOUNT_DIFFERENCE", "netAmount", expectedNet, invoice.netAmount(),
                    "发票未税金额与订单单价计算结果不一致");
            compareDecimal(differences, "TAX_DIFFERENCE", "taxRate", po.taxRate(), invoice.taxRate(),
                    "采购订单税率与发票税率不一致");
            BigDecimal expectedTax = expectedNet.multiply(nvl(po.taxRate())).setScale(2, RoundingMode.HALF_UP);
            compareDecimal(differences, "TAX_DIFFERENCE", "taxAmount", expectedTax, invoice.taxAmount(),
                    "发票税额与订单税率计算结果不一致");
            compareDecimal(differences, "AMOUNT_DIFFERENCE", "grossAmount", expectedNet.add(expectedTax),
                    invoice.grossAmount(), "发票含税金额与订单计算结果不一致");
        }

        for (InboundSnapshot inbound : inbounds) {
            if (po == null) {
                break;
            }
            compareLong(differences, "PARTNER_DIFFERENCE", "inbound.businessPartnerId", po.businessPartnerId(),
                    inbound.businessPartnerId(), "采购入库供应商与采购订单不一致");
            compareText(differences, "CURRENCY_DIFFERENCE", "inbound.currencyCode", po.currencyCode(),
                    inbound.currencyCode(), "采购入库币种与采购订单不一致");
            compareLong(differences, "MATERIAL_DIFFERENCE", "inbound.materialId", po.materialId(),
                    inbound.materialId(), "采购入库物料与采购订单不一致");
            compareText(differences, "MATERIAL_DIFFERENCE", "inbound.materialCode", po.materialCode(),
                    inbound.materialCode(), "采购入库物料编码与采购订单不一致");
            compareText(differences, "SPECIFICATION_DIFFERENCE", "inbound.specification", po.specification(),
                    inbound.specification(), "采购入库规格与采购订单不一致");
            compareDecimal(differences, "PRICE_DIFFERENCE", "inbound.unitPrice", po.unitPrice(),
                    inbound.unitPrice(), "采购入库单价与采购订单不一致");
        }

        String result = differences.isEmpty()
                ? "MATCHED"
                : po == null || invoice == null || inbounds.isEmpty()
                ? "UNMATCHED"
                : "DIFFERENCE";
        return new Evaluation(result, available, List.copyOf(differences));
    }

    private void compareLong(List<Difference> out, String code, String field, Long expected, Long actual, String message) {
        if (!Objects.equals(expected, actual)) {
            out.add(diff(code, field, text(expected), text(actual), "BLOCKING", message));
        }
    }

    private void compareText(List<Difference> out, String code, String field, String expected, String actual, String message) {
        if (!Objects.equals(normalize(expected), normalize(actual))) {
            out.add(diff(code, field, expected, actual, "BLOCKING", message));
        }
    }

    private void compareDecimal(List<Difference> out, String code, String field, BigDecimal expected, BigDecimal actual, String message) {
        if (nvl(expected).compareTo(nvl(actual)) != 0) {
            out.add(diff(code, field, nvl(expected).toPlainString(), nvl(actual).toPlainString(), "BLOCKING", message));
        }
    }

    private Difference diff(String code, String field, String expected, String actual, String severity, String message) {
        return new Difference(code, field, expected, actual, severity, message);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    public record Evaluation(String result, BigDecimal availableInboundQuantity, List<Difference> differences) {
    }
}
