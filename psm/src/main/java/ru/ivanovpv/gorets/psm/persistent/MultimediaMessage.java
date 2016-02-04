/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.persistent;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.MessageBox;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.mms.ContentType;
import ru.ivanovpv.gorets.psm.mms.pdu.*;
import ru.ivanovpv.gorets.psm.nativelib.NativeRandom;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolScramble;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Vector;

/**
 * Created by Pavel on 25.06.2014.
 */
public class MultimediaMessage extends Message implements Serializable {
    public final static String TAG=MultimediaMessage.class.getName();

    //private String messageId; //_id or m_id
    private String subject;  //sub
    private int mmsVersion; //v
    private String transactionId; //tr_id
    private String contentLocation;
    private String contentType; //ct_t
    private long messageSize;  //m_size
    private long messageCount; //parts.size()?
    private int messageType; //m_type
    private int priority; //pri
    private int responseStatus; //resp_st
    private String messageClass; //m_cls
    private long expiry; //exp
    private int mmsStatus; //st
    private int messageBox; //msg_box
    private volatile boolean downloaded;
    private Vector<MultimediaMessagePart> parts;

    public MultimediaMessage() {
        super();
        parts=new Vector<MultimediaMessagePart>(); //init empty parts - will later filled during download
        downloaded=false;
    }

    public static MultimediaMessage createIncomingMessage(GenericPdu pdu) {
        PduHeaders pduHeaders=pdu.getPduHeaders();
        MultimediaMessage multimediaMessage=new MultimediaMessage();
        EncodedStringValue esv=pduHeaders.getEncodedStringValue(PduHeaders.SUBJECT);
        multimediaMessage.setSubject((esv == null) ? "" : esv.getString());

        esv=pduHeaders.getEncodedStringValue(PduHeaders.FROM);
        multimediaMessage.setAddress((esv==null) ? "" : esv.getString());

        multimediaMessage.setMmsVersion(pduHeaders.getOctet(PduHeaders.MMS_VERSION));

        byte[] bytes=pduHeaders.getTextString(PduHeaders.TRANSACTION_ID);
        multimediaMessage.setTransactionId(ByteUtils.byteArrayToString(bytes, 0));

        bytes=pduHeaders.getTextString(PduHeaders.MESSAGE_ID);
        multimediaMessage.setId(ByteUtils.byteArrayToString(bytes, 0));

        bytes=pduHeaders.getTextString(PduHeaders.CONTENT_LOCATION);
        multimediaMessage.setContentLocation(ByteUtils.byteArrayToString(bytes, 0));

        bytes=pduHeaders.getTextString(PduHeaders.CONTENT_TYPE);
        multimediaMessage.setContentType(ByteUtils.byteArrayToString(bytes, 0));

        multimediaMessage.setMessageSize(pduHeaders.getLongInteger(PduHeaders.MESSAGE_SIZE));

        multimediaMessage.setMessageCount(pduHeaders.getLongInteger(PduHeaders.MESSAGE_COUNT));

        long millis=pduHeaders.getLongInteger(PduHeaders.DATE);
        if(millis <= 0)
            millis=System.currentTimeMillis();
        multimediaMessage.setDate(millis);

        multimediaMessage.setType(MESSAGE_BOX_INBOX); //to be compatible with sms

        multimediaMessage.setMessageBox(MESSAGE_BOX_INBOX); //only incoming messages parsed through pdu :)

        multimediaMessage.setMessageType(pduHeaders.getOctet(PduHeaders.MESSAGE_TYPE));
        multimediaMessage.setPriority(pduHeaders.getOctet(PduHeaders.PRIORITY));
        multimediaMessage.setResponseStatus(pduHeaders.getOctet(PduHeaders.RESPONSE_STATUS));

        bytes=pduHeaders.getTextString(PduHeaders.MESSAGE_CLASS);
        multimediaMessage.setMessageClass(ByteUtils.byteArrayToString(bytes, 0));
        multimediaMessage.setExpiry(pduHeaders.getLongInteger(PduHeaders.EXPIRY));
        multimediaMessage.setMmsStatus(pduHeaders.getOctet(PduHeaders.STATUS));
        return multimediaMessage;
    }

    @Override
    public String toString()
    {
        StringBuilder sb=new StringBuilder();
        return sb.append("MMS id=").append(this.getId()).
                append("\n conversation id=").append(this.getConversationId()).
                append("\n body=").append(this.getBody()).
                append("\n address=").append(this.getAddress()).
                append("\n timestamp=").append(this.getDate()).
                append("\n size=").append(this.getMessageSize()).
                append("\n parts=").append(this.getPartsCount()).
                toString();
    }

    private void init(GenericPdu pdu) {
        byte[] bytes;

        PduHeaders pduHeaders=pdu.getPduHeaders();
        EncodedStringValue esv=pduHeaders.getEncodedStringValue(PduHeaders.SUBJECT);
        this.subject=((esv == null) ? "" : esv.getString());

        esv=pduHeaders.getEncodedStringValue(PduHeaders.FROM);
        this.address=((esv==null) ? "" : esv.getString());

        this.mmsVersion=pduHeaders.getOctet(PduHeaders.MMS_VERSION);

        bytes=pduHeaders.getTextString(PduHeaders.TRANSACTION_ID);
        transactionId= ByteUtils.byteArrayToString(bytes, 0);

        bytes=pduHeaders.getTextString(PduHeaders.MESSAGE_ID);
        this.id=ByteUtils.byteArrayToString(bytes, 0);

        bytes=pduHeaders.getTextString(PduHeaders.CONTENT_LOCATION);
        this.contentLocation=ByteUtils.byteArrayToString(bytes, 0);

        bytes=pduHeaders.getTextString(PduHeaders.CONTENT_TYPE);
        this.contentType=ByteUtils.byteArrayToString(bytes, 0);

        this.messageSize=pduHeaders.getLongInteger(PduHeaders.MESSAGE_SIZE);

        this.messageCount=pduHeaders.getLongInteger(PduHeaders.MESSAGE_COUNT);
        this.setDate(pduHeaders.getLongInteger(PduHeaders.DATE));
        this.type=MESSAGE_BOX_INBOX; //only incoming messages parsed through pdu :)
        this.messageType=pduHeaders.getOctet(PduHeaders.MESSAGE_TYPE);
        this.priority=pduHeaders.getOctet(PduHeaders.PRIORITY);
        this.responseStatus=pduHeaders.getOctet(PduHeaders.RESPONSE_STATUS);

        bytes=pduHeaders.getTextString(PduHeaders.MESSAGE_CLASS);
        this.messageClass=ByteUtils.byteArrayToString(bytes, 0);
        this.expiry=pduHeaders.getLongInteger(PduHeaders.EXPIRY);
        this.mmsStatus=pduHeaders.getOctet(PduHeaders.STATUS);
    }

    @Override
    public String getBody() {
        StringBuilder sb=new StringBuilder("");
/*        sb.append("Subject: ");
        sb.append(subject);
        sb.append("\n");*/
        for(int index=0; index < this.getPartsCount(); index++) {
            MultimediaMessagePart mmsPart=this.getPart(index);
            if(ContentType.isTextType(mmsPart.getContentType()))
                sb.append(mmsPart.getString());
        }
        return sb.toString();
    }

    public String getSubject() {
        return (subject==null)?"":subject;
    }

    public int getMmsVersion() {
        return mmsVersion;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getMessageSize() {
        return messageSize;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public int getPartsCount() {
        return parts.size();
    }

    public MultimediaMessagePart getPart(int index) {
        return parts.get(index);
    }

    public void addPart(MultimediaMessagePart part) {
        this.parts.add(part);
    }

    public Vector<MultimediaMessagePart> getParts()
    {
        return parts;
    }

    public synchronized boolean isDownloaded() {
        return downloaded;
    }

    public synchronized void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public int getMessageType() {
        return messageType;
    }

    public int getPriority() {
        return priority;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getMessageClass() {
        return messageClass;
    }

    public long getExpiry() {
        return expiry;
    }

    public int getMmsStatus() {
        return mmsStatus;
    }

    public void setSubject(String subject)
    {
        this.subject=subject;
    }

    public void setMmsVersion(int mmsVersion)
    {
        this.mmsVersion=mmsVersion;
    }

    public void setTransactionId(String transactionId)
    {
        this.transactionId=transactionId;
    }

    public void setContentLocation(String contentLocation)
    {
        this.contentLocation=contentLocation;
    }

    public void setMessageSize(long messageSize)
    {
        this.messageSize=messageSize;
    }

    public void setMessageCount(long messageCount)
    {
        this.messageCount=messageCount;
    }

    public void setMessageType(int messageType)
    {
        this.messageType=messageType;
    }

    public void setPriority(int priority)
    {
        this.priority=priority;
    }

    public void setResponseStatus(int responseStatus)
    {
        this.responseStatus=responseStatus;
    }

    public void setMessageClass(String messageClass)
    {
        this.messageClass=messageClass;
    }

    public void setExpiry(long expiry)
    {
        this.expiry=expiry;
    }

    public void setMmsStatus(int mmsStatus)
    {
        this.mmsStatus=mmsStatus;
    }

    public void setParts(Vector<MultimediaMessagePart> parts)
    {
        this.parts=parts;
    }

    public int getMessageBox()
    {
        return messageBox;
    }

    public void setMessageBox(int messageBox)
    {
        this.messageBox=messageBox;
    }

    @Override
    public boolean isIncoming()
    {
        if(messageType==PduHeaders.MESSAGE_TYPE_SEND_REQ || messageType==PduHeaders.MESSAGE_TYPE_FORWARD_REQ)
            return false;
        if(messageType==PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF || messageType==PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND)
            return true;
        if(this.messageBox== MESSAGE_BOX_INBOX)
            return true;
        return false;
    }

    @Override
    public boolean isSent() {
        return !isIncoming();
    }

    @Override
    public boolean isDraft()
    {
        if(this.messageBox== MESSAGE_BOX_DRAFT)
            return true;
        return false;
    }

    @Override
    public boolean isPending()
    {
        if(this.type== MESSAGE_BOX_QUEUED || this.type==MESSAGE_BOX_OUTBOX)
            return true;
        return false;
    }

    @Override
    public boolean isMms() {
        return true;
    }

    @Override
    public boolean isSms() {
        return false;
    }

    @Override
    public synchronized boolean protect(Context context) {
        throw new IllegalStateException("MMS protection not implemented, yet");
    }

    @Override
    public boolean unprotect(Context context) {
        throw new IllegalStateException("MMS unprotection not implemented, yet");
    }

    public String computeAddress(Context context) {
        String selectionAdd = new String("msg_id=" + id);
        String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
        Uri uriAddress = Uri.parse(uriStr);
        Cursor cAdd = context.getContentResolver().query(uriAddress, null,
                selectionAdd, null, null);
        String name = null;
        if (cAdd.moveToFirst()) {
            do {
                String number = cAdd.getString(cAdd.getColumnIndex("address"));
                if (number != null) {
                    try {
                        Long.parseLong(number.replace("-", ""));
                        name = number;
                    } catch (NumberFormatException nfe) {
                        if (name == null) {
                            name = number;
                        }
                    }
                }
            } while (cAdd.moveToNext());
        }
        if (cAdd != null) {
            cAdd.close();
        }
        this.address=name;
        return name;
    }
}
