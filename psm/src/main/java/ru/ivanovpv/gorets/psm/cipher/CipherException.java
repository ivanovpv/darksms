package ru.ivanovpv.gorets.psm.cipher;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/CipherException.java $
 */

public class CipherException extends Exception
{
    public CipherException(String message, Throwable th)
    {
        super(message, th);
    }

    public CipherException(String message)
    {
        super(message);
    }
}
