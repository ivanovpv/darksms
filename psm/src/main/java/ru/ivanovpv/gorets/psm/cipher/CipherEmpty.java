package ru.ivanovpv.gorets.psm.cipher;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/CipherEmpty.java $
 */

public class CipherEmpty extends Cipher
{
    public CipherEmpty()
    {
    }

    public CipherEmpty(String password)
    {
        super();
    }

    @Override
    public byte[] encryptCBC(byte[] in, byte[] mask)
    {
        return in;
    }

    @Override
    public byte[] decryptCBC(byte[] in, byte[] mask)
    {
        return in;
    }

    @Override
    protected byte[] generateInitVector()
    {
        return new byte[0];
    }

    @Override
    public byte[] encryptBlock(byte[] buffer)
    {
        return buffer;
    }

    @Override
    public byte[] decryptBlock(byte[] buffer)
    {
        return buffer;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clean()
    {
    }

    @Override
    public char getType() {
        return Cipher.CIPHER_EMPTY;
    }

    @Override
    protected byte[] roundBuffer(byte[] buffer)
    {
        return buffer;
    }

    @Override
    protected int getBlockSize()
    {
        return 1024;
    }
}
