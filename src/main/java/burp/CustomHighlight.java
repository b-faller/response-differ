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
    private static int lastSelectedDiffMode = 0;
    private static final String DIFF_MODE_MINIMAL = "Minimal Diff";
    private static final String DIFF_MODE_FULL = "Full Diff";
    private final MontoyaApi api;

    private final RawEditor responseEditor;
    private final JTextArea textArea;
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
        // To keep tabs between different tools (repeater, proxy, etc) somewhat consistent, we create new tabs with the last selected mode
        diffModeDropdown.setSelectedIndex(lastSelectedDiffMode);
        // Update listing if the selection is changed
        diffModeDropdown.addActionListener (e -> {
                lastSelectedDiffMode = diffModeDropdown.getSelectedIndex();
                setRequestResponse(this.requestResponse);
        });

        JButton setBaselineButton = new JButton("Set As Diff Base");
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

        DiffHelper diff = generateDiff(base.toString(), curr.toString());

        this.responseEditor.setContents(ByteArray.byteArray(diff.diffText));
        this.textArea.getHighlighter().removeAllHighlights();

        for (CustomHighlight h : diff.highlighters) {
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

    public DiffHelper generateDiff(String baseText, String newText) {
        boolean isDarkMode = api.userInterface().currentTheme() == Theme.DARK;
        if (diffModeDropdown.getSelectedItem() == DIFF_MODE_FULL) {
            return DiffHelper.generateFullDiffString(baseText, newText, isDarkMode);
        } else {
            return DiffHelper.generateMinimalDiffString(baseText, newText, isDarkMode);
        }
    }
}
