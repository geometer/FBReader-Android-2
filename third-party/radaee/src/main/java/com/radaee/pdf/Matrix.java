package com.radaee.pdf;

/**
class for PDF Matrix.
@author Radaee
@version 1.1
*/
public class Matrix
{
	protected long hand = 0;
	private static native long create( float xx, float yx, float xy, float yy, float x0, float y0 );
	private static native long createScale( float sx, float sy, float x0, float y0 );
	private static native void invert( long matrix );
	private static native void transformPath( long matrix, long path );
	private static native void transformInk( long matrix, long ink );
	private static native void transformRect( long matrix, float[] rect );
	private static native void transformPoint( long matrix, float[] point );
	private static native void destroy( long matrix );
	/**
	 * constructor for full values.<br/>
     * transform formula like:<br/>
     * new_x = (xx * x + xy * y) + x0;<br/>
     * new_y = (yx * x + yy * y) + y0;<br/>
     * for composed with rotate and scale, values like:<br/>
     * xx = scalex * cos(a);<br/>
     * yx = scaley * sin(a);<br/>
     * xy = scalex * sin(-a);<br/>
     * yy = scaley * cos(-a);<br/>
     * where a is rotate angle in radian.<br/>
     * @param xx
	 * @param yx
	 * @param xy
	 * @param yy
	 * @param x0 offset add to x
	 * @param y0 offset add to y
	 */
	public Matrix( float xx, float yx, float xy, float yy, float x0, float y0 )
	{
		hand = create( xx, yx, xy, yy, x0, y0 );
	}
	/**
	 * constructor for scaled values.<br/>
	 * xx = sx;<br/>
	 * yx = 0;<br/>
	 * xy = 0;<br/>
	 * yx = sy;<br/>
     * transform formula like:<br/>
     * new_x = (sx * x) + x0;<br/>
     * new_y = (sy * y) + y0;<br/>
     * because PDF using math coordinate system. (0,0) at left-bottom<br/>
     * and Bitmap is in screen coordinate. (0,0) at left-top<br/>
     * so, Matrix need to map y as inverted. and matrix mostly like:<br/>
     * sx = scale, sy = -scale, x0 = 0, y0 = scale * page_height<br/>
     * where page_height getting from Document.GetPageHeight(pageNo);
	 * @param sx
	 * @param sy
	 * @param x0 offset add to x
	 * @param y0 offset add to y
	 */
	public Matrix( float sx, float sy, float x0, float y0 )
	{
		hand = createScale( sx, sy, x0, y0 );
	}
	public final void Invert()
	{
		invert( hand );
	}
	public final void TransformPath( Path path )
	{
        if(path == null) return;
		transformPath( hand, path.m_hand );
	}
	public final void TransformInk( Ink ink )
	{
        if(ink == null) return;
		transformInk( hand, ink.hand );
	}
	public final void TransformRect( float[] rect )
	{
		transformRect( hand, rect );
	}
	public final void TransformPoint( float[] point )
	{
		transformPoint( hand, point );
	}
	/**
	 * destroy and free memory.
	 */
	public final void Destroy()
	{
		destroy( hand );
		hand = 0;
	}
    @Override
    protected void finalize() throws Throwable
    {
        Destroy();
        super.finalize();
    }
}
