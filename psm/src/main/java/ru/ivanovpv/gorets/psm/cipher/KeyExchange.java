package ru.ivanovpv.gorets.psm.cipher;

import ru.ivanovpv.gorets.psm.nativelib.NativeLib;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.math.BigInteger;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 440 $
 *   $LastChangedDate: 2013-12-06 18:10:07 +0400 (Пт, 06 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/cipher/KeyExchange.java $
 */

public abstract class KeyExchange  {
    public final static int BLOCK_SIZE=4;
    public final static char KEY_EXCHANGE_DUMMY='0';//shouldn't be used, except for test
    public final static char KEY_EXCHANGE_DIFFIE_HELLMAN='A';
    public final static char KEY_EXCHANGE_ELLIPTIC_CURVE_112B='B';
    public final static char KEY_EXCHANGE_ELLIPTIC_CURVE_256B='C';
    public final static char KEY_EXCHANGE_ELLIPTIC_CURVE_384B='D';
    protected byte[] privateKey;
    protected byte[] publicKey;
    public abstract byte[] getSharedKey(byte[] composite);
    public abstract byte[] getParametersHash();
    public abstract boolean checkParametersHash(byte[] hash);
    public abstract char getType();


    protected KeyExchange(byte[] privateKey) {
        this.privateKey=privateKey;
    }

    /**
     * Returns the private key.
     *
     * @return the private key.
     */
    public byte[] getPrivateKey()
    {
        return privateKey;
    }

    public byte[] getSessionKey(int sessionIndex) {
        byte[] sharedKey=this.getSharedKey(publicKey);
        byte[] sessionKey= NativeLib.generateHash(sharedKey, NativeLib.HASH_SHA512, sessionIndex * 10);
        return NativeLib.generateHash(sessionKey, NativeLib.HASH_WHIRLPOOL, (Protocol.SESSION_SIZE-sessionIndex)*10);
    }

    /**
     * Returns the public key.
     *
     * @return the public key.
     */
    public byte[] getPublicKey()
    {
        return publicKey;
    }

}
