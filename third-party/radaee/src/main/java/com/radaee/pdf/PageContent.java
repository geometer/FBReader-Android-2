package com.radaee.pdf;

import com.radaee.pdf.Page.*;

public class PageContent
{
	static private native long create();
	static private native void gsSave(long hand);
	static private native void gsRestore(long hand);
	static private native void gsSetMatrix(long hand, long mat);
	static private native void gsSet( long hand, long gs );
	static private native void drawImage(long hand, long image);
    static private native void drawForm(long hand, long form);

    static private native void fillPath(long hand, long path, boolean winding);
	static private native void clipPath(long hand, long path, boolean winding);
	static private native void strokePath(long hand, long path);
	static private native void setFillColor(long hand, int color);
	static private native void setStrokeColor(long hand, int color);
	static private native void setStrokeCap(long hand, int cap);
	static private native void setStrokeJoin(long hand, int join);
	static private native void setStrokeWidth(long hand, float w);
	static private native void setStrokeMiter(long hand, float miter);
	
	static private native void drawText( long hand, String text );
	static private native void textBegin(long hand);
	static private native void textEnd(long hand);
	static private native void textSetCharSpace(long hand, float space);
	static private native void textSetWordSpace(long hand, float space);
	static private native void textSetLeading(long hand, float leading);
	static private native void textSetRise(long hand, float rise);
	static private native void textSetHScale(long hand, int scale);
	static private native void textNextLine(long hand);
	static private native void textMove(long hand, float x, float y);
	static private native void textSetRenderMode(long hand, int mode);
	static private native void textSetFont(long hand, long font, float size);
	static private native float[] textGetSize( long hand, long font, String text, float width, float height, float char_space, float word_space );
	static private native void destroy(long hand);
	protected long hand = 0;
	/**
	 * create it.
	 */
    final public void Create()
	{
		hand = create();
	}
	/**
	 * destroy and free memory.
	 */
    final public void Destroy()
	{
		destroy(hand);
		hand = 0;
	}
	/**
	 * PDF operator: gs_save, save current GraphicState
	 */
    final public void GSSave()
	{
		gsSave(hand);
	}
	/**
	 * PDF operator: gs_restore, restore GraphicState
	 */
    final public void GSRestore()
	{
		gsRestore(hand);
	}
	/**
	 * PDF operator: set matrix.
	 * @param mat Matrix object
	 */
    final public void GSSetMatrix(Matrix mat)
	{
		gsSetMatrix(hand, mat.hand);
	}
	/**
	 * PDF operator: set ExtGraphicState
	 * @param gs ResGState object created by Page.AddResGState() or Form.AddResGState()
	 */
    final public void GSSet( ResGState gs )
	{
		if( gs != null )
			gsSet(hand, gs.hand);
	}
	/**
	 * PDF operator: show image.
	 * @param image image object created by Page.AddResImage() or Form.AddResImage()
	 */
    final public void DrawImage(ResImage image)
	{
        if(image != null)
		    drawImage(hand, image.hand);
	}

    /**
     * PDF operator: show form.
     * @param frm Form object created by Page.AddResForm() or Form.AddResForm()
     */
    final public void DrawForm(ResForm frm)
    {
        if(frm != null)
            drawForm(hand, frm.hand);
    }
	/**
	 * fill path.
	 * @param path Path object
	 * @param winding winding fill rule?
	 */
    final public void FillPath(Path path, boolean winding)
	{
        if(path != null)
		    fillPath(hand, path.m_hand, winding);
	}
	/**
	 * set the path as clip path.
	 * @param path Path object
	 * @param winding winding fill rule?
	 */
    final public void ClipPath(Path path, boolean winding)
	{
        if(path != null)
    		clipPath(hand, path.m_hand, winding);
	}
	/**
	 * stroke path.
	 * @param path Path object
	 */
    final public void StrokePath(Path path)
	{
        if(path != null)
		    strokePath(hand, path.m_hand);
	}
	/**
	 * PDF operator: set fill and other operations color.
	 * @param color formatted as 0xRRGGBB, no alpha channel. alpha value shall set by ExtGraphicState(ResGState).
	 */
    final public void SetFillColor(int color)
	{
		setFillColor(hand, color);
	}
	/**
	 * PDF operator: set stroke color.
	 * @param color formatted as 0xRRGGBB, no alpha channel. alpha value shall set by ExtGraphicState(ResGState).
	 */
    final public void SetStrokeColor(int color)
	{
		setStrokeColor(hand, color);
	}
	/**
	 * PDF operator: set line cap
	 * @param cap 0:butt, 1:round: 2:square
	 */
    final public void SetStrokeCap(int cap)
	{
		setStrokeCap(hand, cap);
	}
	/**
	 * PDF operator: set line join
	 * @param join 0:miter, 1:round, 2:bevel
	 */
    final public void SetStrokeJoin(int join)
	{
		setStrokeJoin(hand, join);
	}
	/**
	 * PDF operator: set line width
	 * @param w line width in PDF coordinate
	 */
    final public void SetStrokeWidth(float w)
	{
		setStrokeWidth(hand, w);
	}
	/**
	 * PDF operator: set miter limit.
	 * @param miter miter limit.
	 */
    final public void SetStrokeMiter(float miter)
	{
		setStrokeMiter(hand, miter);
	}
	/**
	 * show text
	 * @param text text to show, '\r' or '\n' in string start a new line.
	 */
    final public void DrawText(String text )
	{
		drawText(hand, text);
	}
	/**
	 * PDF operator: begin text and set text position to (0,0).
	 */
    final public void TextBegin()
	{
		textBegin(hand);
	}
	/**
	 * PDF operator: text end.
	 */
    final public void TextEnd()
	{
		textEnd(hand);
	}
	/**
	 * PDF operator: set char space(extra space between chars).
	 * @param space char space
	 */
    final public void TextSetCharSpace(float space)
	{
		textSetCharSpace(hand, space);
	}
	/**
	 * PDF operator: set word space(extra space between words spit by blank char ' ' ).
	 * @param space word space.
	 */
    final public void TextSetWordSpace(float space)
	{
		textSetWordSpace(hand, space);
	}
	/**
	 * PDF operator: set text leading, height between 2 text lines.
	 * @param leading leading in PDF coordinate
	 */
    final public void TextSetLeading(float leading)
	{
		textSetLeading(hand, leading);
	}
	/**
	 * PDF operator: set text rise
	 * @param rise
	 */
    final public void TextSetRise(float rise)
	{
		textSetRise(hand, rise);
	}
	/**
	 * PDF operator: set horizon scale for chars.
	 * @param scale 100 means scale value 1.0f
	 */
    final public void TextSetHScale(int scale)
	{
		textSetHScale(hand, scale);
	}
	/**
	 * PDF operator: new a text line
	 */
    final public void TextNextLine()
	{
		textNextLine(hand);
	}
	/**
	 * PDF operator: move text position relative to previous line
	 * @param x in PDF coordinate add to previous line position
	 * @param y in PDF coordinate add to previous line position
	 */
    final public void TextMove(float x, float y)
	{
		textMove( hand, x, y );
	}
	/**
	 * PDF operator: set text render mode.
	 * @param mode <br/>
	 * 0: filling<br/>
	 * 1: stroke<br/>
	 * 2: fill and stroke<br/>
	 * 3: do nothing<br/>
	 * 4: fill and set clip path<br/>
	 * 5: stroke and set clip path<br/>
	 * 6: fill/stroke/clip<br/>
	 * 7: set clip path.
	 */
    final public void TextSetRenderMode(int mode)
	{
		textSetRenderMode(hand, mode);
	}
	/**
	 * set text font
	 * @param font ResFont object created by Page.AddResFont() or Form.AddResFont()
	 * @param size text size in PDF coordinate.
	 */
    final public void TextSetFont(ResFont font, float size)
	{
        if(font != null)
		    textSetFont(hand, font.hand, size);
	}
	/**
	 * reserved.
	 * @param font
	 * @param text test to display, may container '\r' or '\n'
	 * @param width font width
	 * @param height font height
	 * @param char_space
	 * @param word_space
	 * @return
	 */
    final public float[] TextGetSize(ResFont font, String text, float width, float height, float char_space, float word_space )
	{
        if(font == null) return null;
		return textGetSize(hand, font.hand, text, width, height, char_space, word_space);
	}
    @Override
    protected void finalize() throws Throwable
    {
        Destroy();
        super.finalize();
    }
}
