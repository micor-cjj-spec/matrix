package single.cjj.botp.adapter;

import org.springframework.stereotype.Component;
import single.cjj.bizfi.exception.BizException;

import java.util.List;

@Component
public class BotpAdapterRegistry {

    private final List<BotpDocumentAdapter> adapters;

    public BotpAdapterRegistry(List<BotpDocumentAdapter> adapters) {
        this.adapters = List.copyOf(adapters);
    }

    public BotpDocumentAdapter require(String systemCode, String documentType) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(systemCode, documentType))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        "未注册单据适配器: system=" + systemCode + ", documentType=" + documentType
                ));
    }
}
