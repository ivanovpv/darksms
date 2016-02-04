package ru.ivanovpv.gorets.psm.cipher;
//import java.nio.*;

import android.util.Log;
import ru.ivanovpv.gorets.psm.nativelib.NativeRandom;

import java.io.*;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 457 $
 *   $LastChangedDate: 2013-12-25 13:20:31 +0400 (Ср, 25 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/ByteUtils.java $
 */

public class ByteUtils
{
    private static final String TAG=ByteUtils.class.getName();
    //public static final String ENCODING="ISO-8859-1";
    public static final String ENCODING="UTF-8";
    //public static Randomer RANDOM=new R250RNG();
    private static final String HEX="0123456789ABCDEF";
    String s;
    StringBuilder sb;

    public static byte[] longToByteArray(long value)
    {
/*        byte[] b = new byte[8];
        b[7] = (byte) (n);
        n >>>= 8;
        b[6] = (byte) (n);
        n >>>= 8;
        b[5] = (byte) (n);
        n >>>= 8;
        b[4] = (byte) (n);
        n >>>= 8;
        b[3] = (byte) (n);
        n >>>= 8;
        b[2] = (byte) (n);
        n >>>= 8;
        b[1] = (byte) (n);
        n >>>= 8;
        b[0] = (byte) (n);

        return b;*/
        return new byte[]
                {
                        (byte) (value >>> 56),
                        (byte) (value >>> 48),
                        (byte) (value >>> 40),
                        (byte) (value >>> 32),
                        (byte) (value >>> 24),
                        (byte) (value >>> 16),
                        (byte) (value >>> 8),
                        (byte) value
                };

    }

    public static long byteArrayToLong(byte[] buffer, int offset)
    {
        /*      long val = 0;
       for ( int i=0; i<8; i++ )
       {
           int shift = 7-i;
           val |= (buffer[offset+i]<<shift);
       }
       return val;*/
        return (buffer[offset] << 56)
                + ((buffer[offset + 1] & 0xFF) << 48)
                + ((buffer[offset + 2] & 0xFF) << 40)
                + ((buffer[offset + 3] & 0xFF) << 32)
                + ((buffer[offset + 4] & 0xFF) << 24)
                + ((buffer[offset + 5] & 0xFF) << 16)
                + ((buffer[offset + 6] & 0xFF) << 8)
                + (buffer[offset + 7] & 0xFF);
    }

    public static int[] byteArrayToIntArray(byte[] buffer) {
        int[] buf=new int[buffer.length/4];
        for(int i=0; i < buf.length; i++)
            buf[i]=byteArrayToInt(buffer, i*4);
        return buf;
    }

    public static byte[] intArrayToByteArray(int[] buffer) {
        byte[] buf=new byte[buffer.length*4];
        byte[] bf;
        for(int i=0; i < buffer.length; i++) {
            bf = intToByteArray(buffer[i]);
            System.arraycopy(bf, 0, buf, i*4, 4);
        }
        return buf;
    }

    public static int byteArrayToInt(byte[] buffer, int offset) {
        return (buffer[offset] << 24)
                + ((buffer[offset + 1] & 0xFF) << 16)
                + ((buffer[offset + 2] & 0xFF) << 8)
                + (buffer[offset + 3] & 0xFF);
    }

    public static int byteArrayToShort(byte[] buffer, int offset) {
        return (buffer[offset] << 8)
                + (buffer[offset + 1] & 0xFF);
    }

    public static int byteArrayTo3ByteInt(byte[] buffer, int offset) {
        return (buffer[offset] << 16)
                + ((buffer[offset + 1] & 0xFF) << 8)
                + (buffer[offset + 2] & 0xFF);
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]
                {
                        (byte) (value >>> 24),
                        (byte) (value >>> 16),
                        (byte) (value >>> 8),
                        (byte) value
                };
    }

/*
    public static byte[] longToByteArray(long l)
    {
        byte[] bArray = new byte[8];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        LongBuffer lBuffer = bBuffer.asLongBuffer();
        lBuffer.put(0, l);
        return bArray;
    }

    public static long byteArrayToLong(byte[] buffer, int offset)
    {
        ByteBuffer bBuffer = ByteBuffer.wrap(buffer, offset, 8);
        return bBuffer.getLong();
    }

    public static byte[] intToByteArray(int i)
    {
        byte[] bArray = new byte[4];
        ByteBuffer bBuffer = ByteBuffer.wrap(bArray);
        IntBuffer iBuffer = bBuffer.asIntBuffer();
        iBuffer.put(0, i);
        return bArray;
    }
 */

    public static byte[] stringToByteArray(String s)
    {
        try
        {
            return s.getBytes(ENCODING);
        }
        catch(UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    public static byte[] charArrayToByteArray(char[] buffer)
    {
        try
        {
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            OutputStreamWriter out=new OutputStreamWriter(baos, ENCODING);
            Reader reader=new CharArrayReader(buffer);
            for(int ch; (ch=reader.read()) != -1; )
            {
                out.write(ch);
            }
            return baos.toByteArray();
        }
        catch(Exception ex)
        {
            return null;
        }
    }

    public static String byteArrayToString(byte[] buf, int offset) {
        if(buf==null)
            return null;
        try
        {
            return new String(buf, offset, buf.length - offset, ENCODING);
        }
        catch(UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    /**
     * Convenience method to convert hex string tp byte array
     *
     * @param hexString hex string to convert
     * @return byte[] array converted byte[]
     */
    public static byte[] hexToBytes(String hexString)
    {
        byte[] out=new byte[hexString.length() / 2];

        int n=hexString.length();

        for(int i=0; i < n; i+=2)
        {
            //make a bit representation in an int of the hex value
            int hn=HEX.indexOf(hexString.charAt(i));
            int ln=HEX.indexOf(hexString.charAt(i + 1));
            if(hn < 0 || ln < 0)
                return null;
            //now just shift the high order nibble and add them together
            out[i / 2]=(byte) ((hn << 4) | ln);
        }
        return out;
    }

    public static byte hexToByte(String hexString)
    {
        byte out;
        //make a bit representation in an int of the hex value
        int hn=HEX.indexOf(hexString.charAt(0));
        int ln=HEX.indexOf(hexString.charAt(1));
        //now just shift the high order nibble and add them together
        out=(byte) ((hn << 4) | ln);
        return out;
    }

    /**
     * Convenience method to convert a byte to a hex string.
     *
     * @param b the byte to convert
     * @return String the converted byte
     */
    public static String byteToHex(byte b)
    {
        char[] val=new char[2];
        int i=b & 0xff;
        val[0]=HEX.charAt(b >>> 4);
        val[1]=HEX.charAt(b & 15);
        return String.valueOf(val);
    }


    /**
     * Convenience method to convert a byte array to a hex string.
     *
     * @param array the byte[] to convert
     * @return String the converted byte[]
     */
    public static String bytesToHex(byte[] array)
    {
        if(array==null)
            return "";
        char[] val=new char[2 * array.length];
        for(int i=0; i < array.length; i++)
        {
            int b=array[i] & 0xff;
            val[2 * i]=HEX.charAt(b >>> 4);
            val[2 * i + 1]=HEX.charAt(b & 15);
        }
        return String.valueOf(val);
    }

    /**
     * URL safe version of Base64 encoder
     * @param s string to encode
     * @return encode string
     */
    public static String stringToBase64(String s)
    {
        byte[] buffer;
        try
        {
            buffer = s.getBytes(ENCODING);
            return Base64.encodeBytes(buffer, Base64.URL_SAFE);
        }
        catch (Exception e)
        {
            Log.d(TAG, "Error during Base64 encoding", e);
            return s;
        }
    }

    /**
     * URL safe version of Base64 encoder
     * @param buffer buffer to encode
     * @return encode string
     */
    public static String bytesToBase64(byte[] buffer)
    {
        try
        {
            return Base64.encodeBytes(buffer, Base64.URL_SAFE);
        }
        catch (Exception e)
        {
            Log.d(TAG, "Error during Base64 encoding", e);
            return null;
        }
    }

    /**
     * URL safe version of Base64 decoder
     * @param s string to decode
     * @return decoded string
     */
    public static String base64ToString(String s)
    {
        byte[] buffer;
        try
        {
            if(s==null || s.trim().length() < 4)   //not base64 string
                return s;
            buffer = Base64.decode(s, Base64.URL_SAFE);
            return new String(buffer, ENCODING);
        }
        catch (Exception e)
        {
            Log.d(TAG, "Error during Base64 decoding", e);
            return s;
        }
    }

    /**
     * URL safe version of Base64 decoder
     * @param s string to decode
     * @return decoded string
     */
    public static byte[] base64ToBytes(String s)
    {
        byte[] buffer;
        try
        {
            if(s==null || s.trim().length() < 4)   //not base64 string
                return null;
            buffer = Base64.decode(s, Base64.URL_SAFE);
            return buffer;
        }
        catch (Exception e)
        {
            Log.d(TAG, "Error during Base64 decoding", e);
            return null;
        }
    }

}
