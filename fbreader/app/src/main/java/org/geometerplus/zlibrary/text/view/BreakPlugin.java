package org.geometerplus.zlibrary.text.view;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * feature plugin.
 */
public class BreakPlugin {

    public static @Breakable int getBreakFeatureType() {
        FBReaderApp app = (FBReaderApp) FBReaderApp.Instance();
        if (app != null) {
            return app.ViewOptions.getTextStyleCollection().getBaseStyle().BreakableWordsOption.getValue();
        }
        return Breakable.NATIVE;
    }

    public static BreakInfo createBreakInfo() {
        return new BreakInfo(getBreakFeatureType());
    }

    public static boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.CONNECTOR_PUNCTUATION ||
                type == Character.DASH_PUNCTUATION ||
                type == Character.START_PUNCTUATION ||
                type == Character.END_PUNCTUATION ||
                type == Character.INITIAL_QUOTE_PUNCTUATION ||
                type == Character.FINAL_QUOTE_PUNCTUATION ||
                type == Character.OTHER_PUNCTUATION;
    }


    public static void checkNoBreakElements(final ZLTextParagraphCursor paragraphCursor, final ZLTextElement element,
                                            final int elementWidth, final int currentElementIndex, BreakInfo breakInfo, ZLTextView view) {
        if (breakInfo.getBreakFeature() == BreakPlugin.Breakable.NATIVE) {
            return;
        }
        breakInfo.reset();
        breakInfo.setElementWidth(elementWidth);
        if (!(element instanceof ZLTextWord)) {
            return;
        }
        ZLTextWord lastWord = (ZLTextWord) element;
        if (lastWord.isBreakable()) {
            return;
        }
        breakInfo.getNoBreakWords().add(lastWord);
        int lineBreakWidth = elementWidth;
        int currentIndex = currentElementIndex + 1;

        while (currentIndex < paragraphCursor.getParagraphLength()) {
            ZLTextElement nextElement = paragraphCursor.getElement(currentIndex);
            if (!(nextElement instanceof ZLTextWord) || lastWord.isBreakable()) {
                break;
            }
            ZLTextWord nextWord = (ZLTextWord) nextElement;
            breakInfo.getNoBreakWords().add(nextWord);

            int width = view.getElementWidth(nextElement, 0);
            lineBreakWidth += width;

            lastWord = nextWord;
            currentIndex++;
        }
        breakInfo.setLinebreakWidth(lineBreakWidth);
    }

    public static ZLTextElement getTextElement(final ZLTextElement element, final BreakInfo breakInfo) {
        int noBreakSize = breakInfo.getNoBreakWords().size();
        if (noBreakSize == 0) {
            return element;
        }
        final ZLTextWord first = breakInfo.getNoBreakWords().get(0);
        final ZLTextWord last = breakInfo.getNoBreakWords().get(noBreakSize - 1);
        // construct a ZLTextWord to replace the original word.
        return new ZLTextWord(first.Data, first.Offset, last.Offset - first.Offset + last.Length, first.getParagraphOffset());
    }

    public static int getHyphenationSignWidth(ZLTextView view, ZLTextLineInfo info, ZLTextElement element,
                                              int wordIndex, int charIndex, @BreakPlugin.Breakable int bfType) {
        int hyphenationSignWidth = 0;
        // check whether can add hyphenation sign.
        if (bfType == BreakPlugin.Breakable.CHARACTER && element instanceof ZLTextWord) {
            boolean isAddHyphenationSign = info.isHyphenationSign() && info.isLineEndElement(wordIndex, charIndex);
            ZLTextWord word = (ZLTextWord) element;
            isAddHyphenationSign = isAddHyphenationSign && word.Data[word.Offset] != '-';
            if (isAddHyphenationSign) {
                hyphenationSignWidth = view.getWordWidth(word, charIndex, -1, true);
            }
        }
        return hyphenationSignWidth;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Breakable {
        int NATIVE = 0; // The fb native implementation to obtain a TextWord.
        int WORD = NATIVE + 1; // To obtain a TextWord which is marked breakable by separating word and punctuation.
        int CHARACTER = WORD + 1; // To obtain a TextWord which is marked breakable by separating characters.
    }
}

