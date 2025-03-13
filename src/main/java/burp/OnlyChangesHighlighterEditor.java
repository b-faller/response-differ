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

public class OnlyChangesHighlighterEditor extends CustomHttpResponseEditor {
    OnlyChangesHighlighterEditor(MontoyaApi api, EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public String caption() {
        return "Minimal diff";
    }

    @Override
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
                    // Re remove this not not show equal lines
                    break;
            }
        }

        return result.toString();
    }
}
