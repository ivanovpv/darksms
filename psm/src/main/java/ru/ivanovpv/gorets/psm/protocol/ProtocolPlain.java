package ru.ivanovpv.gorets.psm.protocol;

import ru.ivanovpv.gorets.psm.cipher.ByteUtils;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolPlain.java $
 */

/**
 * No encryption at all, just plain text. Binary data as Base64
 */
public class ProtocolPlain extends Protocol {

    @Override
    public String encodeString(String text) {
        return text;
    }

    @Override
    public String decodeString(String body) {
        return body;
    }
    @Override
    public String encodeBytes(byte[] buffer) {
        return ByteUtils.stringToBase64(ByteUtils.byteArrayToString(buffer, 0));
    }

    @Override
    public byte[] decodeBytes(String body) {
        return ByteUtils.stringToByteArray(ByteUtils.base64ToString(body));
    }

    @Override
    protected String getDescriptor() {
        return "";
    }
}
