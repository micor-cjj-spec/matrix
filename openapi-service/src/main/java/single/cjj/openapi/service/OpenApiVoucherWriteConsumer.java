package single.cjj.openapi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import single.cjj.bizfi.entity.ApiResponse;
import single.cjj.openapi.client.FiVoucherWriteClient;
import single.cjj.openapi.contract.OpenVoucherDraftCreateCommand;
import single.cjj.openapi.contract.OpenVoucherDraftCreateResult;
import single.cjj.openapi.contract.OpenVoucherDraftLineCommand;
import single.cjj.openapi.entity.OpenApiWriteRequest;
import single.cjj.openapi.entity.OpenApiWriteRequestLine;
import single.cjj.openapi.mapper.OpenApiWriteRequestMapper;

import java.util.List;

@Slf4j
@Component
public class OpenApiVoucherWriteConsumer {

    private final OpenApiWriteRequestMapper requestMapper;
    private final OpenApiVoucherWriteService writeService;
    private final OpenApiWriteStateService stateService;
    private final FiVoucherWriteClient voucherWriteClient;

    public OpenApiVoucherWriteConsumer(OpenApiWriteRequestMapper requestMapper,
                                       OpenApiVoucherWriteService writeService,
                                       OpenApiWriteStateService stateService,
                                       FiVoucherWriteClient voucherWriteClient) {
        this.requestMapper = requestMapper;
        this.writeService = writeService;
        this.stateService = stateService;
        this.voucherWriteClient = voucherWriteClient;
    }

    @RabbitListener(queues = "${matrix.openapi.write.queue:matrix.openapi.voucher.write.queue}")
    public void consume(String writeRequestIdText) {
        Long writeRequestId;
        try {
            writeRequestId = Long.valueOf(writeRequestIdText);
        } catch (Exception e) {
            log.error("invalid OpenAPI voucher write message: {}", writeRequestIdText);
            return;
        }

        if (!stateService.claimForProcessing(writeRequestId)) {
            return;
        }
        try {
            OpenApiWriteRequest request = requestMapper.selectById(writeRequestId);
            if (request == null) {
                return;
            }
            List<OpenApiWriteRequestLine> lines = writeService.lines(writeRequestId);
            OpenVoucherDraftCreateCommand command = new OpenVoucherDraftCreateCommand(
                    request.getRequestId(),
                    request.getTenantId(),
                    request.getOrganizationId(),
                    request.getBookId(),
                    request.getVoucherDate(),
                    request.getSummary(),
                    "openapi:" + request.getAppExternalId(),
                    lines.stream().map(this::toCommand).toList()
            );
            ApiResponse<OpenVoucherDraftCreateResult> response = voucherWriteClient.createDraft(command);
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                String message = response == null || !StringUtils.hasText(response.getMessage())
                        ? "财务服务创建凭证草稿失败" : response.getMessage();
                throw new IllegalStateException(message);
            }
            stateService.markSucceeded(writeRequestId, response.getData());
        } catch (Exception e) {
            log.warn("process OpenAPI voucher write failed, writeRequestId={}, message={}",
                    writeRequestId, e.getMessage());
            stateService.markFailed(
                    writeRequestId,
                    "OPENAPI_VOUCHER_50001",
                    StringUtils.hasText(e.getMessage()) ? e.getMessage() : "凭证草稿创建失败"
            );
        }
    }

    private OpenVoucherDraftLineCommand toCommand(OpenApiWriteRequestLine line) {
        return new OpenVoucherDraftLineCommand(
                line.getLineNo(),
                line.getAccountCode(),
                line.getSummary(),
                line.getDebitAmount(),
                line.getCreditAmount(),
                line.getCurrency(),
                line.getRate(),
                line.getOriginalAmount(),
                line.getCashflowItem()
        );
    }
}
