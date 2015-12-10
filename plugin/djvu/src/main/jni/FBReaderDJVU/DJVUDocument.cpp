#include "../DjVuLibre/ddjvuapi.h"
#include "../DjVuLibre/miniexp.h"
#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <sstream>
#include <string>
#include <vector>
#include <map>

#include <unistd.h>


#define SSTR( x ) dynamic_cast< std::ostringstream & >( \
	( std::ostringstream() << std::dec << x ) ).str()

static ddjvu_context_t *context = NULL;

static std::map<int, ddjvu_document_t *> ourDocs;

static ddjvu_document_t * getDoc(int id) {
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "getdoc");
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "%s", SSTR(id).c_str());
	std::map<int, ddjvu_document_t *>::iterator it = ourDocs.find(id);
	if (it != ourDocs.end()) {
		__android_log_print(ANDROID_LOG_ERROR, "DJVU", "got");
		return it->second;
	} else {
		__android_log_print(ANDROID_LOG_ERROR, "DJVU", "not");
		return 0;
	}
}

static int saveDoc(ddjvu_document_t *doc) {
	int i = 1;
	while (true) {
		std::map<int, ddjvu_document_t *>::iterator it = ourDocs.find(i);
		if (it == ourDocs.end()) {
			ourDocs[i] = doc;
			__android_log_print(ANDROID_LOG_ERROR, "DJVU", "savedoc");
			__android_log_print(ANDROID_LOG_ERROR, "DJVU", "%s", SSTR(i).c_str());
			return i;
		}
		++i;
	}
}

static void deleteDoc(int id) {
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "deletedoc");
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "%s", SSTR(id).c_str());
	ddjvu_document_t *doc = getDoc(id);
	if (doc) {
		ddjvu_document_release(doc);
	}
	ourDocs.erase(id);
}

struct outline {
	int page;
	std::string title;
	outline * next;
	outline * child;
	~outline() {
		delete next;
		delete child;
	}
};

struct word {
	std::string text;
	int x1;
	int y1;
	int x2;
	int y2;
};

static std::vector<word> page_text;

extern "C"
JNIEXPORT void Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_initNative(JNIEnv *env, jclass cl) {
	if (context) {
		return;
	}
	context = ddjvu_context_create("FBReaderDJVU");
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "Init");
}

extern "C"
JNIEXPORT void Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_destroyNative(JNIEnv *env, jclass cl) {// Currently never used (?)
//	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "Destroy");
//	if (context) {
//		if (doc) {
//			ddjvu_document_release(doc);
//			doc = NULL;
//			if (root) {
//				delete root;
//			}
//		}
//		ddjvu_context_release(context);
//	}
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_createPageNative(JNIEnv *env, jclass cl, jint doc_id, jint pageNum) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (!doc) {
		return 0;
	}

	if (pageNum < 0) {
		return 0;
	}
	ddjvu_page_t* page = ddjvu_page_create_by_pageno(doc, pageNum);
	int count = 0;
	while (!ddjvu_page_decoding_done(page)) {
		if (++count > 400) {
			ddjvu_page_release(page);
			return 0;
		}
		usleep(50000);
	}
	return (long)page;
}

extern "C"
JNIEXPORT void Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_freePageNative(JNIEnv *env, jclass cl, jlong p) {
	if (p == 0) {
		return;
	}
	ddjvu_page_t* page = (ddjvu_page_t*)(long)p;
	ddjvu_page_release(page);
}

static std::string get_miniexp_str(miniexp_t t, std::string indent) {
	if (miniexp_symbolp(t)) {
		return std::string("sym:") + miniexp_to_name(t);
	}
	if (miniexp_stringp(t)) {
		return std::string("str:") + miniexp_to_str(t);
	}
	if (miniexp_numberp(t)) {
		return std::string("num:") + SSTR( miniexp_to_int(t) );
	}
	if (miniexp_consp(t)) {
		return std::string("con:(\n") + indent + get_miniexp_str(miniexp_car(t), indent + " ") + ",\n" + indent + get_miniexp_str(miniexp_cdr(t), indent + " ") + "\n" + indent + ")";
	}
	return "NULL";
}

extern "C"
JNIEXPORT jint Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_openDocumentNative(JNIEnv *env, jobject thiz, jstring path) {
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "Open");

	const char *fileName = env->GetStringUTFChars(path, 0);
	ddjvu_document_t *doc = ddjvu_document_create_by_filename_utf8(context, fileName, 0);
	int count = 0;
	while (!ddjvu_document_decoding_done(doc)) {
		if (++count > 400) {
			ddjvu_document_release(doc);
			doc = 0;
			break;
		}
		usleep(50000);
	}
	if (doc) {
		return saveDoc(doc);
	}
	return 0;
}

extern "C"
JNIEXPORT void Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_closeNative(JNIEnv *env, jobject thiz, jint doc_id) {
	deleteDoc(doc_id);
	__android_log_print(ANDROID_LOG_ERROR, "DJVU", "Close");
}

extern "C"
JNIEXPORT jint Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getPageCountNative(JNIEnv *env, jobject thiz, jint doc_id) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc) {
		return ddjvu_document_get_pagenum(doc);
	}
	return 0;
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getPageSizeNative(JNIEnv *env, jobject thiz, jint doc_id, jint pageNum) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc) {
		ddjvu_status_t r;
		ddjvu_pageinfo_t info;
		int count = 0;
		while ((r = ddjvu_document_get_pageinfo(doc, pageNum, &info)) < DDJVU_JOB_OK) {
			if (++count > 400) {
				return -1;
			}
			usleep(5000);
//			handle_ddjvu_messages(context, TRUE);
		}
//		if (r >= DDJVU_JOB_FAILED)
//			signal_error();
		jlong size = info.width;
		size <<= 32;
		size += info.height;
		return size;
	}
	return -1;
}

extern "C"
JNIEXPORT void Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_renderNative(JNIEnv *env, jclass clz, jint doc_id, jobject canvas, jint left, jint top, jint right, jint bottom, jlong ptr) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (!doc || ptr == 0) {
		return;
	}

	if (right <= left || bottom <= top) {
		return;
	}

	AndroidBitmapInfo info;
	if (AndroidBitmap_getInfo(env, canvas, &info) < 0) {
		return;
	}

	unsigned int masks[4] = { 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000 };
	ddjvu_format_t* pixelFormat = ddjvu_format_create(DDJVU_FORMAT_RGBMASK32, 4, masks);
	ddjvu_format_set_row_order(pixelFormat, 1);

	ddjvu_page_t* page = (ddjvu_page_t*)(long)ptr;
	ddjvu_rect_t pageRect;
	int pageW = ddjvu_page_get_width(page);
	int pageH = ddjvu_page_get_height(page);

	pageRect.x = 0;
	pageRect.y = 0;
	pageRect.w = pageW * info.width / (right - left);
	pageRect.h = pageH * info.height / (bottom - top);

	ddjvu_rect_t renderRect;
	renderRect.x = left * info.width / (right - left);
	renderRect.y = (pageH - top) * info.height / (bottom - top) - info.height;
	renderRect.w = info.width;
	renderRect.h = info.height;

	void *pixels;
	long num_pixels = info.width * info.height;

	if (AndroidBitmap_lockPixels(env, canvas, &pixels) < 0) {
		return;
	}

	char *buffer = &(((char*)pixels)[0]);

	ddjvu_page_render(
		page, DDJVU_RENDER_COLOR,
		&pageRect, &renderRect,
		pixelFormat, info.width * 4,
		buffer
	);

	AndroidBitmap_unlockPixels(env, canvas);
	ddjvu_format_release(pixelFormat);
}

static const char* get_outline_title(miniexp_t mcur) {
	if (
		miniexp_consp(mcur)
		&& miniexp_consp(miniexp_car(mcur))
		&& miniexp_stringp(miniexp_car(miniexp_car(mcur)))
	) {
		return miniexp_to_str(miniexp_car(miniexp_car(mcur)));
	}
	return "";
}

static int get_outline_page(miniexp_t mcur) {
	if (
		miniexp_consp(mcur)
		&& miniexp_consp(miniexp_car(mcur))
		&& miniexp_consp(miniexp_cdr(miniexp_car(mcur)))
		&& miniexp_stringp(miniexp_car(miniexp_cdr(miniexp_car(mcur))))
	) {
		const char* res = miniexp_to_str(miniexp_car(miniexp_cdr(miniexp_car(mcur))));
		int p = atoi(&res[1]) - 1;
		return p;
	}
	return -1;
}

miniexp_t get_outline_child(miniexp_t mcur) {
	if (
		miniexp_consp(mcur)
		&& miniexp_consp(miniexp_car(mcur))
		&& miniexp_consp(miniexp_cdr(miniexp_car(mcur)))
		&& miniexp_consp(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_car(mcur)))))
	) {
		return miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_car(mcur)))));
	}
	return 0;
}


miniexp_t get_outline_next(miniexp_t mcur) {
	if (miniexp_consp(mcur) && miniexp_consp(miniexp_cdr(mcur))) {
		return miniexp_cdr(mcur);
	}
	return 0;
}

static outline* init_outlines_recursive(miniexp_t m) {
	int pagen = get_outline_page(m);
	if (pagen == -1) {
		return 0;
	}
	outline * temp = new outline();
	temp->title = get_outline_title(m);
	temp->page = pagen;
	temp->child = init_outlines_recursive(get_outline_child(m));
	temp->next = init_outlines_recursive(get_outline_next(m));
	return temp;
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getOutlineRootNative(JNIEnv *env, jobject thiz, jint doc_id) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc) {
		miniexp_t t;
		int count = 0;
		while ((t=ddjvu_document_get_outline(doc))==miniexp_dummy) {
			if (++count > 400) {
				return 0;
			}
			usleep(5000);
		}
		if (t == miniexp_dummy || t == NULL) {
			return 0;
		}
		if (!miniexp_consp(t) || miniexp_car(t) != miniexp_symbol("bookmarks")) {
			return 0;
		}
		miniexp_t res = miniexp_cdr(t);
		outline* root = init_outlines_recursive(res);
		ddjvu_miniexp_release(doc, t);
		return (long)(outline*)root;
	}
	return 0;
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_clearOutlineRootNative(JNIEnv *env, jobject thiz, jlong ptr) {
	outline* root = (outline*) (long)ptr;
	delete root;
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getOutlineNextNative(JNIEnv *env, jobject thiz, jlong cur) {
	outline* mcur = (outline*) (long)cur;
	return (long)(outline*)mcur->next;
}

extern "C"
JNIEXPORT jlong Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getOutlineChildNative(JNIEnv *env, jobject thiz, jlong cur) {
	outline* mcur = (outline*) (long)cur;
	return (long)(outline*)mcur->child;
}

extern "C"
JNIEXPORT jstring Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getOutlineTextNative(JNIEnv *env, jobject thiz, jlong cur) {
	outline* mcur = (outline*) (long)cur;
	return env->NewStringUTF(mcur->title.c_str());
}

extern "C"
JNIEXPORT jint Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getOutlinePageNative(JNIEnv *env, jobject thiz, jlong cur) {
	outline* mcur = (outline*) (long)cur;
	return mcur->page;
}

//TODO: improve
static void create_word_recursive(miniexp_t t) {
	if (miniexp_consp(t) && miniexp_consp(miniexp_car(t)) && miniexp_car(miniexp_car(t)) == miniexp_symbol("word")) {
		word w;
		w.x1 = miniexp_to_int(miniexp_car(miniexp_cdr(miniexp_car(t))));
		w.y1 = miniexp_to_int(miniexp_car(miniexp_cdr(miniexp_cdr(miniexp_car(t)))));
		w.x2 = miniexp_to_int(miniexp_car(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_car(t))))));
		w.y2 = miniexp_to_int(miniexp_car(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_car(t)))))));
		const char *ch = miniexp_to_str(miniexp_car(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_cdr(miniexp_car(t))))))));
		if (ch) {
			w.text = ch;
			page_text.push_back(w);
		}
		create_word_recursive(miniexp_cdr(t));
	} else {
		if (miniexp_consp(t)) {
			create_word_recursive(miniexp_car(t));
			create_word_recursive(miniexp_cdr(t));
		}
	}
}


extern "C"
JNIEXPORT jint Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_createTextNative(JNIEnv *env, jobject thiz, jint doc_id, jint pageNum) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc) {
		miniexp_t t;
		int count = 0;
		while ((t=ddjvu_document_get_pagetext(doc,pageNum,0))==miniexp_dummy) {
			if (++count > 400) {
				return 0;
			}
			usleep(5000);
		}
		if (t == miniexp_dummy || t == NULL) {
			return 0;
		}
		if (!miniexp_consp(t) || miniexp_car(t) != miniexp_symbol("page")) {
			return 0;
		}
		page_text.clear();
		create_word_recursive(t);
		ddjvu_miniexp_release(doc, t);
		return page_text.size();
	}
	return 0;
}

extern "C"
JNIEXPORT jint Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getWordCoordNative(JNIEnv *env, jobject thiz, jint doc_id, jint no, jint type) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc && no < page_text.size()) {
		word w = page_text[no];
		if (type == 0) {
			return w.x1;
		}
		if (type == 1) {
			return w.y1;
		}
		if (type == 2) {
			return w.x2;
		}
		if (type == 3) {
			return w.y2;
		}
	}
	return 0;
}

extern "C"
JNIEXPORT jstring Java_org_geometerplus_fbreader_plugin_base_document_DJVUDocument_getWordTextNative(JNIEnv *env, jobject thiz, jint doc_id, jint no) {
	ddjvu_document_t * doc = getDoc(doc_id);
	if (doc && no < page_text.size()) {
		word w = page_text[no];
		return env->NewStringUTF(w.text.c_str());
	}
	return env->NewStringUTF("");
}
