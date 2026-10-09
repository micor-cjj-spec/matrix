package single.cjj.openapi.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import single.cjj.openapi.entity.OpenApiApp;
import single.cjj.openapi.entity.OpenApiDefinition;
import single.cjj.openapi.entity.OpenApiGrant;

@Getter
@AllArgsConstructor
public class OpenApiContext {

    public static final String REQUEST_ATTRIBUTE = OpenApiContext.class.getName();
    public static final String REQUEST_BODY_HASH_ATTRIBUTE = OpenApiContext.class.getName() + ".bodySha256";

    private final String requestId;
    private final OpenApiApp app;
    private final OpenApiDefinition definition;
    private final OpenApiGrant grant;
}
