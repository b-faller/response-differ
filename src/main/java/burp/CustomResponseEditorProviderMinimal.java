package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;
import burp.api.montoya.ui.editor.extension.HttpResponseEditorProvider;

class CustomResponseEditorProviderMinimal implements HttpResponseEditorProvider {
    private final MontoyaApi api;

    CustomResponseEditorProviderMinimal(MontoyaApi api) {
        this.api = api;
    }

    @Override
    public ExtensionProvidedHttpResponseEditor provideHttpResponseEditor(EditorCreationContext creationContext) {
        CustomHttpResponseEditor editor = new OnlyChangesHighlighterEditor(api, creationContext);
        BaseResponse.addPropertyChangeListener(editor);
        return editor;
    }
}
