/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.persistent;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.mms.pdu.PduPart;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Pavel on 25.06.2014.
 */
public class MultimediaMessagePart implements Serializable {
    private final static String TAG=MultimediaMessagePart.class.getName();

    private String id;          // _id - part id
    private String messageId;   //mid - message id
    private String contentType; //ct
    private String name;        //name
    private int charSet;        //chset
    private String contentDisposition;    //cd (WTF?)
    private String fileName;    //fn
    private String contentId;   //cid
    private String contentLocation;   //cl
    private String text;        //text

    private byte[] data;        //_data
    private Uri dataUri;

    /**
     * constructor called for incoming mms
     * @param pduPart
     */
    public MultimediaMessagePart(PduPart pduPart) {
        this.data=pduPart.getData(); //_data
        dataUri=pduPart.getDataUri(); //
        contentId=ByteUtils.byteArrayToString(pduPart.getContentId(), 0); //cid
        charSet=pduPart.getCharset(); //chset
        contentLocation=ByteUtils.byteArrayToString(pduPart.getContentLocation(), 0); //cl
        contentDisposition=ByteUtils.byteArrayToString(pduPart.getContentDisposition(), 0); //cd
        contentType=ByteUtils.byteArrayToString(pduPart.getContentType(), 0); //ct
        name=ByteUtils.byteArrayToString(pduPart.getName(),0); //name
        fileName=ByteUtils.byteArrayToString(pduPart.getFilename(),0); //filename
    }

    /**
     * constructor called to create MMS from device's database
     * @param id
     * @param messageId
     * @param contentType
     * @param name
     * @param charSet
     * @param contentDisposition
     * @param fileName
     * @param contentId
     * @param contentLocation
     * @param text
     */
    public MultimediaMessagePart(String id, String messageId, String contentType, String name, int charSet, String contentDisposition, String fileName, String contentId, String contentLocation, String text) {
        this.id=id;
        this.messageId=messageId;
        this.contentType=contentType;
        this.charSet=charSet;
        this.contentDisposition=contentDisposition;
        this.fileName=fileName;
        this.contentId=contentId;
        this.contentLocation=contentLocation;
        this.text=text;
    }

    public void debug(String tag) {
        Log.i(tag, "id="+id+", messageId="+messageId+", contentType="+contentType+", charset="+charSet+
                ", contentDisposition="+contentDisposition+", filename="+fileName+", contentId="+contentId+
                ", contentLocation="+contentLocation+", text="+text);
    }

    public String getString() {
        if(text!=null)
            return text;
        if(data!=null)
            return ByteUtils.byteArrayToString(data, 0);
        return "";
    }

    public byte[] getData()
    {
        return data;
    }

    public Uri getDataUri()
    {
        return dataUri;
    }

    public String getContentId()
    {
        return contentId;
    }

    public int getCharSet()
    {
        return charSet;
    }

    public String getContentLocation()
    {
        return contentLocation;
    }

    public String getContentDisposition()
    {
        return contentDisposition;
    }

    public String getContentType()
    {
        return contentType;
    }

    public String getName()
    {
        return name;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
