package ru.ivanovpv.gorets.psm.cipher;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 457 $
 *   $LastChangedDate: 2013-12-25 13:20:31 +0400 (Ср, 25 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/Cipher.java $
 */

/**
 * Abstract wrapper over any block cipher
 */
public abstract class Cipher
{
    private static final String TAG=Cipher.class.getName();
    public final static int DIGEST_SHA1=0;
    public final static int DIGEST_WHIRLPOOL=1;
    public final static int DIGEST_SHA512=2;
    public final static char CIPHER_EMPTY=' ';
    public final static char CIPHER_DES='A';
    public final static char CIPHER_RIJNDAEL='B';
    public final static char CIPHER_ANUBIS='C';

    public abstract byte[] encryptBlock(byte[] block);
    public abstract byte[] decryptBlock(byte[] block);
    public abstract void clean();
    public abstract char getType();
    abstract protected int getBlockSize();
    abstract protected byte[] roundBuffer(byte[] buffer);

    protected Cipher()
    {
    }

    public byte[] encryptBuffer(byte[] buffer)
    {
        if(buffer.length > 1024*1024) //1 meg
            throw new IllegalArgumentException("Buffer is too large to encrypt");
        //generate random lask
        byte[] mask=generateInitVector();
        byte[] maskOriginal=new byte[mask.length];
        System.arraycopy(mask, 0, maskOriginal, 0, mask.length);
        int size=buffer.length;
        byte[] sizes=ByteUtils.intToByteArray(size);
        byte[] in=new byte[sizes.length + buffer.length];
        //writing original size of input buffer
        System.arraycopy(sizes, 0, in, 0, sizes.length);
        //buffer itself
        System.arraycopy(buffer, 0, in, sizes.length, buffer.length);
        //encrypt with CBC
        byte[] out=encryptCBC(in, mask);
        buffer=new byte[out.length + maskOriginal.length];
        //writing mask
        System.arraycopy(maskOriginal, 0, buffer, 0, maskOriginal.length);
        //writing encrypted buffer
        System.arraycopy(out, 0, buffer, mask.length, out.length);
        return buffer;
    }

    public byte[] decryptBuffer(byte[] buffer) throws CipherException
    {
        byte[] mask=new byte[this.getBlockSize()];
        if(buffer.length < mask.length)
            throw new CipherException("Trying to decrypt non-encrypted buffer");
        byte[] in=new byte[buffer.length - mask.length];
        //extract mask
        System.arraycopy(buffer, 0, mask, 0, mask.length);
        //extract encrypted buffer
        System.arraycopy(buffer, mask.length, in, 0, in.length);
        //getDecrypted
        byte[] out=decryptCBC(in, mask);
        //define size of original buffer
        int size=ByteUtils.byteArrayToInt(out, 0);
        if(size <= 0 || size > (out.length-4))
            throw new CipherException("Invalid buffer to decrypt");
        buffer=new byte[size];
        //remove size info
        System.arraycopy(out, 4, buffer, 0, size);
        return buffer;
    }

    /**
     * Encrypts given byte array and CBC's in accordance with padding rule with supplied mask
     * encrypt(in xor thisVector)=vector
     * @param in   input array
     * @param vector mask byte array (modified during encryption)
     * @return encrypted array
     */
    public byte[] encryptCBC(byte[] in, byte[] vector)
    {
        byte[] out;
        int offset, blocks;
        int blockSize=this.getBlockSize();
        byte[] bf, buf=new byte[blockSize];
        byte[] inBuffer=this.roundBuffer(in); //rounding buffer in accordance with padding rule
        byte[] thisVector=vector;
        out=new byte[inBuffer.length];
        //writing ecnrypted array
        blocks=inBuffer.length / blockSize + ((inBuffer.length % blockSize == 0) ? 0 : 1);
        for(int i=offset=0; i < blocks; i++, offset+=blockSize)
        {
            inBuffer=xor(inBuffer, offset, thisVector, 0, blockSize);
            System.arraycopy(inBuffer, offset, buf, 0, blockSize);
            bf=this.encryptBlock(buf);
            System.arraycopy(bf, 0, out, offset, blockSize);
            thisVector=bf;
        }
        //computing last mask
        System.arraycopy(out, (blocks - 1) * blockSize, vector, 0, blockSize);
        return out;
    }

    /**
     * Decrypts given byte array and CBC's in accordance with padding rule with supplied mask
     *
     * @param in   input array
     * @param mask mask byte array (modified during encryption)
     * @return decrypted array
     */
    public byte[] decryptCBC(byte[] in, byte[] mask) throws CipherException
    {
        int offset;
        int blockSize=this.getBlockSize();
        byte[] out, bf, buf=new byte[blockSize];
        out=new byte[in.length];
        int blocks=in.length / blockSize + ((in.length % blockSize == 0) ? 0 : 1);
        if(in.length%blockSize!=0)
            Log.w("Cipher", "Invalid input buffer!"+in);
        byte[] thismask=new byte[blockSize];
        try {
        for(int i=offset=0; i < blocks; i++, offset+=blockSize)
        {
            System.arraycopy(mask, 0, thismask, 0, blockSize);
            System.arraycopy(in, offset, buf, 0, blockSize);
            System.arraycopy(buf, 0, mask, 0, blockSize);
            bf=this.decryptBlock(buf);
            bf=xor(bf, 0, thismask, 0, blockSize);
            System.arraycopy(bf, 0, out, offset, blockSize);
        }
        }
        catch (Exception ex) {
            throw new CipherException("Error during decryption", ex);
        }
        //computing last mask
        System.arraycopy(in, (blocks - 1) * blockSize, mask, 0, blockSize);
        return out;
    }

    protected byte[] generateInitVector()
    {
        //byte[] vector=ByteUtils.RANDOM.getBytes(this.getBlockSize());
        byte[] vector=NativeLib.getRandomBytes(this.getBlockSize());
        return vector;
    }

    /**
     * Generates digest of supplied String based on selected algorythm
     *
     * @param password   password used to generate key
     * @param digestType - selected keygen algorythm either SHA1 or WHIRLPOOL
     * @param keySize    - key length in bits - has to be less than 512 bits
     * @return byte array containing generated digest
     */
    public static final byte[] generateDigest(String password, int digestType, int keySize)
    {
        switch(digestType)
        {
            case DIGEST_WHIRLPOOL:
                Whirlpool w=new Whirlpool();
                byte[] digest=new byte[Whirlpool.DIGESTBYTES];
                w.NESSIEinit();
                w.NESSIEadd(password);
                w.NESSIEfinalize(digest);
                if(keySize < 512)
                {
                    int numBytes=(keySize%8)==0 ? keySize/8 : keySize/8+1;
                    byte[] cut_digest=new byte[numBytes];
                    System.arraycopy(digest, 0, cut_digest, 0, numBytes);
                    return cut_digest;
                }
                else
                    return digest;
            case DIGEST_SHA1:
            default: //by default generate SHA1
                SHA1 kg=new SHA1(keySize);//SHA keygen
                byte[] buf=ByteUtils.stringToByteArray(password);
                kg.update(buf, 0, buf.length);
                kg.generate();
                return kg.getDigest();
        }
    }

    protected byte[] xor(byte[] source, int sourceOffset, byte[] mask, int maskOffset, int length)
    {
        for(int i=0; i < length; i++)
            source[i + sourceOffset]^=mask[i + maskOffset];
        return source;
    }

    public void testBitmapCipher(Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.contact);
        int size=bitmap.getWidth()*bitmap.getHeight();
        int[] pixels=new int[size];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        InputStream is=new ByteArrayInputStream(ByteUtils.intArrayToByteArray(pixels));
        CipherDES cipher=new CipherDES("test12345678");
        CipherOutputStream cos;
        ByteArrayOutputStream bos=new ByteArrayOutputStream();
        try {
            cos = new CipherOutputStream(bos, cipher);
            while(true) {
                int b=is.read();
                if(b==-1)
                    break;
                cos.write(b);
            }
            cos.close();
           /*
            oldSize=width*height;
            blocks=4*oldSize/1024+1;
            newSize=(oldSize+4*blocks)
            */
            int[] newPixels=ByteUtils.byteArrayToIntArray(bos.toByteArray());
            Log.i(TAG, "Original bitmap width="+bitmap.getWidth()+", height="+bitmap.getHeight()+", size="+pixels.length);
            Log.i(TAG, "New bitmap size="+newPixels.length);
            Bitmap bm=bitmap.copy(bitmap.getConfig(), true);
            bm.setPixels(newPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            File file = new File(context.getFilesDir(), "test.jpg");
            FileOutputStream fos=new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();
        }
        catch(Exception ex) {
            Log.i(TAG, "Exception in testBitmapCipher()", ex);
        }
    }
}
