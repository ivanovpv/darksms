package ru.ivanovpv.gorets.psm.protocol;

import android.util.Log;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.cipher.CipherException;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 482 $
 *   $LastChangedDate: 2014-01-26 12:39:43 +0400 (Вс, 26 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolSecure.java $
 */

/**
 * [PSM$PSM][Cipher type (1 char)][Encrypted message in Base64][checksum 1 char][PSM$END]
 *
 */
public class ProtocolSecure extends Protocol
{
    public static final String TAG=ProtocolSecure.class.getName();

    private static final int MAX_LENGTH=256*256; //64 kbytes
    private Cipher cipher;
    private int sessionIndex;

    public ProtocolSecure(Cipher cipher, int sessionIndex)
    {
        this.cipher=cipher;
        this.sessionIndex=sessionIndex;
        if(sessionIndex >= SESSION_SIZE || sessionIndex < 0)
            throw new IllegalArgumentException("Illegal session index, while allocating private session protocol");
    }

    @Override
    public String encodeString(String text)
    {
        char checkSum;
        byte[] buffer=ByteUtils.stringToByteArray(text);
        if(buffer.length > MAX_LENGTH)
            throw new IllegalArgumentException("Too long body");
        buffer=cipher.encryptBuffer(buffer);
        ProtocolBuffer protocolBuffer=ProtocolBuffer.fromBody(buffer);
        String encodedBody=ByteUtils.bytesToBase64(protocolBuffer.getBuffer());
        checkSum=this.getCheckSum(encodedBody);
        StringBuilder sb=new StringBuilder(this.getDescriptor());
        sb.append(cipher.getType());
        sb.append(latinOfAscii.charAt(sessionIndex));
        sb.append(encodedBody);
        sb.append(checkSum);
        sb.append(TERMINATOR);
        return sb.toString();
    }

    @Override
    public String decodeString(String string) throws ProtocolException
    {
        ProtocolBuffer protocolBuffer=this.extractBuffer(string);
        byte[] body=protocolBuffer.getBody();
        try {
            body=cipher.decryptBuffer(body);
        } catch (CipherException e) {
            throw new ProtocolException("Error decrypting buffer", e);
        }
        return ByteUtils.byteArrayToString(body, 0);
    }

    @Override
    public String encodeBytes(byte[] body)
    {
        StringBuilder sb=new StringBuilder(this.getDescriptor());
        sb.append(cipher.getType());
        byte[] buffer=cipher.encryptBuffer(body);
        ProtocolBuffer protocolBuffer=ProtocolBuffer.fromBody(buffer);
        String encodedBody=ByteUtils.bytesToBase64(protocolBuffer.getBuffer());
        sb.append(encodedBody);
        sb.append(this.getCheckSum(encodedBody));
        sb.append(TERMINATOR);
        return sb.toString();
    }

    @Override
    public byte[] decodeBytes(String string) throws ProtocolException
    {
        ProtocolBuffer protocolBuffer=this.extractBuffer(string);
        byte[] body=protocolBuffer.getBody();
        try {
            body=cipher.decryptBuffer(body);
        } catch (CipherException e) {
            Log.e(TAG, "Error decrypting buffer", e);
        }
        return body;
    }

    @Override
    protected String getDescriptor() {
        return Protocol.CIPHER_DESCRIPTOR;
    }

    /**
     * Extracts message body from string
     * @param string
     * @return null in case of any error or if it's just plain text message
     */
    @Override
    public ProtocolBuffer extractBuffer(String string) throws ProtocolException{
         //take care of cipher type (1) and session index (1)=2
        int start=string.indexOf(this.getDescriptor())+this.getDescriptor().length()+2;
        int end=string.indexOf(Protocol.TERMINATOR, start);
        if(start > string.length() || start < 0 || end < 0 || end > string.length()) {
            throw new ProtocolException("Invalid PSM descriptor");
        }
        String buffer=string.substring(start, end-1);
        char checkSum=this.getCheckSum(buffer);
        if(checkSum!=string.charAt(end-1)) {
            throw new ProtocolException("Invalid checksum inside PSM descriptor");
        }
        byte[] byteBuffer= ByteUtils.base64ToBytes(buffer);
        return ProtocolBuffer.fromBuffer(byteBuffer);
    }
}

