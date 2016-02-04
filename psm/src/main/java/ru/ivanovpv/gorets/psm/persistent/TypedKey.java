/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/TypedKey.java $
 */

package ru.ivanovpv.gorets.psm.persistent;

import java.io.Serializable;

public final class TypedKey implements Serializable {
    private char keyType;  //key type
    private long time; //time
    private byte[] key; //actual key

    public TypedKey(byte[] key, long time, char keyType) {
        this.setKey(key);
        this.setTime(time);
        this.setKeyType(keyType);

    }

    public char getKeyType() {
        return keyType;
    }

    public void setKeyType(char keyType) {
        this.keyType = keyType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}
