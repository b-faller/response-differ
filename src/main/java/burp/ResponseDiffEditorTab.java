package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpRequestEditor;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;

public class ResponseDiffEditorTab extends BaseDiffEditorTab implements ExtensionProvidedHttpResponseEditor {
    protected static int lastSelectedDiffMode = 0;

    public ResponseDiffEditorTab(MontoyaApi api, EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public HttpResponse getResponse() {
        if (requestResponse == null) {
            return null;
        }
        return requestResponse.response();
    }

    public DiffHelper calculateDiff() {
        HttpResponse curr = requestResponse.response();
        HttpResponse base = BaseResponse.getBaseResponse().orElse(curr);

        return generateDiff(base.toString(), curr.toString());
    }

    @Override
    protected boolean hasContents() {
        return BaseResponse.getBaseResponse().isPresent();
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
