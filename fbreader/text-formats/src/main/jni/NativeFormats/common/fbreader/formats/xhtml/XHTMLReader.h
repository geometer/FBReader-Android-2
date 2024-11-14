/*
 * Copyright (C) 2004-2017 FBReader.ORG Limited <contact@fbreader.org>
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

#ifndef __XHTMLREADER_H__
#define __XHTMLREADER_H__

#include <string>
#include <map>
#include <vector>
#include <stack>

#include <ZLBoolean3.h>
#include <ZLXMLReader.h>
#include <ZLVideoEntry.h>
#include <FontMap.h>

#include "../css/StyleSheetTable.h"
#include "../css/StyleSheetParser.h"
#include "../../bookmodel/FBTextKind.h"
#include "XHTMLTagInfo.h"

class ZLFile;

class BookReader;

class XHTMLReader;

class EncryptionMap;

enum XHTMLReadingState {
    XHTML_READ_NOTHING,
    XHTML_READ_STYLE,
    XHTML_READ_BODY,
    XHTML_READ_VIDEO
};

/**
 * Define tag operation abstract class, and provide tag start and end interfaces.
 */
class XHTMLTagAction {

public:
    virtual ~XHTMLTagAction();

    virtual void doAtStart(XHTMLReader &reader, const char **xmlattributes) = 0;

    virtual void doAtEnd(XHTMLReader &reader) = 0;

    virtual bool isEnabled(XHTMLReadingState state) = 0;

protected:
    static BookReader &bookReader(XHTMLReader &reader);

    static const std::string &pathPrefix(XHTMLReader &reader);

    static void beginParagraph(XHTMLReader &reader);

    static void endParagraph(XHTMLReader &reader);
};

/**
 * Implement "ZLXMLReader" abstract class to read XHTML file.
 *
 * Realize tag processing function and tag content processing function.
 * Register binding tag action, and define tag start and end implementation for each action.
 * The "BookReader" object is held internally to add the parsed data, eg: addData(const std::string &data).
 * It provides the parser for the style of the book XHTML file, and is responsible for parsing
 * the xx.css file, style tag and style attribute.
 */
class XHTMLReader : public ZLXMLReader {

public:
    struct TagData {
        std::vector<FBTextKind> TextKinds;
        std::vector<shared_ptr<ZLTextStyleEntry> > StyleEntries;
        ZLBoolean3 PageBreakAfter;
        ZLTextStyleEntry::DisplayCode DisplayCode;
        XHTMLTagInfoList Children;

        TagData();
    };

public:
    static XHTMLTagAction *addAction(const std::string &tag, XHTMLTagAction *action);

    static XHTMLTagAction *
    addAction(const std::string &ns, const std::string &name, XHTMLTagAction *action);

    static void fillTagTable();

private:
    static std::map<std::string, XHTMLTagAction *> ourTagActions;
    static std::map<shared_ptr<FullNamePredicate>, XHTMLTagAction *> ourNsTagActions;

public:
    XHTMLReader(BookReader &modelReader, shared_ptr<EncryptionMap> map);

    ~XHTMLReader();

    bool readFile(const ZLFile &file, const std::string &referenceName);

    const std::string &fileAlias(const std::string &fileName) const;

    const std::string normalizedReference(const std::string &reference) const;

    void setMarkFirstImageAsCover();

private:
    XHTMLTagAction *getAction(const std::string &tag);

    void startElementHandler(const char *tag, const char **attributes);

    void endElementHandler(const char *tag);

    void characterDataHandler(const char *text, std::size_t len);

    const std::vector<std::string> &externalDTDs() const;

    bool processNamespaces() const;

    void beginParagraph(bool restarted = false);

    void endParagraph();

    void restartParagraph(bool addEmptyLine);

    const XHTMLTagInfoList &tagInfos(size_t depth) const;

    bool matches(const shared_ptr<CSSSelector::Component> next, int depth = 0, int pos = -1) const;

    void applySingleEntry(shared_ptr<ZLTextStyleEntry> entry);

    void applyTagStyles(const std::string &tag, const std::string &aClass);

    void addTextStyleEntry(const ZLTextStyleEntry &entry, unsigned char depth);

    void addStyleCloseEntry();

    void pushTextKind(FBTextKind kind);

private:
    mutable std::map<std::string, std::string> myFileNumbers;

    BookReader &myModelReader;
    shared_ptr<EncryptionMap> myEncryptionMap;
    std::string myPathPrefix;
    std::string myReferenceAlias;
    std::string myReferenceDirName;
    bool myPreformatted;
    bool myNewParagraphInProgress;
    // .css style sheet, saving attribute selector and style element mapping collection.
    StyleSheetTable myStyleSheetTable;
    shared_ptr<FontMap> myFontMap;
    std::vector<shared_ptr<TagData> > myTagDataStack;
    bool myCurrentParagraphIsEmpty;
    shared_ptr<StyleSheetSingleStyleParser> myStyleParser;
    shared_ptr<StyleSheetTableParser> myTableParser;
    std::map<std::string, shared_ptr<StyleSheetParserWithCache> > myFileParsers;
    XHTMLReadingState myReadState;
    int myBodyCounter;
    // tag: ol / UL list stack, if start-tag into the stack else out of the stack.
    std::stack<int> myListNumStack;
    bool myMarkNextImageAsCover;
    shared_ptr<ZLVideoEntry> myVideoEntry;

    friend class XHTMLTagAction;

    friend class XHTMLTagStyleAction;

    friend class XHTMLTagLinkAction;

    friend class XHTMLTagHyperlinkAction;

    friend class XHTMLTagPreAction;

    friend class XHTMLTagParagraphAction;

    friend class XHTMLTagParagraphWithControlAction;

    friend class XHTMLTagControlAction;

    friend class XHTMLTagBodyAction;

    friend class XHTMLTagListAction;

    friend class XHTMLTagItemAction;

    friend class XHTMLTagImageAction;

    friend class XHTMLTagVideoAction;

    friend class XHTMLTagSourceAction;
};

#endif /* __XHTMLREADER_H__ */
