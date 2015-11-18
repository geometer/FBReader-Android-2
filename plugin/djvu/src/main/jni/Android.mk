LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := DjVuLibre
LOCAL_CFLAGS = -DTHREADMODEL=POSIXTHREADS -DHAVE_STDINT_H -DHAS_WCHAR -DHAVE_WCHAR_H -DHAVE_MBSTATE_T  -DHAS_MBSTATE
LOCAL_CPPFLAGS += -fexceptions
LOCAL_SRC_FILES := \
	FBReaderDJVU/DJVUDocument.cpp \
	DjVuLibre/Arrays.cpp \
	DjVuLibre/atomic.cpp \
	DjVuLibre/BSByteStream.cpp \
	DjVuLibre/BSEncodeByteStream.cpp \
	DjVuLibre/ByteStream.cpp \
	DjVuLibre/DataPool.cpp \
	DjVuLibre/ddjvuapi.cpp \
	DjVuLibre/debug.cpp \
	DjVuLibre/DjVmDir0.cpp \
	DjVuLibre/DjVmDir.cpp \
	DjVuLibre/DjVmDoc.cpp \
	DjVuLibre/DjVmNav.cpp \
	DjVuLibre/DjVuAnno.cpp \
	DjVuLibre/DjVuDocEditor.cpp \
	DjVuLibre/DjVuDocument.cpp \
	DjVuLibre/DjVuDumpHelper.cpp \
	DjVuLibre/DjVuErrorList.cpp \
	DjVuLibre/DjVuFileCache.cpp \
	DjVuLibre/DjVuFile.cpp \
	DjVuLibre/DjVuGlobal.cpp \
	DjVuLibre/DjVuGlobalMemory.cpp \
	DjVuLibre/DjVuImage.cpp \
	DjVuLibre/DjVuInfo.cpp \
	DjVuLibre/DjVuMessage.cpp \
	DjVuLibre/DjVuMessageLite.cpp \
	DjVuLibre/DjVuNavDir.cpp \
	DjVuLibre/DjVuPalette.cpp \
	DjVuLibre/DjVuPort.cpp \
	DjVuLibre/DjVuText.cpp \
	DjVuLibre/DjVuToPS.cpp \
	DjVuLibre/GBitmap.cpp \
	DjVuLibre/GContainer.cpp \
	DjVuLibre/GException.cpp \
	DjVuLibre/GIFFManager.cpp \
	DjVuLibre/GMapAreas.cpp \
	DjVuLibre/GOS.cpp \
	DjVuLibre/GPixmap.cpp \
	DjVuLibre/GRect.cpp \
	DjVuLibre/GScaler.cpp \
	DjVuLibre/GSmartPointer.cpp \
	DjVuLibre/GString.cpp \
	DjVuLibre/GThreads.cpp \
	DjVuLibre/GUnicode.cpp \
	DjVuLibre/GURL.cpp \
	DjVuLibre/IFFByteStream.cpp \
	DjVuLibre/IW44EncodeCodec.cpp \
	DjVuLibre/IW44Image.cpp \
	DjVuLibre/JB2EncodeCodec.cpp \
	DjVuLibre/JB2Image.cpp \
	DjVuLibre/JPEGDecoder.cpp \
	DjVuLibre/miniexp.cpp \
	DjVuLibre/MMRDecoder.cpp \
	DjVuLibre/MMX.cpp \
	DjVuLibre/UnicodeByteStream.cpp \
	DjVuLibre/XMLParser.cpp \
	DjVuLibre/XMLTags.cpp \
	DjVuLibre/ZPCodec.cpp
LOCAL_LDLIBS                  := -llog -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
