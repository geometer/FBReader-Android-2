package com.radaee.pdf;

import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class DIB
{
	/**
	 * create or resize dib, and reset all pixels in dib.<br/>
	 * if dib is 0, function create a new dib object.<br/>
	 * otherwise function resize the dib object.
	 */
	private static native long get(long dib, int width, int height);
	/**
	 * draw a dib to another dib
	 * @param dib
	 * @param dst_dib
	 * @param x
	 * @param y
	 */
	private static native void drawToDIB( long dib, long dst_dib, int x, int y );
	/**
	 * draw dib to bmp.
	 * 
	 * @param bmp
	 *            handle value, that returned by lockBitmap.
	 * @param dib
	 * @param x
	 *            origin position in bmp.
	 * @param y
	 *            origin position in bmp.
	 */
	private static native void drawToBmp(long dib, long bmp, int x, int y);
	/**
	 * draw dib to bmp, with scale
	 * @param bmp
	 * @param dib
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	private static native void drawToBmp2(long dib, long bmp, int x, int y, int w, int h);
	private static native void drawRect(long dib, int color, int x, int y, int width, int height, int mode);
    private static native int glGenTexture(long dib, boolean linear);
    private static native boolean saveRaw( long bmp, String path );
    private static native long restoreRaw( long bmp, String path, int[] info );
	/**
	 * free dib object.
	 */
	private static native int free(long dib);
	protected long hand = 0;
    private int m_w,m_h;
	public final boolean IsEmpty(){return hand == 0;}
	public final void CreateOrResize(int w, int h)
	{
		hand = get(hand, w, h);
        m_w = w;
        m_h = h;
	}
	public final void DrawToDIB(DIB dst, int x, int y)
	{
        if(dst == null) return;
		drawToDIB(hand, dst.hand, x, y);
	}
	public final void DrawToBmp(BMP bmp, int x, int y)
	{
        if(bmp == null) return;
		drawToBmp(hand, bmp.hand, x, y);
	}
	/**
	 * draw dib to bmp, with scale
	 * @param bmp
	 * @param x
	 * @param y
	 * @param w
	 * @param h
	 */
	public final void DrawToBmp2(BMP bmp, int x, int y, int w, int h)
	{
        if(bmp == null) return;
		drawToBmp2(hand, bmp.hand, x, y, w, h);
	}
	public final void DrawRect(int color, int x, int y, int width, int height, int mode)
	{
		drawRect(hand, color, x, y, width, height, mode);
	}
    public final int GLGenTexture(boolean linear)
    {
        return glGenTexture(hand, linear);
    }
    public final int GLGenTexture(int x, int y, int w, int h, boolean linear)
    {
        return glGenTexture(hand, linear);
    }
    static private FloatBuffer buffer_create(float[] val)
    {
        if(val == null) return null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(val.length << 2);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer fbuf = buffer.asFloatBuffer();
        fbuf.put(val);
        fbuf.position(0);
        return fbuf;
    }
    static private ShortBuffer buffer_create(short[] val)
    {
        if(val == null) return null;
        ByteBuffer buffer = ByteBuffer.allocateDirect(val.length << 2);
        buffer.order(ByteOrder.nativeOrder());
        ShortBuffer sbuf = buffer.asShortBuffer();
        sbuf.put(val);
        sbuf.position(0);
        return sbuf;
    }
    static private FloatBuffer m_texture = buffer_create(new float[]{0, 0, 1, 0, 0, 1, 1, 1});
    private float m_vert[] = new float[8];
    public final boolean GLDraw(GL10 gl, int x, int y)
    {
        return GLDraw(gl, x, y, m_w, m_h);
    }

    public final boolean GLDraw(GL10 gl, int x, int y, int w, int h)
    {
        int text_id = glGenTexture(hand, true);
        if(text_id == -1) return false;
        m_vert[0] = x;
        m_vert[1] = y;
        m_vert[2] = x + w;
        m_vert[3] = y;
        m_vert[4] = x;
        m_vert[5] = y + h;
        m_vert[6] = x + w;
        m_vert[7] = y + h;
        FloatBuffer vertices = buffer_create(m_vert);
        //绑定纹理ID
        gl.glBindTexture(GL10.GL_TEXTURE_2D, text_id);
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, vertices);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, m_texture);
        // gl.glRotatef(1, 0, 1, 0);
        //gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, 6, GL10.GL_UNSIGNED_SHORT, m_indecs);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, 0);
        int tmp[] = new int[1];
        tmp[0] = text_id;
        gl.glDeleteTextures(1, IntBuffer.wrap(tmp));
        return true;
    }
	public final void Free()
	{
		free(hand);
		hand = 0;
	}
    /**
     * save pixels data to file. saved as RGBA_8888 format.
     * @param path path-name to the file.
     * @return true or false
     */
    public final boolean SavePixs(String path)
    {
        return saveRaw(hand, path);
    }

    /**
     * restore pixels data from file. must be RGBA_8888 format.
     * @param path path-name to the file
     * @return true or false. pixels format of pixels must match to DIB object, otherwise return false.
     */
    public final boolean RestorePixs(String path)
    {
        int info[] = new int[2];
        long tmp = restoreRaw(hand, path, info);
        if(info[0] > 0 && info[1] > 0)
        {
            m_w = info[0];
            m_h = info[1];
            hand = tmp;
            return true;
        }
        else
            return false;
    }
    @Override
    protected void finalize() throws Throwable
    {
        Free();
        super.finalize();
    }
}
