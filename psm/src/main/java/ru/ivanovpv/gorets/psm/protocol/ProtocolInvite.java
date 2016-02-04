package ru.ivanovpv.gorets.psm.protocol;

import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.KeyExchange;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 457 $
 *   $LastChangedDate: 2013-12-25 13:20:31 +0400 (Ср, 25 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/ProtocolInvite.java $
 */

/**
 *  [PSM$INVT][key-exchange type - 1 char][patameters hash - 40 chars][Public key in Base64]
 */
public class ProtocolInvite extends Protocol
{

    private KeyExchange keyExchange;
    private byte[] parametersHash;
    private final static int BLOCK_SIZE=4;
    private static final byte PADDING_BYTE=-1; //doesn't really matter

    public static final int PARAMETERS_HASH_LENGTH=BLOCK_SIZE;

    public ProtocolInvite(KeyExchange keyExchange)
    {
        this.keyExchange=keyExchange;
        if(keyExchange.getParametersHash().length < PARAMETERS_HASH_LENGTH)
            throw new IllegalArgumentException("Parameters hash need to be at least "+PARAMETERS_HASH_LENGTH +" bytes");
        else
        {
            this.parametersHash=new byte[PARAMETERS_HASH_LENGTH];
            System.arraycopy(keyExchange.getParametersHash(), 0, this.parametersHash, 0, PARAMETERS_HASH_LENGTH);
        }
    }

    @Override
    public String encodeString(String body)
    {
        return this.encodeBytes(ByteUtils.stringToByteArray(body));
    }

    @Override
    public String decodeString(String string) throws ProtocolException
    {
        return ByteUtils.byteArrayToString(this.decodeBytes(string), 0);
    }

    @Override
    public String encodeBytes(byte[] body)
    {
        byte[] buffer=body;
        //byte[] vector=ByteUtils.RANDOM.getBytes(BLOCK_SIZE);
        byte[] vector=NativeLib.getRandomBytes(BLOCK_SIZE);
        byte[] sizeBuf=ByteUtils.intToByteArray(buffer.length);
        byte[] buf=new byte[vector.length+sizeBuf.length+buffer.length];
        System.arraycopy(vector, 0, buf, 0, vector.length);
        System.arraycopy(sizeBuf, 0, buf, vector.length, sizeBuf.length);
        System.arraycopy(buffer, 0, buf, vector.length+sizeBuf.length, buffer.length);
        buf=roundBuffer(buf); //rounding buffer
        //xoring with init vector CBC alike
        for(int i=1; i < buf.length/BLOCK_SIZE; i++)
        {
            xor(buf, i*BLOCK_SIZE, vector, 0, BLOCK_SIZE);
            System.arraycopy(buf, i*BLOCK_SIZE, vector, 0, vector.length);
        }
        ProtocolBuffer protocolBuffer=ProtocolBuffer.fromBody(buf);
        StringBuilder sb=new StringBuilder(this.getDescriptor());
        sb.append(keyExchange.getType());
        String bufferText=ByteUtils.bytesToBase64(protocolBuffer.getBuffer());
        sb.append(bufferText);
        sb.append(this.getCheckSum(bufferText));
        sb.append(TERMINATOR);
        return sb.toString();
    }

    @Override
    public byte[] decodeBytes(String string) throws ProtocolException
    {
        ProtocolBuffer protocolBuffer=this.extractBuffer(string);
        byte[] buf, buffer=protocolBuffer.getBody();
        /*if(buffer==null || buffer.length <= BLOCK_SIZE || buffer.length%BLOCK_SIZE!=0)
            return null;*/
        byte[] newvector=new byte[BLOCK_SIZE];
        byte[] vector=new byte[BLOCK_SIZE];
        System.arraycopy(buffer, 0, vector, 0, vector.length);
        //xoring with init vector CBC alike
        for(int i=1; i < buffer.length/BLOCK_SIZE; i++)
        {
            System.arraycopy(buffer, i*BLOCK_SIZE, newvector, 0, newvector.length);
            xor(buffer, i*BLOCK_SIZE, vector, 0, BLOCK_SIZE);
            System.arraycopy(newvector, 0, vector, 0, vector.length);
        }
        int size=ByteUtils.byteArrayToInt(buffer, BLOCK_SIZE);
        buf=new byte[size];
        System.arraycopy(buffer, BLOCK_SIZE+4, buf, 0, size);
        return buf;
    }

    private byte[] xor(byte[] source, int sourceOffset, byte[] mask, int maskOffset, int length)
    {
        for(int i=0; i < length; i++)
            source[i + sourceOffset]^=mask[i + maskOffset];
        return source;
    }

    private byte[] roundBuffer(byte[] buffer)
    {
        int length=buffer.length;
        int size=BLOCK_SIZE * (length / BLOCK_SIZE) + BLOCK_SIZE * ((length % BLOCK_SIZE == 0) ? 0 : 1); //rounded buffer size including padding bytes
        byte[] newbuf=new byte[size];
        System.arraycopy(buffer, 0, newbuf, 0, length);
        for(int i=length; i < newbuf.length; i++)
            newbuf[i]=PADDING_BYTE;
        return newbuf;
    }

    @Override
    protected String getDescriptor() {
        return Protocol.INVITE_DESCRIPTOR;
    }

    @Override
    public KeyExchange getKeyExchange() {
        return keyExchange;
    }
}

