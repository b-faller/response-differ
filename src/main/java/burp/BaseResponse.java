package burp;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Optional;

import burp.api.montoya.http.message.responses.HttpResponse;

public final class BaseResponse {
    private static Optional<HttpResponse> baseResponse = Optional.empty(); 
    private static PropertyChangeSupport support = new PropertyChangeSupport(BaseResponse.class);

    public static Optional<HttpResponse> getBaseResponse() {
        return baseResponse;
    }

    public static void setBaseResponse(Optional<HttpResponse> baseResponse) {
        Optional<HttpResponse> oldValue = BaseResponse.baseResponse;
        BaseResponse.baseResponse = baseResponse;
        support.firePropertyChange("property", oldValue, BaseResponse.baseResponse);
    }

    public static void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public static void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }
}
