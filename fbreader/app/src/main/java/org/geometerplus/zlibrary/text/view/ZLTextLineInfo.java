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

final class ZLTextLineInfo {
    final ZLTextParagraphCursor ParagraphCursor;
    final int ParagraphCursorLength;

    final int StartElementIndex;
    final int StartCharIndex;
    /**
     * Will be updated in first line of this paragraph.
     */
    int RealStartElementIndex;
    int RealStartCharIndex;

    int EndElementIndex;
    int EndCharIndex;

    private boolean mIsVisible;

    private int mLeftIndent;
    /**
     * The width of this text line.
     */
    private int mWidth;
    /**
     * The height of this text line.
     */
    private int mHeight;
    private int mDescent;
    private int mVSpaceBefore;
    private int mVSpaceAfter;
    boolean PreviousInfoUsed;
    int SpaceCounter;
    ZLTextStyle StartStyle;

    ZLTextLineInfo(ZLTextParagraphCursor paragraphCursor, int elementIndex, int charIndex, ZLTextStyle style) {
        ParagraphCursor = paragraphCursor;
        ParagraphCursorLength = paragraphCursor.getParagraphLength();

        StartElementIndex = elementIndex;
        StartCharIndex = charIndex;
        RealStartElementIndex = elementIndex;
        RealStartCharIndex = charIndex;
        EndElementIndex = elementIndex;
        EndCharIndex = charIndex;

        StartStyle = style;
    }

    boolean isEndOfParagraph() {
        return EndElementIndex == ParagraphCursorLength;
    }

    void adjust(ZLTextLineInfo previous) {
        if (!PreviousInfoUsed && previous != null) {
            mHeight -= Math.min(previous.mVSpaceAfter, mVSpaceBefore);
            PreviousInfoUsed = true;
        }
    }

    public int getDescent() {
        return mDescent;
    }

    public void setDescent(int descent) {
        this.mDescent = descent;
    }


    public boolean isVisible() {
        return mIsVisible;
    }

    public void setVisible(boolean visible) {
        mIsVisible = visible;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public int getVSpaceBefore() {
        return mVSpaceBefore;
    }

    public void setVSpaceBefore(int VSpaceBefore) {
        this.mVSpaceBefore = VSpaceBefore;
    }

    public int getVSpaceAfter() {
        return mVSpaceAfter;
    }

    public void setVSpaceAfter(int VSpaceAfter) {
        this.mVSpaceAfter = VSpaceAfter;
    }

    public int getSpaceCounter() {
        return SpaceCounter;
    }

    public void setSpaceCounter(int spaceCounter) {
        SpaceCounter = spaceCounter;
    }

    public int getLeftIndent() {
        return mLeftIndent;
    }

    public void setLeftIndent(int leftIndent) {
        mLeftIndent = leftIndent;
    }

    /**
     * Judge whether two objects are the same or not.
     * This's need by ArrayList as key and value.
     */
    @Override
    public boolean equals(Object o) {
        ZLTextLineInfo info = (ZLTextLineInfo) o;
        return
                (ParagraphCursor == info.ParagraphCursor) &&
                        (StartElementIndex == info.StartElementIndex) &&
                        (StartCharIndex == info.StartCharIndex);
    }

    @Override
    public int hashCode() {
        return ParagraphCursor.hashCode() + StartElementIndex + 239 * StartCharIndex;
    }
}
