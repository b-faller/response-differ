package burp;

import java.util.Optional;

import burp.api.montoya.http.message.responses.HttpResponse;

public final class BaseResponse {
    public static Optional<HttpResponse> baseResponse = Optional.empty();
}
