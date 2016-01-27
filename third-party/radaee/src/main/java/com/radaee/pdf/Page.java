package com.radaee.pdf;

import android.graphics.Bitmap;
import android.util.Log;

import com.radaee.pdf.Document.DocFont;
import com.radaee.pdf.Document.DocForm;
import com.radaee.pdf.Document.DocGState;
import com.radaee.pdf.Document.DocImage;
import com.radaee.pdf.adv.Ref;

/**
class for PDF Page.
@author Radaee
@version 1.1
*/
public class Page
{
	public class Annotation
	{
		protected long hand;
		protected Page page;
        /**
         * advanced function to get reference of annotation object.<br/>
         * this method need premium license.
         * @return reference
         */
        public final Ref Advance_GetRef()
        {
            if( page == null || page.hand == 0 || hand == 0 ) return null;
            long ret = advGetAnnotRef(page.hand, hand);
            if(ret == 0) return null;
            return new Ref(ret);
        }

        /**
         * advanced function to reload annotation object, after advanced methods update annotation object data.<br/>
         * this method need premium license.
         */
        public final void Advance_Reload()
        {
            if( page == null || page.hand == 0 || hand == 0 ) return;
            advReloadAnnot(page.hand, hand);
        }
		/**
		 * get this annotation index in page.
		 * @return 0 based index value or -1;
		 */
		public int GetIndexInPage()
		{
			if( page == null || page.hand == 0 || hand == 0 ) return -1;
			int index = 0;
			int cnt = Page.getAnnotCount(page.hand);
			while( index < cnt )
			{
				long hand_check = Page.getAnnot(page.hand, index);
				if( hand == hand_check ) return index;
				index++;
			}
			return -1;
		}
		/**
		 * move annotation to another page.<be/>
		 * this method valid in professional or premium version.<br/>
		 * this method just like invoke Page.CopyAnnot() and Annotation.RemoveFromPage(), but, less data generated.<br/>
		 * Notice: ObjsStart or RenderXXX shall be invoked for dst_page.
		 * @param dst_page page to move.
		 * @param rect [left, top, right, bottom] in PDF coordinate in dst_page.
		 * @return true or false.
		 */
        final public boolean MoveToPage( Page dst_page, float[] rect )
		{
			return Page.moveAnnot(page.hand, dst_page.hand, hand, rect);
		}
		/**
		 * render an annotation to Bitmap. this method fully scale annotation to bitmap object.<br/>
		 * this method valid in professional or premium version.<br/>
		 * Notice 1: the render result may not correct for some annotation that not used Alpha Color blending.<br/>
		 * example: highlight annotation may not render correctly.<br/>
		 * Notice 2: you can invoke Global.hideAnnots() in Global.Init(), and invoke this method to handle Annotations by yourself.
		 * @param bitmap Bitmap object.
		 * @return true or false.
		 */
        final public boolean RenderToBmp(Bitmap bitmap )
		{
			return Page.renderAnnotToBmp(page.hand, hand, bitmap);
		}
		/**
		 * get annotation field type in acroForm.<br/>
		 * this method valid in premium version
		 * @return type as these values:<br/>
		 * 0: unknown<br/>
		 * 1: button field<br/>
		 * 2: text field<br/>
		 * 3: choice field<br/>
		 * 4: signature field<br/>
		 */
        final public int GetFieldType()
		{
			return Page.getAnnotFieldType(page.hand, hand);
		}

        /**
         * get annotation field flag in acroForm.<br/>
         * this method valid in premium version
         * @return flag&1 : read-only<br/>
         * flag&2 : is required<br/>
         * flag&4 : no export.
         */
        final public int GetFieldFlag()
        {
            return Page.getAnnotFieldFlag(page.hand, hand);
        }
        /**
         * get name of the annotation without NO. a fields group with same name "field#0","field#1"，got to "field".<br/>
         * this method valid in premium version
         * @return null if it is not field, or name of the annotation, example: "EditBox1[0]".
         */
        final public String GetFieldName()
		{
			return Page.getAnnotFieldNameWithoutNO(page.hand, hand);
		}

        /**
         * get name of the annotation.<br/>
         * this method valid in premium version
         * @return null if it is not field, or name of the annotation, example: "EditBox1[0]".
         */
        final public String GetFieldNameWithNO()
        {
            return Page.getAnnotFieldName(page.hand, hand);
        }
		/**
		 * get name of the annotation.<br/>
		 * this method valid in premium version
		 * @return null if it is not field, or full name of the annotation, example: "Form1.EditBox1".
		 */
        final public String GetFieldFullName()
		{
			return Page.getAnnotFieldFullName(page.hand, hand);
		}
		/**
		 * get full name of the annotation with more details.<br/>
		 * this method valid in premium version
		 * @return null if it is not field, or full name of the annotation, example: "Form1[0].EditBox1[0]".
		 */
        final public String GetFieldFullName2()
		{
			return Page.getAnnotFieldFullName2(page.hand, hand);
		}
		/**
		 * get annotation type.<br/>
		 * this method valid in professional or premium version
		 * @return type as these values:<br/>
		 * 0:  unknown<br/>
		 * 1:  text<br/>
		 * 2:  link<br/>
		 * 3:  free text<br/>
		 * 4:  line<br/>
		 * 5:  square<br/>
		 * 6:  circle<br/>
		 * 7:  polygon<br/>
		 * 8:  polyline<br/>
		 * 9:  text hilight<br/>
		 * 10: text under line<br/>
		 * 11: text squiggly<br/>
		 * 12: text strikeout<br/>
		 * 13: stamp<br/>
		 * 14: caret<br/>
		 * 15: ink<br/>
		 * 16: popup<br/>
		 * 17: file attachment<br/>
		 * 18: sound<br/>
		 * 19: movie<br/>
		 * 20: widget<br/>
		 * 21: screen<br/>
		 * 22: print mark<br/>
		 * 23: trap net<br/>
		 * 24: water mark<br/>
		 * 25: 3d object<br/>
		 * 26: rich media
		 */
        final public int GetType()
		{
			return Page.getAnnotType(page.hand, hand);
		}
		/**
		 * check if position and size of the annotation is locked?<br/>
		 * this method valid in professional or premium version
		 * @return true if locked, or not locked.
		 */
        final public boolean IsLocked()
		{
			return Page.isAnnotLocked(page.hand, hand);
		}
		/**
		 * set annotation lock status.<br/>
		 * @param lock true if lock, otherwise false.
		 */
        final public void SetLocked(boolean lock)
		{
			Page.setAnnotLock(page.hand, hand, lock);
		}
		/**
		 * check if texts of the annotation is locked?<br/>
		 * this method valid in professional or premium version
		 * @return true if locked, or not locked.
		 */
        final public boolean IsLockedContent()
		{
			return Page.isAnnotLockedContent(page.hand, hand);
		}
		/**
		 * check whether the annotation is hide.
		 * @return true or false.
		 */
        final public boolean IsHide()
		{
			return Page.isAnnotHide(page.hand, hand);
		}
		/**
		 * get annotation's box rectangle.<br/>
		 * this method valid in professional or premium version
		 * @return 4 elements: left, top, right, bottom in PDF coordinate system
		 */
        final public float[] GetRect()
		{
			float rect[] = new float[4];
			Page.getAnnotRect(page.hand, hand, rect);
			return rect;
		}
		/**
		 * set annotation's box rectangle.<br/>
		 * this method valid in professional or premium version.<br/>
		 * you shall render page after this invoked, to resize or move annotation.
		 */
        final public void SetRect( float left, float top, float right, float bottom )
		{
			float rect[] = new float[4];
			rect[0] = left;
			rect[1] = top;
			rect[2] = right;
			rect[3] = bottom;
			Page.setAnnotRect(page.hand, hand, rect);
		}
		/**
		 * get markup annotation's boxes.<br/>
		 * this method valid in professional or premium version
		 * @return float array, container many boxes.<br/>
		 * each 4 elements defined a box, as [left, top, right, bottom] in PDF coordinate.<br/>
		 * length of this array must be 4 times.
		 */
        final public float[] GetMarkupRects()
		{
			return Page.getAnnotMarkupRects( page.hand, hand );
		}
		/**
		 * set hide status for annotation.
		 * this method valid in professional or premium version.<br/>
		 * you shall render page after this invoked, to hide annotation.
		 * @param hide true or false.
		 */
        final public void SetHide( boolean hide )
		{
			Page.setAnnotHide(page.hand, hand, hide);
		}

        /**
         * get popup Annotation associate to this annotation.
         * @return Popup Annotation, or null, if this annotation is Popup Annotation, then return same as this.
         */
        final public Annotation GetPopup()
        {
            long ret = Page.getAnnotPopup(page.hand, hand);
            if(ret != 0)
            {
                Annotation annot = new Annotation();
                annot.hand = ret;
                annot.page = page;
                return annot;
            }
            else
                return null;
        }

        /**
         * get open status for Popup Annotation.<br/>
         * if this annotation is not popup annotation, it return Popup annotation open status, which associate to this annotation.<br/>
         * this method valid in professional or premium version.
         * @return true or false.
         */
        final public boolean GetPopupOpen()
        {
            return Page.getAnnotPopupOpen(page.hand, hand);
        }
        /**
         * set open status for Popup Annotation.<br/>
         * if this annotation is not popup annotation, it set Popup annotation open status, which associate to this annotation.<br/>
         * this method valid in professional or premium version.
         * @return true or false.
         */
        final public boolean SetPopupOpen(boolean open)
        {
            return Page.setAnnotPopupOpen(page.hand, hand, open);
        }
        /**
         * get annotation's popup text.<br/>
         * if this annotation is popup annotation, it get parent annotation's text.<br/>
         * this method valid in professional or premium version.
         * @return text string or null if failed.
         */
        final public String GetPopupText()
        {
            return Page.getAnnotPopupText(page.hand, hand);
        }
        /**
         * set annotation's popup label, mostly it means the author.<br/>
         * if this annotation is popup annotation, it set parent annotation's text.<br/>
         * this method valid in professional or premium version
         * @param val text string
         * @return true or false
         */
        final public boolean SetPopupLabel( String val )
        {
            return Page.setAnnotPopupLabel(page.hand, hand, val);
        }
        /**
         * get annotation's popup text, mostly it means the author.<br/>
         * if this annotation is popup annotation, it get parent annotation's label.<br/>
         * this method valid in professional or premium version.
         * @return text string or null if failed.
         */
        final public String GetPopupLabel()
        {
            return Page.getAnnotPopupLabel(page.hand, hand);
        }
        /**
         * set annotation's popup text.<br/>
         * if this annotation is popup annotation, it set parent annotation's label.<br/>
         * this method valid in professional or premium version
         * @param val text string
         * @return true or false
         */
        public boolean SetPopupText( String val )
        {
            return Page.setAnnotPopupText(page.hand, hand, val);
        }
		/**
		 * get annotation's popup subject.<br/>
         * if this annotation is popup annotation, it get parent annotation's subject.<br/>
		 * this method valid in professional or premium version
		 * @return subject string or null if failed.
		 */
        final public String GetPopupSubject()
		{
			return Page.getAnnotPopupSubject(page.hand, hand);
		}
		/**
		 * set annotation's popup subject.<br/>
         * if this annotation is popup annotation, it set parent annotation's subject.<br/>
		 * this method valid in professional or premium version
		 * @param val subject string
		 * @return true or false
		 */
        final public boolean SetPopupSubject( String val )
		{
			return Page.setAnnotPopupSubject(page.hand, hand, val);
		}
		/**
		 * get annotation's destination.<br/>
		 * this method valid in professional or premium version
		 * @return 0 based page NO, or -1 if failed.
		 */
        final public int GetDest()
		{
			return Page.getAnnotDest(page.hand, hand);
		}

        /**
         * get remote link.<br/>
         * this method valid in professional or premium version
         * @return a string format as "path/pageno", example "test.pdf/3", which pageno is 0 based page NO.
         */
        final public String GetRemoteDest()
        {
            return Page.getAnnotRemoteDest(page.hand, hand);
        }
		/**
		 * get annotation's name("NM" entry).<br/>
		 * this method valid in professional or premium version
		 * @return name string.
		 */
        final public String GetName()
		{
			return Page.getAnnotName(page.hand, hand);
		}
		/**
		 * set annotation's name("NM" entry).<br/>
		 * this method valid in professional or premium version
		 * @param name name string to be set.
		 * @return true or false.
		 */
        final public boolean SetName(String name)
		{
			return Page.setAnnotName(page.hand, hand, name);
		}
		/**
		 * get annotation's URL link string.<br/>
		 * this method valid in professional or premium version
		 * @return string of URL, or null
		 */
        final public String GetURI()
		{
			return Page.getAnnotURI(page.hand, hand);
		}
		/**
		 * get annotation's java-script string.<br/>
		 * this method valid in professional or premium version
		 * @return string of java-script, or null.
		 */
        final public String GetJS()
		{
			return Page.getAnnotJS(page.hand, hand);
		}
		/**
		 * get annotation's file link path string.<br/>
		 * this method valid in professional or premium version
		 * @return string of link path, or null
		 */
        final public String GetFileLink()
		{
			return Page.getAnnotFileLink(page.hand, hand);
		}
		/**
		 * get annotation's 3D object name.<br/>
		 * this method valid in professional or premium version
		 * @return name of the 3D object, or null
		 */
        final public String Get3D()
		{
			return Page.getAnnot3D(page.hand, hand);
		}
		/**
		 * get annotation's movie name.<br/>
		 * this method valid in professional or premium version
		 * @return name of the movie, or null
		 */
        final public String GetMovie()
		{
			return Page.getAnnotMovie(page.hand, hand);
		}
		/**
		 * get annotation's sound name.<br/>
		 * this method valid in professional or premium version
		 * @return name of the audio, or null
		 */
        final public String GetSound()
		{
			return Page.getAnnotSound(page.hand, hand);
		}
		/**
		 * get annotation's attachment name.<br/>
		 * this method valid in professional or premium version
		 * @return name of the attachment, or null
		 */
        final public String GetAttachment()
		{
			return Page.getAnnotAttachment(page.hand, hand);
		}
		/**
		 * get annotation's 3D data. must be *.u3d format.<br/>
		 * this method valid in professional or premium version
		 * @param save_file full path name to save data.
		 * @return true if save_file created, or false.
		 */
        final public boolean Get3DData( String save_file )
		{
			return Page.getAnnot3DData(page.hand, hand, save_file);
		}
		/**
		 * get annotation's movie data.<br/>
		 * this method valid in professional or premium version
		 * @param save_file full path name to save data.
		 * @return true if save_file created, or false.
		 */
        final public boolean GetMovieData( String save_file )
		{
			return Page.getAnnotMovieData(page.hand, hand, save_file);
		}
		/**
		 * get annotation's sound data.<br/>
		 * this method valid in professional or premium version
		 * @param paras paras[0] == 0, if formated audio file(*.mp3 ...).
		 * @param save_file full path name to save data.
		 * @return true if save_file created, or false.
		 */
        final public boolean GetSoundData( int paras[], String save_file )
		{
			return Page.getAnnotSoundData(page.hand, hand, paras, save_file);
		}
		/**
		 * get annotation's attachment data.<br/>
		 * this method valid in professional or premium version
		 * @param save_file full path name to save data.
		 * @return true if save_file created, or false.
		 */
        final public boolean GetAttachmentData( String save_file )
		{
			return Page.getAnnotAttachmentData(page.hand, hand, save_file);
		}
		/**
		 * get type of edit-box.<br/>
		 * this method valid in premium version
		 * @return <br/>-1: this annotation is not text-box.<br/> 1: normal single line.<br/>2: password.<br/>3: MultiLine edit area.
		 */
        final public int GetEditType()
		{
			return Page.getAnnotEditType(page.hand, hand);
		}
		/**
		 * get max-len of edit-box.<br/>
		 * this method valid in premium version
		 * @return 0 if no limit, great than 0 if has limit.
		 */
        final public int GetEditMaxlen()
		{
			return Page.getAnnotEditMaxlen(page.hand, hand);
		}
		/**
		 * get position and size of edit-box.<br/>
		 * for FreeText annotation, position of edit-box is not the position of annotation.<br/>
		 * so this function is needed for edit-box.
		 * this method valid in premium version
		 * @param rect 4 elements in order: left, top, right, bottom, in PDF coordinate.
		 * @return true or false
		 */
        final public boolean GetEditTextRect( float[] rect )
		{
			return Page.getAnnotEditTextRect(page.hand, hand, rect);
		}
		/**
		 * get text size of edit-box.<br/>
		 * this method valid in premium version
		 * @return size of text, in PDF coordinate system.
		 */
        final public float GetEditTextSize()
		{
			return Page.getAnnotEditTextSize(page.hand, hand);
		}
		/**
		 * get format of edit-box.<br/>
		 * this method valid in premium version
		 * @return format of edit-box, mostly a java-script like:<br/>
		 * AFDate_FormatEx("dd/mm/yy");<br/>
		 * most common java script function invoked as:
		 * AFNumber_Format<br/>
		 * AFDate_Format<br/>
		 * AFTime_Format<br/>
		 * AFSpecial_Format<br/>
		 * AFPercent_Format<br/>
		 * and so on.
		 */
        final public String GetFieldFormat()
		{
			return Page.getAnnotFieldFormat(page.hand, hand);
		}
		/**
		 * get format of edit-box.<br/>
		 * this method valid in premium version
		 * @return format of edit-box, mostly a java-script like:<br/>
		 * AFDate_FormatEx("dd/mm/yy");<br/>
		 * most common java script function invoked as:
		 * AFNumber_Format<br/>
		 * AFDate_Format<br/>
		 * AFTime_Format<br/>
		 * AFSpecial_Format<br/>
		 * AFPercent_Format<br/>
		 * and so on.
		 */
        final public String GetEditTextFormat()
		{
			return Page.getAnnotEditTextFormat(page.hand, hand);
		}
		/**
		 * get text color for edit-box annotation.include text field and free-text.<br/>
		 * this method valid in premium version
		 * @return 0 or color, format as 0xAARRGGBB.
		 */
        final public int GetEditTextColor()
		{
			return Page.getAnnotEditTextColor(page.hand, hand);
		}
		/**
		 * set text color for edit-box annotation.include text field and free-text<br/>
		 * this method valid in premium version
		 * @param color color format as 0xRRGGBB, alpha channel are ignored.
		 * @return true or false.
		 */
        final public boolean SetEditTextColor(int color)
		{
			return Page.setAnnotEditTextColor(page.hand, hand, color);
		}
		/**
		 * get contents of edit-box.<br/>
		 * this method valid in premium version
		 * @return content in edit-box
		 */
        final public String GetEditText()
		{
			return Page.getAnnotEditText(page.hand, hand);
		}
		/**
		 * set contents of edit-box.<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in premium version.<br/>
		 * Notice: this method not check format as GetEditTextFormat. developers shall check format by developer self.
		 * @param text contents to be set.<br/>in MultiLine mode: '\r' or '\n' means change line.<br/>in password mode the edit box always display "*". 
		 * @return true or false.
		 */
        final public boolean SetEditText( String text )
		{
			return Page.setAnnotEditText(page.hand, hand, text);
		}

        /**
         * set font of edittext.<br/>
         * you should re-render page to display modified data.<br/>
         * this method valid in premium version.<br/>
         * @param font DocFont object from Document.NewFontCID().
         * @return true or false.
         */
        final public boolean SetEditFont(DocFont font)
        {
            if(font == null) return false;
            return setAnnotEditFont(page.hand, hand, font.hand);
        }

		/**
		 * get item count of combo-box.<br/>
		 * this method valid in premium version
		 * @return -1: this is not combo. otherwise: items count.
		 */
        final public int GetComboItemCount()
		{
			return Page.getAnnotComboItemCount(page.hand, hand);
		}
		/**
		 * get an item of combo-box.<br/>
		 * this method valid in premium version
		 * @param item 0 based item index. range:[0, GetAnnotComboItemCount()-1]
		 * @return null if this is not combo-box, "" if no item selected, otherwise the item selected.
		 */
        final public String GetComboItem( int item )
		{
			return Page.getAnnotComboItem(page.hand, hand, item);
		}
		/**
		 * get current selected item index of combo-box.<br/>
		 * this method valid in premium version
		 * @return -1 if this is not combo-box or no item selected, otherwise the item index that selected.
		 */
        final public int GetComboItemSel()
		{
			return Page.getAnnotComboItemSel(page.hand, hand);
		}
		/**
		 * set current selected.<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in premium version
		 * @param item 0 based item index to set.
		 * @return true or false.
		 */
        final public boolean SetComboItem( int item )
		{
			return Page.setAnnotComboItem(page.hand, hand, item);
		}
		/**
		 * get item count of list-box.<br/>
		 * this method valid in premium version
		 * @return -1: this is not a list. otherwise: items count.
		 */
        final public int GetListItemCount()
		{
			return Page.getAnnotListItemCount(page.hand, hand);
		}
		/**
		 * get an item of list-box.<br/>
		 * this method valid in premium version
		 * @param item 0 based item index. range:[0, GetListItemCount()-1]
		 * @return null if this is not list-box, "" if no item selected, otherwise the item selected.
		 */
        final public String GetListItem( int item )
		{
			return Page.getAnnotListItem(page.hand, hand, item);
		}

        /**
         * only works under premium license.
         * @param item
         * @return
         */
        final public boolean RemoveListItem( int item )
        {
            return removeAnnotListItem(page.hand, hand, item);
        }

        /**
         * only works under premium license.
         * @param item index of items, range [0, liet_item_count]
         * @param val export string.
         * @param txt display string.
         * @return
         */
        final public boolean InsertListItem(int item, String val, String txt)
        {
            return insertAnnotListItem(page.hand, hand, item, val, txt);
        }

        /**
         * only works under premium license.
         * @param item
         * @return
         */
        final public boolean RemoveComboItem(int item)
        {
            return removeAnnotComboItem(page.hand, hand, item);
        }

        /**
         * only works under premium license.
         * @param item index of items, range [0, liet_item_count]
         * @param val export string.
         * @param txt display string.
         * @return
         */
        final public boolean InsertComboItem(int item, String val, String txt)
        {
            return insertAnnotComboItem(page.hand, hand, item, val, txt);
        }

        /**
         * is this annotation read-only?
         * @return if annotation is field, return field property. otherwise return annotation property.
         */
        final public boolean IsReadOnly()
        {
            return isAnnotReadOnly(page.hand, hand);
        }
        /**
         * if annotation is field, then set field property<br/>
         * otherwise, set annotation property.
         * @param read_only
         */
        final public void SetReadOnly(boolean read_only)
        {
            setAnnotReadOnly(page.hand, hand, read_only);
        }
		/**
		 * get selected indexes of list-box.<br/>
		 * this method valid in premium version
		 * @return null if it is not a list-box, or no items selected.
		 */
        final public int[] GetListSels()
		{
			return Page.getAnnotListSels(page.hand, hand);
		}
		/**
		 * set selects of list-box
		 * this method valid in premium version
		 * @param items 0 based indexes of items.
		 * @return true or false
		 */
        final public boolean SetListSels( int[] items )
		{
			return Page.setAnnotListSels(page.hand, hand, items);
		}
		/**
		 * get status of check-box and radio-box.<br/>
		 * this method valid in premium version
		 * @return <br/>-1 if annotation is not valid control.<br/>0 if check-box is not checked.<br/>1 if check-box checked.<br/>2 if radio-box is not checked.<br/>3 if radio-box checked.
		 */
        final public int GetCheckStatus()
		{
			return Page.getAnnotCheckStatus(page.hand, hand);
		}
		/**
		 * set value to check-box.<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in premium version
		 * @param check true or false.
		 * @return true or false.
		 */
        final public boolean SetCheckValue( boolean check )
		{
			return Page.setAnnotCheckValue(page.hand, hand, check);
		}
		/**
		 * check the radio-box and deselect others in radio group.<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in premium version
		 * @return true or false.
		 */
        final public boolean SetRadio()
		{
			return Page.setAnnotRadio(page.hand, hand);
		}
		/**
		 * get status of signature field.<br/>
		 * this method valid in premium version
		 * @return -1 if this is not signature field<br/>
		 *  0 if not signed.<br/>
		 *  1 if signed.
		 */
        final public int GetSignStatus()
		{
			return Page.getAnnotSignStatus(page.hand, hand);
		}
		/**
		 * check if the annotation is reset button?<br/>
		 * this method valid in premium version
		 * @return true or false.
		 */
        final public boolean GetReset()
		{
			return Page.getAnnotReset(page.hand, hand);
		}
		/**
		 * perform the button and reset the form.<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in premium version
		 * @return true or false.
		 */
        final public boolean SetReset()
		{
			return Page.setAnnotReset(page.hand, hand);
		}
		/**
		 * get annotation submit target.<br/>
		 * this method valid in premium version
		 * @return null if this is not submit button.
		 */
        final public String GetSubmitTarget()
		{
			return Page.getAnnotSubmitTarget(page.hand, hand);
		}
		/**
		 * get annotation submit parameters.<br/>
		 * mail mode: return whole XML string for form data.<br/>
		 * other mode: url data likes: "para1=xxx&para2=xxx".<br/>
		 * this method valid in premium version
		 * @return null if this is not submit button.
		 */
        final public String GetSubmitPara()
		{
			return Page.getAnnotSubmitPara(page.hand, hand);
		}
		/**
		 * get fill color of square/circle/highlight/line/ploygon/polyline/sticky text/free text/text field annotation.<br/>
		 * this method valid in professional or premium version
		 * @return color value formatted as 0xAARRGGBB, if 0 returned, means false.
		 */
        final public int GetFillColor()
		{
			return Page.getAnnotFillColor(page.hand, hand);
		}
		/**
		 * set fill color of square/circle/highlight/line/ploygon/polyline/sticky text/free text/text field annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param color color value formatted as 0xAARRGGBB.
		 * @return true or false
		 */
        final public boolean SetFillColor( int color )
		{
			return Page.setAnnotFillColor(page.hand, hand, color);
		}
		/**
		 * get stroke color of square/circle/ink/line/underline/Squiggly/strikeout/ploygon/polyline/free text/text field annotation.<br/>
		 * this method valid in professional or premium version
		 * @return color value formatted as 0xAARRGGBB, if 0 returned, means false.
		 */
        final public int GetStrokeColor()
		{
			return Page.getAnnotStrokeColor(page.hand, hand);
		}
		/**
		 * set stroke color of square/circle/ink/line/underline/Squiggly/strikeout/ploygon/polyline/free text/text field annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param color color value formatted as 0xAARRGGBB, if alpha channel is too less or 0, return false.
		 * @return true or false
		 */
        final public boolean SetStrokeColor( int color )
		{
			return Page.setAnnotStrokeColor(page.hand, hand, color);
		}
		/**
		 * get stroke width of square/circle/ink/line/ploygon/polyline/free text/text field annotation.<br/>
		 * for free text annotation: width of edit-box border<br/>
		 * this method valid in professional or premium version
		 * @return width value in PDF coordinate, or 0 if error.
		 */
        final public float GetStrokeWidth()
		{
			return Page.getAnnotStrokeWidth(page.hand, hand);
		}
		/**
		 * set stroke width of square/circle/ink/line/ploygon/polyline/free text/text field annotation.<br/>
		 * for free text annotation: width of edit-box border<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param width stroke width in PDF coordinate.
		 * @return true or false
		 */
        final public boolean SetStrokeWidth( float width )
		{
			return Page.setAnnotStrokeWidth(page.hand, hand, width);
		}
		/**
		 * get Path object from Ink annotation.<br/>
		 * this method valid in professional or premium version
		 * @return a new Path object, you need invoke Path.Destroy() to free memory.
		 */
        final public Path GetInkPath()
		{
			long ret = Page.getAnnotInkPath(page.hand, hand);
			if( ret == 0 ) return null;
			Path path = new Path();
			path.m_hand = ret;
			return path;
		}
		/**
		 * set Path to Ink annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param path Path object.
		 * @return true or false.
		 */
        final public boolean SetInkPath( Path path )
		{
			if( path == null ) return false;
			return Page.setAnnotInkPath(page.hand, hand, path.m_hand);
		}
		/**
		 * get Path object from Polygon annotation.<br/>
		 * this method valid in professional or premium version
		 * @return a new Path object, you need invoke Path.Destroy() to free memory.
		 */
        final public Path GetPolygonPath()
		{
			long ret = Page.getAnnotPolygonPath(page.hand, hand);
			if( ret == 0 ) return null;
			Path path = new Path();
			path.m_hand = ret;
			return path;
		}
		/**
		 * set Path to Polygon annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param path Path object.
		 * @return true or false.
		 */
        final public boolean SetPolygonPath( Path path )
		{
			if( path == null ) return false;
			return Page.setAnnotPolygonPath(page.hand, hand, path.m_hand);
		}
		/**
		 * get Path object from Polyline annotation.<br/>
		 * this method valid in professional or premium version
		 * @return a new Path object, you need invoke Path.Destroy() to free memory.
		 */
        final public Path GetPolylinePath()
		{
			long ret = Page.getAnnotPolylinePath(page.hand, hand);
			if( ret == 0 ) return null;
			Path path = new Path();
			path.m_hand = ret;
			return path;
		}
		/**
		 * set Path to Polyline annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param path Path object.
		 * @return true or false.
		 */
        final public boolean SetPolylinePath( Path path )
		{
			if( path == null ) return false;
			return Page.setAnnotPolylinePath(page.hand, hand, path.m_hand);
		}
		/**
		 * set icon for sticky text note/file attachment/Rubber Stamp annotation.<br/>
		 * you need render page again to show modified annotation.<br/>
		 * this method valid in professional or premium version
		 * @param icon icon value depends on annotation type.<br/>
		 * <strong>For sticky text note:</strong><br/>
		 * 0: Note<br/>
		 * 1: Comment<br/>
		 * 2: Key<br/>
		 * 3: Help<br/>
		 * 4: NewParagraph<br/>
		 * 5: Paragraph<br/>
		 * 6: Insert<br/>
		 * 7: Check<br/>
		 * 8: Circle<br/>
		 * 9: Cross<br/>
		 * <strong>For file attachment:</strong><br/>
		 * 0: PushPin<br/>
		 * 1: Graph<br/>
		 * 2: Paperclip<br/>
		 * 3: Tag<br/>
		 * <strong>For Rubber Stamp:</strong><br/>
		 *  0: "Draft"(default icon)<br/>
		 *  1: "Approved"<br/>
		 *  2: "Experimental"<br/>
		 *  3: "NotApproved"<br/>
		 *  4: "AsIs"<br/>
		 *  5: "Expired"<br/>
		 *  6: "NotForPublicRelease"<br/>
		 *  7: "Confidential"<br/>
		 *  8: "Final"<br/>
		 *  9: "Sold"<br/>
		 * 10: "Departmental"<br/>
		 * 11: "ForComment"<br/>
		 * 12: "TopSecret"<br/>
		 * 13: "ForPublicRelease"<br/>
		 * 14: "Accepted"<br/>
		 * 15: "Rejected"<br/>
		 * 16: "Witness"<br/>
		 * 17: "InitialHere"<br/>
		 * 18: "SignHere"<br/>
		 * 19: "Void"<br/>
		 * 20: "Completed"<br/>
		 * 21: "PreliminaryResults"<br/>
		 * 22: "InformationOnly"<br/>
		 * 23: "End"<br/>
		 * @return true or false.
		 */
        final public boolean SetIcon( int icon )
		{
			return Page.setAnnotIcon(page.hand, hand, icon);
		}
		/**
		 * set customized icon for  sticky text note/file attachment annotation.<br/>
		 * @param name customized icon name.
		 * @param content PageContent object to display icon, must be 20 * 20 size.
		 * @return true or false.
		 */
        final public boolean SetIcon( String name, PageContent content )
		{
			return Page.setAnnotIcon2(page.hand, hand, name, content.hand);
		}
		/**
		 * get icon value for sticky text note/file attachment/Rubber Stamp annotation.<br/>
		 * this method valid in professional or premium version
		 * @return icon value depends on annotation type.<br/>
		 * <strong>For sticky text note:</strong><br/>
		 * 0: Note<br/>
		 * 1: Comment<br/>
		 * 2: Key<br/>
		 * 3: Help<br/>
		 * 4: NewParagraph<br/>
		 * 5: Paragraph<br/>
		 * 6: Insert<br/>
		 * 7: Check<br/>
		 * 8: Circle<br/>
		 * 9: Cross<br/>
		 * <strong>For file attachment:</strong><br/>
		 * 0: PushPin<br/>
		 * 1: Graph<br/>
		 * 2: Paperclip<br/>
		 * 3: Tag<br/>
		 * <strong>For Rubber Stamp:</strong><br/>
		 *  0: "Draft"(default icon)<br/>
		 *  1: "Approved"<br/>
		 *  2: "Experimental"<br/>
		 *  3: "NotApproved"<br/>
		 *  4: "AsIs"<br/>
		 *  5: "Expired"<br/>
		 *  6: "NotForPublicRelease"<br/>
		 *  7: "Confidential"<br/>
		 *  8: "Final"<br/>
		 *  9: "Sold"<br/>
		 * 10: "Departmental"<br/>
		 * 11: "ForComment"<br/>
		 * 12: "TopSecret"<br/>
		 * 13: "ForPublicRelease"<br/>
		 * 14: "Accepted"<br/>
		 * 15: "Rejected"<br/>
		 * 16: "Witness"<br/>
		 * 17: "InitialHere"<br/>
		 * 18: "SignHere"<br/>
		 * 19: "Void"<br/>
		 * 20: "Completed"<br/>
		 * 21: "PreliminaryResults"<br/>
		 * 22: "InformationOnly"<br/>
		 * 23: "End"<br/>
		 */
        final public int GetIcon()
		{
			return Page.getAnnotIcon(page.hand, hand);
		}
		/**
		 * remove annotation<br/>
		 * you should re-render page to display modified data.<br/>
		 * this method valid in professional or premium version
		 * @return true or false
		 */
        final public boolean RemoveFromPage()
		{
			boolean ret = removeAnnot( page.hand, hand );
			hand = 0;
			return ret;
		}
	}
	public class Finder
	{
		protected long hand;
		/**
		 * get find count in this page.
		 * @return count or 0 if no found.
		 */
		public final int GetCount()
		{
			return Page.findGetCount( hand );
		}
		/**
		 * get find count in this page.
		 * @param index 0 based index value. range:[0, FindGetCount()-1]
		 * @return the first char index of texts, see: ObjsGetString. range:[0, ObjsGetCharCount()-1]
		 */
		public final int GetFirstChar( int index )
		{
			return Page.findGetFirstChar( hand, index );
		}
		/**
		 * free memory of find session.
		 */
		public final void Close()
		{
			Page.findClose( hand );
			hand = 0;
		}
        @Override
        protected void finalize() throws Throwable
        {
            Close();
            super.finalize();
        }
	}
	protected long hand = 0;
    protected Document m_doc;
    static private native long advGetAnnotRef(long page, long annot);
    static private native long advGetRef(long page);
    static private native void advReloadAnnot(long page, long annot);
    static private native void advReload(long page);
    static private native float[] getCropBox( long hand );
	static private native float[] getMediaBox( long hand );
	static private native void close( long hand );
	static private native void renderPrepare( long hand, long dib );
	static private native boolean render( long hand, long dib, long matrix, int quality ) throws Exception;
	static private native boolean renderToBmp( long hand, Bitmap bitmap, long matrix, int quality ) throws Exception;
	static private native boolean renderToBuf( long hand, int[] data, int w, int h, long matrix, int quality) throws Exception;
	static private native void renderCancel(long hand);
	static private native boolean renderThumb(long hand, Bitmap bmp) throws Exception;
	static private native boolean renderThumbToDIB( long hand, long dib) throws Exception;
	static private native boolean renderThumbToBuf( long hand, int[] data, int w, int h) throws Exception;
	static private native boolean renderIsFinished(long hand);
	static private native float reflowStart( long hand, float width, float scale, boolean enable_images );
	static private native boolean reflow( long hand, long dib, float orgx, float orgy );
	static private native boolean reflowToBmp( long hand, Bitmap bitmap, float orgx, float orgy );
	static private native int reflowGetParaCount( long hand );
	static private native int reflowGetCharCount( long hand, int iparagraph );
	static private native float reflowGetCharWidth( long hand, int iparagraph, int ichar );
	static private native float reflowGetCharHeight( long hand, int iparagraph, int ichar );
	static private native int reflowGetCharColor( long hand, int iparagraph, int ichar );
	static private native int reflowGetCharUnicode( long hand, int iparagraph, int ichar );
	static private native String reflowGetCharFont( long hand, int iparagraph, int ichar );
	static private native void reflowGetCharRect( long hand, int iparagraph, int ichar, float rect[] );
	static private native String reflowGetText( long hand, int iparagraph1, int ichar1, int iparagraph2, int ichar2 );

	static private native void objsStart( long hand, boolean rtol );
	static private native String objsGetString( long hand, int from, int to );
	static private native int objsAlignWord( long hand, int from, int dir );
	static private native void objsGetCharRect( long hand, int index, float[]vals );
	static private native String objsGetCharFontName( long hand, int index );
	static private native int objsGetCharCount( long hand );
	static private native int objsGetCharIndex( long hand, float[] pt );

	static private native long findOpen( long hand, String str, boolean match_case, boolean whole_word );
	static private native int findGetCount( long hand_finder );
	static private native int findGetFirstChar( long hand_finder, int index );
	static private native void findClose( long hand_finder );

	private static native int getRotate( long hand );
	static private native int getAnnotCount( long hand );
	static private native long getAnnot( long hand, int index );
	static private native boolean renderAnnotToBmp( long hand, long annot, Bitmap bitmap );
	static private native long getAnnotFromPoint( long hand, float x, float y );
	static private native long getAnnotByName( long hand, String name );
	static private native String getAnnotName( long hand, long annot);
    static private native long getAnnotPopup(long page, long annot);
    static private native boolean getAnnotPopupOpen(long page, long annot);
    static private native boolean setAnnotPopupOpen(long page, long annot, boolean open);
	static private native boolean setAnnotName( long hand, long annot, String name);
	static private native int getAnnotFieldType( long hand, long annot );
    static private native int getAnnotFieldFlag( long hand, long annot );
	static private native String getAnnotFieldName( long hand, long annot );
    static private native String getAnnotFieldNameWithoutNO( long hand, long annot );
	static private native String getAnnotFieldFullName( long hand, long annot );
	static private native String getAnnotFieldFullName2( long hand, long annot );
	static private native int getAnnotType( long hand, long annot );
	static private native boolean isAnnotLocked( long hand, long annot );
	static private native boolean isAnnotHide( long hand, long annot );
	static private native boolean isAnnotLockedContent( long hand, long annot );
	static private native float[] getAnnotMarkupRects( long hand, long annot );
	static private native void getAnnotRect( long hand, long annot, float[] rect );
	static private native void setAnnotRect( long hand, long annot, float[] rect );
	static private native void setAnnotHide( long hand, long annot, boolean hide );
	static private native void setAnnotLock( long hand, long annot, boolean lock );
	static private native String getAnnotPopupText( long hand, long annot );
	static private native boolean setAnnotPopupText( long hand, long annot, String val );
	static private native String getAnnotPopupSubject( long hand, long annot );
	static private native boolean setAnnotPopupSubject( long hand, long annot, String val );
    static private native String getAnnotPopupLabel( long hand, long annot );
    static private native boolean setAnnotPopupLabel( long hand, long annot, String val );
	static private native int getAnnotDest( long hand, long annot );
    static private native String getAnnotRemoteDest( long hand, long annot );
	static private native String getAnnotURI( long hand, long annot );
	static private native String getAnnotJS( long hand, long annot);
	static private native String getAnnotFileLink( long hand, long annot );
	static private native String getAnnot3D( long hand, long annot );
	static private native String getAnnotMovie( long hand, long annot );
	static private native String getAnnotSound( long hand, long annot );
	static private native String getAnnotAttachment( long hand, long annot );
	static private native boolean getAnnot3DData( long hand, long annot, String save_file );
	static private native boolean getAnnotMovieData( long hand, long annot, String save_file );
	static private native boolean getAnnotSoundData( long hand, long annot, int paras[], String save_file );
	static private native boolean getAnnotAttachmentData( long hand, long annot, String save_file );
	static private native int getAnnotEditType( long hand, long annot );
	static private native int getAnnotEditMaxlen( long hand, long annot );
	static private native boolean getAnnotEditTextRect( long hand, long annot, float[] rect );
	static private native float getAnnotEditTextSize( long hand, long annot );
	static private native String getAnnotEditTextFormat( long hand, long annot );
	static private native String getAnnotFieldFormat( long hand, long annot );
	static private native int getAnnotEditTextColor(long hand, long annot);
	static private native boolean setAnnotEditTextColor(long hand, long annot, int color);
	static private native String getAnnotEditText( long hand, long annot );
	static private native boolean setAnnotEditText( long hand, long annot, String text );
    static private native boolean setAnnotEditFont( long hand, long annot, long font);
	static private native int getAnnotComboItemCount( long hand, long annot );
	static private native String getAnnotComboItem( long hand, long annot, int item );
	static private native int getAnnotComboItemSel( long hand, long annot );
	static private native boolean setAnnotComboItem( long hand, long annot, int item );
	static private native int getAnnotListItemCount( long hand, long annot );
	static private native String getAnnotListItem( long hand, long annot, int item );
	static private native int[] getAnnotListSels( long hand, long annot );
	static private native boolean setAnnotListSels( long hand, long annot, int[] items );
    static private native boolean removeAnnotListItem( long hand, long annot, int item);
    static private native boolean insertAnnotListItem( long hand, long annot, int item, String val, String txt);
    static private native boolean removeAnnotComboItem( long hand, long annot, int item);
    static private native boolean insertAnnotComboItem( long hand, long annot, int item, String val, String txt);
    static private native boolean isAnnotReadOnly( long hand, long annot);
    static private native void setAnnotReadOnly( long hand, long annot, boolean lock);

	static private native int getAnnotCheckStatus( long hand, long annot );
	static private native boolean setAnnotCheckValue( long hand, long annot, boolean check );
	static private native boolean setAnnotRadio( long hand, long annot );
	static private native int getAnnotSignStatus( long hand, long annot );
	static private native boolean getAnnotReset( long hand, long annot );
	static private native boolean setAnnotReset( long hand, long annot );
	static private native String getAnnotSubmitTarget( long hand, long annot );
	static private native String getAnnotSubmitPara( long hand, long annot );
	static private native int getAnnotFillColor( long hand, long annot );
	static private native boolean setAnnotFillColor( long hand, long annot, int color );
	static private native int getAnnotStrokeColor( long hand, long annot );
	static private native boolean setAnnotStrokeColor( long hand, long annot, int color );
	static private native float getAnnotStrokeWidth( long hand, long annot );
	static private native boolean setAnnotStrokeWidth( long hand, long annot, float width );
	static private native long getAnnotInkPath( long hand, long annot );
	static private native boolean setAnnotInkPath( long hand, long annot, long path );
	static private native long getAnnotPolygonPath( long hand, long annot );
	static private native boolean setAnnotPolygonPath( long hand, long annot, long path );
	static private native long getAnnotPolylinePath( long hand, long annot );
	static private native boolean setAnnotPolylinePath( long hand, long annot, long path );
	static private native boolean setAnnotIcon( long hand, long annot, int icon );
	static private native int getAnnotIcon( long hand, long annot );
	static private native boolean setAnnotIcon2( long hand, long annot, String name, long content );

	static private native boolean removeAnnot( long hand, long annot );
	static private native boolean moveAnnot( long hand_src, long hand_dst, long annot, float[] rect );
	static private native boolean copyAnnot( long hand, long annot, float[] rect );
    static private native boolean addAnnotPopup( long hand, long parent, float[] rect, boolean open );
	static private native boolean addAnnotHWriting( long hand, long matrix, long hwriting, float orgx, float orgy );
	static private native boolean addAnnotInk( long hand, long matrix, long ink, float orgx, float orgy );
	static private native boolean addAnnotGlyph( long hand, long matrix, long path, int color, boolean winding );
	static private native boolean addAnnotLine( long hand, long matrix, float[] pt1, float[] pt2, int style1, int style2, float width, int color, int icolor );
	static private native boolean addAnnotRect( long hand, long matrix, float[] rect, float width, int color, int fill_color );
	static private native boolean addAnnotEllipse( long hand, long matrix, float[] rect, float width, int color, int fill_color );
	static private native boolean addAnnotEditbox( long hand, long matrix, float[] rect, int line_clr, float line_w, int fill_clr, float tsize, int text_clr );
	static private native boolean addAnnotEditbox2( long hand, float[] rect, int line_clr, float line_w, int fill_clr, float tsize, int text_clr );
	static private native boolean addAnnotMarkup( long hand, long matrix, float[] rects, int color, int type );

	static private native boolean addAnnotInk2( long hand, long ink );
	static private native boolean addAnnotLine2( long hand, float[] pt1, float[] pt2, int style1, int style2, float width, int color, int icolor );
	static private native boolean addAnnotRect2( long hand, float[] rect, float width, int color, int fill_color );
	static private native boolean addAnnotEllipse2( long hand, float[] rect, float width, int color, int fill_color );
	static private native boolean addAnnotMarkup2( long hand, int cindex1, int cindex2, int color, int type );
	static private native boolean addAnnotBitmap( long hand, Bitmap bitmap, boolean has_alpha, float[] rect );
	static private native boolean addAnnotAttachment( long hand, String path, int icon, float[] rect );
	static private native boolean addAnnotText( long hand, float[] pt );
	static private native boolean addAnnotGoto( long hand, float[] rect, int pageno, float top );
	static private native boolean addAnnotURI( long hand, float[] rect, String uri );
	static private native boolean addAnnotStamp( long hand, float[] rect, int icon );
	static private native boolean addAnnotPolygon( long hand, long path, int color, int fill_color, float width );
	static private native boolean addAnnotPolyline( long hand, long path, int style1, int style2, int color, int fill_color, float width );

	static private native long addResFont( long hand, long font );
	static private native long addResImage( long hand, long image );
	static private native long addResGState( long hand, long gstate );
    static private native long addResForm(long hand, long form);

    static private native boolean addContent( long hand, long content, boolean flush );

    /**
     * advanced function to get reference of Page object.<br/>
     * this method need premium license.
     * @return reference
     */
    final public Ref Advance_GetRef()
    {
        long ret = advGetRef(hand);
        if(ret == 0) return null;
        return new Ref(ret);
    }

    /**
     * advanced function to reload page object, after advanced methods update Page object data.<br/>
     * all annotations return from Page.GetAnnot() or Page.GetAnnotFromPoint() shall not available. after this method invoked.<br/>
     * this method need premium license.
     */
    final public void Advance_Reload()
    {
        advReload(hand);
    }
	/**
	 * get rotated CropBox, this method need an any type of license.
	 * @return float array as [left, top, right, bottom] in PDF coordinate.
	 */
    final public float[] GetCropBox()
	{
		return getCropBox( hand );
	}
	/**
	 * get rotated MediaBox, this method need an any type of license.
	 * @return float array as [left, top, right, bottom] in PDF coordinate.
	 */
    final public float[] GetMediaBox()
	{
		return getMediaBox( hand );
	}

	/**
	 * Close page object and free memory.
	 */
    final public void Close()
	{
        if(m_doc != null)
        {
            if(m_doc.hand_val != 0)
                close( hand );
            else
                Log.e("Bad Coding", "Document object closed, but Page object not closed, will cause memory leaks.");
        }
        m_doc = null;
        hand = 0;
	}
	/**
	 * prepare to render. it reset dib pixels to white value, and reset page status.
	 * @param dib DIB object to render. obtained by Global.dibGet().
	 */
    final public void RenderPrePare( DIB dib )
	{
        if(dib == null)
            renderPrepare( hand, 0 );
        else
		    renderPrepare( hand, dib.hand );
	}
	/**
	 * render page to dib object. this function returned for cancelled or finished.<br/>before render, you need invoke RenderPrePare.
	 * @param dib DIB object to render. obtained by Global.dibGet().
	 * @param mat Matrix object define scale, rotate, translate operations.
	 * @return true or false.
	 */
    final public boolean Render( DIB dib, Matrix mat )
	{
        if(dib == null || mat == null) return  false;
        try {
            return render(hand, dib.hand, mat.hand, Global.render_mode);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * render page to Bitmap object directly. this function returned for cancelled or finished.<br/>
	 * before render, you need erase Bitmap object. 
	 * @param bitmap Bitmap object to render.
	 * @param mat Matrix object define scale, rotate, translate operations.
	 * @return true or false.
	 */
    final public boolean RenderToBmp( Bitmap bitmap, Matrix mat )
	{
        if(bitmap == null || mat == null) return  false;
        try {
            return renderToBmp(hand, bitmap, mat.hand, Global.render_mode);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * render page to int array directly. this function returned for cancelled or finished.<br/>
	 * before render, you need erase int array object.
	 * @param data  int array store as pixels, must length as w * h, each element formated as 0xAARRGGBB
	 * @param w width.
	 * @param h height.
	 * @param mat Matrix object define scale, rotate, translate operations.
	 * @return true or false.
	 */
    final public boolean RenderToBuf( int[] data, int w, int h, Matrix mat )
	{
        if(data == null || mat == null) return  false;
        try {
            return renderToBuf(hand, data, w, h, mat.hand, Global.render_mode);
        }catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * render to page in normal quality
	 * @param dib same as Render function
	 * @param mat same as Render function
	 */
    final public boolean Render_Normal( DIB dib, Matrix mat )
	{
        if(dib == null || mat == null) return false;
        try {
            return render(hand, dib.hand, mat.hand, 1);
        }catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * set page status to cancelled and cancel render function.
	 */
    final public void RenderCancel()
	{
		renderCancel( hand );
	}
	/**
	 * render thumb image to Bitmap object.<br/>
	 * the image always scale and displayed in center of Bitmap.<br/>
	 * @param bmp Bitmap to render
	 * @return true if the page has thumb image, or false.
	 */
    final public boolean RenderThumb(Bitmap bmp)
	{
        try {
            return renderThumb(hand, bmp);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
    final public boolean RenderThumbToDIB(DIB dib)
	{
        if(dib == null) return false;
        try {
            return renderThumbToDIB(hand, dib.hand);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * render thumb image to int array object.<br/>
	 * the image always scale and displayed in center of Bitmap.<br/>
	 * @param data int array store as pixels, must length as w * h, each element formated as 0xAARRGGBB
	 * @param w
	 * @param h
	 * @return
	 */
    final public boolean RenderThumbToBuf(int []data, int w, int h)
	{
        try {
            return renderThumbToBuf(hand, data, w, h);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
	}
	/**
	 * check if page rendering is finished.
	 * @return true or false
	 */
    final public boolean RenderIsFinished()
	{
		return renderIsFinished( hand );
	}
	/**
	 * get text objects to memory.<br/>
	 * a standard license is needed for this method
	 */
    final public void ObjsStart()
	{
		objsStart( hand, Global.selRTOL );
	}
	/**
	 * get string from range. this can be invoked after ObjsStart
	 * @param from 0 based unicode index.
	 * @param to 0 based unicode index.
	 * @return string or null.
	 */
    final public String ObjsGetString( int from, int to )
	{
		return objsGetString( hand, from, to );
	}
	/**
	 * get index aligned by word. this can be invoked after ObjsStart
	 * @param from 0 based unicode index.
	 * @param dir if dir < 0,  get start index of the word. otherwise get last index of the word.
	 * @return new index value.
	 */
    final public int ObjsAlignWord( int from, int dir )
	{
		return objsAlignWord( hand, from, dir );
	}
	/**
	 * get char's box in PDF coordinate system, this can be invoked after ObjsStart
	 * @param index 0 based unicode index.
	 * @param vals return 4 elements for PDF rectangle.
	 */
	public final void ObjsGetCharRect( int index, float[]vals )
	{
		objsGetCharRect( hand, index, vals );
	}
	/**
	 * get char's font name. this can be invoked after ObjsStart
	 * @param index 0 based unicode index.
	 * @return font name, may be null.
	 */
	public String ObjsGetCharFontName( int index )
	{
		return objsGetCharFontName( hand, index );
	}
	/**
	 * get chars count in this page. this can be invoked after ObjsStart<br/>
	 * a standard license is needed for this method
	 * @return count or 0 if ObjsStart not invoked.
	 */
    final public int ObjsGetCharCount()
	{
		return objsGetCharCount( hand );
	}
	/**
	 * get char index nearest to point
	 * @param pt point as [x,y] in PDF coordinate.
	 * @return char index or -1 failed.
	 */
	public final int ObjsGetCharIndex( float[] pt )
	{
		return objsGetCharIndex( hand, pt );
	}
	/**
	 * create a find session. this can be invoked after ObjsStart
	 * @param str key string to find.
	 * @param match_case match case?
	 * @param whole_word match whole word?
	 * @return handle of find session, or 0 if no found.
	 */
	public Finder FindOpen( String str, boolean match_case, boolean whole_word )
	{
		long ret = findOpen( hand, str, match_case, whole_word );
		if( ret == 0 ) return null;
		Finder find = new Finder();
		find.hand = ret;
		return find;
	}

	/**
	 * get rotate degree for page, example: 0 or 90
	 * @return rotate degree for page
	 */
    final public int GetRotate()
	{
		return getRotate( hand );
	}
	/**
	 * get annotations count in this page.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @return count
	 */
    final public int GetAnnotCount()
	{
		return getAnnotCount( hand );
	}
	/**
	 * get annotations by index.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param index 0 based index value. range:[0, GetAnnotCount()-1]
	 * @return handle of annotation, valid until Close invoked.
	 */
	public Annotation GetAnnot( int index )
	{
		long ret = getAnnot( hand, index );
		if( ret == 0 ) return null;
		Annotation annot = new Annotation();
		annot.hand = ret;
		annot.page = this;
		return annot;
	}
	/**
	 * get annotation by PDF point.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param x x value in PDF coordinate system.
	 * @param y y value in PDF coordinate system.
	 * @return Annotation object, valid until Page.Close invoked.
	 */
	public Annotation GetAnnotFromPoint( float x, float y )
	{
		long ret = getAnnotFromPoint( hand, x, y );
		if( ret == 0 ) return null;
		Annotation annot = new Annotation();
		annot.hand = ret;
		annot.page = this;
		return annot;
	}
	/**
	 * get annotation by name.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param name name string in "NM" entry of annotation.
	 * @return Annotation object, valid until Page.Close invoked.
	 */
	public Annotation GetAnnotByName( String name )
	{
		long ret = getAnnotByName( hand, name );
		if( ret == 0 ) return null;
		Annotation annot = new Annotation();
		annot.hand = ret;
		annot.page = this;
		return annot;
	}
	/**
	 * add goto-page link to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param rect link area rect [left, top, right, bottom] in PDF coordinate.
	 * @param pageno 0 based pageno to goto.
	 * @param top y coordinate in PDF coordinate, page.height is top of page. and 0 is bottom of page.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotGoto( float[] rect, int pageno, float top )
	{
		return addAnnotGoto( hand, rect, pageno, top );
	}
	/**
	 * add URL link to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param rect link area rect [left, top, right, bottom] in PDF coordinate.
	 * @param uri url address, example: "http://www.radaee.com/en"
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotURI( float[] rect, String uri )
	{
		return addAnnotURI( hand, rect, uri );
	}
	/**
	 * add an Rubber Stamp to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param rect icon area rect [left, top, right, bottom] in PDF coordinate.
	 * @param icon predefined value as below:<br/>
	 *  0: "Draft"(default icon)<br/>
	 *  1: "Approved"<br/>
	 *  2: "Experimental"<br/>
	 *  3: "NotApproved"<br/>
	 *  4: "AsIs"<br/>
	 *  5: "Expired"<br/>
	 *  6: "NotForPublicRelease"<br/>
	 *  7: "Confidential"<br/>
	 *  8: "Final"<br/>
	 *  9: "Sold"<br/>
	 * 10: "Departmental"<br/>
	 * 11: "ForComment"<br/>
	 * 12: "TopSecret"<br/>
	 * 13: "ForPublicRelease"<br/>
	 * 14: "Accepted"<br/>
	 * 15: "Rejected"<br/>
	 * 16: "Witness"<br/>
	 * 17: "InitialHere"<br/>
	 * 18: "SignHere"<br/>
	 * 19: "Void"<br/>
	 * 20: "Completed"<br/>
	 * 21: "PreliminaryResults"<br/>
	 * 22: "InformationOnly"<br/>
	 * 23: "End"<br/>
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotStamp( float[] rect, int icon )
	{
		return addAnnotStamp( hand, rect, icon );
	}
	
	/**
	 * add hand-writing to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param ink Ink object
	 * @param orgx origin x coordinate in page. in DIB coordinate system
	 * @param orgy origin y coordinate in page. in DIB coordinate system
	 * @return true or false<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotInk( Matrix mat, Ink ink, float orgx, float orgy )
	{
        if(mat == null || ink == null) return false;
		return addAnnotInk( hand, mat.hand, ink.hand, orgx, orgy );
	}
	/**
	 * add hand-writing to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param hwriting hand writing object
	 * @param orgx origin x coordinate in page. in DIB coordinate system
	 * @param orgy origin y coordinate in page. in DIB coordinate system
	 * @return true or false<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotHWriting( Matrix mat, HWriting hwriting, float orgx, float orgy )
	{
        if(mat == null || hwriting == null) return false;
		return addAnnotHWriting(hand, mat.hand, hwriting.hand, orgx, orgy);
	}
	/**
	 * add a user-defined glyph to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param path Path object.
	 * @param color text color, formated as 0xAARRGGBB.
	 * @param winding if true, using winding fill rule, otherwise using odd-even fill rule.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotGlyph( Matrix mat, Path path, int color, boolean winding )
	{
        if(mat == null || path == null) return false;
		return addAnnotGlyph(hand, mat.hand, path.m_hand, color, winding);
	}
	/**
	 * add rectangle to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param rect 4 elements for left, top, right, bottom in DIB coordinate system
	 * @param width line width
	 * @param color rectangle color, formated as 0xAARRGGBB
	 * @param fill_color fill color in rectangle, formated as 0xAARRGGBB, if alpha channel is 0, means no fill operation, otherwise alpha channel are ignored.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotRect( Matrix mat, float[] rect, float width, int color, int fill_color )
	{
        if(mat == null || rect == null) return false;
		return addAnnotRect(hand, mat.hand, rect, width, color, fill_color);
	}
	/**
	 * add line to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param pt1 start point, 2 elements for x,y
	 * @param pt2 end point, 2 elements for x,y
	 * @param style1 style for start point:<br/>
	 * 0: None<br/>
	 * 1: Arrow<br/>
	 * 2: Closed Arrow<br/>
	 * 3: Square<br/>
	 * 4: Circle<br/>
	 * 5: Butt<br/>
	 * 6: Diamond<br/>
	 * 7: Reverted Arrow<br/>
	 * 8: Reverted Closed Arrow<br/>
	 * 9: Slash
	 * @param style2 style for end point, values are same as style1.
	 * @param width line width in DIB coordinate
	 * @param color line color. same as addAnnotRect.
	 * @param fill_color fill color. same as addAnnotRect.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotLine( Matrix mat, float[] pt1, float[] pt2, int style1, int style2, float width, int color, int fill_color )
	{
        if(mat == null) return false;
		return addAnnotLine(hand, mat.hand, pt1, pt2, style1, style2, width, color, fill_color);
	}
	/**
	 * add ellipse to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param rect 4 elements for left, top, right, bottom in DIB coordinate system
	 * @param width line width
	 * @param color ellipse color, formated as 0xAARRGGBB
	 * @param fill_color fill color in ellipse, formated as 0xAARRGGBB, if alpha channel is 0, means no fill operation, otherwise alpha channel are ignored.
	 * @return true or false<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotEllipse( Matrix mat, float[] rect, float width, int color, int fill_color )
	{
        if(mat == null) return false;
		return addAnnotEllipse(hand, mat.hand, rect, width, color, fill_color);
	}
    final public boolean AddAnnotPopup( Annotation parent, float[] rect, boolean open )
    {
        if(parent == null) return false;
        return addAnnotPopup( hand, parent.hand, rect, open);
    }
	/**
	 * add a text-markup annotation to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param mat Matrix for Render function.
	 * @param rects 4 * n rectangles, each 4 elements: left, top, right, bottom in DIB coordinate system. n is decided by length of array.
	 * @param type 0: Highlight, 1: Underline, 2: StrikeOut, 3: Highlight without round corner.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
	public boolean AddAnnotMarkup( Matrix mat, float[] rects, int type )
	{
        if(mat == null) return false;
		int color = 0xFFFFFF00;//yellow
		if( type == 1 ) color = 0xFF0000C0;//black blue
		if( type == 2 ) color = 0xFFC00000;//black red
		if( type == 2 ) color = 0xFF00C000;//black green
		return addAnnotMarkup( hand, mat.hand, rects, color, type );
	}
	/**
	 * add hand-writing to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param ink Ink object in PDF coordinate.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotInk( Ink ink )
	{
        if(ink == null) return false;
		return addAnnotInk2( hand, ink.hand );
	}
	/**
	 * add line to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param pt1 start point in PDF coordinate, 2 elements for x,y
	 * @param pt2 end point in PDF coordinate, 2 elements for x,y
	 * @param style1 style for start point:<br/>
	 * 0: None<br/>
	 * 1: Arrow<br/>
	 * 2: Closed Arrow<br/>
	 * 3: Square<br/>
	 * 4: Circle<br/>
	 * 5: Butt<br/>
	 * 6: Diamond<br/>
	 * 7: Reverted Arrow<br/>
	 * 8: Reverted Closed Arrow<br/>
	 * 9: Slash
	 * @param style2 style for end point, values are same as style1.
	 * @param width line width in DIB coordinate
	 * @param color line color. same as addAnnotRect.
	 * @param icolor fill color, used to fill arrows of the line.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotLine( float[] pt1, float[] pt2, int style1, int style2, float width, int color, int icolor )
	{
		return addAnnotLine2( hand, pt1, pt2, style1, style2, width, color, icolor );
	}
	/**
	 * add rectangle to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param rect 4 elements for left, top, right, bottom in PDF coordinate system
	 * @param width line width in PDF coordinate.
	 * @param color rectangle color, formated as 0xAARRGGBB
	 * @param fill_color fill color in rectangle, formated as 0xAARRGGBB, if alpha channel is 0, means no fill operation, otherwise alpha channel are ignored.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotRect( float[] rect, float width, int color, int fill_color )
	{
		return addAnnotRect2( hand, rect, width, color, fill_color );
	}
	/**
	 * add ellipse to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param rect 4 elements for left, top, right, bottom in PDF coordinate system
	 * @param width line width in PDF coordinate
	 * @param color ellipse color, formated as 0xAARRGGBB
	 * @param fill_color fill color in ellipse, formated as 0xAARRGGBB, if alpha channel is 0, means no fill operation, otherwise alpha channel are ignored.
	 * @return true or false<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotEllipse( float[] rect, float width, int color, int fill_color )
	{
		return addAnnotEllipse2( hand, rect, width, color, fill_color );
	}
	/**
	 * add an edit-box on page.<br/>
	 * the font of edit box is set by Global.setTextFont in Global.Init().<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in premium version.
	 * @param rect 4 elements: left, top, right, bottom in PDF coordinate system.
	 * @param line_clr color of border line, formated as 0xAARRGGBB.
	 * @param line_w width of border line.
	 * @param fill_clr color of background, formated as 0xAARRGGBB. AA must same to line_clr AA, or 0 means no fill color.
	 * @param tsize text size in PDF coordinate system.
	 * @param text_clr text color, formated as 0xAARRGGBB. AA must same to line_clr AA
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotEditbox( float[] rect, int line_clr, float line_w, int fill_clr, float tsize, int text_clr )
	{
		return addAnnotEditbox2( hand, rect, line_clr, line_w, fill_clr, tsize, text_clr );
	}
	/**
	 * add polygon to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param path must be a closed contour.
	 * @param color stroke color formated as 0xAARRGGBB.
	 * @param fill_color fill color, formated as 0xAARRGGBB. if AA == 0, no fill operations, otherwise alpha value is same to stroke color. 
	 * @param width stroke width in PDF coordinate
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotPolygon( Path path, int color, int fill_color, float width )
	{
        if(path == null) return false;
		return addAnnotPolygon( hand, path.m_hand, color, fill_color, width );
	}
	/**
	 * add polyline to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param path must be a set of unclosed lines. do not container any move-to operation except the first point in the path.
	 * @param style1 style for start point:<br/>
	 * 0: None<br/>
	 * 1: Arrow<br/>
	 * 2: Closed Arrow<br/>
	 * 3: Square<br/>
	 * 4: Circle<br/>
	 * 5: Butt<br/>
	 * 6: Diamond<br/>
	 * 7: Reverted Arrow<br/>
	 * 8: Reverted Closed Arrow<br/>
	 * 9: Slash
	 * @param style2 style for end point, values are same as style1.
	 * @param color stroke color formated as 0xAARRGGBB.
	 * @param fill_color fill color, formated as 0xAARRGGBB. if AA == 0, no fill operations, otherwise alpha value is same to stroke color. 
	 * @param width stroke width in PDF coordinate
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotPolyline( Path path, int style1, int style2, int color, int fill_color, float width )
	{
        if(path == null) return false;
		return addAnnotPolyline( hand, path.m_hand, style1, style2, color, fill_color, width );
	}
	/**
	 * add a text-markup annotation to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be only invoked after ObjsStart.<br/>
	 * this method valid in professional or premium version
	 * @param cindex1 first char index
	 * @param cindex2 second char index
	 * @param type type as following:<br/>
	 * 0: Highlight<br/>
	 * 1: Underline<br/>
	 * 2: StrikeOut<br/>
	 * 3: Highlight without round corner<br/>
	 * 4: Squiggly underline.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
	public boolean AddAnnotMarkup( int cindex1, int cindex2, int type )
	{
		int color = 0xFFFFFF00;//yellow
		if( type == 1 ) color = 0xFF0000C0;//black blue
		if( type == 2 ) color = 0xFFC00000;//black red
		if( type == 4 ) color = 0xFF00C000;//black green
		return addAnnotMarkup2( hand, cindex1, cindex2, color, type );
	}
	/**
	 * add a bitmap object as an annotation to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version, and Document.SetCache() invoked.
	 * @param bitmap Bitmap object to add, which should be formated in ARGB_8888/ARGB_4444/RGB_565
	 * @param has_alpha is need to save alpha channel information?
	 * @param rect 4 elements: left, top, right, bottom in PDF coordinate system.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotBitmap( Bitmap bitmap, boolean has_alpha, float[] rect )
	{
		return addAnnotBitmap( hand, bitmap, has_alpha, rect );
	}
	/**
	 * add a file as an attachment to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version, and Document.SetCache invoked.
	 * @param path absolute path name to the file.
	 * @param icon icon display to the page. values as:<br/>
	 * 0: PushPin<br/>
	 * 1: Graph<br/>
	 * 2: Paperclip<br/>
	 * 3: Tag<br/>
	 * @param rect 4 elements: left, top, right, bottom in PDF coordinate system.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true. 
	 */
    final public boolean AddAnnotAttachment(String path, int icon, float[] rect)
	{
		return addAnnotAttachment( hand, path, icon, rect );
	}
	/**
	 * add a sticky text annotation to page.<br/>
	 * you should re-render page to display modified data.<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in professional or premium version
	 * @param pt 2 elements: x, y in PDF coordinate system.
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotText( float[] pt )
	{
		return addAnnotText( hand, pt );
	}
	/**
	 * add an edit-box on page.<br/>
	 * the font of edit box is set by Global.setTextFont in Global.Init().<br/>
	 * this can be invoked after ObjsStart or Render or RenderToBmp.<br/>
	 * this method valid in premium version.
	 * @param mat Matrix object that passed to Render or RenderToBmp function.
	 * @param rect 4 elements: left, top, right, bottom in PDF coordinate system.
	 * @param line_clr color of border line, formated as 0xAARRGGBB.
	 * @param line_w width of border line.
	 * @param fill_clr color of background, formated as 0xAARRGGBB. AA must same to line_clr AA, or 0 means no fill color.
	 * @param tsize text size in DIB coordinate system.
	 * @param text_clr text color, formated as 0xAARRGGBB. AA must same to line_clr AA
	 * @return true or false.<br/>
	 * the added annotation can be obtained by Page.GetAnnot(Page.GetAnnotCount() - 1), if this method return true.
	 */
    final public boolean AddAnnotEditbox( Matrix mat, float[] rect, int line_clr, float line_w, int fill_clr, int text_clr, float tsize )
	{
		return addAnnotEditbox( hand, mat.hand, rect, line_clr, line_w, fill_clr, tsize, text_clr );
	}
	/**
	 * Start Reflow.<br/>
	 * this method valid in professional or premium version
	 * @param width input width, function calculate height.
	 * @param scale scale base to 72 DPI, 2.0 means 144 DPI. the reflowed text will displayed in scale
	 * @param enable_images enable reflow images.
	 * @return the height that reflow needed.
	 */
    final public float ReflowStart( float width, float scale, boolean enable_images )
	{
		return reflowStart( hand, width, scale, enable_images );
	}
	/**
	 * Reflow to dib.<br/>
	 * this method valid in professional or premium version
	 * @param dib dib to render
	 * @param orgx origin x coordinate
	 * @param orgy origin y coordinate
	 * @return true or false
	 */
    final public boolean Reflow( DIB dib, float orgx, float orgy )
	{
        if(dib == null) return false;
		return reflow( hand, dib.hand, orgx, orgy );
	}
	/**
	 * Reflow to Bitmap object.<br/>
	 * this method valid in professional or premium version
	 * @param bitmap bitmap to reflow
	 * @param orgx origin x coordinate
	 * @param orgy origin y coordinate
	 * @return true or false
	 */
    final public boolean ReflowToBmp( Bitmap bitmap, float orgx, float orgy )
	{
		return reflowToBmp( hand, bitmap, orgx, orgy );
	}
	/**
	 * get reflow paragraph count.<br/>
	 * this method valid in professional or premium version
	 * @return count
	 */
    final public int ReflowGetParaCount()
	{
		return reflowGetParaCount( hand );
	}
	/**
	 * get one paragraph's char count.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @return char count
	 */
    final public int ReflowGetCharCount( int iparagraph )
	{
		if( iparagraph < 0 || iparagraph >= reflowGetParaCount( hand ) ) return 0;
		return reflowGetCharCount( hand, iparagraph );
	}
	/**
	 * get char's font width.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @return font width for this char
	 */
    final public float ReflowGetCharWidth( int iparagraph, int ichar )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return 0;
		return reflowGetCharWidth( hand, iparagraph, ichar );
	}
	/**
	 * get char's font height.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @return font height for this char
	 */
    final public float ReflowGetCharHeight( int iparagraph, int ichar )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return 0;
		return reflowGetCharHeight( hand, iparagraph, ichar );
	}
	/**
	 * get char's fill color for display.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @return color value formatted 0xAARRGGBB, AA: alpha value, RR:red, GG:green, BB:blue
	 */
    final public int ReflowGetCharColor( int iparagraph, int ichar )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return 0;
		return reflowGetCharColor( hand, iparagraph, ichar );
	}
	/**
	 * get char's unicode value.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @return unicode
	 */
    final public int ReflowGetCharUnicode( int iparagraph, int ichar )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return 0;
		return reflowGetCharUnicode( hand, iparagraph, ichar );
	}
	/**
	 * get char's font name.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @return name string
	 */
    final public String ReflowGetCharFont( int iparagraph, int ichar )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return null;
		return reflowGetCharFont( hand, iparagraph, ichar );
	}
	/**
	 * get char's bound box.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph paragraph index range[0, ReflowGetParaCount()-1]
	 * @param ichar char index range[0, ReflowGetCharCount()]
	 * @param rect output: 4 element as [left, top, right, bottom].
	 */
    final public void ReflowGetCharRect( int iparagraph, int ichar, float rect[] )
	{
		if( ichar < 0 || ichar >= ReflowGetCharCount(iparagraph) ) return;
		reflowGetCharRect( hand, iparagraph, ichar, rect );
	}
	/**
	 * get text from range.<br/>
	 * this method valid in professional or premium version
	 * @param iparagraph1 first position
	 * @param ichar1 first position
	 * @param iparagraph2 second position
	 * @param ichar2 second position
	 * @return string value or null
	 */
	public String ReflowGetText( int iparagraph1, int ichar1, int iparagraph2, int ichar2 )
	{
		if( ichar1 < 0 || ichar1 >= ReflowGetCharCount(iparagraph1) ) return null;
		if( ichar2 < 0 || ichar1 >= ReflowGetCharCount(iparagraph2) ) return null;
		return reflowGetText( hand, iparagraph1, ichar1, iparagraph2, ichar2 );
	}
	/**
	 * add a font as resource of this page.<br/>
	 * a premium license is needed for this method.
	 * @param font font object created by Document.NewFontCID()
	 * @return ResFont or null.
	 */
	public ResFont AddResFont( DocFont font )
	{
		if( font == null ) return null;
		long ret = addResFont( hand, font.hand );
		if( ret != 0 )
		{
			ResFont fnt = new ResFont();
			fnt.hand = ret;
			return fnt; 
		}
		else return null;
	}
	/**
	 * add an image as resource of this page.<br/>
	 * a premium license is needed for this method.
	 * @param image image object created by Document.NewImage() or Document.NewImageJPEG()
	 * @return null means failed.
	 */
	public ResImage AddResImage( DocImage image )
	{
		if( image == null ) return null;
		long ret = addResImage( hand, image.hand );
		if( ret != 0 )
		{
			ResImage img = new ResImage();
			img.hand = ret;
			return img; 
		}
		else return null;
	}
	/**
	 * add GraphicState as resource of this page.<br/>
	 * a premium license is needed for this method.
	 * @param gstate ExtGraphicState created by Document.NewGState();
	 * @return null means failed.
	 */
	public ResGState AddResGState( DocGState gstate )
	{
		if( gstate == null ) return null;
		long ret = addResGState( hand, gstate.hand );
		if( ret != 0 )
		{
			ResGState gs = new ResGState();
			gs.hand = ret;
			return gs; 
		}
		else return null;
	}

    /**
     * add Form as resource of this page.<br/>
     * a premium license is needed for this method.
     * @param form Form created by Document.NewForm();
     * @return null means failed.
     */
    public ResForm AddResForm(DocForm form)
    {
        if(form == null) return null;
        long ret = addResForm(hand, form.hand);
        if(ret == 0) return null;
        ResForm res = new ResForm();
        res.hand = ret;
        return res;
    }
	/**
	 * add content stream to this page.<br/>
	 * a premium license is needed for this method.
	 * @param content PageContent object called PageContent.create().
	 * @param flush does need flush all resources?<br/>
	 * true, if you want render page after this method, or false.<br/>
	 * if false, added texts won't displayed till Document.Save() or Document.SaveAs() invoked. 
	 * @return true or false.
	 */
	public boolean AddContent( PageContent content, boolean flush )
	{
		if( content == null ) return false;
		return addContent( hand, content.hand, flush );
	}
	/**
	 * clone an annotation object to this page.<br/>
	 * this method need a professional or premium license.
	 * @param annot Annotation object must be in this document..
	 * @param rect [left, top, right, bottom] in PDF coordinate.
	 * @return true or false.
	 */
	public boolean CopyAnnot( Annotation annot, float[] rect )
	{
		return copyAnnot( hand, annot.hand, rect );
	}
    @Override
    protected void finalize() throws Throwable
    {
        Close();
        super.finalize();
    }
}
