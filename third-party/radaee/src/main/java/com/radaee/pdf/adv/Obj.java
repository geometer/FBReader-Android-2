package com.radaee.pdf.adv;

/**
 * Created by radaee on 2015/3/30.
 */
public class Obj
{
    static private native int dictGetItemCount(long hand);
    static private native String dictGetItemName(long hand, int index);
    static private native long dictGetItemByIndex(long hand, int index);
    static private native long dictGetItemByName(long hand, String name);
    static private native void dictSetItem(long hand, String name);
    static private native void dictRemoveItem(long hand, String name);
    static private native int arrayGetItemCount(long hand);
    static private native long arrayGetItem(long hand, int index);
    static private native void arrayAppendItem(long hand);
    static private native void arrayInsertItem(long hand, int index);
    static private native void arrayRemoveItem(long hand, int index);
    static private native void arrayClear(long hand);
    static private native boolean getBoolean(long hand);
    static private native void setBoolean(long hand, boolean v);
    static private native int getInt(long hand);
    static private native void setInt(long hand, int v);
    static private native float getReal(long hand);
    static private native void setReal(long hand, float v);
    static private native String getName(long hand);
    static private native void setName(long hand, String v);
    static private native String getAsciiString(long hand);
    static private native String getTextString(long hand);
    static private native byte[] getHexString(long hand);
    static private native void setAsciiString(long hand, String v);
    static private native void setTextString(long hand, String v);
    static private native void setHexString(long hand, byte[] v);
    static private native long getReference(long hand);
    static private native void setReference(long hand, long v);
    static private native int getType(long hand);
    protected long hand;
    public Obj(long handle)
    {
        hand = handle;
    }
    public long get_hand(){return hand;}
    static private final Obj adv_create_obj(long ret)
    {
        if(ret == 0) return null;
        return new Obj(ret);
    }
    static private Ref adv_create_ref(long ret)
    {
        if(ret == 0) return null;
        return new Ref(ret);
    }
    /**
     * get item count of dictionary or stream obj
     * @return count.
     */
    final public int DictGetItemCount()
    {
        return dictGetItemCount(hand);
    }

    /**
     * get item name of dictionary or stream by index
     * @param index 0 based index value.
     * @return tag of item.
     */
    final public String DictGetItemTag(int index)
    {
        return dictGetItemName(hand, index);
    }

    /**
     * get item of dictionary or stream by index
     * @param index o based index value.
     * @return PDF object data.
     */
    final public Obj DictGetItem(int index)
    {
        return adv_create_obj(dictGetItemByIndex(hand, index));
    }

    /**
     * get item of dictionary or stream by tag
     * @param tag tag.same as DictSetItem
     * @return PDF object data.
     */
    final public Obj DictGetItem(String tag)
    {
        return adv_create_obj(dictGetItemByName(hand, tag));
    }

    /**
     * set empty object to item by tag.<br/>
     * u can use DictGetItem(key) to get object, after DictSetItem.
     * @param tag tag same as DictGetItem
     */
    final public void DictSetItem(String tag)
    {
        dictSetItem(hand, tag);
    }

    /**
     * remove item by tag
     * @param tag tag of item
     */
    final public void DictRemoveItem(String tag)
    {
        dictRemoveItem(hand, tag);
    }

    /**
     * get item count of array
     * @return count.
     */
    final public int ArrayGetItemCount()
    {
        return arrayGetItemCount(hand);
    }

    /**
     * get item of array by index.
     * @param index 0 based index.
     * @return PDF object data.
     */
    final public Obj ArrayGetItem(int index)
    {
        return adv_create_obj(arrayGetItem(hand, index));
    }

    /**
     * add an empty object to tail of array.<br/>
     * u can use ArrayGetItem(ArrayGetItemCount() - 1) to get Object after ArrayAppendItem()
     */
    final public void ArrayAppendItem()
    {
        arrayAppendItem(hand);
    }

    /**
     * insert an empty object to array by position.<br/>
     * u can use ArrayGetItem(index) to get Object after ArrayInsertItem()
     * @param index 0 based index
     */
    final public void ArrayInsertItem(int index)
    {
        arrayInsertItem(hand, index);
    }

    /**
     * remove an item from array
     * @param index 0 based index.
     */
    final public void ArrayRemoveItem(int index)
    {
        arrayRemoveItem(hand, index);
    }

    /**
     * remove all items from array.
     */
    final public void ArrayClear()
    {
        arrayClear(hand);
    }

    /**
     * get boolean value from object.
     * @return boolean value.
     */
    final public boolean GetBoolean()
    {
        return getBoolean(hand);
    }

    /**
     * set boolean value to object, and set object type to boolean.
     * @param v boolean value
     */
    final public void SetBoolean(boolean v)
    {
        setBoolean(hand, v);
    }
    /**
     * get int value from object.
     * @return int value.
     */
    final public int GetInt()
    {
        return getInt(hand);
    }
    /**
     * set int value to object, and set object type to int.
     * @param v int value
     */
    final public void SetInt(int v)
    {
        setInt(hand, v);
    }
    /**
     * get float value from object.
     * @return float value.
     */
    final public float GetReal()
    {
        return getReal(hand);
    }
    /**
     * set float value to object, and set object type to float.
     * @param v float value
     */
    final public void SetReal(float v)
    {
        setReal(hand, v);
    }
    /**
     * get name value from object.
     * @return name value.
     */
    final public String GetName()
    {
        return getName(hand);
    }
    /**
     * set name value to object, and set object type to name.
     * @param v name value
     */
    final public void SetName(String v)
    {
        setName(hand, v);
    }
    /**
     * get string value from object.<br/>
     * @return ascii string value.
     */
    final public String GetAsciiString()
    {
        return getAsciiString(hand);
    }
    /**
     * get string value from object.<br/>
     * @return Unicode string value.
     */
    final public String GetTextString()
    {
        return getTextString(hand);
    }
    /**
     * get string value from object.<br/>
     * @return binary string value.
     */
    final public byte[] GetHexString()
    {
        return getHexString(hand);
    }
    /**
     * set ascii string value to object, and set object type to string.
     * @param v ascii string value
     */
    final public void SetAsciiString(String v)
    {
        setAsciiString(hand, v);
    }
    /**
     * set unicode string value to object, and set object type to string.
     * @param v unicode string value
     */
    final public void SetTextString(String v)
    {
        setTextString(hand, v);
    }
    /**
     * set binary string value to object, and set object type to string.
     * @param v binary string value
     */
    final public void SetHexString(byte[] v)
    {
        setHexString(hand, v);
    }

    /**
     * get cross reference from object.
     * @return cross reference item.
     */
    final public Ref GetReference()
    {
        return adv_create_ref(getReference(hand));
    }
    /**
     * set cross reference to object, and set object type to reference.
     * @param v cross reference
     */
    final public void SetReference(Ref v)
    {
        setReference(hand, v.hand);
    }

    /**
     * set object type to dictionary.
     * and empty dictionary data created.
     */
    final public void SetDict()
    {
        dictGetItemCount(hand);
    }

    /**
     * set object type to array.
     * and empty array created.
     */
    final public void SetArray()
    {
        arrayClear(hand);
    }

    /**
     * get type of object
     * @return object type as following:
     * null = 0,<br/>
     * boolean = 1,<br/>
     * int = 2,<br/>
     * real = 3,<br/>
     * string = 4,<br/>
     * name = 5,<br/>
     * array = 6,<br/>
     * dictionary = 7,<br/>
     * reference = 8,<br/>
     * stream = 9,<br/>
     * others unknown.,
     */
    final public int GetType()
    {
        return getType(hand);
    }
}
