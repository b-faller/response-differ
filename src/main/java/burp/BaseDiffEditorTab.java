package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.Selection;
import burp.api.montoya.ui.Theme;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.RawEditor;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Abstract base class for Request and Response editor tabs that contains shared code
 */
public abstract class BaseDiffEditorTab implements PropertyChangeListener {
    public static final String DIFF_MODE_MINIMAL = "Minimal Diff";
    public static final String DIFF_MODE_FULL = "Full Diff";
    public static final String SET_BASE_BUTTON_TEXT = "Compare to this";
    protected final MontoyaApi api;

    protected final RawEditor responseEditor;
    protected final JTextArea textArea;
    protected final JComboBox<String> diffModeDropdown;
    protected final JPanel tabRoot;

    protected HttpRequestResponse requestResponse;

    public BaseDiffEditorTab(MontoyaApi api, EditorCreationContext creationContext) {
        this.api = api;
        this.responseEditor = api.userInterface().createRawEditor(EditorOptions.READ_ONLY);

        // Register for events when the baseline response changes
        BaseResponse.addPropertyChangeListener(this);

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
        diffModeDropdown.setSelectedIndex(getInitialDiffMode());
        // Update listing if the selection is changed
        diffModeDropdown.addActionListener (e -> {
            setInitialDiffMode(diffModeDropdown.getSelectedIndex());
            setRequestResponse(this.requestResponse);
        });

        JButton setBaselineButton = new JButton(SET_BASE_BUTTON_TEXT);
        setBaselineButton.addActionListener(e -> {
            BaseResponse.setBaseRequestResponsePair(requestResponse);
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


    public abstract DiffHelper calculateDiff();
    protected abstract boolean hasContents();

    // These are split, so that requests and responses can have their own initial values
    protected abstract int getInitialDiffMode();
    protected abstract void setInitialDiffMode(int newValue);


    public void setRequestResponse(HttpRequestResponse requestResponse) {
        this.requestResponse = requestResponse;
        if (requestResponse == null) {
            this.textArea.setText("No request/response pair is set");
            return;
        }

        if (this.hasContents()){
            DiffHelper diff = calculateDiff();

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
        } else {
            this.textArea.setText("No baseline request/response pair is set. Use the '"+SET_BASE_BUTTON_TEXT+"' button above, to set a baseline response/request pair");
        }
    }

    public boolean isEnabledFor(HttpRequestResponse requestResponse) {
        return true;
    }

    public String caption() {
        return "Diff";
    }

    public Component uiComponent() {
        return tabRoot;
    }

    public Selection selectedData() {
        return responseEditor.selection().isPresent() ? responseEditor.selection().get() : null;
    }

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
