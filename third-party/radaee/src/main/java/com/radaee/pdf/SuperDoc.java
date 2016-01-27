package com.radaee.pdf;

import android.graphics.Bitmap;

/**
 * managed many Documents as 1 Document object.
 * @author radaee
 */
public class SuperDoc extends Document
{
	private class DocInfo
	{
		String path;
		String password;
		Document doc;
		int page_start;
		int page_count;
	}
	private DocInfo m_docs[] = null;
	/**
	 * Initialize SuperDoc object
	 * @param paths all full path list, it is better: length of list does not exceed 2048
	 * @param passwords password list, passwords[index] is used for paths[index].<br>
	 * 	this can be null, if all documents has no password.
	 */
	public SuperDoc(String paths[], String passwords[])
	{
		if(paths != null)
		{
			int cnt = paths.length;
			m_docs = new DocInfo[cnt];
			int cur = 0;
			int page_cnt = 0;
			while( cur < cnt )
			{
				DocInfo di = new DocInfo();
				di.path = paths[cur];
				if(passwords != null)
				{
					if( passwords.length > cur )
						di.password = passwords[cur];
				}
				di.doc = new Document();
				di.page_start = page_cnt;
				di.doc.Open(di.path, di.password);
				di.page_count = di.doc.GetPageCount();//if open failed, return 0.
				m_docs[cur] = di;
				page_cnt += di.page_count;
				cur++;
			}
		}
	}
	private final int lookup_doc(int pageno)
	{
		int left = 0;
		int right = m_docs.length - 1;
		while(left <= right)
		{
			int mid = (left + right)>>1;
			if(pageno >= m_docs[mid].page_start && pageno < m_docs[mid].page_start + m_docs[mid].page_count)
			{
				while(m_docs[mid].page_count == 0 && mid < m_docs.length)//skip all invalid Documents.
					mid++;
				if(mid >= m_docs.length) return -1;
				return mid;
			}
			else if( pageno < m_docs[mid].page_start )
				right = mid - 1;
			else
				left = mid + 1;
		}
		return -1;
	}
	/**
	 * check if opened.
	 * @return true or false.
	 */
	public boolean IsOpened()
	{
		return (m_docs != null);
	}
	/**
	 * create a empty PDF document
	 * @param path path to create
	 * @return 0 or less than 0 means failed, same as Open.
	 */
	public int Create( String path )
	{
		return -3;
	}
	public int CreateForStream( PDFStream stream )
	{
		return -3;
	}
	/**
	 * set cache file to PDF.<br/>
	 * a premium license is needed for this method.
	 * @param path a path to save some temporary data, compressed images and so on
	 * @return true or false
	 */
	public boolean SetCache( String path )
	{
		return false;
	}
	/**
	 * set font delegate to PDF.<br/>
	 * a professional or premium license is needed for this method.
	 * @param del delegate for font mapping, or null to remove delegate.
	 */
	public void SetFontDel( PDFFontDelegate del )
	{
	}
	
	/**
	 * open document.<br/>
	 * first time, SDK try password as user password, and then try password as owner password.
	 * @param path PDF file to be open.
	 * @param password password or null.
	 * @return error code:<br/>
	 * 0:succeeded, and continue<br/>
	 * -1:need input password<br/>
	 * -2:unknown encryption<br/>
	 * -3:damaged or invalid format<br/>
	 * -10:access denied or invalid file path<br/>
	 * others:unknown error
	 */
	public int Open( String path, String password )
	{
		return -3;
	}
	/**
	 * open document in memory.
	 * first time, SDK try password as user password, and then try password as owner password.
	 * @param data data for whole PDF file in byte array. developers should retain array data, till document closed.
	 * @param password password or null.
	 * @return error code:<br/>
	 * 0:succeeded, and continue<br/>
	 * -1:need input password<br/>
	 * -2:unknown encryption<br/>
	 * -3:damaged or invalid format<br/>
	 * -10:access denied or invalid file path<br/>
	 * others:unknown error
	 */
	public int OpenMem( byte[] data, String password )
	{
		return -3;
	}
	/**
	 * open document from stream.
	 * first time, SDK try password as user password, and then try password as owner password.
	 * @param stream PDFStream object.
	 * @param password password or null.
	 * @return error code:<br/>
	 * 0:succeeded, and continue<br/>
	 * -1:need input password<br/>
	 * -2:unknown encryption<br/>
	 * -3:damaged or invalid format<br/>
	 * -10:access denied or invalid file path<br/>
	 * others:unknown error
	 */
	public int OpenStream( PDFStream stream, String password )
	{
		return -3;
	}
	/**
	 * get permission of PDF, this value defined in PDF reference 1.7<br/>
	 * mostly, it means the permission from encryption.<br/>
	 * this method need a professional or premium license.
	 * bit 1-2 reserved<br/>
	 * bit 3(0x4) print<br/>
	 * bit 4(0x8) modify<br/>
	 * bit 5(0x10) extract text or image<br/>
	 * others: see PDF reference
	 * @return permission flags
	 */
	public int GetPermission()
	{
		return 0;
	}
	/**
	 * get permission of PDF, this value defined in "Perm" entry in Catalog object.<br/>
	 * mostly, it means the permission from signature.<br/>
	 * this method need a professional or premium license.
	 * @return 0 means not defined<br/>
	 * 1 means can't modify<br/>
	 * 2 means can modify some form fields<br/>
	 * 3 means can do any modify<br/>
	 */
	public int GetPerm()
	{
		return 0;
	}
	/**
	 * export form data as xml string.<br/>
	 * this method need premium license.
	 * @return xml string or null.
	 */
	public String ExportForm()
	{
		return null;
	}
	/**
	 * close the document.
	 */
	public void Close()
	{
        if(m_docs != null) {
            int cur = 0;
            int cnt = m_docs.length;
            while (cur < cnt) {
                m_docs[cur].doc.Close();
                cur++;
            }
            m_docs = null;
        }
	}
	/**
	 * get a Page object for page NO.
	 * @param pageno 0 based page NO. range:[0, GetPageCount()-1]
	 * @return Page object
	 */
	public Page GetPage( int pageno )
	{
		if(m_docs == null) return null;
		int index = lookup_doc(pageno);
		if( index < 0 ) return null;
		return m_docs[index].doc.GetPage(pageno - m_docs[index].page_start);
	}
	/**
	 * get pages count.
	 * @return pages count.
	 */
	public int GetPageCount()
	{
		if(m_docs == null) return 0;
		int index = m_docs.length - 1;
		return m_docs[index].page_start + m_docs[index].page_count;
	}
	/**
	 * get page width by page NO.
	 * @param pageno 0 based page NO. range:[0, GetPageCount()-1]
	 * @return width value.
	 */
	public float GetPageWidth( int pageno )
	{
		if(m_docs == null) return 0;
		int index = lookup_doc(pageno);
		if( index < 0 ) return 0;
		return m_docs[index].doc.GetPageWidth(pageno - m_docs[index].page_start);
	}
	/**
	 * get page height by page NO.
	 * @param pageno 0 based page NO. range:[0, GetPageCount()-1]
	 * @return height value.
	 */
	public float GetPageHeight( int pageno )
	{
		if(m_docs == null) return 0;
		int index = lookup_doc(pageno);
		if( index < 0 ) return 0;
		return m_docs[index].doc.GetPageHeight(pageno - m_docs[index].page_start);
	}
	/**
	 * get meta data of document.
	 * @param tag Predefined values:"Title", "Author", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate".<br/>or you can pass any key that self-defined.
	 * @return Meta string value, or null.
	 */
	public String GetMeta( String tag )
	{
		return null;
	}
	/**
	 * get id of document.
	 * @param index must 0 or 1, 0 means first 16 bytes, 1 means last 16 bytes.
	 * @return bytes or null if no id for this document.
	 */
	public byte[] GetID(int index)
	{
		return null;
	}
	/**
	 * set meta data for document.<br/>
	 * this method valid only in premium version.
	 * @param tag Predefined values:"Title", "Author", "Subject", "Keywords", "Creator", "Producer", "CreationDate", "ModDate".<br/>or you can pass any key that self-defined.
	 * @param val string value.
	 * @return true or false.
	 */
	public boolean SetMeta( String tag, String val )
	{
		return false;
	}
	/**
	 * get first root outline item.
	 * @return handle value of first root outline item. or null if no outlines.<br/>
	 */
	public Outline GetOutlines()
	{
		return null;
	}
	/**
	 * check if document can be modified or saved.<br/>
	 * this always return false, if no license actived.
	 * @return true or false.
	 */
	public boolean CanSave()
	{
		return false;
	}
	/**
	 * save the document.<br/>
	 * this always return false, if no license actived.
	 * @return true or false
	 */
	public boolean Save()
	{
		return false;
	}
	/**
	 * save as the document to another file.<br/>
	 * this method need professional or premium license.
	 * @param path path to save.
	 * @param rem_sec remove security info?
	 * @return true or false.
	 */
	public boolean SaveAs( String path, boolean rem_sec )
	{
		return false;
	}
	/**
	 * encrypt document and save as the document to another file.<br/>
	 * this method need premium license.
	 * @param dst path to save， same as path parameter of SaveAs.
	 * @param upswd user password, can be null.
	 * @param opswd owner password, can be null.
	 * @param perm permission to set, same as GetPermission() method.<br/>
	 * bit 1-2 reserved<br/>
	 * bit 3(0x4) print<br/>
	 * bit 4(0x8) modify<br/>
	 * bit 5(0x10) extract text or image<br/>
	 * others: see PDF reference
	 * @param method reserved, currently only AES with V=4 and R=4 mode can be working.
	 * @param id must be 32 bytes for file ID. it is divided to 2 array in native library, as each 16 bytes.
	 * @return true or false. 
	 */
	public boolean EncryptAs( String dst, String upswd, String opswd, int perm, int method, byte[] id)
	{
		return false;
	}
	/**
	 * check if document is encrypted.
	 * @return true or false.
	 */
	public boolean IsEncrypted()
	{
		return false;
	}
	/**
	 * new a root outline to document, it insert first root outline to Document.<br/>
	 * the old first root outline, shall be next of this outline. 
	 * @param label label to display
	 * @param pageno pageno to jump
	 * @param top y position in PDF coordinate
	 * @return true or false
	 */
	public boolean NewRootOutline( String label, int pageno, float top )
	{
		return false;
	}
	/**
	 * Start import operations, import page from src<br/>
	 * a premium license is needed for this method.<br/>
	 * you shall maintenance the source Document object until all pages are imported and ImportContext.Destroy() invoked. 
	 * @param src source Document object that opened.
	 * @return a context object used in ImportPage. 
	 */
	public ImportContext ImportStart( Document src )
	{
		return null;
	}
	/**
	 * import a page to the document.<br/>
	 * a premium license is needed for this method.<br/>
	 * do not forget to invoke ImportContext.Destroy() after all pages are imported.
	 * @param ctx context object created from ImportStart
	 * @param srcno 0 based page NO. from source Document that passed to ImportStart.
	 * @param dstno 0 based page NO. to insert in this document object.
	 * @return true or false.
	 */
	public boolean ImportPage( ImportContext ctx, int srcno, int dstno )
	{
		return false;
	}
	/**
	 * insert a page to Document<br/>
	 * if pagheno >= page_count, it do same as append.<br/>
	 * otherwise, insert to pageno.<br/>
	 * a premium license is needed for this method.
	 * @param pageno 0 based page NO.
	 * @param w page width in PDF coordinate
	 * @param h page height in PDF coordinate
	 * @return Page object or null means failed.
	 */
	public Page NewPage( int pageno, float w, float h )
	{
		return null;
	}
	/**
	 * remove page by page NO.<br/>
	 * a premium license is needed for this method.
	 * @param pageno 0 based page NO.
	 * @return true or false
	 */
	public boolean RemovePage( int pageno )
	{
		return false;
	}
	/**
	 * move the page to other position.<br/>
	 * a premium license is needed for this method.
	 * @param pageno1 page NO, move from
	 * @param pageno2 page NO, move to
	 * @return true or false
	 */
	public boolean MovePage( int pageno1, int pageno2 )
	{
		return false;
	}
	/**
	 * create a font object, used to write texts.<br/>
	 * a premium license is needed for this method.
	 * @param font_name <br/>
	 * font name exists in font list.<br/>
	 * using Global.getFaceCount(), Global.getFaceName() to enumerate fonts.
	 * @param style <br/>
	 *   (style&1) means bold,<br/>
	 *   (style&2) means Italic,<br/>
	 *   (style&8) means embed,<br/>
	 *   (style&16) means vertical writing, mostly used in Asia fonts.
	 * @return DocFont object or null is failed.
	 */
	public DocFont NewFontCID( String font_name, int style )
	{
		return null;
	}
	/**
	 * create a ExtGraphicState object, used to set alpha values.<br/>
	 * a premium license is needed for this method.
	 * @return DocGState object or null.
	 */
	public DocGState NewGState()
	{
		return null;
	}
	/**
	 * create an image from Bitmap object.<br/>
	 * a premium license is needed for this method.
	 * @param bmp Bitmap object in ARGB_8888 format.
	 * @param has_alpha generate alpha channel information?
	 * @return DocImage object or null.
	 */
	public DocImage NewImage( Bitmap bmp, boolean has_alpha )
	{
		return null;
	}
	/**
	 * create an image from JPEG/JPG file.<br/>
	 * supported image color space:<br/>
	 * --GRAY<br/>
	 * --RGB<br/>
	 * --CMYK<br/>
	 * a premium license is needed for this method.
	 * @param path path to JPEG file.
	 * @return DocImage object or null.
	 */
	public DocImage NewImageJPEG( String path )
	{
		return null;
	}
	/**
	 * create an image from JPX/JPEG 2k file.<br/>
	 * a premium license is needed for this method.
	 * @param path path to JPX file.
	 * @return DocImage object or null.
	 */
	public DocImage NewImageJPX( String path )
	{
		return null;
	}
	/**
	 * change page rect.<br/>
	 * a premium license is needed for this method.
	 * @param pageno 0 based page NO.
	 * @param dl delta to left, page_left += dl;
	 * @param dt delta to top, page_top += dt;
	 * @param dr delta to right, page_right += dr;
	 * @param db delta to bottom, page_bottom += db;
	 * @return true or false.
	 */
	public boolean ChangePageRect( int pageno, float dl, float dt, float dr, float db )
	{
		return false;
	}
	/**
	 * set page rotate.<br/>
	 * a premium license is needed for this method.
	 * @param pageno 0 based page NO.
	 * @param degree rotate angle in degree, must be 90 * n.
	 * @return true or false
	 */
	public boolean SetPageRotate( int pageno, int degree )
	{
		return false;
	}

	/**
	 * get signature contents. mostly an encrypted digest.<br/>
	 * this method valid in professional or premium version.<br/>
	 * @return byte array which format depends on Filter and SubFilter.<br/>
	 * or null, if not signed for document.
	 */
	public byte[] GetSignContents()
	{
		return null;
	}
	/**
	 * get signature filter name.<br/>
	 * this method valid in professional or premium version.<br/>
	 * @return The name of the preferred signature handler to use.<br/>
	 * Example signature handlers are "Adobe.PPKLite", "Entrust.PPKEF", "CICI.SignIt", and "VeriSign.PPKVS".<br/>
	 * others maybe user defined.
	 */
	public String GetSignFilter()
	{
		return null;
	}
	/**
	 * get sub filter name of signature.<br/>
	 * this method valid in professional or premium version.<br/>
	 * @return name that describes the encoding of the signature value and key information in the signature dictionary.<br/>
	 * like "adbe.x509.rsa_sha1", "adbe.pkcs7.detached", and "adbe.pkcs7.sha1"<br/>
	 * others maybe user defined.
	 */
	public String GetSignSubFilter()
	{
		return null;
	}
	/**
	 * get byte ranges from PDF file, to get digest.<br/>
	 * this method valid in professional or premium version.<br/>
	 * @return an integer pair array, to record byte ranges.<br/>
	 * each pair describing a range to digest.<br/>
	 * 1st element of pair is offset.<br/>
	 * 2nd element of pair is length.
	 */
	public int[] GetSignByteRange()
	{
		return null;
	}
	/**
	 * check object defined in signature("Data" entry), is in byte ranges defined in signature.
	 * this method valid in professional or premium version.<br/>
	 * to ensure PDF file modified, mostly you shall(Adobe Standard):<br/>
	 * 1. invoke this method first.<br/>
	 * 2. if succeeded, then get signature contents(see GetSignContents).<br/>
	 * 3. decode public key from contents(see GetSignContents).<br/>
	 * 4. decode encrypted digest from contents.<br/>
	 * 5. decrypt digest.1 using public key, for step 4.<br/>
	 * 6. calculate digest.2 by yourself, using byte ranges(GetSignByteRange).<br/>
	 * 7. check digest.1 == digest.2
	 * @return <br/>
	 * -1: unknown or not defined in signature.<br/>
	 *  0: check failed, means modified.<br/>
	 *  1: check succeeded, means no new objects after signature.
	 */
	public int CheckSignByteRange()
	{
		return -1;
	}

    public float[] GetPagesMaxSize()
    {
        if(m_docs == null) return null;
        float [] max = m_docs[0].doc.GetPagesMaxSize();
        int cur = 1;
        int cnt = m_docs.length;
        while(cur < cnt)
        {
            float[] cs1 = m_docs[cur].doc.GetPagesMaxSize();
            if(max[0] < cs1[0]) max[0] = cs1[0];
            if(max[1] < cs1[1]) max[1] = cs1[1];
            cur++;
        }
        return max;
    }
    @Override
    protected void finalize() throws Throwable
    {
        Close();
        super.finalize();
    }
}
