LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

EXPAT_DIR                     := expat-2.0.1

LOCAL_MODULE                  := expat
LOCAL_SRC_FILES               := $(EXPAT_DIR)/lib/xmlparse.c $(EXPAT_DIR)/lib/xmlrole.c $(EXPAT_DIR)/lib/xmltok.c
LOCAL_CFLAGS                  := -DHAVE_EXPAT_CONFIG_H
LOCAL_C_INCLUDES              := $(LOCAL_PATH)/$(EXPAT_DIR)
LOCAL_EXPORT_C_INCLUDES       := $(LOCAL_PATH)/$(EXPAT_DIR)/lib

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE                  := NativeFormats-v4
LOCAL_CFLAGS                  := -Wall
LOCAL_LDLIBS                  := -lz -llog
LOCAL_STATIC_LIBRARIES        := expat

LOCAL_SRC_FILES               := \
	NativeFormats/android/main.cpp \
	NativeFormats/android/JavaNativeFormatPlugin.cpp \
	NativeFormats/android/JavaPluginCollection.cpp \
	NativeFormats/android/AndroidUtil.cpp \
	NativeFormats/android/JniEnvelope.cpp \
	NativeFormats/common/zlibrary/core/constants/ZLXMLNamespace.cpp \
	NativeFormats/common/zlibrary/core/drm/FileEncryptionInfo.cpp \
	NativeFormats/common/zlibrary/core/encoding/DummyEncodingConverter.cpp \
	NativeFormats/common/zlibrary/core/encoding/Utf16EncodingConverters.cpp \
	NativeFormats/common/zlibrary/core/encoding/Utf8EncodingConverter.cpp \
	NativeFormats/android/zlibrary/core/encoding/JavaEncodingConverter.cpp \
	NativeFormats/common/zlibrary/core/encoding/ZLEncodingCollection.cpp \
	NativeFormats/common/zlibrary/core/encoding/ZLEncodingConverter.cpp \
	NativeFormats/common/zlibrary/core/filesystem/ZLDir.cpp \
	NativeFormats/common/zlibrary/core/filesystem/ZLFSManager.cpp \
	NativeFormats/common/zlibrary/core/filesystem/ZLFile.cpp \
	NativeFormats/common/zlibrary/core/filesystem/ZLInputStreamDecorator.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLGzipInputStream.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLZDecompressor.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLZipDir.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLZipEntryCache.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLZipHeader.cpp \
	NativeFormats/common/zlibrary/core/filesystem/zip/ZLZipInputStream.cpp \
	NativeFormats/common/zlibrary/core/language/ZLCharSequence.cpp \
	NativeFormats/common/zlibrary/core/language/ZLLanguageDetector.cpp \
	NativeFormats/common/zlibrary/core/language/ZLLanguageList.cpp \
	NativeFormats/common/zlibrary/core/language/ZLLanguageMatcher.cpp \
	NativeFormats/common/zlibrary/core/language/ZLStatistics.cpp \
	NativeFormats/common/zlibrary/core/language/ZLStatisticsGenerator.cpp \
	NativeFormats/common/zlibrary/core/language/ZLStatisticsItem.cpp \
	NativeFormats/common/zlibrary/core/language/ZLStatisticsXMLReader.cpp \
	NativeFormats/common/zlibrary/core/library/ZLibrary.cpp \
	NativeFormats/common/zlibrary/core/logger/ZLLogger.cpp \
	NativeFormats/android/zlibrary/core/logger/ZLLogger.cpp \
	NativeFormats/common/zlibrary/core/util/ZLFileUtil.cpp \
	NativeFormats/common/zlibrary/core/util/ZLLanguageUtil.cpp \
	NativeFormats/common/zlibrary/core/util/ZLStringUtil.cpp \
	NativeFormats/common/zlibrary/core/util/ZLUnicodeUtil.cpp \
	NativeFormats/android/zlibrary/core/util/ZLUnicodeUtil.cpp \
	NativeFormats/common/zlibrary/core/util/JSONWriter.cpp \
	NativeFormats/common/zlibrary/core/xml/ZLAsynchronousInputStream.cpp \
	NativeFormats/common/zlibrary/core/xml/ZLPlainAsynchronousInputStream.cpp \
	NativeFormats/common/zlibrary/core/xml/ZLXMLReader.cpp \
	NativeFormats/common/zlibrary/core/xml/expat/ZLXMLReaderInternal.cpp \
	NativeFormats/common/zlibrary/core/unix/filesystem/ZLUnixFSDir.cpp \
	NativeFormats/common/zlibrary/core/unix/filesystem/ZLUnixFSManager.cpp \
	NativeFormats/common/zlibrary/core/unix/filesystem/ZLUnixFileInputStream.cpp \
	NativeFormats/common/zlibrary/core/unix/filesystem/ZLUnixFileOutputStream.cpp \
	NativeFormats/common/zlibrary/core/unix/library/ZLUnixLibrary.cpp \
	NativeFormats/common/zlibrary/text/model/ZLCachedMemoryAllocator.cpp \
	NativeFormats/common/zlibrary/text/model/ZLTextModel.cpp \
	NativeFormats/common/zlibrary/text/model/ZLTextParagraph.cpp \
	NativeFormats/common/zlibrary/text/model/ZLTextStyleEntry.cpp \
	NativeFormats/common/zlibrary/text/model/ZLVideoEntry.cpp \
	NativeFormats/common/zlibrary/text/fonts/FontManager.cpp \
	NativeFormats/common/zlibrary/text/fonts/FontMap.cpp \
	NativeFormats/android/zlibrary/core/filesystem/JavaFSDir.cpp \
	NativeFormats/android/zlibrary/core/filesystem/JavaInputStream.cpp \
	NativeFormats/android/zlibrary/core/filesystem/ZLAndroidFSManager.cpp \
	NativeFormats/android/zlibrary/core/library/ZLAndroidLibraryImplementation.cpp \
	NativeFormats/common/fbreader/bookmodel/BookModel.cpp \
	NativeFormats/common/fbreader/bookmodel/BookReader.cpp \
	NativeFormats/common/fbreader/formats/EncodedTextReader.cpp \
	NativeFormats/common/fbreader/formats/FormatPlugin.cpp \
	NativeFormats/common/fbreader/formats/PluginCollection.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2BookReader.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2CoverReader.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2MetaInfoReader.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2Plugin.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2Reader.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2TagManager.cpp \
	NativeFormats/common/fbreader/formats/fb2/FB2UidReader.cpp \
	NativeFormats/common/fbreader/formats/css/CSSInputStream.cpp \
	NativeFormats/common/fbreader/formats/css/CSSSelector.cpp \
	NativeFormats/common/fbreader/formats/css/StringInputStream.cpp \
	NativeFormats/common/fbreader/formats/css/StyleSheetParser.cpp \
	NativeFormats/common/fbreader/formats/css/StyleSheetTable.cpp \
	NativeFormats/common/fbreader/formats/css/StyleSheetUtil.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlBookReader.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlDescriptionReader.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlEntityCollection.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlPlugin.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlReader.cpp \
	NativeFormats/common/fbreader/formats/html/HtmlReaderStream.cpp \
	NativeFormats/common/fbreader/formats/oeb/NCXReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBBookReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBCoverReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBEncryptionReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBMetaInfoReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBPlugin.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBSimpleIdReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBTextStream.cpp \
	NativeFormats/common/fbreader/formats/oeb/OEBUidReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/OPFReader.cpp \
	NativeFormats/common/fbreader/formats/oeb/XHTMLImageFinder.cpp \
	NativeFormats/common/fbreader/formats/pdb/BitReader.cpp \
	NativeFormats/common/fbreader/formats/pdb/DocDecompressor.cpp \
	NativeFormats/common/fbreader/formats/pdb/HtmlMetainfoReader.cpp \
	NativeFormats/common/fbreader/formats/pdb/HuffDecompressor.cpp \
	NativeFormats/common/fbreader/formats/pdb/MobipocketHtmlBookReader.cpp \
	NativeFormats/common/fbreader/formats/pdb/MobipocketPlugin.cpp \
	NativeFormats/common/fbreader/formats/pdb/PalmDocLikePlugin.cpp \
	NativeFormats/common/fbreader/formats/pdb/PalmDocLikeStream.cpp \
	NativeFormats/common/fbreader/formats/pdb/PalmDocStream.cpp \
	NativeFormats/common/fbreader/formats/pdb/PdbPlugin.cpp \
	NativeFormats/common/fbreader/formats/pdb/PdbReader.cpp \
	NativeFormats/common/fbreader/formats/pdb/PdbStream.cpp \
	NativeFormats/common/fbreader/formats/pdb/SimplePdbPlugin.cpp \
	NativeFormats/common/fbreader/formats/rtf/RtfBookReader.cpp \
	NativeFormats/common/fbreader/formats/rtf/RtfDescriptionReader.cpp \
	NativeFormats/common/fbreader/formats/rtf/RtfPlugin.cpp \
	NativeFormats/common/fbreader/formats/rtf/RtfReader.cpp \
	NativeFormats/common/fbreader/formats/rtf/RtfReaderStream.cpp \
	NativeFormats/common/fbreader/formats/txt/PlainTextFormat.cpp \
	NativeFormats/common/fbreader/formats/txt/TxtBookReader.cpp \
	NativeFormats/common/fbreader/formats/txt/TxtPlugin.cpp \
	NativeFormats/common/fbreader/formats/txt/TxtReader.cpp \
	NativeFormats/common/fbreader/formats/util/EntityFilesCollector.cpp \
	NativeFormats/common/fbreader/formats/util/MergedStream.cpp \
	NativeFormats/common/fbreader/formats/util/MiscUtil.cpp \
	NativeFormats/common/fbreader/formats/util/XMLTextStream.cpp \
	NativeFormats/common/fbreader/formats/xhtml/XHTMLReader.cpp \
	NativeFormats/common/fbreader/formats/xhtml/XHTMLTagInfo.cpp \
	NativeFormats/common/fbreader/formats/doc/DocBookReader.cpp \
	NativeFormats/common/fbreader/formats/doc/DocMetaInfoReader.cpp \
	NativeFormats/common/fbreader/formats/doc/DocPlugin.cpp \
	NativeFormats/common/fbreader/formats/doc/DocStreams.cpp \
	NativeFormats/common/fbreader/formats/doc/OleMainStream.cpp \
	NativeFormats/common/fbreader/formats/doc/OleStorage.cpp \
	NativeFormats/common/fbreader/formats/doc/OleStream.cpp \
	NativeFormats/common/fbreader/formats/doc/OleStreamParser.cpp \
	NativeFormats/common/fbreader/formats/doc/OleStreamReader.cpp \
	NativeFormats/common/fbreader/formats/doc/OleUtil.cpp \
	NativeFormats/common/fbreader/formats/doc/DocInlineImageReader.cpp \
	NativeFormats/common/fbreader/formats/doc/DocFloatImageReader.cpp \
	NativeFormats/common/fbreader/formats/doc/DocAnsiConverter.cpp \
	NativeFormats/common/fbreader/library/Author.cpp \
	NativeFormats/common/fbreader/library/Book.cpp \
	NativeFormats/common/fbreader/library/Comparators.cpp \
	NativeFormats/common/fbreader/library/Tag.cpp \
	NativeFormats/common/fbreader/library/UID.cpp

LOCAL_C_INCLUDES              := \
	$(LOCAL_PATH)/NativeFormats/android \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/constants \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/drm \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/encoding \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/filesystem \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/image \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/language \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/library \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/logger \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/util \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/core/xml \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/text/model \
	$(LOCAL_PATH)/NativeFormats/common/zlibrary/text/fonts

include $(BUILD_SHARED_LIBRARY)
