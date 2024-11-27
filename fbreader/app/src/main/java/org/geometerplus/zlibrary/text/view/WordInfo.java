package org.geometerplus.zlibrary.text.view;

/**
 * The word helper class for typesetting.
 */
class WordInfo {
    final ZLTextWord Word;
    final int ElementIndex;
    final int StartCharIndex;

    final int Width;
    final int Height;
    final int Descent;

    final int SpaceCounter;

    final ZLTextStyle Style;

    WordInfo(ZLTextWord word, int elementIndex, int startCharIndex, int width, int height, int descent, int spaceCounter, ZLTextStyle style) {
        Word = word;
        ElementIndex = elementIndex;
        StartCharIndex = startCharIndex;
        Width = width;
        Height = height;
        Descent = descent;
        SpaceCounter = spaceCounter;
        Style = style;
    }
}
