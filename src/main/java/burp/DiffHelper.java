package burp;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.myers.MyersDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import javax.swing.text.Highlighter;
import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DiffHelper {
    private static final int HIGHLIGHT_ALPHA_LIGHT_MODE = 0x60;
    private static final int HIGHLIGHT_ALPHA_DARK_MODE = 0x2a;
    public final List<CustomHighlight> highlighters;
    public final String diffText;

    public static Color getInsertColor(boolean darkMode) {
        return darkMode ? new Color(0, 255, 0, HIGHLIGHT_ALPHA_DARK_MODE) : new Color(0, 255, 0, HIGHLIGHT_ALPHA_LIGHT_MODE);
    }

    public static Color getDeleteColor(boolean darkMode) {
        return darkMode ? new Color(255, 84, 0, HIGHLIGHT_ALPHA_DARK_MODE) : new Color(244, 77, 0, HIGHLIGHT_ALPHA_LIGHT_MODE);
    }

    public static Color getChangeNewColor(boolean darkMode) {
        return getInsertColor(darkMode);
    }

    public static Color getChangeOldColor(boolean darkMode) {
        return getDeleteColor(darkMode);
    }

    public DiffHelper(List<CustomHighlight> highlighters, String diffText) {
        this.highlighters = highlighters;
        this.diffText = diffText;
    }

    public static DiffHelper generateFullDiffString(String baseText, String newText, boolean darkMode) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder diffText = new StringBuilder();
        List<CustomHighlight> highlighters = new LinkedList<CustomHighlight>();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        int startLine = 0;
        int endLine = 0;

        Color insertColor = getInsertColor(darkMode);
        Color deleteColor = getDeleteColor(darkMode);
        Color changeNewColor = getChangeNewColor(darkMode);
        Color changeOldColor = getChangeOldColor(darkMode);

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    delta.getTarget().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, insertColor));
                    startLine = endLine;
                    break;

                case DELETE:
                    delta.getSource().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, deleteColor));
                    startLine = endLine;
                    break;

                case CHANGE:
                    delta.getSource().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, changeOldColor));
                    startLine = endLine;
                    delta.getTarget().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, changeNewColor));
                    startLine = endLine;
                    break;

                case EQUAL:
                    delta.getSource().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    startLine = endLine;
                    break;
            }
        }

        return new DiffHelper(highlighters, diffText.toString());
    }



    public static DiffHelper generateMinimalDiffString(String baseText, String newText, boolean darkMode) {
        List<String> baseLines = Arrays.asList(baseText.split("\n"));
        List<String> newLines = Arrays.asList(newText.split("\n"));
        StringBuilder diffText = new StringBuilder();
        List<CustomHighlight> highlighters = new LinkedList<CustomHighlight>();

        Patch<String> patch = DiffUtils.diff(baseLines, newLines, new MyersDiff<>(), null, true);

        int startLine = 0;

        Color insertColor = getInsertColor(darkMode);
        Color deleteColor = getDeleteColor(darkMode);
        Color changeNewColor = getChangeNewColor(darkMode);
        Color changeOldColor = getChangeOldColor(darkMode);
        int lineNumberInUnshortenedText = 1;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            if (delta.getType() != DeltaType.EQUAL) {
                // Print a header where the difference is
                diffText.append(String.format("Difference in line %d:\n", lineNumberInUnshortenedText));
                startLine++;
            }
            int endLine = -1;

            switch (delta.getType()) {
                case INSERT:
                    delta.getTarget().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, insertColor));
                    break;

                case DELETE:
                    delta.getSource().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, deleteColor));
                    break;

                case CHANGE:
                    delta.getSource().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getSource().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, changeOldColor));
                    startLine = endLine;
                    delta.getTarget().getLines().forEach(line -> diffText.append(line).append("\n"));
                    endLine = startLine + delta.getTarget().getLines().size();
                    highlighters.add(new CustomHighlight(startLine, endLine, changeNewColor));
                    break;

                case EQUAL:
                    // result.append(String.format("[%d equal lines]\n", delta.getTarget().getLines().size()));
                    // startLine++;
                    break;
            }
            if (delta.getType() != DeltaType.EQUAL) {
                // Print a header where the difference is
                diffText.append("\n");
                startLine = endLine + 1;
            }

            lineNumberInUnshortenedText += delta.getTarget().getLines().size();
        }

        return new DiffHelper(highlighters, diffText.toString());
    }
}
