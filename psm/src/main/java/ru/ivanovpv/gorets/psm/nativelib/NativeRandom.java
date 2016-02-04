/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Oleksandr Lashchenko (gsorron@gmail.com)2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 405 $
 *    $LastChangedDate: 2013-11-05 17:14:54 +0400 (Вт, 05 ноя 2013) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/nativelib/NativeRandom.java $
 */

package ru.ivanovpv.gorets.psm.nativelib;

import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.Randomer;

public final class NativeRandom extends Randomer
{
    @Override
    public int getInt()
    {
        byte[] bytes=NativeLib.getRandomBytes(4);
        int random=ByteUtils.byteArrayToInt(bytes, 0);
        if(random==Integer.MIN_VALUE) //see http://stackoverflow.com/questions/5444611/math-abs-returns-wrong-value-for-integer-min-value
            return Integer.MAX_VALUE;
        return Math.abs(random);
    }

    @Override
    public int getInt(int n)
    {
        int size, random;
        if(n <= Byte.MAX_VALUE)
            size=1;
        else if(n <= Short.MAX_VALUE)
            size=2;
        else
            size=4;
        byte[] bytes=NativeLib.getRandomBytes(size);
        switch(bytes.length) {
            case 1:
                random=Math.abs(bytes[0]);
                //random=random/Byte.MAX_VALUE*n;
                break;
            case 2:
                random=Math.abs(ByteUtils.byteArrayToShort(bytes, 0));
                //random=random/(Short.MAX_VALUE)*n;
                break;
            default:
                random=ByteUtils.byteArrayToInt(bytes, 0);
                if(random==Integer.MIN_VALUE) //see http://stackoverflow.com/questions/5444611/math-abs-returns-wrong-value-for-integer-min-value
                    random=Integer.MAX_VALUE;
                random=Math.abs(random);
                //random=random/Integer.MAX_VALUE*n;
                break;
        }
        return random%n;
    }

    @Override
    public byte[] getBytes(int size)
    {
        return NativeLib.getRandomBytes(size);
    }
}
