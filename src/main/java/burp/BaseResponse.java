package burp;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Optional;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.http.message.requests.HttpRequest;

public final class BaseResponse {
    private static Optional<HttpResponse> baseResponse = Optional.empty(); 
    private static Optional<HttpRequest> baseRequest = Optional.empty(); 
    private static PropertyChangeSupport support = new PropertyChangeSupport(BaseResponse.class);

    public static Optional<HttpResponse> getBaseResponse() {
        return baseResponse;
    }

    public static Optional<HttpRequest> getBaseRequest() {
        return baseRequest;
    }

    public static void setBaseRequestResponsePair(HttpRequestResponse newRequestResponse) {
        BaseResponse.baseResponse = Optional.ofNullable(newRequestResponse.response());
        BaseResponse.baseRequest = Optional.ofNullable(newRequestResponse.request());

        support.firePropertyChange("property", null, null);
    }

    public static void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public static void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
}
