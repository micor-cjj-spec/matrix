package single.cjj.openapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import single.cjj.openapi.exception.OpenApiCallException;

import java.net.InetAddress;
import java.net.URI;

@Service
public class OpenApiCallbackUrlValidator {

    private final boolean allowHttp;

    public OpenApiCallbackUrlValidator(
            @Value("${matrix.openapi.callback.allow-http:false}") boolean allowHttp) {
        this.allowHttp = allowHttp;
    }

    public String validateAndNormalize(String callbackUrl) {
        if (!StringUtils.hasText(callbackUrl)) {
            return null;
        }
        try {
            URI uri = URI.create(callbackUrl.trim());
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || (allowHttp && "http".equalsIgnoreCase(scheme)))) {
                throw badRequest("回调地址必须使用HTTPS");
            }
            if (!StringUtils.hasText(uri.getHost()) || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw badRequest("回调地址格式不合法");
            }
            String host = uri.getHost().trim();
            if ("localhost".equalsIgnoreCase(host) || host.endsWith(".localhost")) {
                throw badRequest("回调地址不能指向本机");
            }
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    throw badRequest("回调地址不能指向内网或保留地址");
                }
            }
            return uri.normalize().toString();
        } catch (OpenApiCallException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("回调地址无法解析");
        }
    }

    private OpenApiCallException badRequest(String message) {
        return new OpenApiCallException("OPENAPI_CALLBACK_40001", message, 400);
    }
}
