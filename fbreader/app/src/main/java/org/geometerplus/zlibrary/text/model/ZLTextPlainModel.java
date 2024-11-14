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

package org.geometerplus.zlibrary.text.model;

import org.geometerplus.zlibrary.core.fonts.FontManager;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.util.CSSUtil;
import org.geometerplus.zlibrary.core.util.ZLSearchPattern;
import org.geometerplus.zlibrary.core.util.ZLSearchUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ZLTextPlainModel implements ZLTextModel, ZLTextStyleEntry.Feature {
    private final String myId;
    private final String myLanguage;
    /**
     * Use to read ncahe file and storage char[] datas.
     */
    private final CachedCharStorage myStorage;
    /**
     * key                                                 value
     * "media/cover.png" - ZFileImage{ ZLZipEntryFile { ZLFile [/storage/emulated/0/GGG.epub:media/cover.png]} }
     */
    private final Map<String, ZLImage> myImageMap;
    /**
     * The font manager is used to storage the font info which provided by native so.
     */
    private final FontManager myFontManager;

    /**
     * Provide the index of the ncache file array which the paragraph is in.
     */
    private int[] myStartEntryIndices;
    /**
     * Provide the starting offset of the paragraph which in the corresponding ncache char array.
     */
    private int[] myStartEntryOffsets;
    /**
     * Provide the element number of every paragraph.
     */
    private int[] myParagraphLengths;
    /**
     * Provide the total number of words at any paragraph position
     */
    private int[] myTextSizes;
    /**
     * Provide the kind of every paragraph.
     */
    private byte[] myParagraphKinds;
    /**
     * Provide the total number of paragraphs.
     */
    private int myParagraphsNumber;

    /**
     * Save the marks results of searching word.
     */
    private ArrayList<ZLTextMark> myMarks;

    public ZLTextPlainModel(
            String id,
            String language,
            int paragraphsNumber,
            int[] entryIndices,
            int[] entryOffsets,
            int[] paragraphLengths,
            int[] textSizes,
            byte[] paragraphKinds,
            String directoryName,
            String fileName,
            String fileExtension,
            int blocksNumber,
            Map<String, ZLImage> imageMap,
            FontManager fontManager
    ) {
        myId = id;
        myLanguage = language;
        myParagraphsNumber = paragraphsNumber;
        myStartEntryIndices = entryIndices;
        myStartEntryOffsets = entryOffsets;
        myParagraphLengths = paragraphLengths;
        myTextSizes = textSizes;
        myParagraphKinds = paragraphKinds;
        myStorage = new CachedCharStorage(directoryName, fileName, fileExtension, blocksNumber);
        myImageMap = imageMap;
        myFontManager = fontManager;
    }

    /**
     * Binary search the index of array which the input text size value locates array element range.
     *
     * @param array  the total text size before the corresponding paragraph which is array.
     * @param length the number of paragraphs.
     * @param value  the input text size.
     * @return if the value isn't equal to the array element(such as: value = 10850, array = [...,9805,11000,15300,...]),
     * this will return (-lowIndex - 1), namely 15300's index, this because the caller will
     * use the negative return result minus one to get the correct index.
     */
    private static int binarySearch(int[] array, int length, int value) {
        int lowIndex = 0;
        int highIndex = length - 1;

        while (lowIndex <= highIndex) {
            // maybe odd number which is equal to lowIndex.
            int midIndex = (lowIndex + highIndex) >>> 1;
            int midValue = array[midIndex];
            if (midValue > value) {
                highIndex = midIndex - 1;
            } else if (midValue < value) {
                lowIndex = midIndex + 1;
            } else {
                return midIndex;
            }
        }
        return -lowIndex - 1;
    }

    @Override
    public final String getId() {
        return myId;
    }

    @Override
    public final String getLanguage() {
        return myLanguage;
    }

    @Override
    public final ZLTextMark getFirstMark() {
        return (myMarks == null || myMarks.isEmpty()) ? null : myMarks.get(0);
    }

    @Override
    public final ZLTextMark getLastMark() {
        return (myMarks == null || myMarks.isEmpty()) ? null : myMarks.get(myMarks.size() - 1);
    }

    @Override
    public final ZLTextMark getNextMark(ZLTextMark position) {
        if (position == null || myMarks == null) {
            return null;
        }

        ZLTextMark mark = null;
        for (ZLTextMark current : myMarks) {
            if (current.compareTo(position) >= 0) {
                if ((mark == null) || (mark.compareTo(current) > 0)) {
                    mark = current;
                }
            }
        }
        return mark;
    }

    @Override
    public final ZLTextMark getPreviousMark(ZLTextMark position) {
        if ((position == null) || (myMarks == null)) {
            return null;
        }

        ZLTextMark mark = null;
        for (ZLTextMark current : myMarks) {
            if (current.compareTo(position) < 0) {
                if ((mark == null) || (mark.compareTo(current) < 0)) {
                    mark = current;
                }
            }
        }
        return mark;
    }

    @Override
    public final int search(final String text, int startIndex, int endIndex, boolean ignoreCase) {
        int count = 0;
        ZLSearchPattern pattern = new ZLSearchPattern(text, ignoreCase);
        myMarks = new ArrayList<ZLTextMark>();
        if (startIndex > myParagraphsNumber) {
            startIndex = myParagraphsNumber;
        }
        if (endIndex > myParagraphsNumber) {
            endIndex = myParagraphsNumber;
        }
        int index = startIndex;
        final EntryIteratorImpl it = new EntryIteratorImpl(index);
        while (true) {
            int offset = 0;
            while (it.next()) {
                if (it.getType() == ZLTextParagraph.Entry.TEXT) {
                    char[] textData = it.getTextData();
                    int textOffset = it.getTextOffset();
                    int textLength = it.getTextLength();
                    for (ZLSearchUtil.Result res = ZLSearchUtil.find(textData, textOffset, textLength, pattern); res != null;
                         res = ZLSearchUtil.find(textData, textOffset, textLength, pattern, res.Start + 1)) {
                        myMarks.add(new ZLTextMark(index, offset + res.Start, res.Length));
                        ++count;
                    }
                    offset += textLength;
                }
            }
            if (++index >= endIndex) {
                break;
            }
            it.reset(index);
        }
        return count;
    }

    @Override
    public final List<ZLTextMark> getMarks() {
        return myMarks != null ? myMarks : Collections.<ZLTextMark>emptyList();
    }

    @Override
    public final void removeAllMarks() {
        myMarks = null;
    }

    @Override
    public final int getParagraphsNumber() {
        return myParagraphsNumber;
    }

    @Override
    public final ZLTextParagraph getParagraph(int index) {
        final byte kind = myParagraphKinds[index];
        return (kind == ZLTextParagraph.Kind.TEXT_PARAGRAPH) ?
                new ZLTextParagraphImpl(this, index) :
                new ZLTextSpecialParagraphImpl(kind, this, index);
    }

    @Override
    public final int getTextLength(int index) {
        if (index < 0 || myTextSizes.length == 0 || myParagraphsNumber == 0) {
            return 0;
        }
        return myTextSizes[Math.min(index, myParagraphsNumber - 1)];
    }

    /**
     * Binary search the paragraph index of the specified text size.
     */
    @Override
    public final int findParagraphByTextLength(int length) {
        int index = binarySearch(myTextSizes, myParagraphsNumber, length);
        if (index >= 0) {
            return index;
        }
        return Math.min(-index - 1, myParagraphsNumber - 1);
    }

    /**
     * A core iterator class which is used to loop the paragraph's element.
     * Reference {@link #next()} function.
     */
    final class EntryIteratorImpl implements ZLTextParagraph.EntryIterator {
        int myDataIndex;
        int myDataOffset;
        private int myCounter;
        private int myLength;
        private byte myType;
        // TextEntry data
        private char[] myTextData;
        private int myTextOffset;
        private int myTextLength;

        // ControlEntry data
        private byte myControlKind;
        private boolean myControlIsStart;
        // HyperlinkControlEntry data
        private byte myHyperlinkType;
        private String myHyperlinkId;

        // ImageEntry
        private ZLImageEntry myImageEntry;

        // VideoEntry
        private ZLVideoEntry myVideoEntry;

        // ExtensionEntry
        private ExtensionEntry myExtensionEntry;

        // StyleEntry
        private ZLTextStyleEntry myStyleEntry;

        // FixedHSpaceEntry data
        private short myFixedHSpaceLength;

        EntryIteratorImpl(int index) {
            reset(index);
        }

        void reset(int index) {
            myCounter = 0;
            myLength = myParagraphLengths[index];
            myDataIndex = myStartEntryIndices[index];
            myDataOffset = myStartEntryOffsets[index];
        }

        @Override
        public byte getType() {
            return myType;
        }

        @Override
        public char[] getTextData() {
            return myTextData;
        }

        @Override
        public int getTextOffset() {
            return myTextOffset;
        }

        @Override
        public int getTextLength() {
            return myTextLength;
        }

        @Override
        public byte getControlKind() {
            return myControlKind;
        }

        @Override
        public boolean getControlIsStart() {
            return myControlIsStart;
        }

        @Override
        public byte getHyperlinkType() {
            return myHyperlinkType;
        }

        @Override
        public String getHyperlinkId() {
            return myHyperlinkId;
        }

        @Override
        public ZLImageEntry getImageEntry() {
            return myImageEntry;
        }

        @Override
        public ZLVideoEntry getVideoEntry() {
            return myVideoEntry;
        }

        @Override
        public ExtensionEntry getExtensionEntry() {
            return myExtensionEntry;
        }

        @Override
        public ZLTextStyleEntry getStyleEntry() {
            return myStyleEntry;
        }

        @Override
        public short getFixedHSpaceLength() {
            return myFixedHSpaceLength;
        }

        @Override
        public boolean next() {
            if (myCounter >= myLength) {
                return false;
            }

            int dataOffset = myDataOffset;
            char[] data = myStorage.block(myDataIndex);
            if (data == null) {
                return false;
            }
            if (dataOffset >= data.length) {
                data = myStorage.block(++myDataIndex);
                if (data == null) {
                    return false;
                }
                dataOffset = 0;
            }
            short first = (short) data[dataOffset];
            byte type = (byte) first;
            if (type == 0) {
                data = myStorage.block(++myDataIndex);
                if (data == null) {
                    return false;
                }
                dataOffset = 0;
                first = (short) data[0];
                type = (byte) first;
            }
            myType = type;
            ++dataOffset;
            switch (type) {
                case ZLTextParagraph.Entry.TEXT: {
                    int textLength = (int) data[dataOffset++];
                    textLength += (((int) data[dataOffset++]) << 16);
                    textLength = Math.min(textLength, data.length - dataOffset);
                    myTextLength = textLength;
                    myTextData = data;
                    myTextOffset = dataOffset;
                    dataOffset += textLength;
                    break;
                }
                case ZLTextParagraph.Entry.CONTROL: {
                    short kind = (short) data[dataOffset++];
                    myControlKind = (byte) kind;
                    myControlIsStart = (kind & 0x0100) == 0x0100;
                    myHyperlinkType = 0;
                    break;
                }
                case ZLTextParagraph.Entry.HYPERLINK_CONTROL: {
                    final short kind = (short) data[dataOffset++];
                    myControlKind = (byte) kind;
                    myControlIsStart = true;
                    myHyperlinkType = (byte) (kind >> 8);
                    final short labelLength = (short) data[dataOffset++];
                    myHyperlinkId = new String(data, dataOffset, labelLength);
                    dataOffset += labelLength;
                    break;
                }
                case ZLTextParagraph.Entry.IMAGE: {
                    final short vOffset = (short) data[dataOffset++];
                    final short len = (short) data[dataOffset++];
                    final String id = new String(data, dataOffset, len);
                    dataOffset += len;
                    final boolean isCover = data[dataOffset++] != 0;
                    myImageEntry = new ZLImageEntry(myImageMap, id, vOffset, isCover);
                    break;
                }
                case ZLTextParagraph.Entry.FIXED_HSPACE:
                    myFixedHSpaceLength = (short) data[dataOffset++];
                    break;
                case ZLTextParagraph.Entry.STYLE_CSS:
                case ZLTextParagraph.Entry.STYLE_OTHER: {
                    final short depth = (short) ((first >> 8) & 0xFF);
                    final ZLTextStyleEntry entry =
                            type == ZLTextParagraph.Entry.STYLE_CSS
                                    ? new ZLTextCSSStyleEntry(depth)
                                    : new ZLTextOtherStyleEntry();
                    int mask = (int) data[dataOffset++];
                    mask += (((int) data[dataOffset++]) << 16);
                    for (int i = 0; i < NUMBER_OF_LENGTHS; ++i) {
                        if (ZLTextStyleEntry.isFeatureSupported(mask, i)) {
                            final short size = (short) data[dataOffset++];
                            final byte unit = (byte) data[dataOffset++];
                            entry.setLength(i, size, unit);
                        }
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, ALIGNMENT_TYPE) ||
                            ZLTextStyleEntry.isFeatureSupported(mask, NON_LENGTH_VERTICAL_ALIGN)) {
                        final short value = (short) data[dataOffset++];
                        if (ZLTextStyleEntry.isFeatureSupported(mask, ALIGNMENT_TYPE)) {
                            entry.setAlignmentType((byte) (value & 0xFF));
                        }
                        if (ZLTextStyleEntry.isFeatureSupported(mask, NON_LENGTH_VERTICAL_ALIGN)) {
                            entry.setVerticalAlignCode((byte) ((value >> 8) & 0xFF));
                        }
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, FONT_FAMILY)) {
                        entry.setFontFamilies(myFontManager, (short) data[dataOffset++]);
                    }
                    if (ZLTextStyleEntry.isFeatureSupported(mask, FONT_STYLE_MODIFIER)) {
                        final short value = (short) data[dataOffset++];
                        entry.setFontModifiers((byte) (value & 0xFF), (byte) ((value >> 8) & 0xFF));
                    }

                    // add text color.
                    if (ZLTextStyleEntry.isFeatureSupported(mask, COLOR)) {
                        final short colorLen = (short) data[dataOffset++];
                        String color = new String(data, dataOffset, colorLen);
                        entry.setColor(CSSUtil.transferColor(color));
                        dataOffset += colorLen;
                    }

                    if (ZLTextStyleEntry.isFeatureSupported(mask, BACKGROUND_COLOR)) {
                        final short colorLen = (short) data[dataOffset++];
                        String color = new String(data, dataOffset, colorLen);
                        entry.setBgColor(CSSUtil.transferColor(color));
                        dataOffset += colorLen;
                    }

                    myStyleEntry = entry;
                }
                case ZLTextParagraph.Entry.STYLE_CLOSE:
                    // No data
                    break;
                case ZLTextParagraph.Entry.RESET_BIDI:
                    // No data
                    break;
                case ZLTextParagraph.Entry.AUDIO:
                    // No data
                    break;
                case ZLTextParagraph.Entry.VIDEO: {
                    myVideoEntry = new ZLVideoEntry();
                    final short mapSize = (short) data[dataOffset++];
                    for (short i = 0; i < mapSize; ++i) {
                        short len = (short) data[dataOffset++];
                        final String mime = new String(data, dataOffset, len);
                        dataOffset += len;
                        len = (short) data[dataOffset++];
                        final String src = new String(data, dataOffset, len);
                        dataOffset += len;
                        myVideoEntry.addSource(mime, src);
                    }
                    break;
                }
                case ZLTextParagraph.Entry.EXTENSION: {
                    final short kindLength = (short) data[dataOffset++];
                    final String kind = new String(data, dataOffset, kindLength);
                    dataOffset += kindLength;

                    final Map<String, String> map = new HashMap<String, String>();
                    final short dataSize = (short) ((first >> 8) & 0xFF);
                    for (short i = 0; i < dataSize; ++i) {
                        final short keyLength = (short) data[dataOffset++];
                        final String key = new String(data, dataOffset, keyLength);
                        dataOffset += keyLength;
                        final short valueLength = (short) data[dataOffset++];
                        map.put(key, new String(data, dataOffset, valueLength));
                        dataOffset += valueLength;
                    }
                    myExtensionEntry = new ExtensionEntry(kind, map);
                    break;
                }
                default:
                    break;
            }
            ++myCounter;
            myDataOffset = dataOffset;
            return true;
        }
    }
}
