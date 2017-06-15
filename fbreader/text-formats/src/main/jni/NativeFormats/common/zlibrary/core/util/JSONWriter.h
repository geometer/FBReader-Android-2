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

#ifndef __JSONWRITER_H__
#define __JSONWRITER_H__

#include <shared_ptr.h>
#include <ZLOutputStream.h>

class JSONArrayWriter;
class JSONMapWriter;

class JSONWriter {

protected:
	JSONWriter(const std::string &path, char start, char end);
	JSONWriter(shared_ptr<ZLOutputStream> stream, char start, char end);

public:
	virtual ~JSONWriter();
	void close();

protected:
	bool preAddElement();
	shared_ptr<JSONArrayWriter> createArray();
	shared_ptr<JSONMapWriter> createMap();
	void writeString(const std::string &str);
	void writeNumber(int number);

private:
	void closeDescendants();

protected:
	shared_ptr<ZLOutputStream> myStream;

private:
	const char myEndBracket;

	const bool myRoot;
	bool myIsClosed;
	bool myIsEmpty;

	shared_ptr<JSONArrayWriter> myCurrentArray;
	shared_ptr<JSONMapWriter> myCurrentMap;
};

class JSONMapWriter : public JSONWriter {

friend class JSONWriter;

public:
	JSONMapWriter(const std::string &path);

private:
	JSONMapWriter(shared_ptr<ZLOutputStream> stream);

public:
	~JSONMapWriter();

	shared_ptr<JSONMapWriter> addMap(const std::string &key);
	shared_ptr<JSONArrayWriter> addArray(const std::string &key);
	void addElement(const std::string &key, const std::string &value);
	void addElement(const std::string &key, int value);

private:
	bool writeKeyAndColon(const std::string &key);
};

class JSONArrayWriter : public JSONWriter {

friend class JSONWriter;

public:
	JSONArrayWriter(const std::string &path);

private:
	JSONArrayWriter(shared_ptr<ZLOutputStream> stream);

public:
	~JSONArrayWriter();

	shared_ptr<JSONMapWriter> addMap();
	shared_ptr<JSONArrayWriter> addArray();
	void addElement(const std::string &value);
	void addElement(int value);
};

inline JSONWriter::JSONWriter(shared_ptr<ZLOutputStream> stream, char start, char end) : myStream(stream), myEndBracket(end), myRoot(false), myIsClosed(false), myIsEmpty(true) {
	stream->write(start);
}

inline JSONWriter::~JSONWriter() {
	close();
	if (myRoot) {
		myStream->close();
	}
}

inline void JSONWriter::close() {
	if (!myIsClosed) {
		closeDescendants();
		myStream->write(myEndBracket);
		myIsClosed = true;
	}
}

inline shared_ptr<JSONMapWriter> JSONWriter::createMap() {
	myCurrentMap = new JSONMapWriter(myStream);
	return myCurrentMap;
}

inline shared_ptr<JSONArrayWriter> JSONWriter::createArray() {
	myCurrentArray = new JSONArrayWriter(myStream);
	return myCurrentArray;
}

inline JSONMapWriter::JSONMapWriter(const std::string &path) : JSONWriter(path, '{', '}') {
}

inline JSONMapWriter::JSONMapWriter(shared_ptr<ZLOutputStream> stream) : JSONWriter(stream, '{', '}') {
}

inline JSONMapWriter::~JSONMapWriter() {
}

inline bool JSONMapWriter::writeKeyAndColon(const std::string &key) {
	if (preAddElement()) {
		writeString(key);
		myStream->write(':');
		return true;
	} else {
		return false;
	}
}

inline void JSONMapWriter::addElement(const std::string &key, const std::string &value) {
	if (writeKeyAndColon(key)) {
		writeString(value);
	}
}

inline void JSONMapWriter::addElement(const std::string &key, int value) {
	if (writeKeyAndColon(key)) {
		writeNumber(value);
	}
}

inline shared_ptr<JSONMapWriter> JSONMapWriter::addMap(const std::string &key) {
	return writeKeyAndColon(key) ? createMap() : 0;
}

inline shared_ptr<JSONArrayWriter> JSONMapWriter::addArray(const std::string &key) {
	return writeKeyAndColon(key) ? createArray() : 0;
}

inline JSONArrayWriter::JSONArrayWriter(const std::string &path) : JSONWriter(path, '[', ']') {
}

inline JSONArrayWriter::JSONArrayWriter(shared_ptr<ZLOutputStream> stream) : JSONWriter(stream, '[', ']') {
}

inline JSONArrayWriter::~JSONArrayWriter() {
}

inline void JSONArrayWriter::addElement(const std::string &value) {
	if (preAddElement()) {
		writeString(value);
	}
}

inline void JSONArrayWriter::addElement(int value) {
	if (preAddElement()) {
		writeNumber(value);
	}
}

inline shared_ptr<JSONMapWriter> JSONArrayWriter::addMap() {
	return preAddElement() ? createMap() : 0;
}

inline shared_ptr<JSONArrayWriter> JSONArrayWriter::addArray() {
	return preAddElement() ? createArray() : 0;
}

#endif /* __JSONWRITER_H__ */
