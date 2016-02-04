package ru.ivanovpv.gorets.psm.protocol;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 482 $
 *   $LastChangedDate: 2014-01-26 12:39:43 +0400 (Вс, 26 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolScramble.java $
 */

import ru.ivanovpv.gorets.psm.cipher.Cipher;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 19.04.13
 * Time: 14:44
 * To change this template use File | Settings | File Templates.
 */
public class ProtocolScramble extends ProtocolSecure
{

    public ProtocolScramble(Cipher cipher, int sessionIndex) {
        super(cipher, sessionIndex);
    }

    @Override
    protected String getDescriptor() {
        return Protocol.SCRAMBLE_DESCRIPTOR;
    }

}
