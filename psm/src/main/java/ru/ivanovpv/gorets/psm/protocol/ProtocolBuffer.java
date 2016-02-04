/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolBuffer.java $
 */

package ru.ivanovpv.gorets.psm.protocol;

import ru.ivanovpv.gorets.psm.cipher.ByteUtils;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 494 $
 *   $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolBuffer.java $
 */
public class ProtocolBuffer
{
    private static final byte VERSION=1;
    private static final int VERSION_LENGTH=1;
    private byte[] version={VERSION};
    private byte[] body;

    private ProtocolBuffer(byte[] buffer) {
        this.version[0]=VERSION;
        this.body=buffer;
    }

    private ProtocolBuffer(byte version, byte[] body) throws ProtocolException {
        this.version[0]=version;
        this.body=body;
        if(version!=VERSION)
            throw new ProtocolException("This protocol version="+version+" not supported yet!");
    }

    public static ProtocolBuffer fromBody(byte[] body) {
        return new ProtocolBuffer(body);
    }

    public static ProtocolBuffer fromBuffer(byte[] buffer) throws ProtocolException {
        byte[] body=new byte[buffer.length-VERSION_LENGTH];
        System.arraycopy(buffer, VERSION_LENGTH, body, 0, buffer.length-VERSION_LENGTH);
        return new ProtocolBuffer(buffer[0], body);
    }

    public static ProtocolBuffer fromBuffer(String bufferText) throws ProtocolException {
        byte[] buffer=ByteUtils.base64ToBytes(bufferText);
        byte[] body=new byte[buffer.length-VERSION_LENGTH];
        System.arraycopy(buffer, VERSION_LENGTH, body, 0, buffer.length-VERSION_LENGTH);
        return new ProtocolBuffer(buffer[0], body);
    }

    public byte[] getBuffer() {
        byte[] buffer=new byte[this.body.length+VERSION_LENGTH];
        System.arraycopy(version, 0, buffer, 0, VERSION_LENGTH);
        System.arraycopy(body, 0, buffer, VERSION_LENGTH, body.length);
        return buffer;
    }

    public byte[] getBody() {
        return body;
    }

    public int getVersion() {
        return version[0];
    }
}
