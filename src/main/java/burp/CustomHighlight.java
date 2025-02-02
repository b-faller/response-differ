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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter.Highlight;
import javax.swing.text.Highlighter.HighlightPainter;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffAlgorithmFactory;
import com.github.difflib.algorithm.DiffAlgorithmI;
import com.github.difflib.algorithm.DiffAlgorithmListener;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

public record CustomHighlight(int start, int end, Color color) {

}

class CustomHttpResponseEditor implements ExtensionProvidedHttpResponseEditor {

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
        HttpResponse base = BaseResponse.baseResponse.orElse(curr);

        List<CustomHighlight> highlighters = new ArrayList<>();
        String diff = generateDiff(base.toString(), curr.toString(), highlighters);

        this.responseEditor.setContents(ByteArray.byteArray(diff));

        for (CustomHighlight h : highlighters) {
            try {
                textArea.getHighlighter().addHighlight(h.start(), h.end(),
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
        return "Diff";
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

    public String generateDiff(String baseText, String newText, List<CustomHighlight> highlighters) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder result = new StringBuilder();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int start = result.length();

            switch (delta.getType()) {
                case INSERT:
                    delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                    highlighters.add(new CustomHighlight(start, result.length(), new Color(80, 120, 93)));
                    break;

                case DELETE:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    highlighters.add(new CustomHighlight(start, result.length(), new Color(179, 84, 71)));
                    break;

                case CHANGE:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    highlighters.add(new CustomHighlight(start, result.length(), new Color(105, 46, 75)));
                    start = result.length();
                    delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                    highlighters.add(new CustomHighlight(start, result.length(), new Color(55, 69, 102)));
                    break;

                case EQUAL:
                    delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                    break;
            }
        }

        return result.toString();
    }
}
