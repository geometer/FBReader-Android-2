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

#include <AndroidUtil.h>

#include <ZLImage.h>
#include <ZLFile.h>
#include <ZLLogger.h>

#include "BookModel.h"
#include "BookReader.h"

#include "../formats/FormatPlugin.h"
#include "../library/Book.h"

const static std::string TAG = "BookModel";

BookModel::BookModel(const shared_ptr<Book> book, jobject javaModel, const std::string &cacheDir)
        : CacheDir(cacheDir), myBook(book) {
    ZLLogger::Instance().registerClass(TAG);
    myJavaModel = AndroidUtil::getEnv()->NewGlobalRef(javaModel);

    // The local book is no need to add name due to cacheDir.
    std::string fileName = book->file().path();
    if (!book->isLocalBook()) {
        int pos1 = fileName.find_last_of('/');
        int pos2 = fileName.find_last_of('.');
        if (pos2 <= 0) {
            pos2 = fileName.size() - 1;
        }
        fileName = fileName.substr(pos1 + 1, pos2 - pos1 - 1);
    } else {
        fileName = "";
    }
    Name = fileName;
    myBookTextModel = new ZLTextPlainModel(std::string(), book->language(), 131072, CacheDir,
                                           fileName, "ncache", myFontManager);
    myContentsTree = new ContentsTree();
}

BookModel::~BookModel() {
    AndroidUtil::getEnv()->DeleteGlobalRef(myJavaModel);
    ZLLogger::Instance().unregisterClass(TAG);
}

void BookModel::setHyperlinkMatcher(shared_ptr<HyperlinkMatcher> matcher) {
    myHyperlinkMatcher = matcher;
}

/**
 * Get the Label of input id.
 *
 * @param id The xhtml file's alias [0,1,2...] or use alias to splicing '#' and linkId.
 */
BookModel::Label BookModel::label(const std::string &id) const {
    if (!myHyperlinkMatcher.isNull()) {
        ZLLogger::Instance().println(TAG, " label()   id = " + id);
        return myHyperlinkMatcher->match(myInternalHyperlinks, id);
    }

    std::map<std::string, Label>::const_iterator it = myInternalHyperlinks.find(id);
    return (it != myInternalHyperlinks.end()) ? it->second : Label(0, -1);
}

const shared_ptr<Book> BookModel::book() const {
    return myBook;
}

bool BookModel::isLocal() const {
    return myBook->isLocalBook();
}

std::string BookModel::addedInnerTitle() const {
    return myBook->addedInnerTitle();
}

bool BookModel::flush() {
    myBookTextModel->flush();
    if (myBookTextModel->allocator().failed()) {
        return false;
    }

    std::map<std::string, shared_ptr<ZLTextModel> >::const_iterator it = myFootnotes.begin();
    for (; it != myFootnotes.end(); ++it) {
        it->second->flush();
        if (it->second->allocator().failed()) {
            return false;
        }
    }
    return true;
}
