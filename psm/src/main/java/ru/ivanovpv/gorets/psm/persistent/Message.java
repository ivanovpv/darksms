package ru.ivanovpv.gorets.psm.persistent;

import android.content.Context;

import java.io.Serializable;
import java.text.DateFormat;

import android.util.Log;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.MessageBox;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.nativelib.NativeRandom;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolException;
import ru.ivanovpv.gorets.psm.protocol.ProtocolScramble;

import java.util.Calendar;
import java.util.Date;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 494 $
 *   $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/Message.java $
 */

public class Message implements Serializable
{
    private final static String TAG=Message.class.getName();
    public static final String EMPTY_ADDRESS="";
    //message sent/delivery statuses
    public static final int STATUS_NONE = -1;
    public static final int STATUS_COMPLETE = 0;
    public static final int STATUS_PENDING = 32;
    public static final int STATUS_FAILED = 64;
    //message boxes
    public static final int MESSAGE_BOX_ALL= 0;
    public static final int MESSAGE_BOX_INBOX= 1;
    public static final int MESSAGE_BOX_SENT= 2;
    public static final int MESSAGE_BOX_DRAFT= 3;
    public static final int MESSAGE_BOX_OUTBOX = 4;
    public static final int MESSAGE_BOX_FAILED= 5; // for failed outgoing messages
    public static final int MESSAGE_BOX_QUEUED= 6; // for messages to send later

    protected String id; //message Id (_ID field in phone DB)
    protected String address;
    protected int type;
    protected String body;
    protected Date date;
    protected String conversationId; //conversation ID (thread_id field in phone DB)
    protected Boolean privateFlag = false;
    protected int sentStatus;
    protected int deliveredStatus;
    protected int read; //0 - unread, 1 - read, used by stock alike messengers
    protected int seen; //0 - unseen, 1 - seen, used by stock alike messengers
    protected MessagePersistent messagePersistent;

    private transient String displayName; //contact display name
    private transient String contactId; // contactId from contacts list if sender/recipient exists in contacts
    private transient Calendar calendar=Calendar.getInstance();

    public Message()
    {
        address=displayName=EMPTY_ADDRESS;
        id=conversationId=body=contactId=null;
        date=new Date(System.currentTimeMillis());
        messagePersistent=new MessagePersistent(this, 0, "");
    }

    public Date getDate() {
        return date;
    }

    public Calendar getCalendar() {
        calendar.setTime(date);
        return calendar;
    }

    public void setDate(long ldate) {
        this.date = new Date(ldate);
    }

    public Boolean getPrivateFlag() {
        return privateFlag;
    }

    public void setPrivateFlag(Boolean privateFlag) {
        this.privateFlag = privateFlag;
    }

    public static Message createSendMessage(String address, String body, String realBody)
    {
        Message message=new Message();
        message.setAddress(address);
        message.setType(MESSAGE_BOX_SENT);
        message.setBody(new String(body));
        message.setMillis(System.currentTimeMillis());
        message.setSentStatus(STATUS_NONE);
        message.setDeliveredStatus(STATUS_NONE);
        message.setRead(1);
        message.setSeen(1);
        MessagePersistent mp=new MessagePersistent(message, 1, message.getStoredBody());
        if(realBody!=null)
            mp.setRealBody(realBody);
        message.setMessagePersistent(mp);
        return message;
    }

    public static Message createIncomingMessage(String address, String body, String realBody)
    {
        Message message=new Message();
        message.setAddress(address);
        message.setType(MESSAGE_BOX_INBOX);
        message.setBody(body);
        message.setMillis(System.currentTimeMillis());
        message.setSentStatus(STATUS_NONE);
        message.setDeliveredStatus(STATUS_NONE);
        MessagePersistent mp=new MessagePersistent(message, 0, message.getStoredBody());
        if(realBody!=null)
            mp.setRealBody(realBody);
        message.setMessagePersistent(mp);
        return message;
    }

    @Override
    public String toString()
    {
        StringBuilder sb=new StringBuilder();
        return sb.append("Message id=").append(this.getId()).
                append("\n conversation id=").append(this.getConversationId()).
                append("\n body=").append(this.getBody()).
                append("\n address=").append(this.getAddress()).
                append("\n timestamp=").append(this.getDate()).toString();
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address) {
        this.address=address;
    }

    public String getBody() {
        return body;
    }

    public String getStoredBody() {
        if(messagePersistent.getStoredBody()==null || messagePersistent.getStoredBody().length()==0)
            return body;
        else
            return messagePersistent.getStoredBody();
    }

    public boolean isBodyReal() {
        if(messagePersistent.getStoredBody()==null || messagePersistent.getStoredBody().length()==0)
            return true;
        return false;
    }

    public void setBody(String body) {
        this.body=body;
    }

    public void setRealBody(String realBody) {
        messagePersistent.setRealBody(realBody);
    }

    public long getMillis()
    {
        return date.getTime();
    }

    public void setMillis(long millis)
    {
        this.date=new Date(millis);
    }

    public String getConversationId()
    {
        return conversationId;
    }

    public void setConversationId(String conversationId)
    {
        this.conversationId=conversationId;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
        this.messagePersistent.setId(id);
    }

    public boolean isIncoming()
    {
        if(this.type== MESSAGE_BOX_INBOX || this.type== MESSAGE_BOX_ALL)
            return true;
        return false;
    }

    public boolean isSent()
    {
        //if(this.type== MESSAGE_BOX_SENT && (this.sentStatus==STATUS_NONE || this.sentStatus==STATUS_COMPLETE))
        if(this.type== MESSAGE_BOX_SENT)
            return true;
        return false;
    }

    public boolean isSentSuccess()
    {
        if(this.type== MESSAGE_BOX_SENT && (this.sentStatus==STATUS_NONE || this.sentStatus==STATUS_COMPLETE))
            return true;
        return false;
    }

    public boolean isDraft()
    {
        if(this.type== MESSAGE_BOX_DRAFT)
            return true;
        return false;
    }

    public boolean isFailed()
    {
        if(this.type== MESSAGE_BOX_FAILED || this.sentStatus==STATUS_FAILED)
            return true;
        return false;
    }

    public boolean isPending()
    {
        if(this.type== MESSAGE_BOX_QUEUED || this.sentStatus==STATUS_PENDING)
            return true;
        return false;
    }

    public boolean isMms() {
        return false;
    }

    public boolean isSms() {
        return true;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getContactAddress() {
        if(displayName==null || displayName.length()==0)
            return this.getAddress();
        return displayName;
    }

    public String getFullDate(Context context) {
        calendar.setTimeInMillis(date.getTime());
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(calendar.getTime());
    }

    public String getDate(Context context) {
        calendar.setTimeInMillis(date.getTime());
        StringBuilder sb=new StringBuilder();
        Calendar now=Calendar.getInstance();
        if(calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR))
            sb.append(context.getString(R.string.today));
        else if((calendar.get(Calendar.DAY_OF_YEAR) + 1) == now.get(Calendar.DAY_OF_YEAR) && calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR))
            sb.append(context.getString(R.string.yesterday));
        else
            sb.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(calendar.getTime()));
        return sb.toString();
    }

    public String getTime(Context context) {
        calendar.setTimeInMillis(date.getTime());
        StringBuilder sb=new StringBuilder();
        sb.append(DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime()).toString()); //format time using system default locale
        return sb.toString();
    }

    public String getReplyAddress() {
        return null;
    }

    public int getSentStatus() {
        return sentStatus;
    }

    public void setSentStatus(int sentStatus) {
        this.sentStatus = sentStatus;
    }

    public int getDeliveredStatus() {
        return deliveredStatus;
    }

    public void setDeliveredStatus(int deliveredStatus) {
        this.deliveredStatus = deliveredStatus;
    }

    public boolean isPlain() {
        return Protocol.isPlain(body);
    }

    public boolean isInvitation() {
        if(Me.TEST)
            return Protocol.isTestInvitation(body);
        return Protocol.isInvitation(body);
    }

    public boolean isAccept() {
        if(Me.TEST)
            return Protocol.isTestAccept(body);
        return Protocol.isAccept(body);
    }

    public synchronized boolean isCiphered() {
        return Protocol.isCiphered(body);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isProcessed() {
        return messagePersistent.isProcessed() || (read==0);
    }

    public void setProcessed(boolean processed) {
        read=1;
        messagePersistent.setId(this.id);
        messagePersistent.setProcessed(processed);
    }

    public MessagePersistent getMessagePersistent() {
        return messagePersistent;
    }

    public void setMessagePersistent(MessagePersistent messagePersistent) {
        this.messagePersistent = messagePersistent;
    }

/*    public boolean getProcessed() {
        return read!=1;
    }*/

    public int getRead() {
        return read;
    }

    public void setRead(int read) {
        this.read = read;
    }

    public int getSeen() {
        return seen;
    }

    public void setSeen(int seen) {
        this.seen = seen;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public synchronized boolean isProtected() {
        return Protocol.isScrambled(body);
    }

    public synchronized boolean protect(Context context) {
        final Cipher cipher= Hash.getDefaultSymmetricCipher();
        NativeRandom r=new NativeRandom();
        Protocol protocol=new ProtocolScramble(cipher, r.getInt(Protocol.SESSION_SIZE));
        this.setBody(protocol.encodeString(this.getBody()));
        //return (Me.getMe().getMessageDAO().save(context, this)!=null)?true:false;
        Me.getMe().getMessageDAO().updateSmsBody(context, id, body);
        Me.getMe().getMessageDAO().updateLastConversation(context, conversationId);
        return true;
    }

    public boolean unprotect(Context context) {
        try {
            Protocol protocol=Protocol.parseProtocol(this.getBody());
            this.setBody(protocol.decodeString(this.getBody()));
        }
        catch (Exception e) {
            if(Me.DEBUG)
                Log.e(TAG, "Error decrypting message", e);
            new MessageBox(context, context.getString(R.string.cantUnprotect));
            return false;
        }
        Me.getMe().getMessageDAO().updateSmsBody(context, id, body);
        Me.getMe().getMessageDAO().updateLastConversation(context, conversationId);
        return true;
    }

    public String getUnprotected(Context context) {
        try {
            Protocol protocol=Protocol.parseProtocol(this.getBody());
            return protocol.decodeString(this.getBody());
        }
        catch (Exception e) {
            if(Me.DEBUG)
                Log.e(TAG, "Error decrypting message", e);
            return this.getBody();
        }
    }

    public synchronized boolean unprotectSilent(Context context) {
        try {
            Protocol protocol=Protocol.parseProtocol(this.getBody());
            this.setBody(protocol.decodeString(this.getBody()));
        }
        catch (Exception e) {
            if(Me.DEBUG)
                Log.w(TAG, "Error decrypting message", e);
            return false;
        }
        return (Me.getMe().getMessageDAO().save(context, this)!=null)?true:false;
    }

    public synchronized boolean decipherSilent(Context context) {
        Protocol protocol;
        String s;
        if(!Protocol.isCiphered(body))
            return true;
        byte[] sharedKey=Me.getMe().getContactDAO().getSharedKey(context, this.getAddress(), this.getMillis());
        if(sharedKey!=null) {
            try {
                protocol=Protocol.parseProtocol(body, sharedKey);
                s=protocol.decodeString(body);
                this.body=s;
            }
            catch (ProtocolException e) {
                if(Me.DEBUG)
                    Log.e(TAG, "Error decrypting message", e);
                return false;
            }
        }
        else {
            return false;
        }
        return (Me.getMe().getMessageDAO().save(context, this)!=null)?true:false;
    }

    public String getDecrypted(Context context) {
        Protocol protocol;
        String s;
        if(!Protocol.isCiphered(body))
            return body;
        byte[] sharedKey=Me.getMe().getContactDAO().getSharedKey(context, this.getAddress(), this.getMillis());
        if(sharedKey!=null) {
            try {
                protocol=Protocol.parseProtocol(body, sharedKey);
                s=protocol.decodeString(body);
                return s;
            }
            catch (ProtocolException e) {
                if(Me.DEBUG)
                    Log.e(TAG, "Error decrypting message", e);
                return body;
            }
        }
        return body;
    }


}
