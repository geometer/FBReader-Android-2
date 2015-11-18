package com.radaee.pdf;

/**
 * class for Path
 * @author radaee<br/>
 * Examples:
 *   Path path = new Path();<br/>
 *   path.MoveTo(0, 0);<br/>
 *   path.LineTo(10, 10);<br/>
 *   path.CurveTo(100, 0, 100, 100, 30, 70);<br/>
 *   path.ClosePath();<br/>
 *   int cnt = path.GetNodeCount();<br/>
 *   int cur = 0;<br/>
 *   float point[] = new float[2];<br/>
 *   while( cur < cnt )<br/>
 *   {<br/>
 *   	path.GetNode(cur, point);<br/>
 *   	cur++;<br/>
 *   }<br/>
 *   Page page = m_doc.GetPage(0);<br/>
 *   page.ObjsStart();<br/>
 *   Matrix mat = new Matrix( 1, 0, 0, -1, 0, m_doc.GetPageHeight(0) );<br/>
 *   page.AddAnnotGlyph(mat, path, 0xFF0000, true);<br/>
 *   page.Close();<br/>
 *   path.Destroy();<br/>
 */
public class Path
{
	private static native long create();
	private static native void destroy( long hand );
	private static native void moveTo( long hand, float x, float y );
	private static native void lineTo( long hand, float x, float y );
	private static native void curveTo( long hand, float x1, float y1, float x2, float y2, float x3, float y3 );
	private static native void closePath( long hand );
	private static native int getNodeCount(long hand);
	private static native int getNode( long hand, int index, float[]pt );
	protected long m_hand = create();
	/**
	 * move to operation
	 * @param x
	 * @param y
	 */
	public final void MoveTo( float x, float y )
	{
		moveTo( m_hand, x, y );
	}
	/**
	 * line to operation
	 * @param x
	 * @param y
	 */
	public final void LineTo( float x, float y )
	{
		lineTo( m_hand, x, y );
	}
	public final void CurveTo( float x1, float y1, float x2, float y2, float x3, float y3 )
	{
		curveTo( m_hand, x1, y1, x2, y2, x3, y3 );
	}
	/**
	 * close a contour.
	 */
	public final void ClosePath()
	{
		closePath(m_hand);
	}
	/**
	 * free memory
	 */
	public final void Destroy()
	{
		destroy(m_hand);
		m_hand = 0;
	}
	public final int GetNodeCount()
	{
		return getNodeCount( m_hand );
	}
	/**
	 * get each node
	 * @param index range [0, GetNodeCount() - 1]
	 * @param pt output value: 2 elements coordinate point
	 * @return node type:<br/>
	 * 0: move to<br/>
	 * 1: line to<br/>
	 * 3: curve to, index, index + 1, index + 2 are all data<br/>
	 * 4: close operation<br/>
	 */
	public final int GetNode( int index, float pt[] )
	{
		return getNode( m_hand, index, pt );
	}
    @Override
    protected void finalize() throws Throwable
    {
        Destroy();
        super.finalize();
    }
}
