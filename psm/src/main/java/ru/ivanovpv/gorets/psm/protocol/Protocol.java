package ru.ivanovpv.gorets.psm.protocol;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.cipher.*;
import ru.ivanovpv.gorets.psm.nativelib.NativeLib;
import ru.ivanovpv.gorets.psm.persistent.Contact;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.service.SmsSendIntentService;

import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 495 $
 *   $LastChangedDate: 2014-02-03 22:50:58 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/Protocol.java $
 */

public abstract class Protocol
{
    private static final String TAG=Protocol.class.getName();
    public static final String DESCRIPTOR="PSM$";
    public static final String CIPHER_DESCRIPTOR=DESCRIPTOR+"C";
    public static final String INVITE_DESCRIPTOR=DESCRIPTOR+"I";
    public static final String ACCEPT_DESCRIPTOR=DESCRIPTOR+"A";
    public static final String SCRAMBLE_DESCRIPTOR =DESCRIPTOR+"S";
    public static final String TERMINATOR="$MSP";
    protected static final String latinOfAscii="!?1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int SESSION_SIZE=latinOfAscii.length();

    protected Protocol()
    {
    }

    public KeyExchange getKeyExchange() {
        return new DummyExchange();
    }

    public static Protocol parseProtocol(String body, byte[] key) throws ProtocolException
    {
        if(body==null)
            return new ProtocolPlain();
        if(Protocol.isCiphered(body))
        {
            char cipherType=extractCipherType(body);
            int sessionIndex=extractSessionIndex(body);
            byte[] sessionKey=NativeLib.generateHash(key, NativeLib.HASH_SHA512, sessionIndex*10);
            sessionKey=NativeLib.generateHash(sessionKey, NativeLib.HASH_WHIRLPOOL, (SESSION_SIZE-sessionIndex)*10);
            switch (cipherType)
            {
                case Cipher.CIPHER_ANUBIS:
                    return new ProtocolSecure(new CipherAnubis(sessionKey), sessionIndex);
                case Cipher.CIPHER_DES:
                    return new ProtocolSecure(new CipherDES(sessionKey), sessionIndex);
                case Cipher.CIPHER_RIJNDAEL:
                    return new ProtocolSecure(new Rijndael(sessionKey, 32), sessionIndex);
                default:
                    Log.w(TAG, "Not supported cipher type declared='"+cipherType+"'");
                    throw new ProtocolException("Not supported cipher protocol, probably you'd need to upgrade your application.\n"+
                            "Also please take care of possible security hacking attempt");
            }
        }
        else
            return parseProtocol(body);
    }

    public static Protocol parseProtocol(String body) throws ProtocolException
    {
        if(body==null)
            return new ProtocolPlain();
        else if(Protocol.isInvitation(body))
        {
            KeyExchange keyExchange;
            char keyExchangeType=extractKeyExchangeType(body);
            switch(keyExchangeType)
            {
                case KeyExchange.KEY_EXCHANGE_DIFFIE_HELLMAN:
                    keyExchange=new DiffieHellman(Me.getMe().getHashDAO().get().getKey());
//                    keyExchange=new DiffieHellman(new BigInteger("123").toByteArray());
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B:
                    keyExchange=new EllipticCurve112B(Me.getMe().getHashDAO().get().getKey());
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B:
                    keyExchange=new EllipticCurve256B(Me.getMe().getHashDAO().get().getKey());
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B:
                    keyExchange=new EllipticCurve384B(Me.getMe().getHashDAO().get().getKey());
                    break;
                default:
                    Log.w(TAG, "Not supported key exchange protocol declared='"+keyExchangeType+"'");
                    throw new ProtocolException("Not supported key-exchange protocol, probably you'd need to upgrade your application.\n"+
                            "Also please take care of possible security hacking attempt");
            }
            return new ProtocolInvite(keyExchange);
        }
        else if(Protocol.isAccept(body))
        {
            KeyExchange keyExchange;
            char keyExchangeType=extractKeyExchangeType(body);
            switch(keyExchangeType)
            {
                case KeyExchange.KEY_EXCHANGE_DIFFIE_HELLMAN:
                    keyExchange=new DiffieHellman(Me.getMe().getHashDAO().get().getKey());
                    //keyExchange=new DiffieHellman(new BigInteger("123"));
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B:
                    keyExchange=new EllipticCurve112B(Me.getMe().getHashDAO().get().getKey());
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_256B:
                    keyExchange=new EllipticCurve256B(Me.getMe().getHashDAO().get().getKey());
                    break;
                case KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_384B:
                    keyExchange=new EllipticCurve384B(Me.getMe().getHashDAO().get().getKey());
                    break;
                default:
                    Log.w(TAG, "Not supported key exchange protocol declared='"+keyExchangeType+"'");
                    throw new ProtocolException("Not supported key-exchange protocol, probably you'd need to upgrade your application.\n"+
                            "Also please take care of possible security hacking attempt");
            }
            return new ProtocolAccept(keyExchange);
        }
        else if(Protocol.isScrambled(body))
        {
            char cipherType=extractScrambleType(body);
            int sessionIndex=extractSessionIndex(body);
            switch (cipherType)
            {
                case Cipher.CIPHER_ANUBIS:
                    return new ProtocolScramble(new CipherAnubis(Me.getMe().getHashDAO().get().getKey()), sessionIndex);
                case Cipher.CIPHER_DES:
                    return new ProtocolScramble(new CipherDES(Me.getMe().getHashDAO().get().getKey()), sessionIndex);
                case Cipher.CIPHER_RIJNDAEL:
                    return new ProtocolScramble(new Rijndael(Me.getMe().getHashDAO().get().getKey(), 32), sessionIndex);
                default:
                    Log.w(TAG, "Not supported scrambling type declared='"+cipherType+"'");
                    throw new ProtocolException("Not supported scramble protocol, probably you'd need to upgrade your application.\n"+
                            "Also please take care of possible security hacking attempt");
            }
        }
        else if(Protocol.isCiphered(body))
            throw new IllegalArgumentException("Illegal usage of parseProtocol() - please specify protocol key");
        return new ProtocolPlain();
    }

    public static boolean isInvitation(String body) {
        if(Me.TEST)
            return isTestInvitation(body);
        int start=getPSMStartBlock(body);
        if(start < 0)
            return false;
        int index = body.indexOf(INVITE_DESCRIPTOR, start);
        if(index < 0)
            return false;
        return true;
    }

    public static boolean isTestInvitation(String body) {
        if(body==null)
            return false;
        return body.toLowerCase().startsWith("testPSMInvite".toLowerCase());
    }

    public static boolean isAccept(String body) {
        if(Me.TEST)
            return isTestAccept(body);
        int start=getPSMStartBlock(body);
        if(start < 0)
            return false;
        int index = body.indexOf(ACCEPT_DESCRIPTOR, start);
        if(index==start)
            return true;
        return false;
    }

    public static boolean isTestAccept(String body) {
        if(body==null)
            return false;
        return body.toLowerCase().startsWith("testPSMAccept".toLowerCase());
    }

    public static boolean isCiphered(String body) {
        int start=getPSMStartBlock(body);
        if(start < 0)
            return false;
        int index = body.indexOf(CIPHER_DESCRIPTOR, start);
        if(index == start)
            return true;
        return false;
    }

    public static boolean isScrambled(String body) {
        int start=getPSMStartBlock(body);
        if(start < 0)
            return false;
        int index = body.indexOf(SCRAMBLE_DESCRIPTOR, start);
        if(index == start)
            return true;
        return false;
    }

    /**
     * Return starting index for PSM message block
     * @param body input String
     * @return int
     */
    private static int getPSMStartBlock(String body) {
        if(body==null)
            return -1;
        if(body.trim().length()==0)
            return -1;
        int start=body.indexOf(DESCRIPTOR);
        //int end=body.indexOf(TERMINATOR, start+DESCRIPTOR.length()+1);
        if(start < 0)
            return -1;
        return start;
    }

    public static boolean isPlain(String body) {
        int start=getPSMStartBlock(body);
        if(start < 0)
            return true;
        return false;
    }


    protected abstract String getDescriptor();
    public abstract String encodeString(String text);
    public abstract String decodeString(String body) throws ProtocolException;

    public abstract String encodeBytes(byte[] text);
    public abstract byte[] decodeBytes(String body) throws ProtocolException;

    public static Message sendInvitation(Context context, PhoneNumber pn) {
        if(Me.DEBUG)
            Log.i(TAG, "Sending invite to #"+pn.getRawAddress());
        KeyExchange keyExchange=Hash.getDefaultKeyExchange(Me.getMe().getHashDAO().get().getKey());
        byte[] key=keyExchange.getPublicKey();
        if(Me.DEBUG)
            Log.i(TAG, "Sending public key="+ByteUtils.bytesToHex(key));
        Protocol protocol=new ProtocolInvite(keyExchange);
        String body=protocol.encodeBytes(key);
        if(Me.DEBUG)
            Log.i(TAG, "Invitation message="+body);
        FingerPrint fingerPrint=new FingerPrint(key, keyExchange.getType());
        String msgText=context.getString(R.string.invitationSent)+fingerPrint.toString();
        String invitation=getPSMLink(context)+body;
        Message message=Message.createSendMessage(pn.getRawAddress(), msgText, invitation);
        SmsSendIntentService.startActionSend(context, message, pn, false);
/*        message.setSentStatus(Message.STATUS_PENDING);
        message.setDeliveredStatus(Message.STATUS_PENDING);
        message=Me.getMe().getMessageDAO().save(context, message);

        protocol.sendRawMessage(context, message.getAddress(), invitation, message.getId());*/
        Toast.makeText(context, context.getString(R.string.invitationTo) +
                pn.getRawAddress() + context.getString(R.string.messageSent), Toast.LENGTH_LONG).show();

        return message;
    }

    protected char getCheckSum(String string) {
        long sum=0;
        char ch;
        for(int i=0; i < string.length(); i++) {
            ch=string.charAt(i);
            sum=sum+ch;
        }
        int index=(int )(sum%(long )latinOfAscii.length());
        return latinOfAscii.charAt(index);
    }

    protected static boolean isSubstring(String string, String subString) {
        if(string==null || subString==null)
            return false;
        if(string.indexOf(subString) >= 0)
            return true;
        return false;
    }

    /**
     * Extracts key exchange type from body string
     * @param body
     * @return '0' (zero) if not found
     */
    public static char extractKeyExchangeType(String body) {
        int index;
        if(Me.TEST)
            return KeyExchange.KEY_EXCHANGE_ELLIPTIC_CURVE_112B;
        int start=Protocol.getPSMStartBlock(body);
        if(start < 0)
            return KeyExchange.KEY_EXCHANGE_DUMMY;
        //looking if invitation
        index=body.indexOf(Protocol.INVITE_DESCRIPTOR, start);
        if(index >= 0) {
            index+=Protocol.INVITE_DESCRIPTOR.length();
            return body.charAt(index);
        }
        //looking if accept
        index=body.indexOf(Protocol.ACCEPT_DESCRIPTOR, start);
        if(index >= 0) {
            index+=Protocol.ACCEPT_DESCRIPTOR.length();
            return body.charAt(index);
        }
        return KeyExchange.KEY_EXCHANGE_DUMMY;
    }

    /**
     * Extracts cipher type from body string
     * @param body
     * @return '0' (zero) if not found
     */
    public static char extractCipherType(String body) {
        int start=Protocol.getPSMStartBlock(body);
        if(start < 0)
            return Cipher.CIPHER_EMPTY;
        int index=body.indexOf(Protocol.CIPHER_DESCRIPTOR, start);
        if(index < 0)
            return Cipher.CIPHER_EMPTY;
        index+=Protocol.CIPHER_DESCRIPTOR.length();
        return body.charAt(index);
    }

    /**
     * Extracts session key index from body string
     * @param body
     * @return -1 if not found
     */
    public static int extractSessionIndex(String body) {
        int start=Protocol.getPSMStartBlock(body);
        if(start < 0)
            return 0;
        int index=body.indexOf(Protocol.CIPHER_DESCRIPTOR, start);
        if(index < 0)
            return 0;
        index+=Protocol.CIPHER_DESCRIPTOR.length()+1;
        char keyValue=body.charAt(index);
        return latinOfAscii.indexOf(keyValue);
    }

    /**
     * Extracts key exchange type from body string
     * @param body
     * @return '0' (zero) if not found
     */
    public static char extractScrambleType(String body) {
        int start=Protocol.getPSMStartBlock(body);
        if(start < 0)
            return Cipher.CIPHER_EMPTY;
        int index=body.indexOf(Protocol.SCRAMBLE_DESCRIPTOR, start);
        if(index < 0)
            return Cipher.CIPHER_EMPTY;
        index+=Protocol.CIPHER_DESCRIPTOR.length();
        return body.charAt(index);
    }

    /**
     * Extracts message body from string
     * @param string
     * @return null in case of any error or if it's just plain text message
     */
    public ProtocolBuffer extractBuffer(String string) throws ProtocolException{
        //take care of cipher type (1)
        int index=Protocol.getPSMStartBlock(string);
        if(index < 0)
            throw new ProtocolException("Invalid PSM start block");
        int start=string.indexOf(this.getDescriptor(), index);
        int end=string.indexOf(Protocol.TERMINATOR, start);
        if(start < 0 || end < 0 ) {
            throw new ProtocolException("Invalid PSM descriptor/terminator");
        }
        start+=this.getDescriptor().length()+1;
        String buffer=string.substring(start, end-1);
        char checkSum=this.getCheckSum(buffer);
        if(checkSum!=string.charAt(end-1)) {
            throw new ProtocolException("Invalid checksum inside PSM descriptor");
        }
        byte[] byteBuffer= ByteUtils.base64ToBytes(buffer);
        return ProtocolBuffer.fromBuffer(byteBuffer);
    }

    private static String getPSMLink(Context context) {
        StringBuilder sb=new StringBuilder();
        sb.append(context.getString(R.string.invitationMessageText));
        if(Me.ENABLE_MARKET)
                sb.append("\n").append(context.getString(R.string.psmMarketUrl));
        sb.append("\n");
        return sb.toString();
    }

}
