package single.cjj.bizfi.partner.dto;

import java.util.List;

public final class BusinessPartnerContracts {

    private BusinessPartnerContracts() {
    }

    public record LegacyPartyRequest(
            Long fid,
            String fname,
            String fcode,
            String funifiedSocialCreditCode
    ) {
    }

    public record LegacyPartyResponse(
            Long fid,
            String fname,
            String fcode,
            String fstatus,
            Long businessPartnerId,
            String roleType,
            String lifecycleStatus,
            String approvalStatus,
            String funifiedSocialCreditCode
    ) {
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
