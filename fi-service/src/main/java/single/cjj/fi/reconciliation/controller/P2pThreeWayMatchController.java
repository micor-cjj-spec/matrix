package single.cjj.fi.reconciliation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchRequest;
import single.cjj.fi.reconciliation.api.P2pThreeWayMatchContracts.ThreeWayMatchResponse;
import single.cjj.fi.reconciliation.service.P2pThreeWayMatchService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/reconciliation/p2p")
public class P2pThreeWayMatchController {

    private final P2pThreeWayMatchService service;
    private final String internalToken;

    public P2pThreeWayMatchController(
            P2pThreeWayMatchService service,
            @Value("${fi.reconciliation.internal-token:change-me-before-production}") String internalToken
    ) {
        this.service = service;
        this.internalToken = internalToken;
    }

    @PostMapping("/three-way-match")
    public ThreeWayMatchResponse match(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody ThreeWayMatchRequest request
    ) {
        assertInternalToken(token);
        return service.execute(request);
    }

    private void assertInternalToken(String token) {
        if (!StringUtils.hasText(internalToken) || !StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "internal token required");
        }
        if (!MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid internal token");
        }
    }
}
