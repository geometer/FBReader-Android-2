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

#include <cctype>

#include <AndroidUtil.h>
#include <JniEnvelope.h>

#include <ZLUnicodeUtil.h>

std::string ZLUnicodeUtil::toLowerFull(const std::string &utf8String) {
	if (utf8String.empty()) {
		return utf8String;
	}

	bool isAscii = true;
	const int size = utf8String.size();
	for (int i = size - 1; i >= 0; --i) {
		if ((utf8String[i] & 0x80) != 0) {
			isAscii = false;
			break;
		}
	}
	if (isAscii) {
		std::string result(size, ' ');
		for (int i = size - 1; i >= 0; --i) {
			result[i] = std::tolower(utf8String[i]);
		}
		return result;
	}
	JNIEnv *env = AndroidUtil::getEnv();
	jstring javaString = AndroidUtil::createJavaString(env, utf8String);
	jstring lowerCased = AndroidUtil::Method_java_lang_String_toLowerCase->callForJavaString(javaString);
	if (javaString == lowerCased) {
		env->DeleteLocalRef(lowerCased);
		env->DeleteLocalRef(javaString);
		return utf8String;
	} else {
		const std::string result = AndroidUtil::fromJavaString(env, lowerCased);
		env->DeleteLocalRef(lowerCased);
		env->DeleteLocalRef(javaString);
		return result;
	}
}

std::string ZLUnicodeUtil::toUpperFull(const std::string &utf8String) {
	if (utf8String.empty()) {
		return utf8String;
	}

	bool isAscii = true;
	const int size = utf8String.size();
	for (int i = size - 1; i >= 0; --i) {
		if ((utf8String[i] & 0x80) != 0) {
			isAscii = false;
			break;
		}
	}
	if (isAscii) {
		std::string result(size, ' ');
		for (int i = size - 1; i >= 0; --i) {
			result[i] = std::toupper(utf8String[i]);
		}
		return result;
	}
	JNIEnv *env = AndroidUtil::getEnv();
	jstring javaString = AndroidUtil::createJavaString(env, utf8String);
	jstring upperCased = AndroidUtil::Method_java_lang_String_toUpperCase->callForJavaString(javaString);
	if (javaString == upperCased) {
		env->DeleteLocalRef(upperCased);
		env->DeleteLocalRef(javaString);
		return utf8String;
	} else {
		const std::string result = AndroidUtil::fromJavaString(env, upperCased);
		env->DeleteLocalRef(upperCased);
		env->DeleteLocalRef(javaString);
		return result;
	}
}

std::string ZLUnicodeUtil::convertNonUtfString(const std::string &str) {
	if (isUtf8String(str)) {
		return str;
	}

	JNIEnv *env = AndroidUtil::getEnv();

	const int len = str.length();
	jchar *chars = new jchar[len];
	for (int i = 0; i < len; ++i) {
		chars[i] = (unsigned char)str[i];
	}
	jstring javaString = env->NewString(chars, len);
	const std::string result = AndroidUtil::fromJavaString(env, javaString);
	env->DeleteLocalRef(javaString);
	delete[] chars;

	return result;
}
