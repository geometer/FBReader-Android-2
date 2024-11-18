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

import android.graphics.Rect;

import org.LogUtils;
import org.geometerplus.zlibrary.core.application.ZLApplication;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.util.RationalNumber;
import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.view.Hull;
import org.geometerplus.zlibrary.core.view.SelectionCursor;
import org.geometerplus.zlibrary.core.view.ZLPaintContext;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenationInfo;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.model.ZLTextAlignmentType;
import org.geometerplus.zlibrary.text.model.ZLTextMark;
import org.geometerplus.zlibrary.text.model.ZLTextModel;
import org.geometerplus.zlibrary.text.model.ZLTextParagraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public abstract class ZLTextView extends ZLTextViewBase {
    protected static final String TAG = ZLTextView.class.getSimpleName();

    public static final int SCROLLBAR_HIDE = 0;
    public static final int SCROLLBAR_SHOW = 1;
    public static final int SCROLLBAR_SHOW_AS_PROGRESS = 2;

    private static final char[] ourDefaultLetters = "System developers have used modeling languages for decades to specify, visualize, construct, and document systems. The Unified Modeling Language (UML) is one of those languages. UML makes it possible for team members to collaborate by providing a common language that applies to a multitude of different systems. Essentially, it enables you to communicate solutions in a consistent, tool-supported language.".toCharArray();
    private static final char[] SPACE = new char[]{' '};
    private final HashMap<ZLTextLineInfo, ZLTextLineInfo> myLineInfoCache = new HashMap<ZLTextLineInfo, ZLTextLineInfo>();
    private final ZLTextSelection mySelection = new ZLTextSelection(this);
    private final Set<ZLTextHighlighting> myHighlightings =
            Collections.synchronizedSet(new TreeSet<ZLTextHighlighting>());
    private final char[] myLettersBuffer = new char[512];
    private ZLTextModel myModel;
    private int myScrollingMode;
    private int myOverlappingValue;
    private ZLTextPage myPreviousPage = new ZLTextPage();
    private ZLTextPage myCurrentPage = new ZLTextPage();
    private ZLTextPage myNextPage = new ZLTextPage();
    private ZLTextRegion.Soul myOutlinedRegionSoul;
    private boolean myShowOutline = true;
    private CursorManager myCursorManager;
    private int myLettersBufferLength = 0;
    private ZLTextModel myLettersModel = null;
    private float myCharWidth = -1f;
    private volatile ZLTextWord myCachedWord;
    private volatile ZLTextHyphenationInfo myCachedInfo;

    public ZLTextView(ZLApplication application) {
        super(application);
    }

    public final ZLTextModel getModel() {
        return myModel;
    }

    public synchronized void setModel(ZLTextModel model) {
        myCursorManager = model != null ? new CursorManager(model, getExtensionManager()) : null;

        mySelection.clear();
        myHighlightings.clear();

        myModel = model;
        myCurrentPage.reset();
        myPreviousPage.reset();
        myNextPage.reset();
        if (myModel != null) {
            final int paragraphsNumber = myModel.getParagraphsNumber();
            if (paragraphsNumber > 0) {
                myCurrentPage.moveStartCursor(myCursorManager.get(0));
            }
        }
        Application.getViewWidget().reset();
    }

    public ZLTextWordCursor getStartCursor() {
        if (myCurrentPage.StartCursor.isNull()) {
            preparePaintInfo(myCurrentPage);
        }
        return myCurrentPage.StartCursor;
    }

    public ZLTextWordCursor getEndCursor() {
        if (myCurrentPage.EndCursor.isNull()) {
            preparePaintInfo(myCurrentPage);
        }
        return myCurrentPage.EndCursor;
    }

    private synchronized void gotoMark(ZLTextMark mark) {
        if (mark == null) {
            return;
        }

        myPreviousPage.reset();
        myNextPage.reset();
        boolean doRepaint = false;
        if (myCurrentPage.StartCursor.isNull()) {
            doRepaint = true;
            preparePaintInfo(myCurrentPage);
        }
        if (myCurrentPage.StartCursor.isNull()) {
            return;
        }
        if (myCurrentPage.StartCursor.getParagraphIndex() != mark.ParagraphIndex ||
                myCurrentPage.StartCursor.getMark().compareTo(mark) > 0) {
            doRepaint = true;
            gotoPosition(mark.ParagraphIndex, 0, 0);
            preparePaintInfo(myCurrentPage);
        }
        if (myCurrentPage.EndCursor.isNull()) {
            preparePaintInfo(myCurrentPage);
        }
        while (mark.compareTo(myCurrentPage.EndCursor.getMark()) > 0) {
            doRepaint = true;
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0);
            preparePaintInfo(myCurrentPage);
        }
        if (doRepaint) {
            if (myCurrentPage.StartCursor.isNull()) {
                preparePaintInfo(myCurrentPage);
            }
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
    }

    public synchronized void gotoHighlighting(ZLTextHighlighting highlighting) {
        myPreviousPage.reset();
        myNextPage.reset();
        boolean doRepaint = false;
        if (myCurrentPage.StartCursor.isNull()) {
            doRepaint = true;
            preparePaintInfo(myCurrentPage);
        }
        if (myCurrentPage.StartCursor.isNull()) {
            return;
        }
        if (!highlighting.intersects(myCurrentPage)) {
            gotoPosition(highlighting.getStartPosition().getParagraphIndex(), 0, 0);
            preparePaintInfo(myCurrentPage);
        }
        if (myCurrentPage.EndCursor.isNull()) {
            preparePaintInfo(myCurrentPage);
        }
        while (!highlighting.intersects(myCurrentPage)) {
            doRepaint = true;
            turnPage(true, ScrollingMode.NO_OVERLAPPING, 0);
            preparePaintInfo(myCurrentPage);
        }
        if (doRepaint) {
            if (myCurrentPage.StartCursor.isNull()) {
                preparePaintInfo(myCurrentPage);
            }
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
    }

    public synchronized int search(final String text, boolean ignoreCase, boolean wholeText, boolean backward, boolean thisSectionOnly) {
        if (myModel == null || text.length() == 0) {
            return 0;
        }
        int startIndex = 0;
        int endIndex = myModel.getParagraphsNumber();
        if (thisSectionOnly) {
            // TODO: implement
        }
        int count = myModel.search(text, startIndex, endIndex, ignoreCase);
        myPreviousPage.reset();
        myNextPage.reset();
        if (!myCurrentPage.StartCursor.isNull()) {
            rebuildPaintInfo();
            if (count > 0) {
                ZLTextMark mark = myCurrentPage.StartCursor.getMark();
                gotoMark(wholeText ?
                        (backward ? myModel.getLastMark() : myModel.getFirstMark()) :
                        (backward ? myModel.getPreviousMark(mark) : myModel.getNextMark(mark)));
            }
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
        return count;
    }

    public boolean canFindNext() {
        final ZLTextWordCursor end = myCurrentPage.EndCursor;
        return !end.isNull() && (myModel != null) && (myModel.getNextMark(end.getMark()) != null);
    }

    public synchronized void findNext() {
        final ZLTextWordCursor end = myCurrentPage.EndCursor;
        if (!end.isNull()) {
            gotoMark(myModel.getNextMark(end.getMark()));
        }
    }

    public boolean canFindPrevious() {
        final ZLTextWordCursor start = myCurrentPage.StartCursor;
        return !start.isNull() && (myModel != null) && (myModel.getPreviousMark(start.getMark()) != null);
    }

    public synchronized void findPrevious() {
        final ZLTextWordCursor start = myCurrentPage.StartCursor;
        if (!start.isNull()) {
            gotoMark(myModel.getPreviousMark(start.getMark()));
        }
    }

    public void clearFindResults() {
        if (!findResultsAreEmpty()) {
            myModel.removeAllMarks();
            rebuildPaintInfo();
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
    }

    public boolean findResultsAreEmpty() {
        return myModel == null || myModel.getMarks().isEmpty();
    }

    @Override
    public synchronized void onScrollingFinished(PageIndex pageIndex) {
        switch (pageIndex) {
            case current:
                break;
            case previous: {
                final ZLTextPage swap = myNextPage;
                myNextPage = myCurrentPage;
                myCurrentPage = myPreviousPage;
                myPreviousPage = swap;
                myPreviousPage.reset();
                if (myCurrentPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myNextPage);
                    myCurrentPage.EndCursor.setCursor(myNextPage.StartCursor);
                    myCurrentPage.PaintState = PaintStateEnum.END_IS_KNOWN;
                } else if (!myCurrentPage.EndCursor.isNull() &&
                        !myNextPage.StartCursor.isNull() &&
                        !myCurrentPage.EndCursor.samePositionAs(myNextPage.StartCursor)) {
                    myNextPage.reset();
                    myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor);
                    myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN;
                    Application.getViewWidget().reset();
                }
                break;
            }
            case next: {
                final ZLTextPage swap = myPreviousPage;
                myPreviousPage = myCurrentPage;
                myCurrentPage = myNextPage;
                myNextPage = swap;
                myNextPage.reset();
                switch (myCurrentPage.PaintState) {
                    case PaintStateEnum.NOTHING_TO_PAINT:
                        preparePaintInfo(myPreviousPage);
                        myCurrentPage.StartCursor.setCursor(myPreviousPage.EndCursor);
                        myCurrentPage.PaintState = PaintStateEnum.START_IS_KNOWN;
                        break;
                    case PaintStateEnum.READY:
                        myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor);
                        myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN;
                        break;
                }
                break;
            }
        }
    }

    public boolean removeHighlightings(Class<? extends ZLTextHighlighting> type) {
        boolean result = false;
        synchronized (myHighlightings) {
            for (Iterator<ZLTextHighlighting> it = myHighlightings.iterator(); it.hasNext(); ) {
                final ZLTextHighlighting h = it.next();
                if (type.isInstance(h)) {
                    it.remove();
                    result = true;
                }
            }
        }
        return result;
    }

    public void highlight(ZLTextPosition start, ZLTextPosition end) {
        removeHighlightings(ZLTextManualHighlighting.class);
        addHighlighting(new ZLTextManualHighlighting(this, start, end));
    }

    public final void addHighlighting(ZLTextHighlighting h) {
        myHighlightings.add(h);
        Application.getViewWidget().reset();
        Application.getViewWidget().repaint();
    }

    public final void addHighlightings(Collection<ZLTextHighlighting> hilites) {
        myHighlightings.addAll(hilites);
        Application.getViewWidget().reset();
        Application.getViewWidget().repaint();
    }

    public void clearHighlighting() {
        if (removeHighlightings(ZLTextManualHighlighting.class)) {
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
    }

    protected void moveSelectionCursorTo(SelectionCursor.Which which, int x, int y) {
        y -= getTextStyleCollection().getBaseStyle().getFontSize() / 2;
        mySelection.setCursorInMovement(which, x, y);
        mySelection.expandTo(myCurrentPage, x, y);
        Application.getViewWidget().reset();
        Application.getViewWidget().repaint();
    }

    protected void releaseSelectionCursor() {
        mySelection.stop();
        Application.getViewWidget().reset();
        Application.getViewWidget().repaint();
    }

    protected SelectionCursor.Which getSelectionCursorInMovement() {
        return mySelection.getCursorInMovement();
    }

    private ZLTextSelection.Point getSelectionCursorPoint(ZLTextPage page, SelectionCursor.Which which) {
        if (which == null) {
            return null;
        }

        if (which == mySelection.getCursorInMovement()) {
            return mySelection.getCursorInMovementPoint();
        }

        if (which == SelectionCursor.Which.Left) {
            if (mySelection.hasPartBeforePage(page)) {
                return null;
            }
            final ZLTextElementArea area = mySelection.getStartArea(page);
            if (area != null) {
                return new ZLTextSelection.Point(area.XStart, (area.YStart + area.YEnd) / 2);
            }
        } else {
            if (mySelection.hasPartAfterPage(page)) {
                return null;
            }
            final ZLTextElementArea area = mySelection.getEndArea(page);
            if (area != null) {
                return new ZLTextSelection.Point(area.XEnd, (area.YStart + area.YEnd) / 2);
            }
        }
        return null;
    }

    private float distance2ToCursor(int x, int y, SelectionCursor.Which which) {
        final ZLTextSelection.Point point = getSelectionCursorPoint(myCurrentPage, which);
        if (point == null) {
            return Float.MAX_VALUE;
        }
        final float dX = x - point.X;
        final float dY = y - point.Y;
        return dX * dX + dY * dY;
    }

    protected SelectionCursor.Which findSelectionCursor(int x, int y) {
        return findSelectionCursor(x, y, Float.MAX_VALUE);
    }

    protected SelectionCursor.Which findSelectionCursor(int x, int y, float maxDistance2) {
        if (mySelection.isEmpty()) {
            return null;
        }

        final float leftDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Left);
        final float rightDistance2 = distance2ToCursor(x, y, SelectionCursor.Which.Right);

        if (rightDistance2 < leftDistance2) {
            return rightDistance2 <= maxDistance2 ? SelectionCursor.Which.Right : null;
        } else {
            return leftDistance2 <= maxDistance2 ? SelectionCursor.Which.Left : null;
        }
    }

    private void drawSelectionCursor(ZLPaintContext context, ZLTextPage page, SelectionCursor.Which which) {
        final ZLTextSelection.Point pt = getSelectionCursorPoint(page, which);
        if (pt != null) {
            SelectionCursor.draw(context, which, pt.X, pt.Y, getSelectionBackgroundColor());
        }
    }

    @Override
    public synchronized void preparePage(ZLPaintContext context, PageIndex pageIndex) {
        setContext(context);
        preparePaintInfo(getPage(pageIndex));
    }

    @Override
    public synchronized void paint(ZLPaintContext context, PageIndex pageIndex) {
        setContext(context);
        // clear and reset wallpaper.
        final ZLFile wallpaper = getWallpaperFile();
        if (wallpaper != null) {
            context.clear(wallpaper, getFillMode());
        } else {
            context.clear(getBackgroundColor());
        }

        if (myModel == null || myModel.getParagraphsNumber() == 0) {
            return;
        }

        ZLTextPage page;
        switch (pageIndex) {
            default:
            case current:
                page = myCurrentPage;
                break;
            case previous:
                page = myPreviousPage;
                if (myPreviousPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myCurrentPage);
                    myPreviousPage.EndCursor.setCursor(myCurrentPage.StartCursor);
                    myPreviousPage.PaintState = PaintStateEnum.END_IS_KNOWN;
                }
                break;
            case next:
                page = myNextPage;
                if (myNextPage.PaintState == PaintStateEnum.NOTHING_TO_PAINT) {
                    preparePaintInfo(myCurrentPage);
                    myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor);
                    myNextPage.PaintState = PaintStateEnum.START_IS_KNOWN;
                }
        }

        page.TextElementMap.clear();

        // build the info of this page.
        preparePaintInfo(page);

        if (page.StartCursor.isNull() || page.EndCursor.isNull()) {
            return;
        }

        // calculate all the elements' coordinate.
        final ArrayList<ZLTextLineInfo> lineInfos = page.LineInfos;
        final int[] labels = new int[lineInfos.size() + 1];
        int x = getLeftMargin();
        int y = getTopMargin();
        int index = 0;
        int columnIndex = 0;
        ZLTextLineInfo previousInfo = null;
        for (ZLTextLineInfo info : lineInfos) {
            info.adjust(previousInfo);
            prepareTextLine(page, info, x, y, columnIndex);
            y += info.getHeight() + info.getDescent() + info.getVSpaceAfter();
            labels[++index] = page.TextElementMap.size();
            if (index == page.Column0Height) {
                y = getTopMargin();
                x += page.getTextWidth() + getSpaceBetweenColumns();
                columnIndex = 1;
            }
            previousInfo = info;
        }

        // then, start to draw the page.
        final List<ZLTextHighlighting> hilites = findHilites(page);

        index = 0;
        for (ZLTextLineInfo info : lineInfos) {
            drawTextLine(page, hilites, info, labels[index], labels[index + 1]);
            ++index;
        }

        // TODO: find backgroud color area, and draw.

        for (ZLTextHighlighting h : hilites) {
            int mode = Hull.DrawMode.None;

            final ZLColor bgColor = h.getBackgroundColor();
            if (bgColor != null) {
                context.setFillColor(bgColor, 128);
                mode |= Hull.DrawMode.Fill;
            }

            final ZLColor outlineColor = h.getOutlineColor();
            if (outlineColor != null) {
                context.setLineColor(outlineColor);
                mode |= Hull.DrawMode.Outline;
            }

            if (mode != Hull.DrawMode.None) {
                h.hull(page).draw(getContext(), mode);
            }
        }

        final ZLTextRegion outlinedElementRegion = getOutlinedRegion(page);
        if (outlinedElementRegion != null && myShowOutline) {
            context.setLineColor(getSelectionBackgroundColor());
            outlinedElementRegion.hull().draw(context, Hull.DrawMode.Outline);
        }

        drawSelectionCursor(context, page, SelectionCursor.Which.Left);
        drawSelectionCursor(context, page, SelectionCursor.Which.Right);
    }

    private ZLTextPage getPage(PageIndex pageIndex) {
        switch (pageIndex) {
            default:
            case current:
                return myCurrentPage;
            case previous:
                return myPreviousPage;
            case next:
                return myNextPage;
        }
    }

    public abstract int scrollbarType();

    @Override
    public final boolean isScrollbarShown() {
        return scrollbarType() == SCROLLBAR_SHOW || scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS;
    }

    protected final synchronized int sizeOfTextBeforeParagraph(int paragraphIndex) {
        return myModel != null ? myModel.getTextLength(paragraphIndex - 1) : 0;
    }

    protected final synchronized int sizeOfFullText() {
        if (myModel == null || myModel.getParagraphsNumber() == 0) {
            return 1;
        }
        return myModel.getTextLength(myModel.getParagraphsNumber() - 1);
    }

    private final synchronized int getCurrentCharNumber(PageIndex pageIndex, boolean startNotEndOfPage) {
        if (myModel == null || myModel.getParagraphsNumber() == 0) {
            return 0;
        }
        final ZLTextPage page = getPage(pageIndex);
        preparePaintInfo(page);
        if (startNotEndOfPage) {
            return Math.max(0, sizeOfTextBeforeCursor(page.StartCursor));
        } else {
            int end = sizeOfTextBeforeCursor(page.EndCursor);
            if (end == -1) {
                end = myModel.getTextLength(myModel.getParagraphsNumber() - 1) - 1;
            }
            return Math.max(1, end);
        }
    }

    @Override
    public final synchronized int getScrollbarFullSize() {
        return sizeOfFullText();
    }

    @Override
    public final synchronized int getScrollbarThumbPosition(PageIndex pageIndex) {
        return scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS ? 0 : getCurrentCharNumber(pageIndex, true);
    }

    @Override
    public final synchronized int getScrollbarThumbLength(PageIndex pageIndex) {
        int start = scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS
                ? 0 : getCurrentCharNumber(pageIndex, true);
        int end = getCurrentCharNumber(pageIndex, false);
        return Math.max(1, end - start);
    }

    private int sizeOfTextBeforeCursor(ZLTextWordCursor wordCursor) {
        final ZLTextParagraphCursor paragraphCursor = wordCursor.getParagraphCursor();
        if (paragraphCursor == null) {
            return -1;
        }
        final int paragraphIndex = paragraphCursor.Index;
        int sizeOfText = myModel.getTextLength(paragraphIndex - 1);
        final int paragraphLength = paragraphCursor.getParagraphLength();
        if (paragraphLength > 0) {
            sizeOfText +=
                    (myModel.getTextLength(paragraphIndex) - sizeOfText)
                            * wordCursor.getElementIndex()
                            / paragraphLength;
        }
        return sizeOfText;
    }

    /**
     * Calculate the counts of characters per page.
     */
    // Can be called only when (myModel.getParagraphsNumber() != 0)
    private synchronized float computeCharsPerPage() {
        setTextStyle(getTextStyleCollection().getBaseStyle());

        // pages' width and height.
        final int textWidth = getTextColumnWidth();
        final int textHeight = getTextAreaHeight();

        // calculate the average length of the paragraph.
        final int num = myModel.getParagraphsNumber();
        final int totalTextSize = myModel.getTextLength(num - 1);
        final float charsPerParagraph = ((float) totalTextSize) / num;

        final float charWidth = computeCharWidth();

        final int indentWidth = getElementWidth(ZLTextElement.Indent, 0);
        final float effectiveWidth = textWidth - (indentWidth + 0.5f * textWidth) / charsPerParagraph;
        float charsPerLine = Math.min(effectiveWidth / charWidth,
                charsPerParagraph * 1.2f);

        final int strHeight = getWordHeight() + getContext().getDescent();
        final int effectiveHeight = (int)
                (textHeight -
                        (getTextStyle().getSpaceBefore(metrics())
                                + getTextStyle().getSpaceAfter(metrics()) / 2) / charsPerParagraph);
        final int linesPerPage = effectiveHeight / strHeight;

        return charsPerLine * linesPerPage;
    }

    /**
     * Calculate the number of pages about the specified text sizes.
     */
    private synchronized int computeTextPageNumber(int textSize) {
        if (myModel == null || myModel.getParagraphsNumber() == 0) {
            return 1;
        }

        final float factor = 1.0f / computeCharsPerPage();
        final float pages = textSize * factor;
        return Math.max((int) (pages + 1.0f - 0.5f * factor), 1);
    }

    private final float computeCharWidth() {
        if (myLettersModel != myModel) {
            myLettersModel = myModel;
            myLettersBufferLength = 0;
            myCharWidth = -1f;

            int paragraph = 0;
            final int textSize = myModel.getTextLength(myModel.getParagraphsNumber() - 1);
            if (textSize > myLettersBuffer.length) {
                paragraph = myModel.findParagraphByTextLength((textSize - myLettersBuffer.length) / 2);
            }
            while (paragraph < myModel.getParagraphsNumber()
                    && myLettersBufferLength < myLettersBuffer.length) {
                final ZLTextParagraph.EntryIterator it = myModel.getParagraph(paragraph++).iterator();
                while (myLettersBufferLength < myLettersBuffer.length && it.next()) {
                    if (it.getType() == ZLTextParagraph.Entry.TEXT) {
                        final int len = Math.min(it.getTextLength(),
                                myLettersBuffer.length - myLettersBufferLength);
                        System.arraycopy(it.getTextData(), it.getTextOffset(),
                                myLettersBuffer, myLettersBufferLength, len);
                        myLettersBufferLength += len;
                    }
                }
            }

            if (myLettersBufferLength == 0) {
                myLettersBufferLength = Math.min(myLettersBuffer.length, ourDefaultLetters.length);
                System.arraycopy(ourDefaultLetters, 0, myLettersBuffer, 0, myLettersBufferLength);
            }
        }

        if (myCharWidth < 0f) {
            myCharWidth = computeCharWidth(myLettersBuffer, myLettersBufferLength);
        }
        return myCharWidth;
    }

    private final float computeCharWidth(char[] pattern, int length) {
        return getContext().getStringWidth(pattern, 0, length) / ((float) length);
    }

    /**
     * Calculate the number of pages read and the number of total pages.
     */
    public final synchronized PagePosition pagePosition() {
        int current = computeTextPageNumber(getCurrentCharNumber(PageIndex.current, false));
        int total = computeTextPageNumber(sizeOfFullText());

        if (total > 3) {
            return new PagePosition(current, total);
        }

        preparePaintInfo(myCurrentPage);
        ZLTextWordCursor cursor = myCurrentPage.StartCursor;
        if (cursor == null || cursor.isNull()) {
            return new PagePosition(current, total);
        }

        if (cursor.isStartOfText()) {
            current = 1;
        } else {
            ZLTextWordCursor prevCursor = myPreviousPage.StartCursor;
            if (prevCursor == null || prevCursor.isNull()) {
                preparePaintInfo(myPreviousPage);
                prevCursor = myPreviousPage.StartCursor;
            }
            if (prevCursor != null && !prevCursor.isNull()) {
                current = prevCursor.isStartOfText() ? 2 : 3;
            }
        }

        total = current;
        cursor = myCurrentPage.EndCursor;
        if (cursor == null || cursor.isNull()) {
            return new PagePosition(current, total);
        }
        if (!cursor.isEndOfText()) {
            ZLTextWordCursor nextCursor = myNextPage.EndCursor;
            if (nextCursor == null || nextCursor.isNull()) {
                preparePaintInfo(myNextPage);
                nextCursor = myNextPage.EndCursor;
            }
            if (nextCursor != null) {
                total += nextCursor.isEndOfText() ? 1 : 2;
            }
        }

        return new PagePosition(current, total);
    }

    public final RationalNumber getProgress() {
        final PagePosition position = pagePosition();
        return RationalNumber.create(position.Current, position.Total);
    }

    public final synchronized void gotoPage(int page) {
        if (myModel == null || myModel.getParagraphsNumber() == 0) {
            return;
        }

        final float factor = computeCharsPerPage();
        final float textSize = page * factor;

        int intTextSize = (int) textSize;
        int paragraphIndex = myModel.findParagraphByTextLength(intTextSize);

        if (paragraphIndex > 0 && myModel.getTextLength(paragraphIndex) > intTextSize) {
            --paragraphIndex;
        }
        intTextSize = myModel.getTextLength(paragraphIndex);

        int sizeOfTextBefore = myModel.getTextLength(paragraphIndex - 1);
        while (paragraphIndex > 0 && intTextSize == sizeOfTextBefore) {
            --paragraphIndex;
            intTextSize = sizeOfTextBefore;
            sizeOfTextBefore = myModel.getTextLength(paragraphIndex - 1);
        }

        final int paragraphLength = intTextSize - sizeOfTextBefore;

        final int wordIndex;
        if (paragraphLength == 0) {
            wordIndex = 0;
        } else {
            preparePaintInfo(myCurrentPage);
            final ZLTextWordCursor cursor = new ZLTextWordCursor(myCurrentPage.EndCursor);
            cursor.moveToParagraph(paragraphIndex);
            wordIndex = cursor.getParagraphCursor().getParagraphLength();
        }

        gotoPositionByEnd(paragraphIndex, wordIndex, 0);
    }

    public void gotoHome() {
        final ZLTextWordCursor cursor = getStartCursor();
        if (!cursor.isNull() && cursor.isStartOfParagraph() && cursor.getParagraphIndex() == 0) {
            return;
        }
        gotoPosition(0, 0, 0);
        preparePaintInfo();
    }

    private List<ZLTextHighlighting> findHilites(ZLTextPage page) {
        final LinkedList<ZLTextHighlighting> hilites = new LinkedList<ZLTextHighlighting>();
        if (mySelection.intersects(page)) {
            hilites.add(mySelection);
        }
        synchronized (myHighlightings) {
            for (ZLTextHighlighting h : myHighlightings) {
                if (h.intersects(page)) {
                    hilites.add(h);
                }
            }
        }
        return hilites;
    }

    protected abstract ZLPaintContext.ColorAdjustingMode getAdjustingModeForImages();

    private void drawTextLine(ZLTextPage page, List<ZLTextHighlighting> hilites, ZLTextLineInfo info, int from, int to) {
        final ZLPaintContext context = getContext();
        final ZLTextParagraphCursor paragraph = info.ParagraphCursor;
        int index = from;
        final int endElementIndex = info.EndElementIndex;
        int charIndex = info.RealStartCharIndex;
        final List<ZLTextElementArea> pageAreas = page.TextElementMap.areas();
        if (to > pageAreas.size()) {
            return;
        }
        for (int wordIndex = info.RealStartElementIndex; wordIndex != endElementIndex && index < to; ++wordIndex, charIndex = 0) {
            final ZLTextElement element = paragraph.getElement(wordIndex);
            final ZLTextElementArea area = pageAreas.get(index);
            if (element == area.Element) {
                ++index;
                if (area.ChangeStyle) {
                    setTextStyle(area.Style);
                }

                final int areaX = area.XStart;
                final int areaY = area.YEnd - getElementDescent(element);
                if (element instanceof ZLTextWord) {
                    final ZLTextPosition pos =
                            new ZLTextFixedPosition(info.ParagraphCursor.Index, wordIndex, 0);
                    final ZLTextHighlighting hl = getWordHilite(pos, hilites);
                    final ZLColor hlColor = hl != null ? hl.getForegroundColor() : null;

                    // Test for wrapper text rect.
                    if (LogUtils.DEBUG) {
                        context.drawRect(new Rect(area.XStart, area.YStart, area.XEnd, area.YEnd));
                    }

                    drawWord(
                            areaX, areaY, (ZLTextWord) element, charIndex, -1, false,
                            hlColor != null ? hlColor : getTextColor(getTextStyle())
                    );
                } else if (element instanceof ZLTextImageElement) {
                    final ZLTextImageElement imageElement = (ZLTextImageElement) element;
                    context.drawImage(
                            areaX, areaY,
                            imageElement.ImageData,
                            getTextAreaSize(),
                            getScalingType(imageElement),
                            getAdjustingModeForImages()
                    );
                } else if (element instanceof ZLTextVideoElement) {
                    // TODO: draw
                    context.setLineColor(getTextColor(getTextStyleCollection().getBaseStyle()));
                    context.setFillColor(new ZLColor(127, 127, 127));
                    final int xStart = area.XStart + 10;
                    final int xEnd = area.XEnd - 10;
                    final int yStart = area.YStart + 10;
                    final int yEnd = area.YEnd - 10;
                    context.fillRectangle(xStart, yStart, xEnd, yEnd);
                    context.drawLine(xStart, yStart, xStart, yEnd);
                    context.drawLine(xStart, yEnd, xEnd, yEnd);
                    context.drawLine(xEnd, yEnd, xEnd, yStart);
                    context.drawLine(xEnd, yStart, xStart, yStart);
                    final int l = xStart + (xEnd - xStart) * 7 / 16;
                    final int r = xStart + (xEnd - xStart) * 10 / 16;
                    final int t = yStart + (yEnd - yStart) * 2 / 6;
                    final int b = yStart + (yEnd - yStart) * 4 / 6;
                    final int c = yStart + (yEnd - yStart) / 2;
                    context.setFillColor(new ZLColor(196, 196, 196));
                    context.fillPolygon(new int[]{l, l, r}, new int[]{t, b, c});
                } else if (element instanceof ExtensionElement) {
                    ((ExtensionElement) element).draw(context, area);
                } else if (element == ZLTextElement.HSpace || element == ZLTextElement.NBSpace) {
                    final int cw = context.getSpaceWidth();
                    for (int len = 0; len < area.XEnd - area.XStart; len += cw) {
                        context.drawString(areaX + len, areaY, SPACE, 0, 1);
                    }
                }
            }
        }

        // For english, this line may contain endElementIndex position(hyphenation),
        // but it hasn't been handled in above for loop, need to handle the hyphenation word.
        if (index != to) {
            ZLTextElementArea area = pageAreas.get(index++);
            if (area.ChangeStyle) {
                setTextStyle(area.Style);
            }
            final int start = info.StartElementIndex == info.EndElementIndex
                    ? info.StartCharIndex : 0;
            final int len = info.EndCharIndex - start;
            final ZLTextWord word = (ZLTextWord) paragraph.getElement(info.EndElementIndex);
            final ZLTextPosition pos =
                    new ZLTextFixedPosition(info.ParagraphCursor.Index, info.EndElementIndex, 0);
            final ZLTextHighlighting hl = getWordHilite(pos, hilites);
            final ZLColor hlColor = hl != null ? hl.getForegroundColor() : null;
            drawWord(
                    area.XStart, area.YEnd - context.getDescent() - getTextStyle().getVerticalAlign(metrics()),
                    word, start, len, area.AddHyphenationSign,
                    hlColor != null ? hlColor : getTextColor(getTextStyle())
            );
        }
    }

    private ZLTextHighlighting getWordHilite(ZLTextPosition pos, List<ZLTextHighlighting> hilites) {
        for (ZLTextHighlighting h : hilites) {
            if (h.getStartPosition().compareToIgnoreChar(pos) <= 0
                    && pos.compareToIgnoreChar(h.getEndPosition()) <= 0) {
                return h;
            }
        }
        return null;
    }

    /**
     * Build the specific page info base on the start cursor.
     * This will output the end cursor result.
     *
     * @param page   The page which to build.
     * @param start  The known page start cursor.
     * @param result To output the end cursor.
     */
    private void buildInfos(ZLTextPage page, ZLTextWordCursor start, ZLTextWordCursor result) {
        // Step one: init some params.
        result.setCursor(start);
        int textAreaHeight = page.getTextHeight();
        page.LineInfos.clear();
        page.Column0Height = 0;
        boolean nextParagraph;
        ZLTextLineInfo info = null;

        // Step two: compute the each line info for this page in loop.
        do {
            final ZLTextLineInfo previousInfo = info;
            // reset style and apply the current paragraph position style.
            resetTextStyle();
            final ZLTextParagraphCursor paragraphCursor = result.getParagraphCursor();
            final int wordIndex = result.getElementIndex();
            applyStyleChanges(paragraphCursor, 0, wordIndex);

            // init the current new text-line start info and end index range.
            info = new ZLTextLineInfo(paragraphCursor, wordIndex, result.getCharIndex(), getTextStyle());
            final int endIndex = info.ParagraphCursorLength;

            // loop to deal with the current paragraph to obtain these new text-line info.
            while (info.EndElementIndex != endIndex) {
                // compute the current new text-line detail info.
                info = processTextLine(page, paragraphCursor, info.EndElementIndex, info.EndCharIndex, endIndex, previousInfo);
                // adjust the available height.
                textAreaHeight -= info.getHeight() + info.getDescent();

                // if no height to draw, and the numbers of the page's text-line are greater
                // than the first column line counts.
                if (textAreaHeight < 0 && page.LineInfos.size() > page.Column0Height) {
                    // if the first column line counts: 0 and should show two column view.
                    if (page.Column0Height == 0 && page.twoColumnView()) {
                        // re-init the available text height.
                        textAreaHeight = page.getTextHeight();
                        textAreaHeight -= info.getHeight() + info.getDescent();
                        // save the first column view text-line counts to Column0Height.
                        // we will show the second column view according to the Column0Height value.
                        page.Column0Height = page.LineInfos.size();
                    } else {
                        break;
                    }
                }

                // the available height minus the current line VSpaceAfter value.
                textAreaHeight -= info.getVSpaceAfter();
                // move the cursor to the expected location.
                result.moveTo(info.EndElementIndex, info.EndCharIndex);
                // add a new text-line info to list.
                page.LineInfos.add(info);

                // detect the available text height again if need to show two column view.
                if (textAreaHeight < 0) {
                    if (page.Column0Height == 0 && page.twoColumnView()) {
                        textAreaHeight = page.getTextHeight();
                        page.Column0Height = page.LineInfos.size();
                    } else {
                        // jump the while loop.
                        break;
                    }
                }
            } // end the paragraph while loop.

            // judge whether the result cursor's next position is next paragraph.
            nextParagraph = result.isEndOfParagraph() && result.nextParagraph();
            if (nextParagraph && result.getParagraphCursor().isEndOfSection()) {
                // if the result position is the end section and two column view.
                // re-init text height and set Column0Height value.
                if (page.Column0Height == 0 && page.twoColumnView() && !page.LineInfos.isEmpty()) {
                    textAreaHeight = page.getTextHeight();
                    page.Column0Height = page.LineInfos.size();
                }
            }

            // 1. if exist next paragraph and available height.
            // 2. not the section end.
            // 3. should start from the second column view.
        } while (nextParagraph && textAreaHeight >= 0 &&
                (!result.getParagraphCursor().isEndOfSection() ||
                        page.LineInfos.size() == page.Column0Height)
        ); // end the [do ... while] loop when the page's lines is full.
        resetTextStyle();
    }

    /**
     * whether allow to hyphenate or not by appending '-'.
     */
    private boolean isHyphenationPossible() {
        return getTextStyleCollection().getBaseStyle().AutoHyphenationOption.getValue()
                && getTextStyle().allowHyphenations();
    }

    private final synchronized ZLTextHyphenationInfo getHyphenationInfo(ZLTextWord word) {
        if (myCachedWord != word) {
            myCachedWord = word;
            myCachedInfo = ZLTextHyphenator.Instance().getInfo(word);
        }
        return myCachedInfo;
    }

    /**
     * Compute the text-line info based on the input params.
     *
     * @param page
     * @param paragraphCursor The input paragraph cursor.
     * @param startIndex      The element start index in the paragraph elements list
     *                        {@link ZLTextParagraphCursor#getElement(int)}.
     * @param startCharIndex  The start char index in the ZLTextElement.
     *                        Such as: "good" is divided into "go-" and "od"
     * @param endIndex        The end index limitation which can't be arrived.
     * @param previousInfo    The last text-line info, maybe null.
     * @return {@link #processTextLineInternal }
     */
    private ZLTextLineInfo processTextLine(
            ZLTextPage page,
            ZLTextParagraphCursor paragraphCursor,
            final int startIndex,
            final int startCharIndex,
            final int endIndex,
            ZLTextLineInfo previousInfo
    ) {
        final ZLTextLineInfo info = processTextLineInternal(
                page, paragraphCursor, startIndex, startCharIndex, endIndex, previousInfo
        );
        if (info.EndElementIndex == startIndex && info.EndCharIndex == startCharIndex) {
            info.EndElementIndex = paragraphCursor.getParagraphLength();
            info.EndCharIndex = 0;
            // TODO: add error element
        }
        return info;
    }

    /**
     * Obtain a new text-line info based on the input params.
     */
    private ZLTextLineInfo processTextLineInternal(
            ZLTextPage page,
            ZLTextParagraphCursor paragraphCursor,
            final int startIndex,
            final int startCharIndex,
            final int endIndex,
            ZLTextLineInfo previousInfo
    ) {
        final ZLPaintContext context = getContext();
        // construct the new text-line info according to some params.
        final ZLTextLineInfo info = new ZLTextLineInfo(paragraphCursor, startIndex, startCharIndex, getTextStyle());

        // firstly obtain result from cached.
        final ZLTextLineInfo cachedInfo = myLineInfoCache.get(info);
        if (cachedInfo != null) {
            cachedInfo.adjust(previousInfo);
            applyStyleChanges(paragraphCursor, startIndex, cachedInfo.EndElementIndex);
            return cachedInfo;
        }

        int currentElementIndex = startIndex;
        int currentCharIndex = startCharIndex;
        // judge whether it is first line or not.
        final boolean isFirstLine = startIndex == 0 && startCharIndex == 0;

        // update text-style when in the paragraph start.
        if (isFirstLine) {
            ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
            while (isStyleChangeElement(element)) {
                applyStyleChangeElement(element);
                ++currentElementIndex;
                currentCharIndex = 0;
                if (currentElementIndex == endIndex) {
                    break;
                }
                element = paragraphCursor.getElement(currentElementIndex);
            }
            info.StartStyle = getTextStyle();
            info.RealStartElementIndex = currentElementIndex;
            info.RealStartCharIndex = currentCharIndex;
            LogUtils.d(TAG, "processTextLineInternal: 1. isFirstLine and compute start location.");
        }

        ZLTextStyle storedStyle = getTextStyle();

        // compute the max available width limitation.
        final int maxWidth = page.getTextWidth() - storedStyle.getRightIndent(metrics());
        info.setLeftIndent(storedStyle.getLeftIndent(metrics()));

        // compute the first line left-indent of this paragraph.
        if (isFirstLine && storedStyle.getAlignment() != ZLTextAlignmentType.ALIGN_CENTER) {
            info.setLeftIndent(info.getLeftIndent() + storedStyle.getFirstLineIndent(metrics()));
        }

        // make a correction if necessary.
        if (info.getLeftIndent() > maxWidth - 20) {
            info.setLeftIndent(maxWidth * 3 / 4);
        }

        info.setWidth(info.getLeftIndent());

        // this maybe empty line paragraph, only one paragraph-start-control element.
        if (info.RealStartElementIndex == endIndex) {
            info.EndElementIndex = info.RealStartElementIndex;
            info.EndCharIndex = info.RealStartCharIndex;
            LogUtils.i(TAG, "processTextLineInternal: 2. The start index is equal to end index, return info. index = " + endIndex);
            return info;
        }

        // init the computing used width and height, etc.
        int newWidth = info.getWidth();
        int newHeight = info.getHeight();
        int newDescent = info.getDescent();
        boolean wordOccurred = false;
        boolean isVisible = false;
        int lastSpaceWidth = 0;
        int internalSpaceCounter = 0;
        boolean removeLastSpace = false;

        // define a inner class WordInfo.
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
        // define a wordInfo list.
        final ArrayList<WordInfo> words = new ArrayList<WordInfo>();

        // begin [do ... while] loop to compute a line wordInfo.
        do {
            ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
            // obtain the width of element and add to newWidth.
            newWidth += getElementWidth(element, currentCharIndex);
            // adjust the appropriate height and descent.
            newHeight = Math.max(newHeight, getElementHeight(element));
            newDescent = Math.max(newDescent, getElementDescent(element));

            // HSpace: {@link Character.isWhitespace(ch)} means you can begin a newline
            // NBSpace: if not the HSpace,but {@link Character.isSpaceChar(ch}. means Non Breaking Space.
            if (element == ZLTextElement.HSpace) {
                if (wordOccurred) {
                    wordOccurred = false;
                    ++internalSpaceCounter;
                    lastSpaceWidth = context.getSpaceWidth();
                    newWidth += lastSpaceWidth;
                }
            } else if (element == ZLTextElement.NBSpace) {
                wordOccurred = true;
                ++internalSpaceCounter;
                newWidth += context.getSpaceWidth();
            } else if (element instanceof ZLTextWord) {
                wordOccurred = true;
                isVisible = true;
            } else if (element instanceof ZLTextImageElement) {
                wordOccurred = true;
                isVisible = true;
            } else if (element instanceof ZLTextVideoElement) {
                wordOccurred = true;
                isVisible = true;
            } else if (element instanceof ExtensionElement) {
                wordOccurred = true;
                isVisible = true;
            } else if (isStyleChangeElement(element)) {
                applyStyleChangeElement(element);
            }

            // if reach the max width and jump loop.
            if (newWidth > maxWidth) {
                if (info.EndElementIndex != startIndex || element instanceof ZLTextWord) {
                    LogUtils.d(TAG, "processTextLineInternal: 3. It reachs the max width of line, break do ... while");
                    break;
                }
            }

            // mark it as previous element.
            final ZLTextElement previousElement = element;
            final int previousStartCharIndex = currentCharIndex;

            // point to next element to detect allow-break status and use to next loop.
            ++currentElementIndex;
            currentCharIndex = 0;

            // whether it can allow to break(reach the end index) or not.
            boolean allowBreak = currentElementIndex == endIndex;
            if (!allowBreak) {
                element = paragraphCursor.getElement(currentElementIndex);
                // if not reach the end index, again fix the allow-break status.
                // 1> previous and current element are neither not NBSpace element
                // 2> and {current is not ZLTextWord, or previous is ZLTextWord}
                // 3> and current element is neither ZLTextImageElement or ZLTextControlElement.
                allowBreak =
                        previousElement != ZLTextElement.NBSpace &&
                                element != ZLTextElement.NBSpace &&
                                (!(element instanceof ZLTextWord) || previousElement instanceof ZLTextWord) &&
                                !(element instanceof ZLTextImageElement) &&
                                !(element instanceof ZLTextControlElement);
            }

            // allow to break.(1. reach the end index 2. every word in the line.)
            if (allowBreak) {
                words.clear();
                // update element info to ZLTextLineInfo.
                info.setVisible(isVisible);
                info.setWidth(newWidth);
                if (info.getHeight() < newHeight) {
                    info.setHeight(newHeight);
                }
                if (info.getDescent() < newDescent) {
                    info.setDescent(newDescent);
                }

                // For Chinese, the end of the line doesn't contain the end value;
                // for English, it may contain the first half of a word.
                info.EndElementIndex = currentElementIndex;
                info.EndCharIndex = currentCharIndex;
                info.SpaceCounter = internalSpaceCounter;
                storedStyle = getTextStyle();
                removeLastSpace = !wordOccurred && internalSpaceCounter > 0;
            } else if (previousElement instanceof ZLTextWord) {
                LogUtils.i(TAG, "processTextLineInternal: 4. do ... while, end = "
                        + (currentElementIndex != endIndex) + ", add WordInfo =" + (ZLTextWord) previousElement);
                // save this loop index element.
                words.add(new WordInfo(
                        (ZLTextWord) previousElement,
                        currentElementIndex - 1, previousStartCharIndex,
                        newWidth, newHeight, newDescent,
                        internalSpaceCounter, getTextStyle()
                ));
            }
        } while (currentElementIndex != endIndex);

        // if not reach the endIndex, and {can hyphenate or the EndElementIndex is equal to startIndex}.
        if (currentElementIndex != endIndex &&
                (isHyphenationPossible() || info.EndElementIndex == startIndex)) {
            final ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
            boolean hyphenated = false;
            LogUtils.d(TAG, "processTextLineInternal: 5.  handle hyphenation info of needing newline. startIndex == "
                    + (info.EndElementIndex == startIndex));

            // make a hyphenation to ZLTextWord if no sapce to show the whole word.
            // such as: "suddenly" can be separated into "sud-denly".
            if (element instanceof ZLTextWord) {
                final ZLTextWord word = (ZLTextWord) element;
                // The newWidth(>maxWidth) minus the width of the critical element.
                newWidth -= getWordWidth(word, currentCharIndex);
                // calculate the remaining space.
                int spaceLeft = maxWidth - newWidth;

                if ((word.Length > 3 && spaceLeft > 2 * context.getSpaceWidth())
                        || info.EndElementIndex == startIndex) {
                    // get the hyphenation info about this ZLTextWord.
                    final ZLTextHyphenationInfo hyphenationInfo = getHyphenationInfo(word);
                    int hyphenationPosition = currentCharIndex;
                    // save the width of front part, such as "sud-"
                    int subwordWidth = 0;

                    // Hyphenation Case 1: use ZLTextHyphenationInfo to hyphenate.
                    // traversing the word letters to get the best hyphenation point with the available spaceLeft.
                    for (int right = word.Length - 1, left = currentCharIndex; right > left; ) {
                        // binary search to find the location.
                        final int mid = (right + left + 1) / 2;
                        int m1 = mid;
                        // look for left-side hyphenation point.
                        while (m1 > left && !hyphenationInfo.isHyphenationPossible(m1)) {
                            --m1;
                        }
                        if (m1 > left) {
                            // The m1 > left means that exist left-side hyphenation point.
                            final int w = getWordWidth(
                                    word,
                                    currentCharIndex,
                                    m1 - currentCharIndex,
                                    // if the char which is in front of the allow-break point isn't '-',
                                    // set hyphenation sign to true.
                                    word.Data[word.Offset + m1 - 1] != '-'
                            );
                            if (w < spaceLeft) {
                                // here, enough space, need update left.
                                left = mid;
                                hyphenationPosition = m1;
                                subwordWidth = w;
                            } else {
                                // no space is available, update right value.
                                right = mid - 1;
                            }
                        } else {
                            // not exist left-side hyphenation point, update left value.
                            left = mid;
                        }
                    } // end "for" loop.

                    // debug to print info.
                    if (currentCharIndex != 0) {
                        LogUtils.d(TAG, "processTextLineInternal: 6. currentCharIndex: " + currentCharIndex + " word :" + word.toString());
                    }

                    // Hyphenation Case 2: need to force hyphenate (not use ZLTextHyphenationInfo).
                    // if not find the hyphenation point by "for" loop and this line only contains one element.
                    if (hyphenationPosition == currentCharIndex && info.EndElementIndex == startIndex) {
                        LogUtils.i(TAG, "processTextLineInternal: 7.  " + " word :" + word.toString());
                        subwordWidth = getWordWidth(word, currentCharIndex, 1, false);

                        // force binary search to find the best hyphenation position.
                        int right = word.Length == currentCharIndex + 1 ? word.Length : word.Length - 1;
                        int left = currentCharIndex + 1;
                        while (right > left) {
                            final int mid = (right + left + 1) / 2;
                            final int w = getWordWidth(
                                    word,
                                    currentCharIndex,
                                    mid - currentCharIndex,
                                    word.Data[word.Offset + mid - 1] != '-'
                            );
                            if (w <= spaceLeft) {
                                left = mid;
                                subwordWidth = w;
                            } else {
                                right = mid - 1;
                            }
                        }
                        hyphenationPosition = right;
                    }

                    // this means hyphenated.
                    if (hyphenationPosition > currentCharIndex) {
                        hyphenated = true;
                        info.setVisible(true);
                        info.setWidth(newWidth + subwordWidth);
                        if (info.getHeight() < newHeight) {
                            info.setHeight(newHeight);
                        }
                        if (info.getDescent() < newDescent) {
                            info.setDescent(newDescent);
                        }
                        info.EndElementIndex = currentElementIndex;
                        // this hyphenation position is contained.
                        info.EndCharIndex = hyphenationPosition;
                        info.SpaceCounter = internalSpaceCounter;
                        storedStyle = getTextStyle();
                        removeLastSpace = false;
                    }
                }
                // if "hyphenated" true, word will be spilt into two parts at least by '-' char which is shown at two lines.
                // Or false, word will be shown in the start of the next line.
                LogUtils.d(TAG, "processTextLineInternal: 8.  hyphenated: " + hyphenated + "  word: " + word);
            }

            // if cann't hyphenate, again need to check WordInfo list from the end.
            // such as: "tao yuan ming" must be in the same line when happen in the end of line.
            // you must attempt to hyphenate to {tao、yuan、ming}.
            if (!hyphenated) {
                // add element to words list referencing [processTextLineInternal: 4]
                for (int i = words.size() - 1; i >= 0; --i) {
                    final WordInfo wi = words.get(i);
                    final ZLTextWord word = wi.Word;
                    if (word.Length <= 3) {
                        continue;
                    }
                    final ZLTextHyphenationInfo hyphenationInfo = getHyphenationInfo(word);
                    int pos = word.Length - 1;
                    for (; pos > wi.StartCharIndex; --pos) {
                        if (hyphenationInfo.isHyphenationPossible(pos)) {
                            break;
                        }
                    }
                    if (pos > wi.StartCharIndex) {
                        final int subwordWidth = getWordWidth(
                                word,
                                wi.StartCharIndex,
                                pos - wi.StartCharIndex,
                                word.Data[word.Offset + pos - 1] != '-'
                        );
                        info.setVisible(true);
                        info.setWidth(wi.Width - getWordWidth(word, wi.StartCharIndex) + subwordWidth);
                        if (info.getHeight() < wi.Height) {
                            info.setHeight(wi.Height);
                        }
                        if (info.getDescent() < wi.Descent) {
                            info.setDescent(wi.Descent);
                        }
                        info.EndElementIndex = wi.ElementIndex;
                        info.EndCharIndex = pos;
                        info.SpaceCounter = wi.SpaceCounter;
                        storedStyle = wi.Style;
                        removeLastSpace = false;
                        break;
                    }
                }
                int size = words.size();
                if (size > 0) {
                    LogUtils.d(TAG, "processTextLineInternal: 9.  cann't hyphenate, words:  " + size);
                }
            }
        }

        if (removeLastSpace) {
            info.setWidth(info.getWidth() - lastSpaceWidth);
            info.SpaceCounter--;
        }

        setTextStyle(storedStyle);

        // handle with the first line in this page.
        if (isFirstLine) {
            info.setVSpaceBefore(info.StartStyle.getSpaceBefore(metrics()));
            if (previousInfo != null) {
                info.PreviousInfoUsed = true;
                info.setHeight(info.getHeight() + Math.max(0, info.getVSpaceBefore() - previousInfo.getVSpaceAfter()));
            } else {
                info.PreviousInfoUsed = false;
                info.setHeight(info.getHeight() + info.getVSpaceBefore());
            }
        }

        // handle with the end of paragraph.
        if (info.isEndOfParagraph()) {
            info.setVSpaceAfter(getTextStyle().getSpaceAfter(metrics()));
        }

        // save this ZLTextLineInfo into cache to avoid the next repeating computing.
        if (info.EndElementIndex != endIndex || endIndex == info.ParagraphCursorLength) {
            myLineInfoCache.put(info, info);
        }

        return info;
    }

    private void prepareTextLine(ZLTextPage page, ZLTextLineInfo info, int x, int y, int columnIndex) {
        y = Math.min(y + info.getHeight(), getTopMargin() + page.getTextHeight() - 1);

        final ZLPaintContext context = getContext();
        final ZLTextParagraphCursor paragraphCursor = info.ParagraphCursor;

        setTextStyle(info.StartStyle);
        int spaceCounter = info.SpaceCounter;
        int fullCorrection = 0;
        final boolean endOfParagraph = info.isEndOfParagraph();
        boolean wordOccurred = false;
        boolean changeStyle = true;
        x += info.getLeftIndent();

        final int maxWidth = page.getTextWidth();
        switch (getTextStyle().getAlignment()) {
            case ZLTextAlignmentType.ALIGN_RIGHT:
                x += maxWidth - getTextStyle().getRightIndent(metrics()) - info.getWidth();
                break;
            case ZLTextAlignmentType.ALIGN_CENTER:
                x += (maxWidth - getTextStyle().getRightIndent(metrics()) - info.getWidth()) / 2;
                break;
            case ZLTextAlignmentType.ALIGN_JUSTIFY:
                if (!endOfParagraph && (paragraphCursor.getElement(info.EndElementIndex) != ZLTextElement.AfterParagraph)) {
                    fullCorrection = maxWidth - getTextStyle().getRightIndent(metrics()) - info.getWidth();
                }
                break;
            case ZLTextAlignmentType.ALIGN_LEFT:
            case ZLTextAlignmentType.ALIGN_UNDEFINED:
                break;
        }

        final ZLTextParagraphCursor paragraph = info.ParagraphCursor;
        final int paragraphIndex = paragraph.Index;
        final int endElementIndex = info.EndElementIndex;
        int charIndex = info.RealStartCharIndex;
        ZLTextElementArea spaceElement = null;
        // for chinese not contains endElementIndex, for english may contains it(if can hyphenate)
        for (int wordIndex = info.RealStartElementIndex; wordIndex != endElementIndex; ++wordIndex, charIndex = 0) {
            final ZLTextElement element = paragraph.getElement(wordIndex);
            final int width = getElementWidth(element, charIndex);
            if (element == ZLTextElement.HSpace || element == ZLTextElement.NBSpace) {
                if (wordOccurred && spaceCounter > 0) {
                    final int correction = fullCorrection / spaceCounter;
                    final int spaceLength = context.getSpaceWidth() + correction;
                    if (getTextStyle().isUnderline()) {
                        spaceElement = new ZLTextElementArea(
                                paragraphIndex, wordIndex, 0,
                                0, // length
                                true, // is last in element
                                false, // add hyphenation sign
                                false, // changed style
                                getTextStyle(), element, x, x + spaceLength, y, y, columnIndex
                        );
                    } else {
                        spaceElement = null;
                    }
                    x += spaceLength;
                    fullCorrection -= correction;
                    wordOccurred = false;
                    --spaceCounter;
                }
            } else if (element instanceof ZLTextWord || element instanceof ZLTextImageElement || element instanceof ZLTextVideoElement || element instanceof ExtensionElement) {
                final int height = getElementHeight(element);
                final int descent = getElementDescent(element);
                final int ascent = getElementAscent(element);
                boolean isWord = element instanceof ZLTextWord;
                int yEnd = isWord ? (y + descent - getTextStyle().getVerticalAlign(metrics())) : (y + descent);
                int yStart = isWord ? (yEnd - (descent - ascent) - 1) : (y - height + 1);
                final int length = element instanceof ZLTextWord ? ((ZLTextWord) element).Length : 0;
                if (spaceElement != null) {
                    page.TextElementMap.add(spaceElement);
                    spaceElement = null;
                }
                page.TextElementMap.add(new ZLTextElementArea(
                        paragraphIndex, wordIndex, charIndex,
                        length - charIndex,
                        true, // is last in element
                        false, // add hyphenation sign
                        changeStyle, getTextStyle(), element,
//                        x, x + width - 1, y - height + 1, y + descent, columnIndex
                        x, x + width - 1, yStart, yEnd, columnIndex
                ));
                changeStyle = false;
                wordOccurred = true;
            } else if (isStyleChangeElement(element)) {
                applyStyleChangeElement(element);
                changeStyle = true;
            }
            x += width;
        }
        if (!endOfParagraph) {
            final int len = info.EndCharIndex;
            if (len > 0) {
                final int wordIndex = info.EndElementIndex;
                final ZLTextWord word = (ZLTextWord) paragraph.getElement(wordIndex);
                final boolean addHyphenationSign = word.Data[word.Offset + len - 1] != '-';
                final int width = getWordWidth(word, 0, len, addHyphenationSign);
                final int height = getElementHeight(word);
                final int descent = context.getDescent();
                final int ascent = getElementAscent(word);
                int yEnd = y + descent - getTextStyle().getVerticalAlign(metrics());
                int yStart = yEnd - (descent - ascent) - 1;
                page.TextElementMap.add(
                        new ZLTextElementArea(
                                paragraphIndex, wordIndex, 0,
                                len,
                                false, // is last in element
                                addHyphenationSign,
                                changeStyle, getTextStyle(), word,
//                                x, x + width - 1, y - height + 1, y + descent, columnIndex
                                x, x + width - 1, yStart, yEnd, columnIndex
                        )
                );
            }
        }
    }

    /**
     * Mainly used to turn page when in scrolling mode with {@link ScrollingMode#SCROLL_LINES}.
     * Besides, it maybe used to {@link #gotoMark} {@link #gotoHighlighting}
     * {@link #gotoPosition} {@link #gotoPositionByEnd} with {@link ScrollingMode#NO_OVERLAPPING}.
     *
     * @param forward       page down if true, or page up.
     * @param scrollingMode
     * @param value         This value maybe 0( when NO_OVERLAPPING) or 1(when SCROLL_LINES).
     */
    public synchronized final void turnPage(boolean forward, int scrollingMode, int value) {
        preparePaintInfo(myCurrentPage);
        myPreviousPage.reset();
        myNextPage.reset();
        if (myCurrentPage.PaintState == PaintStateEnum.READY) {
            myCurrentPage.PaintState = forward ? PaintStateEnum.TO_SCROLL_FORWARD : PaintStateEnum.TO_SCROLL_BACKWARD;
            myScrollingMode = scrollingMode;
            myOverlappingValue = value;
        }
    }

    public final synchronized void gotoPosition(ZLTextPosition position) {
        if (position != null) {
            gotoPosition(position.getParagraphIndex(), position.getElementIndex(), position.getCharIndex());
        }
    }

    public final synchronized void gotoPosition(int paragraphIndex, int wordIndex, int charIndex) {
        if (myModel != null && myModel.getParagraphsNumber() > 0) {
            Application.getViewWidget().reset();
            myCurrentPage.moveStartCursor(paragraphIndex, wordIndex, charIndex);
            myPreviousPage.reset();
            myNextPage.reset();
            preparePaintInfo(myCurrentPage);
            if (myCurrentPage.isEmptyPage()) {
                turnPage(true, ScrollingMode.NO_OVERLAPPING, 0);
            }
        }
    }

    private final synchronized void gotoPositionByEnd(int paragraphIndex, int wordIndex, int charIndex) {
        if (myModel != null && myModel.getParagraphsNumber() > 0) {
            myCurrentPage.moveEndCursor(paragraphIndex, wordIndex, charIndex);
            myPreviousPage.reset();
            myNextPage.reset();
            preparePaintInfo(myCurrentPage);
            if (myCurrentPage.isEmptyPage()) {
                turnPage(false, ScrollingMode.NO_OVERLAPPING, 0);
            }
        }
    }

    protected synchronized void preparePaintInfo() {
        myPreviousPage.reset();
        myNextPage.reset();
        preparePaintInfo(myCurrentPage);
    }

    private synchronized void preparePaintInfo(ZLTextPage page) {
        page.setSize(getTextColumnWidth(), getTextAreaHeight(), twoColumnView(), page == myPreviousPage);

        if (page.PaintState == PaintStateEnum.NOTHING_TO_PAINT || page.PaintState == PaintStateEnum.READY) {
            return;
        }
        final int oldState = page.PaintState;

        final HashMap<ZLTextLineInfo, ZLTextLineInfo> cache = myLineInfoCache;
        for (ZLTextLineInfo info : page.LineInfos) {
            cache.put(info, info);
        }

        switch (page.PaintState) {
            default:
                break;
            case PaintStateEnum.TO_SCROLL_FORWARD:
                if (!page.EndCursor.isEndOfText()) {
                    final ZLTextWordCursor startCursor = new ZLTextWordCursor();
                    switch (myScrollingMode) {
                        case ScrollingMode.NO_OVERLAPPING:
                            break;
                        case ScrollingMode.KEEP_LINES:
                            page.findLineFromEnd(startCursor, myOverlappingValue);
                            break;
                        case ScrollingMode.SCROLL_LINES:
                            page.findLineFromStart(startCursor, myOverlappingValue);
                            if (startCursor.isEndOfParagraph()) {
                                startCursor.nextParagraph();
                            }
                            break;
                        case ScrollingMode.SCROLL_PERCENTAGE:
                            page.findPercentFromStart(startCursor, myOverlappingValue);
                            break;
                    }

                    if (!startCursor.isNull() && startCursor.samePositionAs(page.StartCursor)) {
                        page.findLineFromStart(startCursor, 1);
                    }

                    if (!startCursor.isNull()) {
                        final ZLTextWordCursor endCursor = new ZLTextWordCursor();
                        buildInfos(page, startCursor, endCursor);
                        if (!page.isEmptyPage() && (myScrollingMode != ScrollingMode.KEEP_LINES || !endCursor.samePositionAs(page.EndCursor))) {
                            page.StartCursor.setCursor(startCursor);
                            page.EndCursor.setCursor(endCursor);
                            break;
                        }
                    }

                    page.StartCursor.setCursor(page.EndCursor);
                    buildInfos(page, page.StartCursor, page.EndCursor);
                }
                break;
            case PaintStateEnum.TO_SCROLL_BACKWARD:
                if (!page.StartCursor.isStartOfText()) {
                    switch (myScrollingMode) {
                        case ScrollingMode.NO_OVERLAPPING:
                            page.StartCursor.setCursor(findStartOfPrevousPage(page, page.StartCursor));
                            break;
                        case ScrollingMode.KEEP_LINES: {
                            ZLTextWordCursor endCursor = new ZLTextWordCursor();
                            page.findLineFromStart(endCursor, myOverlappingValue);
                            if (!endCursor.isNull() && endCursor.samePositionAs(page.EndCursor)) {
                                page.findLineFromEnd(endCursor, 1);
                            }
                            if (!endCursor.isNull()) {
                                ZLTextWordCursor startCursor = findStartOfPrevousPage(page, endCursor);
                                if (startCursor.samePositionAs(page.StartCursor)) {
                                    page.StartCursor.setCursor(findStartOfPrevousPage(page, page.StartCursor));
                                } else {
                                    page.StartCursor.setCursor(startCursor);
                                }
                            } else {
                                page.StartCursor.setCursor(findStartOfPrevousPage(page, page.StartCursor));
                            }
                            break;
                        }
                        case ScrollingMode.SCROLL_LINES:
                            page.StartCursor.setCursor(findStart(page, page.StartCursor, SizeUnit.LINE_UNIT, myOverlappingValue));
                            break;
                        case ScrollingMode.SCROLL_PERCENTAGE:
                            page.StartCursor.setCursor(findStart(page, page.StartCursor, SizeUnit.PIXEL_UNIT, page.getTextHeight() * myOverlappingValue / 100));
                            break;
                    }
                    buildInfos(page, page.StartCursor, page.EndCursor);
                    if (page.isEmptyPage()) {
                        page.StartCursor.setCursor(findStart(page, page.StartCursor, SizeUnit.LINE_UNIT, 1));
                        buildInfos(page, page.StartCursor, page.EndCursor);
                    }
                }
                break;
            case PaintStateEnum.START_IS_KNOWN:
                if (!page.StartCursor.isNull()) {
                    buildInfos(page, page.StartCursor, page.EndCursor);
                }
                break;
            case PaintStateEnum.END_IS_KNOWN:
                if (!page.EndCursor.isNull()) {
                    page.StartCursor.setCursor(findStartOfPrevousPage(page, page.EndCursor));
                    buildInfos(page, page.StartCursor, page.EndCursor);
                }
                break;
        }
        page.PaintState = PaintStateEnum.READY;
        // TODO: cache?
        myLineInfoCache.clear();

        if (page == myCurrentPage) {
            if (oldState != PaintStateEnum.START_IS_KNOWN) {
                myPreviousPage.reset();
            }
            if (oldState != PaintStateEnum.END_IS_KNOWN) {
                myNextPage.reset();
            }
        }
    }

    public void clearCaches() {
        resetMetrics();
        rebuildPaintInfo();
        Application.getViewWidget().reset();
        myCharWidth = -1;
    }

    protected synchronized void rebuildPaintInfo() {
        myPreviousPage.reset();
        myNextPage.reset();
        if (myCursorManager != null) {
            myCursorManager.evictAll();
        }

        if (myCurrentPage.PaintState != PaintStateEnum.NOTHING_TO_PAINT) {
            myCurrentPage.LineInfos.clear();
            if (!myCurrentPage.StartCursor.isNull()) {
                myCurrentPage.StartCursor.rebuild();
                myCurrentPage.EndCursor.reset();
                myCurrentPage.PaintState = PaintStateEnum.START_IS_KNOWN;
            } else if (!myCurrentPage.EndCursor.isNull()) {
                myCurrentPage.EndCursor.rebuild();
                myCurrentPage.StartCursor.reset();
                myCurrentPage.PaintState = PaintStateEnum.END_IS_KNOWN;
            }
        }

        myLineInfoCache.clear();
    }

    private int infoSize(ZLTextLineInfo info, int unit) {
        return (unit == SizeUnit.PIXEL_UNIT) ? (info.getHeight() + info.getDescent() + info.getVSpaceAfter()) : (info.isVisible() ? 1 : 0);
    }

    /**
     * Calculate the paragraph's height and margin.
     *
     * @param page
     * @param cursor                The input cursor position, maybe start or end which is decided by "beforeCurrentPosition".
     * @param beforeCurrentPosition
     * @param unit                  The unit is defined by {@link SizeUnit} PIXEL_UNIT or LINE_UNIT, usually equal to PIXEL_UNIT.
     */
    private ParagraphSize paragraphSize(ZLTextPage page, ZLTextWordCursor cursor, boolean beforeCurrentPosition, int unit) {
        final ParagraphSize size = new ParagraphSize();

        final ZLTextParagraphCursor paragraphCursor = cursor.getParagraphCursor();
        if (paragraphCursor == null) {
            return size;
        }
        final int endElementIndex =
                beforeCurrentPosition ? cursor.getElementIndex() : paragraphCursor.getParagraphLength();

        resetTextStyle();

        int wordIndex = 0;
        int charIndex = 0;
        ZLTextLineInfo info = null;
        while (wordIndex != endElementIndex) {
            final ZLTextLineInfo prev = info;
            info = processTextLine(page, paragraphCursor, wordIndex, charIndex, endElementIndex, prev);
            wordIndex = info.EndElementIndex;
            charIndex = info.EndCharIndex;
            size.Height += infoSize(info, unit);
            // update margin info to "size".
            if (prev == null) {
                size.TopMargin = info.getVSpaceBefore();
            }
            size.BottomMargin = info.getVSpaceAfter();
        }

        return size;
    }

    /**
     * Jump back the number of rows of the specified height.
     *
     * @param page
     * @param cursor The current line's start cursor.
     * @param unit
     * @param size   The height of Jumping back.
     */
    private void skip(ZLTextPage page, ZLTextWordCursor cursor, int unit, int size) {
        final ZLTextParagraphCursor paragraphCursor = cursor.getParagraphCursor();
        if (paragraphCursor == null) {
            return;
        }
        final int endElementIndex = paragraphCursor.getParagraphLength();

        resetTextStyle();
        applyStyleChanges(paragraphCursor, 0, cursor.getElementIndex());

        ZLTextLineInfo info = null;
        while (!cursor.isEndOfParagraph() && size > 0) {
            info = processTextLine(page, paragraphCursor, cursor.getElementIndex(), cursor.getCharIndex(), endElementIndex, info);
            cursor.moveTo(info.EndElementIndex, info.EndCharIndex);
            size -= infoSize(info, unit);
        }
    }

    private ZLTextWordCursor findStartOfPrevousPage(ZLTextPage page, ZLTextWordCursor end) {
        if (twoColumnView()) {
            end = findStart(page, end, SizeUnit.PIXEL_UNIT, page.getTextHeight());
        }
        end = findStart(page, end, SizeUnit.PIXEL_UNIT, page.getTextHeight());
        return end;
    }

    private ZLTextWordCursor findStart(ZLTextPage page, ZLTextWordCursor end, int unit, int height) {
        final ZLTextWordCursor start = new ZLTextWordCursor(end);
        ParagraphSize size = paragraphSize(page, start, true, unit);
        height -= size.Height;
        boolean positionChanged = !start.isStartOfParagraph();
        start.moveToParagraphStart();
        while (height > 0) {
            final ParagraphSize previousSize = size;
            if (positionChanged && start.getParagraphCursor().isEndOfSection()) {
                break;
            }
            if (!start.previousParagraph()) {
                break;
            }
            if (!start.getParagraphCursor().isEndOfSection()) {
                positionChanged = true;
            }
            size = paragraphSize(page, start, false, unit);
            height -= size.Height;
            if (previousSize != null) {
                height += Math.min(size.BottomMargin, previousSize.TopMargin);
            }
        }
        skip(page, start, unit, -height);

        if (unit == SizeUnit.PIXEL_UNIT) {
            boolean sameStart = start.samePositionAs(end);
            if (!sameStart && start.isEndOfParagraph() && end.isStartOfParagraph()) {
                ZLTextWordCursor startCopy = new ZLTextWordCursor(start);
                startCopy.nextParagraph();
                sameStart = startCopy.samePositionAs(end);
            }
            if (sameStart) {
                start.setCursor(findStart(page, end, SizeUnit.LINE_UNIT, 1));
            }
        }

        return start;
    }

    protected ZLTextElementArea getElementByCoordinates(int x, int y) {
        return myCurrentPage.TextElementMap.binarySearch(x, y);
    }

    public final void outlineRegion(ZLTextRegion region) {
        outlineRegion(region != null ? region.getSoul() : null);
    }

    public final void outlineRegion(ZLTextRegion.Soul soul) {
        myShowOutline = true;
        myOutlinedRegionSoul = soul;
    }

    public void hideOutline() {
        myShowOutline = false;
        Application.getViewWidget().reset();
    }

    private ZLTextRegion getOutlinedRegion(ZLTextPage page) {
        return page.TextElementMap.getRegion(myOutlinedRegionSoul);
    }

    public ZLTextRegion getOutlinedRegion() {
        return getOutlinedRegion(myCurrentPage);
    }

    protected ZLTextHighlighting findHighlighting(int x, int y, int maxDistance) {
        final ZLTextRegion region = findRegion(x, y, maxDistance, ZLTextRegion.AnyRegionFilter);
        if (region == null) {
            return null;
        }
        synchronized (myHighlightings) {
            for (ZLTextHighlighting h : myHighlightings) {
                if (h.getBackgroundColor() != null && h.intersects(region)) {
                    return h;
                }
            }
        }
        return null;
    }

    protected ZLTextRegion findRegion(int x, int y, ZLTextRegion.Filter filter) {
        return findRegion(x, y, Integer.MAX_VALUE - 1, filter);
    }

    protected ZLTextRegion findRegion(int x, int y, int maxDistance, ZLTextRegion.Filter filter) {
        return myCurrentPage.TextElementMap.findRegion(x, y, maxDistance, filter);
    }

    protected ZLTextElementAreaVector.RegionPair findRegionsPair(int x, int y, ZLTextRegion.Filter filter) {
        return myCurrentPage.TextElementMap.findRegionsPair(x, y, getColumnIndex(x), filter);
    }

/*
	public void resetRegionPointer() {
		myOutlinedRegionSoul = null;
		myShowOutline = true;
	}
*/

    protected boolean initSelection(int x, int y) {
        y -= getTextStyleCollection().getBaseStyle().getFontSize() / 2;
        if (!mySelection.start(x, y)) {
            return false;
        }
        Application.getViewWidget().reset();
        Application.getViewWidget().repaint();
        return true;
    }

    public void clearSelection() {
        if (mySelection.clear()) {
            Application.getViewWidget().reset();
            Application.getViewWidget().repaint();
        }
    }

    public ZLTextHighlighting getSelectionHighlighting() {
        return mySelection;
    }

    public int getSelectionStartY() {
        if (mySelection.isEmpty()) {
            return 0;
        }
        final ZLTextElementArea selectionStartArea = mySelection.getStartArea(myCurrentPage);
        if (selectionStartArea != null) {
            return selectionStartArea.YStart;
        }
        if (mySelection.hasPartBeforePage(myCurrentPage)) {
            final ZLTextElementArea firstArea = myCurrentPage.TextElementMap.getFirstArea();
            return firstArea != null ? firstArea.YStart : 0;
        } else {
            final ZLTextElementArea lastArea = myCurrentPage.TextElementMap.getLastArea();
            return lastArea != null ? lastArea.YEnd : 0;
        }


    }

    public int getSelectionEndY() {
        if (mySelection.isEmpty()) {
            return 0;
        }
        final ZLTextElementArea selectionEndArea = mySelection.getEndArea(myCurrentPage);
        if (selectionEndArea != null) {
            return selectionEndArea.YEnd;
        }
        if (mySelection.hasPartAfterPage(myCurrentPage)) {
            final ZLTextElementArea lastArea = myCurrentPage.TextElementMap.getLastArea();
            return lastArea != null ? lastArea.YEnd : 0;
        } else {
            final ZLTextElementArea firstArea = myCurrentPage.TextElementMap.getFirstArea();
            return firstArea != null ? firstArea.YStart : 0;
        }
    }

    public ZLTextPosition getSelectionStartPosition() {
        return mySelection.getStartPosition();
    }

    public ZLTextPosition getSelectionEndPosition() {
        return mySelection.getEndPosition();
    }

    public boolean isSelectionEmpty() {
        return mySelection.isEmpty();
    }

    public ZLTextRegion nextRegion(Direction direction, ZLTextRegion.Filter filter) {
        return myCurrentPage.TextElementMap.nextRegion(getOutlinedRegion(), direction, filter);
    }

    @Override
    public boolean canScroll(PageIndex index) {
        switch (index) {
            default:
                return true;
            case next: {
                final ZLTextWordCursor cursor = getEndCursor();
                return cursor != null && !cursor.isNull() && !cursor.isEndOfText();
            }
            case previous: {
                final ZLTextWordCursor cursor = getStartCursor();
                return cursor != null && !cursor.isNull() && !cursor.isStartOfText();
            }
        }
    }

    ZLTextParagraphCursor cursor(int index) {
        return myCursorManager.get(index);
    }

    protected abstract ExtensionElementManager getExtensionManager();

    public interface ScrollingMode {
        int NO_OVERLAPPING = 0;
        // no use.
        int KEEP_LINES = 1;
        int SCROLL_LINES = 2;
        // no use.
        int SCROLL_PERCENTAGE = 3;
    }

    private interface SizeUnit {
        int PIXEL_UNIT = 0;
        int LINE_UNIT = 1;
    }

    public static class PagePosition {
        public final int Current;
        public final int Total;

        PagePosition(int current, int total) {
            Current = current;
            Total = total;
        }
    }

    /**
     * Define height and margin info of a paragraph.
     */
    private static class ParagraphSize {
        public int Height;
        public int TopMargin;
        public int BottomMargin;
    }
}
