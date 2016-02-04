package ru.ivanovpv.gorets.psm.cipher;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 395 $
 *   $LastChangedDate: 2013-10-29 14:47:47 +0400 (Вт, 29 окт 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/Randomer.java $
 */

public abstract class Randomer
{
    /**
     * returns a random unsigned integer uniformly distributed in the interval 0 <= k < MAX_INT
     * @return int
     */
    public abstract int getInt();

    /**
     * returns a random unsigned integer uniformly distributed in the interval 0 <= k < n
     * @return int
     */
    public abstract int getInt(int n);

    public abstract byte[] getBytes(int size);
}
