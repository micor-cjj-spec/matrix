package single.cjj.erp.integration.base;

import java.util.List;

public final class BaseBusinessPartnerContracts {

    private BaseBusinessPartnerContracts() {
    }

    public record BusinessPartnerDetail(
            Long fid,
            String ftenantId,
            String fcode,
            String fname,
            String fpartnerType,
            String funifiedSocialCreditCode,
            String fstatus,
            String fapprovalStatus,
            List<String> roles
    ) {
        public BusinessPartnerDetail {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
