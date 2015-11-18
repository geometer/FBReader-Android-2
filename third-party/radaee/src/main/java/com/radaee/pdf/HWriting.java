package com.radaee.pdf;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

/**
class for hand-writing ink.<br/>
not same to Ink class, this class has max line width and min line width.<br/>
so the line in HWriting will not in same width.
@author Radaee
@version 1.1
*/
public class HWriting
{
	protected long hand = 0;
	private Bitmap m_bmp;
	private static native int create( int w, int h, float min_w, float max_w, int clr_r, int clr_g, int clr_b );
	private static native void onDown( long hand, float x, float y );
	private static native void onMove( long hand, float x, float y );
	private static native void onUp( long hand, float x, float y );
	private static native void onDraw( long hand, long bmp );
	private static native void destroy( long hand );
	/**
	 * constructor for hand-writing.
	 * @param w width of cache.
	 * @param h height of cache.
	 * @param min_w min-width for ink width.
	 * @param max_w max-width for ink width.
	 * @param clr_r r values of ink color [0-255]
	 * @param clr_g g values of ink color [0-255]
	 * @param clr_b b values of ink color [0-255]
	 */
	public HWriting( int w, int h, float min_w, float max_w, int clr_r, int clr_g, int clr_b )
	{
		hand = create( w, h, min_w, max_w, clr_r, clr_g, clr_b );
		m_bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
	}
	/**
	 * destroy and free memory.
	 */
	public void Destroy()
	{
		destroy( hand );
		hand = 0;
        if(m_bmp != null) {
            m_bmp.recycle();
            m_bmp = null;
        }
	}
	/**
	 * call when click down
	 * @param x x value of point in this object.
	 * @param y y value of point in this object.
	 */
	public void OnDown( float x, float y )
	{
		onDown( hand, x, y );
	}
	/**
	 * call when moving
	 * @param x x value of point in this object.
	 * @param y y value of point in this object.
	 */
	public void OnMove( float x, float y )
	{
		onMove( hand, x, y );
	}
	/**
	 * call when click up
	 * @param x x value of point in this object.
	 * @param y y value of point in this object.
	 */
	public void OnUp( float x, float y )
	{
		onUp( hand, x, y );
	}
	/**
	 * draw to locked bitmap handle.
	 * @param bmp, obtained by Global.lockBitmap()
	 */
	public void OnDraw( BMP bmp )
	{
		onDraw( hand, bmp.hand );
	}
    @Override
    protected void finalize() throws Throwable
    {
        Destroy();
        super.finalize();
    }
}
