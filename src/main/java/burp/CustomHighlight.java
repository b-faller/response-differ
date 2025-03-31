package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.Theme;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;
import burp.api.montoya.ui.editor.extension.ExtensionProvidedHttpResponseEditor;

import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

public record CustomHighlight(int startLine, int endLine, Color color) {

}

class CustomHttpResponseEditor implements ExtensionProvidedHttpResponseEditor, PropertyChangeListener {

    private static final int HIGHLIGHT_ALPHA_LIGHT_MODE = 0x60;
    private static final int HIGHLIGHT_ALPHA_DARK_MODE = 0x2a;
    private static final String DIFF_MODE_MINIMAL = "Minimal Diff";
    private static final String DIFF_MODE_FULL = "Full Diff";
    private final MontoyaApi api;

    private final RawEditor responseEditor;
    private final JTextArea textArea;
    private final JButton setBaselineButton;
    private final JComboBox<String> diffModeDropdown;
    private final JPanel tabRoot;

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

        String[] choices = {DIFF_MODE_FULL, DIFF_MODE_MINIMAL};
        this.diffModeDropdown = new JComboBox<String>(choices);
        // Update listing if the selection is changed
        diffModeDropdown.addActionListener (e -> setRequestResponse(this.requestResponse));

        this.setBaselineButton = new JButton("Set As Diff Base");
        setBaselineButton.addActionListener(e -> {
            Optional<HttpResponse> response = Optional.ofNullable(getResponse());
            BaseResponse.setBaseResponse(response);
        });
        
        tabRoot = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(setBaselineButton, BorderLayout.WEST);
        topPanel.add(diffModeDropdown, BorderLayout.EAST);

        tabRoot.add(topPanel, BorderLayout.NORTH);
        tabRoot.add(responseEditor.uiComponent(), BorderLayout.CENTER);

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
        return "Diff";
    }

    @Override
    public Component uiComponent() {
        return tabRoot;
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

    public boolean isDarkMode() {
        return api.userInterface().currentTheme() == Theme.DARK;
    }

    public Color getInsertColor() {
        return isDarkMode() ? new Color(0, 255, 0, HIGHLIGHT_ALPHA_DARK_MODE) : new Color(0, 255, 0, HIGHLIGHT_ALPHA_LIGHT_MODE);
    }

    public Color getDeleteColor() {
        return isDarkMode() ? new Color(255, 84, 0, HIGHLIGHT_ALPHA_DARK_MODE) : new Color(244, 77, 0, HIGHLIGHT_ALPHA_LIGHT_MODE);
    }

    public Color getChangeNewColor() {
        return this.getInsertColor();
    }

    public Color getChangeOldColor() {
        return this.getDeleteColor();
    }

    public String generateDiff(String baseText, String newText, List<CustomHighlight> highlighters) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder result = new StringBuilder();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        int startLine = 0;

        Color insertColor = getInsertColor();
        Color deleteColor = getDeleteColor();
        Color changeNew = getChangeNewColor();
        Color changeOld = getChangeOldColor();

        if (diffModeDropdown.getSelectedItem() == DIFF_MODE_FULL) {
            int endLine = 0;

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                switch (delta.getType()) {
                    case INSERT:
                        delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getTarget().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, insertColor));
                        startLine = endLine;
                        break;

                    case DELETE:
                        delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getSource().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, deleteColor));
                        startLine = endLine;
                        break;

                    case CHANGE:
                        delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getSource().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, changeOld));
                        startLine = endLine;
                        delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getTarget().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, changeNew));
                        startLine = endLine;
                        break;

                    case EQUAL:
                        delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getTarget().getLines().size();
                        startLine = endLine;
                        break;
                }
            }
        } else {
            int lineNumberInUnshortenedText = 1;

            for (AbstractDelta<String> delta : patch.getDeltas()) {
                if (delta.getType() != DeltaType.EQUAL) {
                    // Print a header where the difference is
                    result.append(String.format("Difference in line %d:\n", lineNumberInUnshortenedText));
                    startLine++;
                }
                int endLine = -1;

                switch (delta.getType()) {
                    case INSERT:
                        delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getTarget().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, insertColor));
                        break;

                    case DELETE:
                        delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getSource().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, deleteColor));
                        break;

                    case CHANGE:
                        delta.getSource().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getSource().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, deleteColor));
                        startLine = endLine;
                        delta.getTarget().getLines().forEach(line -> result.append(line).append("\n"));
                        endLine = startLine + delta.getTarget().getLines().size();
                        highlighters.add(new CustomHighlight(startLine, endLine, insertColor));
                        break;

                    case EQUAL:
                        // result.append(String.format("[%d equal lines]\n", delta.getTarget().getLines().size()));
                        // startLine++;
                        break;
                }
                if (delta.getType() != DeltaType.EQUAL) {
                    // Print a header where the difference is
                    result.append("\n");
                    startLine = endLine + 1;
                }

                lineNumberInUnshortenedText += delta.getTarget().getLines().size();
            }
        }

        return result.toString();
    }
}
