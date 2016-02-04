package ru.ivanovpv.gorets.psm.cipher;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 353 $
 *   $LastChangedDate: 2013-10-01 17:47:07 +0400 (Вт, 01 окт 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/CipherAnubis.java $
 */

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/**
 * Standard wrapper around Anubis. As-is, just rounds buffer to blocks limits,
 * During decryption doesn't stripe padding bytes
 */

public class CipherAnubis extends Cipher
{
    private static final String TAG=CipherAnubis.class.getName();
    private Anubis anubis;
    private static final byte PADDING_BYTE=0; //doesn't really matter
    private static final int BLOCK_SIZE=16;   //128 bits
    private static final int KEY_SIZE=320;    //key size in bits

    public CipherAnubis(byte[] key)
    {
        anubis = new Anubis();
        if(key.length > KEY_SIZE/8) {
            byte[] newKey=new byte[KEY_SIZE/8];
            System.arraycopy(key, 0, newKey, 0, newKey.length);
            anubis.keySetup(newKey);
        }
        else
            anubis.keySetup(key);
    }


    /**
     * Default constructor uses SHA512 keygen procedure
     * @param password password used to generate key
     */
    public CipherAnubis(String password)
    {
        //byte[] digest=generateDigest(password, DIGEST_WHIRLPOOL, KEY_SIZE);
        byte[] digest=NativeLib.generatePK(password, "", NativeLib.HASH_SHA512, KEY_SIZE);
        byte[] newDigest=new byte[KEY_SIZE/8];
        System.arraycopy(digest, 0, newDigest, 0, KEY_SIZE/8);
        anubis = new Anubis();
        anubis.keySetup(newDigest);
    }

    @Override
    protected int getBlockSize()
    {
        return BLOCK_SIZE;
    }


    @Override
    public byte[] encryptBlock(byte[] block)
    {
        byte[] out=new byte[block.length];
        System.arraycopy(block, 0, out, 0, block.length);
        anubis.encrypt(out);
        return out;
    }

    @Override
    public byte[] decryptBlock(byte[] block)
    {
        byte[] out=new byte[block.length];
        System.arraycopy(block, 0, out, 0, block.length);
        anubis.decrypt(out);
        return out;
    }

    @Override
    public void clean()
    {
        anubis.clean();
    }

    @Override
    public char getType() {
        return Cipher.CIPHER_ANUBIS;
    }

    public static void testStandard320() throws CipherException
    {
        String s;
        byte[] key=new byte[40];
        key[0]=(byte )0;
        for(int i=1; i < key.length; i++)
            key[i]=0;
        System.out.println("key: "+ Anubis.display(key));
        Cipher aw=new CipherAnubis(key);
        byte[] ebuf, dbuf, buffer=new byte[32];
        for(int i=0; i < buffer.length; i++)
            buffer[i]=0;
        s=ByteUtils.bytesToHex(buffer);
        System.out.println("Buffer: "+s);
        long start=System.currentTimeMillis();
        ebuf=aw.encryptBlock(buffer);
        long end=System.currentTimeMillis();
        s= ByteUtils.bytesToHex(ebuf);
        System.out.println("Cipher: "+s);
        System.out.println(" - encrypted in "+(end-start)+" ms");
        start=System.currentTimeMillis();
        dbuf=aw.decryptBlock(ebuf);
        end=System.currentTimeMillis();
        s=ByteUtils.bytesToHex(dbuf);
        System.out.println("Decipher: "+s);
        System.out.println(" - decrypted in "+(end-start)+" ms");
    }
    /**
     * Rounds buffer in accordance with Anubis padding policy (16 bytes)
     * @param buffer byte array to be rounded/padded
     * @return rounded byte array (always new buffer)
     */
    @Override
    protected final byte[] roundBuffer(byte[] buffer)
    {
        int length=buffer.length;
        int size=BLOCK_SIZE*(length/BLOCK_SIZE) + BLOCK_SIZE*((length%BLOCK_SIZE==0)?0:1); //rounded buffer size including padding bytes
        byte[] newbuf=new byte[size];
        System.arraycopy(buffer, 0, newbuf, 0, length);
        for(int i=length; i < newbuf.length; i++)
           newbuf[i]=PADDING_BYTE;
        return newbuf;
    }

    // runs original 320 bit test vector
    public static void main(String[] args) throws CipherException {
        testStandard320();
        String password="lfkl;fkl;sdf";
        byte[] key=Cipher.generateDigest(password, Cipher.DIGEST_WHIRLPOOL, 320);
        Cipher cipher=new CipherAnubis(key);
        String org="gfhgfhjghjghjghj";
        byte[] text=ByteUtils.stringToByteArray(org);
        byte[] enc=cipher.encryptBlock(text);
        byte[] key2=Cipher.generateDigest(password, Cipher.DIGEST_WHIRLPOOL, 320);
        Cipher cipher2=new CipherAnubis(key2);
        byte[] dec=cipher2.decryptBlock(enc);
        String s=ByteUtils.byteArrayToString(dec, 0);
        System.out.println("orign="+org);
        System.out.println("check="+s);
    }
}

