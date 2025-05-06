package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;

public class RequestDiffEditorTab extends BaseDiffEditorTab implements ExtensionProvidedHttpRequestEditor {
    protected static int lastSelectedDiffMode = 0;

    public RequestDiffEditorTab(MontoyaApi api, EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public HttpRequest getRequest() {
        if (requestResponse == null) {
            return null;
        }
        return requestResponse.request();
    }

    @Override
    protected boolean hasContents() {
        return BaseResponse.getBaseRequest().isPresent();
    }

    public DiffHelper calculateDiff() {
        HttpRequest curr = requestResponse.request();
        HttpRequest base = BaseResponse.getBaseRequest().orElse(curr);

        return generateDiff(base.toString(), curr.toString());
    }

    @Override
    protected int getInitialDiffMode() {
        return lastSelectedDiffMode;
    }

    @Override
    protected void setInitialDiffMode(int newValue) {
        lastSelectedDiffMode = newValue;
    }
}
