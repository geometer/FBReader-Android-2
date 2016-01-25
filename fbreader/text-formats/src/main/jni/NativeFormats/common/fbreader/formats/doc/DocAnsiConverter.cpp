/*
 * Copyright (C) 2004-2014 Geometer Plus <contact@geometerplus.com>
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

#include "DocAnsiConverter.h"

DocAnsiConverter::DocAnsiConverter() : myCharMap(256) {
	for (int c = 0; c < 256; ++c) {
		myCharMap[c] = c;
	}
	myCharMap[0x80] = 0x20AC;
	myCharMap[0x81] = (ZLUnicodeUtil::Ucs2Char)' '; // space for char missing in win1252
	myCharMap[0x82] = 0x201A;
	myCharMap[0x83] = 0x0192;
	myCharMap[0x84] = 0x201E;
	myCharMap[0x85] = 0x2026;
	myCharMap[0x86] = 0x2020;
	myCharMap[0x87] = 0x2021;
	myCharMap[0x88] = 0x02C6;
	myCharMap[0x89] = 0x2030;
	myCharMap[0x8A] = 0x0160;
	myCharMap[0x8B] = 0x2039;
	myCharMap[0x8C] = 0x0152;
	myCharMap[0x8D] = (ZLUnicodeUtil::Ucs2Char)' '; // space for char missing in win1252
	myCharMap[0x8E] = 0x017D;
	myCharMap[0x8F] = (ZLUnicodeUtil::Ucs2Char)' '; // space for char missing in win1252
	myCharMap[0x90] = (ZLUnicodeUtil::Ucs2Char)' '; // space for char missing in win1252
	myCharMap[0x91] = 0x2018;
	myCharMap[0x92] = 0x2019;
	myCharMap[0x93] = 0x201C;
	myCharMap[0x94] = 0x201D;
	myCharMap[0x95] = 0x2022;
	myCharMap[0x96] = 0x2013;
	myCharMap[0x97] = 0x2014;
	myCharMap[0x98] = 0x02DC;
	myCharMap[0x99] = 0x2122;
	myCharMap[0x9A] = 0x0161;
	myCharMap[0x9B] = 0x203A;
	myCharMap[0x9C] = 0x0153;
	myCharMap[0x9D] = (ZLUnicodeUtil::Ucs2Char)' '; // space for char missing in win1252
	myCharMap[0x9E] = 0x017E;
	myCharMap[0x9F] = 0x0178;
}

void DocAnsiConverter::convert(ZLUnicodeUtil::Ucs2String &to, const char *srcStart, const char *srcEnd) {
	for (const char *ptr = srcStart; ptr < srcEnd; ++ptr) {
		to.push_back(myCharMap[*ptr]);
	}
}
