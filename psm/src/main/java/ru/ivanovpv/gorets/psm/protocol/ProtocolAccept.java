package ru.ivanovpv.gorets.psm.protocol;

import ru.ivanovpv.gorets.psm.cipher.KeyExchange;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolAccept.java $
 */

public final class ProtocolAccept extends ProtocolInvite {

    public ProtocolAccept(KeyExchange keyExchange) {
        super(keyExchange);
    }

    @Override
    protected String getDescriptor() {
        return Protocol.ACCEPT_DESCRIPTOR;
    }
}
