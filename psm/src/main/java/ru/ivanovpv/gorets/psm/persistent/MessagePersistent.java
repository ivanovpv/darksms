package ru.ivanovpv.gorets.psm.persistent;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 491 $
 *   $LastChangedDate: 2014-01-30 08:02:09 +0400 (Чт, 30 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/MessagePersistent.java $
 */

import java.io.Serializable;

public class MessagePersistent implements Serializable {
    private String id;
    private String conversationId;
    private int processed;    //0 - not processed (read), 1 - processed (read)
    private String storedBody;

    public MessagePersistent() {
        id=null;
        conversationId=null;
        processed=1;
        storedBody="";
    }

    public MessagePersistent(Message message, int processed, String storedBody) {
        id=message.getId();
        conversationId=message.getConversationId();
        this.processed=processed;
        this.storedBody=storedBody;
    }

    /*public MessagePersistent(String id, String conversationId, int processed, String storedBody) {
        this.id=id;
        this.conversationId=conversationId;
        this.processed=processed;
        this.storedBody=storedBody;
    }*/

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getProcessed() {
        return processed;
    }

    public boolean isProcessed() {
        return (processed==0)?false:true;
    }

    public void setProcessed(int processed) {
        this.processed=processed;
    }

    public void setProcessed(boolean processed) {
        this.processed=(processed)?1:0;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getStoredBody() {
        return storedBody;
    }

    public void setRealBody(String storedBody) {
        this.storedBody=storedBody;
    }
}
