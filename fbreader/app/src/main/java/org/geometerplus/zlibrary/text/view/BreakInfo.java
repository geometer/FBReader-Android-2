package org.geometerplus.zlibrary.text.view;

import java.util.ArrayList;
import java.util.List;

/**
 * 断字信息
 */
public class BreakInfo {

    private @BreakPlugin.Breakable int breakFeature;
    /**
     * Continuous and uninterrupted Word starting from this Word.
     */
    private List<ZLTextWord> noBreakWords = new ArrayList<>();
    /**
     * The current width of element.
     */
    private int elementWidth;
    /**
     * The cumulative width of continuous uninterrupted words starting from this word.
     */
    private int linebreakWidth;

    public BreakInfo(@BreakPlugin.Breakable int breakFeature) {
        this.breakFeature = breakFeature;
    }

    public @BreakPlugin.Breakable int getBreakFeature() {
        return this.breakFeature;
    }

    public List<ZLTextWord> getNoBreakWords() {
        return noBreakWords;
    }

    public int getElementWidth() {
        return elementWidth;
    }

    public void setElementWidth(int elementWidth) {
        this.elementWidth = elementWidth;
    }

    public int getLinebreakWidth() {
        return linebreakWidth;
    }

    public void setLinebreakWidth(int linebreakWidth) {
        this.linebreakWidth = linebreakWidth;
    }

    /**
     * Subtract the width of the first element from the valid word (because the first element is
     * already accumulated during calculation: newWidth+=elementWidth;)
     */
    public int getAdjustedLineBreakWidth() {
        return Math.max(0, getLinebreakWidth() - getElementWidth());
    }

    public void reset() {
        noBreakWords.clear();
        elementWidth = 0;
        linebreakWidth = 0;
    }
}
