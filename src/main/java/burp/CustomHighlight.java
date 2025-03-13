package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

public record CustomHighlight(int startLine, int endLine, Color color) {

}

class CustomHttpResponseEditor implements ExtensionProvidedHttpResponseEditor, PropertyChangeListener {

    private final MontoyaApi api;

    private final RawEditor responseEditor;
    private final JTextArea textArea;

    private HttpRequestResponse requestResponse;

    CustomHttpResponseEditor(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api;
        this.responseEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);

        // Safety: This is potentially unstable UI hacking which may break with future
        // Burp Suite versions. It is an easy hack to get around Burp Suite's limited
        // API.
        // ---
        // The second component of the RawEditor component is the scroll pane
        JScrollPane scrollPane = (JScrollPane) ((Container) responseEditor.uiComponent()).getComponents()[1];
        // Burp Suite uses apparently a modified JTextArea, so we can cast to to one
        this.textArea = (JTextArea) scrollPane.getViewport().getView();

        // Enable line wrap
        textArea.setLineWrap(true);
    }

    @Override
    public HttpResponse getResponse() {
        if (requestResponse == null) {
            return null;
        }
        return requestResponse.response();
    }

    @Override
    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
        if (requestResponse == null) {
            return;
        }

        HttpResponse curr = requestResponse.response();
        HttpResponse base = BaseResponse.getBaseResponse().orElse(curr);

        List<CustomHighlight> highlighters = new ArrayList<>();
        String diff = generateDiff(base.toString(), curr.toString(), highlighters);

        this.responseEditor.setContents(ByteArray.byteArray(diff));
        this.textArea.getHighlighter().removeAllHighlights();

        for (CustomHighlight h : highlighters) {
            try {
                int startOffset = textArea.getLineStartOffset(h.startLine());
                int endOffset = textArea.getLineEndOffset(h.endLine() - 1);
                textArea.getHighlighter().addHighlight(startOffset, endOffset,
                        new DefaultHighlighter.DefaultHighlightPainter(h.color()));
            } catch (BadLocationException e) {
                api.logging().logToError(e);
            }
        }
    }

    @Override
    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        return true;
    }

    @Override
    public String caption() {
        return "Full diff";
    }

    @Override
    public Component uiComponent() {
        return responseEditor.uiComponent();
    }

    @Override
    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

    @Override
    public boolean isModified() {
        return responseEditor.isModified();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // Update with itself (last set response)
        setRequestResponse(this.requestResponse);
    }

    public String generateDiff(String baseText, String newText, List<CustomHighlight> highlighters) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder result = new StringBuilder();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        int startLine = 0;
        int endLine = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, new Color(80, 120, 93)));
                    startLine = endLine;
                    break;

                case DELETE:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, new Color(179, 84, 71)));
                    startLine = endLine;
                    break;

                case CHANGE:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, new Color(105, 46, 75)));
                    startLine = endLine;
                    delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, new Color(55, 69, 102)));
                    startLine = endLine;
                    break;

                case EQUAL:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    startLine = endLine;
                    break;
            }
        }

        return result.toString();
    }
}
