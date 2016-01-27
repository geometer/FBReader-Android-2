/*
 * Copyright (C) 2004-2015 FBReader.ORG Limited <contact@fbreader.org>
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
#include <JniEnvelope.h>

#include "SafeAndroidOutputStream.h"

SafeAndroidOutputStream::SafeAndroidOutputStream(shared_ptr<ZLOutputStream> base, jobject fileHandler, const std::string &name) : myBase(base), myFileHandler(fileHandler), myName(name) {
	myUseBase = !myBase.isNull();
}

SafeAndroidOutputStream::~SafeAndroidOutputStream() {
}

bool SafeAndroidOutputStream::open() {
	if (myUseBase) {
		myUseBase = myBase->open();
	}
	return true;
}

void SafeAndroidOutputStream::write(const char chr) {
	if (myUseBase) {
		myBase->write(chr);
		myUseBase = !myBase->hasErrors();
	}
	myBuffer += chr;
}

void SafeAndroidOutputStream::write(const char *data, std::size_t len) {
	if (myUseBase) {
		myBase->write(data, len);
		myUseBase = !myBase->hasErrors();
	}
	myBuffer.append(data, len);
}

void SafeAndroidOutputStream::close() {
	if (!myBase.isNull()) {
		myBase->close();
	}
	if (!myUseBase || myBase->hasErrors()) {
		JNIEnv *env = AndroidUtil::getEnv();
		JString name(env, myName, false);
		jbyteArray content = env->NewByteArray(myBuffer.size());
		env->SetByteArrayRegion(content, 0, myBuffer.size(), (jbyte*)myBuffer.data());
		AndroidUtil::Method_SafeFileHandler_setContent->call(myFileHandler, name.j(), content);
		env->DeleteLocalRef(content);
	}
}
