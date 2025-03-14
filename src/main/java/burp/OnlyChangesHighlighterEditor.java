package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.editor.extension.EditorCreationContext;

import java.awt.*;

import java.util.Arrays;
import java.util.List;


import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

public class OnlyChangesHighlighterEditor extends CustomHttpResponseEditor {
    OnlyChangesHighlighterEditor(MontoyaApi api, EditorCreationContext creationContext) {
        super(api, creationContext);
    }

    @Override
    public String caption() {
        return "Minimal diff";
    }

    
    public String generateDiff(String baseText, String newText, List<CustomHighlight> highlighters) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder result = new StringBuilder();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        int startLine = 0;
        int lineNumberInUnshortenedText = 1;

        Color insertColor = getInsertColor();
        Color deleteColor = getDeleteColor();

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


        return result.toString();
    }
}
