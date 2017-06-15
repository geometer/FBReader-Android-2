/*
 * Copyright (C) 2011-2017 FBReader.ORG Limited <contact@fbreader.org>
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

#include "JSONWriter.h"
#include <ZLFile.h>
#include <ZLStringUtil.h>

JSONWriter::JSONWriter(const std::string &path, char start, char end) : myEndBracket(end), myRoot(true), myIsClosed(false), myIsEmpty(true) {
	myStream = ZLFile(path).outputStream();
	myStream->open();
	myStream->write(start);
}

void JSONWriter::closeDescendants() {
	if (!myCurrentArray.isNull()) {
		myCurrentArray->close();
		myCurrentArray.reset();
	}
	if (!myCurrentMap.isNull()) {
		myCurrentMap->close();
		myCurrentMap.reset();
	}
}

bool JSONWriter::preAddElement() {
	if (myIsClosed) {
		return false;
	}

	closeDescendants();

	if (!myIsEmpty) {
		myStream->write(',');
	}
	myIsEmpty = false;

	return true;
}

void JSONWriter::writeString(const std::string &str) {
	myStream->write('\"');
	std::size_t start = 0;
	const std::size_t len = str.length();
	std::string escaped;
	for (std::size_t i = 0; i < len; ++i) {
		switch ((char)str[i]) {
			default:
				continue;
			case (char)0x08:
				escaped = "\\b";
				break;
			case (char)0x0C:
				escaped = "\\f";
				break;
			case '\n':
				escaped = "\\n";
				break;
			case '\r':
				escaped = "\\r";
				break;
			case '\t':
				escaped = "\\t";
				break;
			case '\"':
				escaped = "\\\"";
				break;
			case '\\':
				escaped = "\\\\";
				break;
		}
		myStream->write(str, start, i - start);
		myStream->write(escaped);
		start = i + 1;
	}
	myStream->write(str, start, len - start);
	myStream->write('\"');
}

void JSONWriter::writeNumber(int number) {
	myStream->write(ZLStringUtil::numberToString(number));
}
