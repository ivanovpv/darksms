/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/DummyExchange.java $
 */

package ru.ivanovpv.gorets.psm.cipher;

public class DummyExchange extends KeyExchange {

    public DummyExchange(byte[] privateKey)
    {
        super(privateKey);
    }

    public DummyExchange()
    {
        super(new byte[] {0});
    }

    @Override
    public byte[] getSharedKey(byte[] composite) {
        return new byte[0];
    }

    @Override
    public byte[] getParametersHash() {
        return new byte[0];
    }

    @Override
    public boolean checkParametersHash(byte[] hash) {
        return false;
    }

    @Override
    public char getType() {
        return KeyExchange.KEY_EXCHANGE_DUMMY;
    }
}
