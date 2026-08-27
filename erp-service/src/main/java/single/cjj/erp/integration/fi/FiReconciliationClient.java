package single.cjj.erp.integration.fi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchRequest;
import single.cjj.erp.integration.fi.P2pReconciliationContracts.ThreeWayMatchResponse;

@FeignClient(
        name = "fi-reconciliation-client",
        url = "${erp.fi-reconciliation.base-url:http://127.0.0.1:10003/api}"
)
public interface FiReconciliationClient {

    @PostMapping("/internal/reconciliation/p2p/three-way-match")
    ThreeWayMatchResponse threeWayMatch(
            @RequestHeader("X-Internal-Token") String internalToken,
            @RequestBody ThreeWayMatchRequest request
    );
}
