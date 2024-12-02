/*
 * Copyright (C) 2007-2017 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.text.view;

import org.geometerplus.zlibrary.core.view.ZLPaintContext;

public final class ZLTextWord extends ZLTextElement {

    /**
     * Char array of a cache file.
     */
    public final char[] Data;
    /**
     * The start index in data array.
     */
    public final int Offset;
    /**
     * The word's length.
     */
    public final int Length;

    /**
     * Save the measure width of this word.
     */
    private int myWidth = -1;
    /**
     * Indicate whether this word is a mark of searching result or not,
     * if not, means the value is null.
     */
    private Mark myMark;
    /**
     * The start offset in its' paragraph.
     */
    private int myParagraphOffset;
    /**
     * Indicate whether this word can be broken or not.
     * Only can be used when in FeaturePlugin.Breakable.CHARACTER or FeaturePlugin.Breakable.WORD.
     */
    private boolean breakable = true;

    ZLTextWord(String word, int paragraphOffset) {
        this(word.toCharArray(), 0, word.length(), paragraphOffset);
    }

    ZLTextWord(char[] data, int offset, int length, int paragraphOffset) {
        Data = data;
        Offset = offset;
        Length = length;
        myParagraphOffset = paragraphOffset;
    }

    public boolean isASpace() {
        for (int i = Offset; i < Offset + Length; ++i) {
            if (!Character.isWhitespace(Data[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public void setBreakable(boolean breakable) {
        this.breakable = breakable;
    }

    public Mark getMark() {
        return myMark;
    }

    public int getParagraphOffset() {
        return myParagraphOffset;
    }

    public void addMark(int start, int length) {
        Mark existingMark = myMark;
        Mark mark = new Mark(start, length);
        if ((existingMark == null) || (existingMark.Start > start)) {
            mark.setNext(existingMark);
            myMark = mark;
        } else {
            while ((existingMark.getNext() != null) && (existingMark.getNext().Start < start)) {
                existingMark = existingMark.getNext();
            }
            mark.setNext(existingMark.getNext());
            existingMark.setNext(mark);
        }
    }

    public int getWidth(ZLPaintContext context) {
        int width = myWidth;
        if (width <= 1) {
            width = context.getStringWidth(Data, Offset, Length);
            myWidth = width;
        }
        return width;
    }

    @Override
    public String toString() {
        return getString();
    }

    public String getString() {
        return new String(Data, Offset, Length);
    }

    /**
     * A mark class which is used to marking the search results.
     */
    class Mark {
        public final int Start;
        public final int Length;
        private Mark myNext;

        private Mark(int start, int length) {
            Start = start;
            Length = length;
            myNext = null;
        }

        public Mark getNext() {
            return myNext;
        }

        private void setNext(Mark mark) {
            myNext = mark;
        }
    }
}
