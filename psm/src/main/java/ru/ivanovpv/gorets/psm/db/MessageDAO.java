package ru.ivanovpv.gorets.psm.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.mms.ContentType;
import ru.ivanovpv.gorets.psm.persistent.*;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 495 $
 *   $LastChangedDate: 2014-02-03 22:50:58 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/MessageDAO.java $
 */

public class MessageDAO extends AbstractBaseDAO<Message>
{
    private final static String TAG = MessageDAO.class.getName();
    //private final static String CANONICAL_ADDRESS_URL="content://mms-sms/canonical-address/";
    private final static String CANONICAL_ADDRESSES_URL="content://mms-sms/canonical-address/";
    private final static String CONVERSATIONS_URL="content://mms-sms/conversations/";
    public final static String CONVERSATIONS_SIMPLE_URL="content://mms-sms/conversations?simple=true";
    private final static String MESSAGE_SEARCH_URL="content://mms-sms/search";
    //private final static String CONVERSATIONS_URL="content://sms/conversations";
    public final static Uri CONVERSATIONS_SIMPLE_URI=Uri.parse(CONVERSATIONS_SIMPLE_URL);
    public final static Uri SMS_URI=Uri.parse("content://sms/");
    public final static Uri SMS_INBOX_URI =Uri.parse("content://sms/inbox");
    public final static Uri SMS_SENT_URI =Uri.parse("content://sms/sent");
    public final static Uri SMS_DRAFTS_URI =Uri.parse("content://sms/draft");
    public final static String DATE_ASC="date asc";
    public final static String DATE_DESC="date desc";
    public static final String PERSON="person";
    public static final String ADDRESS="address";
    public static final String BODY="body";
    public static final String DATE="date";
    public static final String TYPE="type";
    public static final String UNREAD_COUNT="unread_count";
    public static final String UNPROCESSED_COUNT="unprocessed_count";
    public static final String CONVERSATION_ID="thread_id";
    public static final String _ID="_id";
    public static final String STATUS="status";
    public static final String READ="read";
    public static final String SEEN="seen";
    public static final String ROWID="rowid";

    public static final String RECIPIENT_IDS="recipient_ids";
    public static final String SNIPPET="snippet";

    public final static String MMS_URL="content://mms";
    public final static Uri MMS_URI =Uri.parse(MMS_URL);
    public final static Uri MMS_INBOX_URI =Uri.parse("content://mms/inbox");
    public final static Uri MMS_SENT_URI =Uri.parse("content://mms/sent");
    public final static Uri MMS_DRAFTS_URI =Uri.parse("content://mms/drafts");
    public final static Uri MMS_OUTBOX_URI =Uri.parse("content://mms/outbox");
    public static final Uri MMS_PENDING_URI = Uri.withAppendedPath(MMS_URI, "pending");

    public final static String MMS_PART_URL=MMS_URL+"/{0}/part";
    public final static String MMS_ADDRESS_URL=MMS_URL+"/{0}/addr";
    public final static String MMS_CONTENT_LOCATION="ct_l";
    public final static String MMS_CONTENT_TYPE="ct_t";
    public final static String MMS_CHARSET="charset";
    public final static String MMS_MESSAGE_BOX="msg_box";
    public final static String MMS_SUBJECT="sub";
    public final static String MMS_SUBJECT_CS="sub_cs";
    public final static String MMS_EXPIRY="exp";
    public final static String MMS_MESSAGE_SIZE="m_size";
    public final static String MMS_MESSAGE_CLASS="m_cls";
    public final static String MMS_MESSAGE_TYPE="m_type";
    public final static String MMS_VERSION="v";
    public final static String MMS_PRIORITY="pri";
    public final static String MMS_TRANSACTION_ID="tr_id";
    public final static String MMS_STATUS="st";
    public final static String MMS_RESPONSE_STATUS="resp_st";

    public final static String MMS_PART_MSG_ID="mid";
    public final static String MMS_PART_SEQUENCE="seq";
    public final static String MMS_PART_CONTENT_TYPE="ct";
    public final static String MMS_PART_NAME="name";
    public final static String MMS_PART_CHARSET="chset";
    public final static String MMS_PART_CONTENT_DISPOSITION="cd";
    public final static String MMS_PART_FILENAME="fn";
    public final static String MMS_PART_CONTENT_ID="cid";
    public final static String MMS_PART_CONTENT_LOCATION="cl";
    public final static String MMS_PART_DATA="_data";
    public final static String MMS_PART_TEXT="text";

    Type messagePersistentType = new TypeToken<List<MessagePersistent>>(){}.getClass();

    public MessageDAO(final DbMainHelper dbHelper)
    {
        super(dbHelper);
    }

    private ContentValues mapToContentValues(final MessagePersistent messagePersistent)
    {
        final Gson gson = new Gson();
        String s=gson.toJson(messagePersistent, MessagePersistent.class);
        byte[] buffer=ByteUtils.stringToByteArray(s);
        ContentValues cv=new ContentValues();
        cv.put(DbMainHelper.AUX_MESSAGE_ID, messagePersistent.getId());
        cv.put(DbMainHelper.AUX_CONVERSATION_ID, messagePersistent.getConversationId());
        cv.put(DbMainHelper.AUX_READ, messagePersistent.isProcessed());
        cv.put(DbMainHelper.AUX_BLOB, buffer);
        return cv;
    }

    public boolean savePersistent(Message message) {
        if(message==null)
            return false;
        if(message.getType()==Message.MESSAGE_BOX_DRAFT)
            return true;
        //try to update
        try {
            int recs = this.database.update(DbMainHelper.AUX_TABLE, this.mapToContentValues(message.getMessagePersistent()),
                DbMainHelper.AUX_MESSAGE_ID+"="+ message.getId(), null);
            if(recs==1) {
                return true;
            }
            else //if there's no anything insert new record
            {
                this.database.insert(DbMainHelper.AUX_TABLE, null, this.mapToContentValues(message.getMessagePersistent()));
                return true;
            }
        }
        catch (SQLiteException e) {
            Log.e(TAG, "Can't insert record!", e);
        }
        return false;
    }

    public boolean updateSentStatus(Context context, Message message) {
        if(message.getId()==null)
            return false;
        database.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(STATUS, message.getSentStatus());
        int recs=context.getContentResolver().update(SMS_SENT_URI, values, _ID+" = "+message.getId(), null);
        database.setTransactionSuccessful();
        database.endTransaction();
        Message msg=this.get(context, message.getId());
        return (recs >= 0) ? true : false;
    }

    /**
     * Saves SMS message to SMS database
     * @param context current Context
     * @param message message to save - doesn't modified!
     * @return saved message
     */
    @Override
    public Message save(Context context, Message message)
    {
        if(message==null)
            return null;
        long now=System.currentTimeMillis();
        database.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(BODY, message.getBody());
        values.put(READ, message.getRead());
        values.put(DATE, (message.getMillis() == 0L) ? now : message.getMillis());
        values.put(SEEN, message.getSeen());
        values.put(STATUS, message.getSentStatus());
        values.put(ADDRESS, message.getAddress());
        ContactInfo contactInfo=Me.getMe().getContactDAO().getContactInfoByAddress(context, message.getAddress());
        values.put(PERSON, contactInfo.id); //reference to contact id
        if(message.getConversationId()==null)
            values.put(CONVERSATION_ID, getOrCreateThreadId(context, message.getAddress()));
        else
            values.put(CONVERSATION_ID, message.getConversationId());
        values.put(TYPE, message.getType());
        if (message.getId() == null) {   //insert new message
            Uri uri;
            if (message.isIncoming()) { //insert to inbox
                uri = context.getContentResolver().insert(MessageDAO.SMS_INBOX_URI, values);
            } else if (message.isSent()) {   //insert to sent
                uri = context.getContentResolver().insert(MessageDAO.SMS_SENT_URI, values);
            } else if (message.isDraft()) {
                uri = context.getContentResolver().insert(MessageDAO.SMS_DRAFTS_URI, values);
            } else {
                Log.w(TAG, "Unknown message type to save=" + message.getType());
                uri = context.getContentResolver().insert(MessageDAO.SMS_URI, values);
            }
            if (uri != null) {
                Message savedMessage = this.getSmsMessage(context, uri); //update real message id and conversation id
                message.setId(savedMessage.getId());
                message.setConversationId(savedMessage.getConversationId());
                MessagePersistent mp=new MessagePersistent(savedMessage, message.isProcessed()?1:0, message.getStoredBody());
                message.setMessagePersistent(mp);
            }
        } else {   //update existing message
            int rows;
/*            Uri uri=Uri.parse("content://sms/"+message.getId());
        rows=context.getContentResolver().update(uri, values, null, null);*/
            rows = context.getContentResolver().update(SMS_URI, values, _ID + "=?", new String[]{message.getId()});
        }
        savePersistent(message);
        database.setTransactionSuccessful();
        database.endTransaction();
        return message;
    }

    public void updateSmsBody(Context context, String messageId, String body)
    {
//        database.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(BODY, body);
        int rows;
            rows = context.getContentResolver().update(Uri.parse("content://sms/" + messageId), values, null, null);
        //savePersistent(message);
    //    database.setTransactionSuccessful();
    //    database.endTransaction();
    }

    public void updateLastConversation(Context context, String threadId)
    {
        String selection=CONVERSATION_ID+"=?";
        Cursor cursor=this.getMmsSmsCursor(context, selection, new String[]{threadId});
        Message message;
        if(cursor.moveToLast()) { //looking to last one
            if(isMms(cursor)) {
                message = getMmsMessage(context, cursor);
            }
            else {
                message = getSmsMessage(context, cursor);
                updateSmsBody(context, message.getId(), message.getBody());
            }
        }
    }

    /**
     * Saves MMS message to MMS database (saves only header)
     * @param context current Context
     * @param multimediaMessage message to save - doesn't modified!
     * @return saved message
     */
    public MultimediaMessage saveMms(Context context, MultimediaMessage multimediaMessage)
    {
        if(multimediaMessage==null)
            return null;
        long now;
        if(multimediaMessage.getMillis()<=0) {
            now=System.currentTimeMillis()/1000;
        }
        else
            now=multimediaMessage.getMillis()/1000;
        database.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(READ, multimediaMessage.getRead());
        values.put(DATE, now);
        values.put(SEEN, multimediaMessage.getSeen());
        if(multimediaMessage.getConversationId()==null)
            values.put(CONVERSATION_ID, getOrCreateThreadId(context, multimediaMessage.getAddress()));
        else
            values.put(CONVERSATION_ID, multimediaMessage.getConversationId());
        values.put(MMS_MESSAGE_BOX, multimediaMessage.getMessageBox());
        values.put(MMS_MESSAGE_TYPE, multimediaMessage.getMessageType());
        values.put(MMS_SUBJECT, multimediaMessage.getSubject());
        values.put(MMS_SUBJECT_CS, 106); //utf-8
        values.put(MMS_CONTENT_LOCATION, multimediaMessage.getContentLocation());
        //values.put(MMS_CONTENT_TYPE, multimediaMessage.getContentType());
        values.put(MMS_CONTENT_TYPE, ContentType.MULTIPART_RELATED);
        values.put(MMS_EXPIRY, multimediaMessage.getExpiry());
        values.put(MMS_MESSAGE_SIZE, multimediaMessage.getMessageSize());
        values.put(MMS_MESSAGE_CLASS, multimediaMessage.getMessageClass());
        values.put(MMS_MESSAGE_TYPE, multimediaMessage.getMessageType()); // 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
        values.put(MMS_VERSION, multimediaMessage.getMmsVersion());
        values.put(MMS_PRIORITY, multimediaMessage.getPriority());
        values.put(MMS_TRANSACTION_ID, multimediaMessage.getTransactionId());
        values.put(MMS_STATUS, multimediaMessage.getMmsStatus());
        values.put(MMS_RESPONSE_STATUS, multimediaMessage.getResponseStatus());
        if (multimediaMessage.getId() == null) {   //insert new message
            Uri uri;
            uri = context.getContentResolver().insert(MessageDAO.MMS_URI, values);
            if (uri != null) {
                MultimediaMessage savedMessage = this.getMmsMessage(context, uri); //update real message id and conversation id
                multimediaMessage.setId(savedMessage.getId());
                multimediaMessage.setConversationId(savedMessage.getConversationId());
            }
        } else {   //update existing message
            int rows;
/*            Uri uri=Uri.parse("content://sms/"+message.getId());
        rows=context.getContentResolver().update(uri, values, null, null);*/
            rows = context.getContentResolver().update(MMS_URI, values, _ID + "=?", new String[]{multimediaMessage.getId()});
        }
        savePersistent(multimediaMessage);
        database.setTransactionSuccessful();
        database.endTransaction();
        return multimediaMessage;
    }

    /**
     * reads all attachments and if successful returns true
     * @param context
     * @param mms
     * @return boolean
     */
    private boolean readMmsMessageParts(Context context, MultimediaMessage mms) {
        Uri partUri=Uri.parse(MessageFormat.format(MMS_PART_URL, mms.getId()));
        Cursor cursor=context.getContentResolver().query(partUri, new String[] {"*"}, null, null, MMS_PART_SEQUENCE+" ASC");
        while(cursor!=null && cursor.moveToNext()) {
            if(Me.DEBUG) {
                Log.i(TAG, "mms id="+mms.getId()+" parts *********************************");
                debugCursor(cursor);
            }
            String id = cursor.getString(cursor.getColumnIndex(_ID));
            String messageId = cursor.getString(cursor.getColumnIndex(MMS_PART_MSG_ID));
            String contentType = cursor.getString(cursor.getColumnIndex(MMS_PART_CONTENT_TYPE));
            String name = cursor.getString(cursor.getColumnIndex(MMS_PART_NAME));
            int charset = cursor.getInt(cursor.getColumnIndex(MMS_PART_CHARSET));
            String contentDisposition = cursor.getString(cursor.getColumnIndex(MMS_PART_CONTENT_DISPOSITION));
            String fileName = cursor.getString(cursor.getColumnIndex(MMS_PART_FILENAME));
            String contentId = cursor.getString(cursor.getColumnIndex(MMS_PART_CONTENT_ID));
            String contentLocation = cursor.getString(cursor.getColumnIndex(MMS_PART_CONTENT_LOCATION));
            String text=cursor.getString(cursor.getColumnIndex(MMS_PART_TEXT));
            MultimediaMessagePart multimediaMessagePart = new MultimediaMessagePart(id, messageId, contentType, name, charset, contentDisposition, fileName, contentId, contentLocation, text);
            multimediaMessagePart.debug(TAG);
            mms.addPart(multimediaMessagePart);
        }
        if(cursor!=null)
            cursor.close();
        if(mms.getPartsCount() >= mms.getMessageCount()) //compares real and declared parts
            return true;
        return false;
    }


    public static Bitmap getMmsDataAsImage(Context context, Uri partUri) {
        InputStream is=null;
        Bitmap bitmap=null;
        try {
            is=context.getContentResolver().openInputStream(partUri);
            bitmap=BitmapFactory.decodeStream(is);
        }
        catch(Exception ex) {
            Log.w(TAG, "Can't read mms image data", ex);
            return null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return bitmap;
    }

    public static String getMmsDataAsText(Context context, Uri partUri) {
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partUri);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException ex) {
            Log.w(TAG, "Can't read mms text data", ex);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
        return sb.toString();
    }

    @Deprecated
    public MultimediaMessage saveMmsParts(Context context, MultimediaMessage message)
    {
        MultimediaMessagePart mmsPart;
        Uri partUri, res;
        if(message.getId()==null)   //if not saved yet save mms headers
            saveMms(context, message);
        database.beginTransaction();
        for(int index=0; index < message.getPartsCount(); index++)
        {
            mmsPart=message.getPart(index);
            ContentValues mmsPartValue = new ContentValues();
            mmsPartValue.put(MMS_PART_MSG_ID, message.getId());  //mid
            mmsPartValue.put(MMS_PART_SEQUENCE, index);   //seq
            mmsPartValue.put(MMS_PART_CONTENT_TYPE, mmsPart.getContentType()); //ct
            mmsPartValue.put(MMS_PART_CONTENT_LOCATION, mmsPart.getContentLocation()); //cl
            mmsPartValue.put(MMS_PART_CONTENT_ID, mmsPart.getContentId()); //cid
            mmsPartValue.put(MMS_PART_CONTENT_DISPOSITION, mmsPart.getContentDisposition()); //cd
            mmsPartValue.put(MMS_PART_CHARSET, mmsPart.getCharSet()); //chset
            mmsPartValue.put(MMS_PART_FILENAME, mmsPart.getFileName()); //filename
            mmsPartValue.put(MMS_PART_NAME, mmsPart.getName()); //name

            partUri=Uri.parse(MessageFormat.format(MMS_PART_URL, message.getId()));
            res=context.getContentResolver().insert(partUri, mmsPartValue);
            if(res==null) {
                Log.w(TAG, "Something went wrong during part saving"+message);
                return message;
            }
            if(Me.DEBUG)
                Log.i(TAG, "Part uri is " + res);
            OutputStream os;
            ByteArrayInputStream is=null;
            try
            {
                os=context.getContentResolver().openOutputStream(res);
                // Add data to part
                is=new ByteArrayInputStream(message.getPart(index).getData());
                byte[] buffer=new byte[256];
                for(int len=0; (len=is.read(buffer)) != -1; )
                    os.write(buffer, 0, len);
                os.close();
                if(is != null)
                    is.close();
            }
            catch(Exception ex) {
                Log.w(TAG, "Error trying to save MMS part, message id="+message.getId(), ex);
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return message;
    }

    /**
     * Saves message to SMS database
     * @param context current Context
     * @param message message to save - doesn't modified!
     * @return saved message
     */

    @Deprecated
    public Message saveByThread(Context context, Message message)
    {
        Message savedMessage=null;
        if(message==null)
            return null;
        ContentValues values = new ContentValues();
        values.put(BODY, message.getBody());
        values.put(READ, message.getRead());
        values.put(DATE, message.getMillis());
        values.put(SEEN, message.getSeen());
        values.put(STATUS, message.getSentStatus());
        values.put(ADDRESS, message.getAddress());
        ContactInfo contactInfo=Me.getMe().getContactDAO().getContactInfoByAddress(context, message.getAddress());
        values.put(PERSON, contactInfo.id); //reference to contact id
        if(message.getConversationId()==null)
            values.put(CONVERSATION_ID, getOrCreateThreadId(context, message.getAddress()));
        else
            values.put(CONVERSATION_ID, message.getConversationId());
        values.put(TYPE, message.getType());
        Uri uri;
        if(message.getId()==null) {
            uri=context.getContentResolver().insert(MessageDAO.SMS_URI, values);
            if(uri!=null) {
                savedMessage=this.getSmsMessage(context, uri); //update real message id and conversation id
                savedMessage.setMessagePersistent(message.getMessagePersistent());
                savedMessage.getMessagePersistent().setId(savedMessage.getId());
                savedMessage.getMessagePersistent().setConversationId(savedMessage.getConversationId());
            }
        }
        else
        {   //update existing message
//            values.put(_ID, message.getId());
            //uri=Uri.parse(SMMessageDAO.CONVERSATIONS_URL+message.getConversationId());
            int rows;
            rows=context.getContentResolver().update(SMS_URI, values, _ID+"=?", new String[] {message.getId()});
            if(rows==1)
                savedMessage=message;
        }
        if(savedMessage!=null)
            savePersistent(savedMessage);
        return savedMessage;
    }

    public String getMainCanonicalAddress(Context context, String recipient_ids) {
        recipient_ids=recipient_ids.trim();
        int index=recipient_ids.indexOf(' ');  //may contain list of address ids
        String recipient_id;
        if(index > 0)
            recipient_id=recipient_ids.substring(0, index);
        else
            recipient_id=recipient_ids;
        String[] columns={MessageDAO.ADDRESS};
        Cursor cursor=null;
        String address="";
        try {
            cursor=context.getContentResolver().query(Uri.parse(CANONICAL_ADDRESSES_URL+recipient_id), columns, null, null, null);
        }
        catch(Exception ex) {
            if(cursor!=null)
                cursor.close();
            return address;
        }
        if(cursor==null)
            return address;
        if(cursor.moveToFirst()) {
            address = cursor.getString(cursor.getColumnIndex(MessageDAO.ADDRESS));
        }
        cursor.close();
        return address;
    }

    public Vector<String> getCanonicalAddresses(Context context, String recipient_ids) {
        String[] columns={"*"};
        Cursor cursor=null;
        recipient_ids=recipient_ids.trim();
        Vector<String> addresses=new Vector<String>();
        String[] recipient_id=recipient_ids.split(" ");
        try {
            for(int i=0; i < recipient_id.length; i++) {
                cursor=context.getContentResolver().query(Uri.parse(CANONICAL_ADDRESSES_URL+recipient_id[i]), columns, null, null, null);
                if(cursor==null)
                    return addresses;
                if(cursor.moveToFirst())
                    addresses.add(cursor.getString(cursor.getColumnIndex(MessageDAO.ADDRESS)));
                cursor.close();
            }
        }
        catch(Exception ex) {
            Log.w(TAG, "Ignoring exception during getting recipient info", ex);
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return addresses;
    }

    public Loader<Cursor> getConversationsCursorLoader(Context context, String where) {
        final String[] columns = {"*"};//_ID, RECIPIENT_IDS, SNIPPET, DATE, UNREAD_COUNT};
        String selection;
        String[] selectionArgs=new String[1];
        if (TextUtils.isEmpty(where)) {
            selection = null;
            selectionArgs=null;
        }
        else {
            selection = "snippet match '%"+where+"%'";
            selectionArgs=null;
        }
        return new CursorLoader(context, MessagesContentProvider.CONVERSATION_URI, columns, selection, selectionArgs, DATE_DESC);
//        return context.getContentResolver().query(CONVERSATIONS_URI, columns, selection, null, DATE_DESC);

    }

    public Loader<Cursor> getConversationsCursorLoader(Context context) {
        final String[] columns = {"*"};//_ID, RECIPIENT_IDS, SNIPPET, DATE, UNREAD_COUNT};
        return new CursorLoader(context, MessagesContentProvider.CONVERSATION_URI, columns, null, null, DATE_DESC);
    }

    public Loader<Cursor> getConversationCursorLoader(Context context, String conversationId) {
        Uri uri= MessagesContentProvider.CONVERSATION_URI.buildUpon().appendPath(conversationId).build();
        return new CursorLoader(context, uri, null, null, null, null);
    }

    public Loader<Cursor> getSearchCursorLoader(Context context, String pattern) {
        Uri uri = Uri.parse(MESSAGE_SEARCH_URL).buildUpon().appendQueryParameter("pattern", pattern).build();
        return new CursorLoader(context, uri, null, null, null, null);
    }

    public Cursor getSearchCursor(Context context, String pattern) {
    //fields are
        //Column #0=_id
        //Column #1=thread_id
        //Column #2=address
        //Column #3=body
        //Column #4=date
        //Column #5=index_text
        //Column #6=words._id
        return context.getContentResolver().query(Uri.parse(MESSAGE_SEARCH_URL+"?pattern="+pattern), null, null, null, null);
    }



    public Cursor getSearchSuggestCursor(Context context, String pattern) {
        return context.getContentResolver().query(Uri.parse("content://mms-sms/searchSuggest?pattern="+pattern), null, null, null, null);
    }

    public Cursor getConversationsCursor(Context context, String where) {
        final String[] columns = {"*"};//TYPE, ADDRESS, DATE, BODY, _ID, CONVERSATION_ID, STATUS}; //{"*"}
        String selection;
        String[] selectionArgs=new String[1];
        if (where == null || where.trim().length()==0) {
            selection = null;
            selectionArgs=null;
        }
        else {
            selection = "snippet like '%"+where+"%'";
            selectionArgs=null;
        }
            return context.getContentResolver().query(CONVERSATIONS_SIMPLE_URI, columns, selection, selectionArgs, DATE_DESC);
//        return context.getContentResolver().query(CONVERSATIONS_URI, columns, selection, null, DATE_DESC);
    }

    public Cursor getConversationsCursor(Context context) {
        String[] columns={"*"};
        return context.getContentResolver().query(CONVERSATIONS_SIMPLE_URI, columns, null, null, DATE_DESC);
//        return context.getContentResolver().query(CONVERSATIONS_URI, columns, null, null, DATE_DESC);
    }

    private Cursor getSmsConversationCursor(Context context, String conversationId) {
        String[] columns= {"*"};
        Cursor cursor;
        if(conversationId==null)
            conversationId="0";
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns, CONVERSATION_ID+"=?", new String[]{conversationId}, DATE_ASC);
        } catch (SQLiteException e) {
            Log.e(TAG, "Can't get SMS necessary fields", e);
            return null;
        }
        return cursor;
    }

    private Cursor getMmsConversationCursor(Context context,  String conversationId) {
        String[] columns= {"*"};
        Cursor cursor;
        try {
            cursor=context.getContentResolver().query(MMS_URI, columns, MessageDAO.CONVERSATION_ID + "=?", new String[]{conversationId}, MessageDAO.DATE_ASC);
        } catch (SQLiteException e) {
            Log.e(TAG, "Can't get MMS necessary fields", e);
            return null;
        }
        return cursor;
    }


    public Cursor getMmsSmsConversationCursor(Context context,  String conversationId) {
        Cursor smsCursor=Me.getMe().getMessageDAO().getSmsConversationCursor(context, conversationId);
        Cursor mmsCursor=Me.getMe().getMessageDAO().getMmsConversationCursor(context, conversationId);
        if(mmsCursor.getCount()==0) //if there's no MMS no need to use sorted cursor
            return smsCursor;
        Cursor[] cursors=new Cursor[2];
        cursors[0]=smsCursor;
        cursors[1]=mmsCursor;
        Cursor cursor=new SortedCursor(cursors, DATE);
        return cursor;
    }

    private Cursor getSmsCursor(Context context, String selection, String[] selectionArgs) {
        String[] columns= {"*"};
        Cursor cursor;
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns, selection, selectionArgs, DATE_ASC);
        } catch (SQLiteException e) {
            Log.e(TAG, "Can't get SMS necessary fields", e);
            return null;
        }
        return cursor;
    }

    private Cursor getMmsCursor(Context context, String selection, String[] selectionArgs) {
        String[] columns= {"*"};
        Cursor cursor;
        try {
            cursor=context.getContentResolver().query(MMS_URI, columns, selection, selectionArgs, MessageDAO.DATE_ASC);
        } catch (SQLiteException e) {
            Log.e(TAG, "Can't get MMS necessary fields", e);
            return null;
        }
        return cursor;
    }


    public Cursor getMmsSmsCursor(Context context, String selection, String[] selectionArgs) {
        Cursor smsCursor=Me.getMe().getMessageDAO().getSmsCursor(context, selection, selectionArgs);
        Cursor mmsCursor=Me.getMe().getMessageDAO().getMmsCursor(context, selection, selectionArgs);
        if(mmsCursor.getCount()==0) //if there's no MMS no need to use sorted cursor
            return smsCursor;
        Cursor[] cursors=new Cursor[2];
        cursors[0]=smsCursor;
        cursors[1]=mmsCursor;
        Cursor cursor=new SortedCursor(cursors, DATE);
        return cursor;
    }

    public Message getSmsMessage(Context context, String conversationId, String id) {
        Message message=null;
        String[] columns={"*"};
        Cursor cursor = context.getContentResolver().query(SMS_URI, columns,
                CONVERSATION_ID+"=? and "+MessageDAO._ID+"=?",
                new String[] {conversationId, id}, null);
        if(cursor!=null && cursor.moveToFirst())
            message=this.getSmsMessage(context, cursor);
        if(cursor!=null)
            cursor.close();
        return message;
    }

    public MultimediaMessage getMmsMessage(Context context, String conversationId, String id) {
        String[] columns= {"*"};
        MultimediaMessage message=null;
        Cursor cursor;
        cursor=context.getContentResolver().query(MMS_URI, columns,
                MessageDAO.CONVERSATION_ID + "=? and "+MessageDAO._ID+"=?",
                new String[]{conversationId, id}, null);
        if(cursor!=null && cursor.moveToFirst())
            message=this.getMmsMessage(context, cursor);
        if(cursor!=null)
            cursor.close();
        return message;
    }

    public Message getSmsMessage(Context context, Cursor cursor) {
        if(cursor==null)
            return null;
        String address=cursor.getString(cursor.getColumnIndex(ADDRESS));
        final String body=cursor.getString(cursor.getColumnIndex(BODY));
        long date=cursor.getLong(cursor.getColumnIndex(DATE));
        final int type=cursor.getInt(cursor.getColumnIndex(TYPE));
        final String id=cursor.getString(cursor.getColumnIndex(_ID));
        final String conversation_id=cursor.getString(cursor.getColumnIndex(CONVERSATION_ID));
        final int read=cursor.getInt(cursor.getColumnIndex(READ));
        final int seen=cursor.getInt(cursor.getColumnIndex(SEEN));
        final int status=cursor.getInt(cursor.getColumnIndex(STATUS));

        Message message=new Message();
        message.setAddress(address);
        String[] contactInfo = getContactNameByPhoneNumber(context, address);
        if(contactInfo!=null) {
            message.setDisplayName(contactInfo[0]);
            message.setContactId(contactInfo[1]);
        }
        message.setType(type);

        if(isMms(cursor) )//date<13427696600L)
            date *= 1000; // HACK: in mms date saved in secs instead msecs in sms
        message.setDate(date);
        message.setBody(body);
        message.setId(id);
        message.setConversationId(conversation_id);
        message.setRead(read);
        message.setSeen(seen);
        message.setSentStatus(status);
        message.setMessagePersistent(this.getMessagePersistent(message));
        return message;
    }

    public MultimediaMessage getMmsMessage(Context context, Cursor cursor) {
        if(cursor==null)
            return null;
        final String id=cursor.getString(cursor.getColumnIndex(_ID));
        final String conversation_id=cursor.getString(cursor.getColumnIndex(CONVERSATION_ID));
        final int read=cursor.getInt(cursor.getColumnIndex(READ));
        final int seen=cursor.getInt(cursor.getColumnIndex(SEEN));
        long date=1000*cursor.getLong(cursor.getColumnIndex(DATE));

        final String subject=cursor.getString(cursor.getColumnIndex(MMS_SUBJECT));
        final int mmsVersion=cursor.getInt(cursor.getColumnIndex(MMS_VERSION));
        final String transactionId=cursor.getString(cursor.getColumnIndex(MMS_TRANSACTION_ID));
        final String contentLocation=cursor.getString(cursor.getColumnIndex(MMS_CONTENT_LOCATION));
        final String contentType=cursor.getString(cursor.getColumnIndex(MMS_CONTENT_TYPE));
        final long messageSize=cursor.getLong(cursor.getColumnIndex(MMS_MESSAGE_SIZE));
        final int messageType=cursor.getInt(cursor.getColumnIndex(MMS_MESSAGE_TYPE));
        final int priority=cursor.getInt(cursor.getColumnIndex(MMS_PRIORITY));
        final int responseStatus=cursor.getInt(cursor.getColumnIndex(MMS_RESPONSE_STATUS));
        final String messageClass=cursor.getString(cursor.getColumnIndex(MMS_MESSAGE_CLASS));
        final long expiry=cursor.getLong(cursor.getColumnIndex(MMS_EXPIRY));
        final int mmsStatus=cursor.getInt(cursor.getColumnIndex(MMS_STATUS));
        //final int type=cursor.getInt(cursor.getColumnIndex(TYPE));

        MultimediaMessage mms=new MultimediaMessage();
        mms.setId(id);
        mms.setConversationId(conversation_id);
        mms.setRead(read);
        mms.setSeen(seen);
        mms.setDate(date);
        mms.setSubject(subject);
        mms.setMmsVersion(mmsVersion);
        mms.setTransactionId(transactionId);
        mms.setContentLocation(contentLocation);
        mms.setContentType(contentType);
        mms.setMessageSize(messageSize);
        mms.setMessageType(messageType);
        mms.setPriority(priority);
        mms.setResponseStatus(responseStatus);
        mms.setMessageClass(messageClass);
        mms.setExpiry(expiry);
        mms.setMmsStatus(mmsStatus);
        //mms.setType(type);
        mms.setDownloaded(readMmsMessageParts(context, mms));
        mms.setMessagePersistent(this.getMessagePersistent(mms));
        return mms;
    }

    @Deprecated
    public Message getSmsConversationMessage(final Context context, final Cursor cursor, final String conversationId) {
        if(cursor==null)
            return null;
        Message message=null;
        String body="";
        String address=null;
        String id="";
        int read=1;
        int type=Message.MESSAGE_BOX_FAILED;
        long date=System.currentTimeMillis();
        int seen=1;
        int status=Message.STATUS_NONE;
        if(!MessageDAO.isMms(cursor)) {
            try {
                body = cursor.getString(cursor.getColumnIndexOrThrow(BODY));
                address = cursor.getString(cursor.getColumnIndexOrThrow(ADDRESS));
                date = cursor.getLong(cursor.getColumnIndexOrThrow(DATE));
                type = cursor.getInt(cursor.getColumnIndexOrThrow(TYPE));
                id = cursor.getString(cursor.getColumnIndexOrThrow(_ID));
                read = cursor.getInt(cursor.getColumnIndexOrThrow(READ));
                seen = cursor.getInt(cursor.getColumnIndexOrThrow(SEEN));
                status = cursor.getInt(cursor.getColumnIndexOrThrow(STATUS));
            } catch (Exception ex) {
                if (Me.DEBUG)
                    Log.e(TAG, "Error trying to get column. Assuming default value", ex);
            }
            message = new Message();
            message.setAddress(address);
            message.setType(type);
            message.setDate(date);
            message.setBody(body);
            message.setId(id);
            message.setConversationId(conversationId);
            message.setRead(read);
            message.setSeen(seen);
            message.setSentStatus(status);
            message.setMessagePersistent(this.getMessagePersistent(message));
/*        if(Me.DEBUG)
            Log.i(TAG, "Message id="+id+", date="+(new Date(date).toString()));*/
        }
        else {
//            address = getAddressFromMms(context, id);
        }
        return message;
    }

    public static boolean isMms(Cursor cursor) {
        int index=cursor.getColumnIndex(MMS_CONTENT_TYPE);  //check for "ct_t" field
        if(index < 0) {//no mms at all
            /*if (Me.DEBUG)
                Log.i(TAG, "message id=" + cursor.getString(cursor.getColumnIndex(_ID)) + ", type=sms");*/
            return false;
        }
        String contentType=cursor.getString(index);
/*        if(Me.DEBUG)
            Log.i(TAG, "message id="+cursor.getString(cursor.getColumnIndex(_ID))+", type=" + contentType);*/
        if(contentType==null || contentType.trim().length()==0)
            return false;
        if(contentType.startsWith(ContentType.MMS_MESSAGE_STARTER))
            return true;
        return false;
    }

    @Deprecated
    @Override
    public ArrayList<Message> getAll(Context context)
    {
        ArrayList<Message> messages=new ArrayList<Message>();
        Message message;
        Cursor cursor=null;
        try
        {
            cursor=this.getConversationsCursor(context);
            if(cursor == null)
                return messages;
            for(boolean hasData=cursor.moveToFirst(); hasData; hasData=cursor.moveToNext())
            {
                //debugCursor(cur);
                message=getSmsMessage(context, cursor);
                messages.add(message);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error reading conversations list", e);
        }
        if(cursor != null) {
            cursor.close();
        }
        Collections.sort(messages, new Comparator<Message>() {
            public int compare(Message message, Message message1) {
                return message1.getDate().compareTo(message.getDate());
            }
        });
        return messages;
    }

    public String getAddressFromMms(Context context, String mmsId) {
        String selectionAdd = new String("msg_id=" + mmsId);
        Uri uriAddress = Uri.parse("content://mms/" + mmsId + "/addr");
        Cursor cursor = context.getContentResolver().query(uriAddress, null, selectionAdd, null, null);
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndex("address"));
        }
        if (cursor != null) {
            cursor.close();
        }
        return name;
    }

    @Override
    public ArrayList<Message> getGroup(Context context, String conversationId)
    {
        ArrayList<Message> messages=new ArrayList<Message>();
        Message message;
        Cursor cur=null;
        try
        {
            cur=this.getMmsSmsConversationCursor(context, conversationId);
            if(cur == null)
                return messages;
            for(boolean hasData=cur.moveToFirst(); hasData; hasData=cur.moveToNext())
            {
//                debugCursor(cur);
                message=this.getSmsMessage(context, cur);
                messages.add(message);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error reading conversation="+conversationId, e);
        }
        if(cur != null)
            cur.close();
        return messages;
    }

    @Override
    public Message get(Context context, String id)
    {
        Message message=null;
        String[] columns={"*"};
        //look in sms table
        Cursor cursor = context.getContentResolver().query(SMS_URI, columns, _ID+" = "+id, null, null);
        if(cursor!=null && cursor.moveToFirst())
            message=this.getSmsMessage(context, cursor);
        else { //look in MMS
            cursor = context.getContentResolver().query(MMS_URI, columns,
                    MessageDAO._ID + "=?",
                    new String[]{id}, null);
            if (cursor != null && cursor.moveToFirst())
                message = this.getMmsMessage(context, cursor);
        }
        if(cursor!=null)
            cursor.close();
        return message;
    }

    public Message getMessageByRowId(Context context, long rowId, boolean isSms)
    {
        Cursor cursor=null;
        if(isSms) {
            cursor = this.getSmsCursor(context, _ID + "=?", new String[]{new Long(rowId).toString()});
            if (cursor.moveToFirst())
                return this.getSmsMessage(context, cursor);
        }
        else {
            cursor = this.getMmsCursor(context, _ID + "=?", new String[]{new Long(rowId).toString()});
            if (cursor.moveToFirst())
                return this.getMmsMessage(context, cursor);
        }
        if(cursor!=null)
            cursor.close();
        return null;
    }

    public Message getMessageById(Context context, String messageId)
    {
        Cursor cursor = context.getContentResolver().query(MessagesContentProvider.MESSAGES_URI, null, _ID+" = "+messageId, null, null);
        if(cursor.moveToFirst()) {
            if(isMms(cursor))
                return this.getMmsMessage(context, cursor);
            else
                return this.getSmsMessage(context, cursor);
        }
        if(cursor!=null)
            cursor.close();
        return null;
    }

    private MessagePersistent getMessagePersistent(final Message message) {
        String[] selectionArgs={message.getId()};
        Cursor cursor=null;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.AUX_TABLE,
                    null,
                    DbMainHelper.AUX_MESSAGE_ID + "=?",
                    selectionArgs,
                    null,
                    null,
                    null
            );
            if(cursor!=null && cursor.moveToFirst())
            {
                byte[] buffer=cursor.getBlob(DbMainHelper.AUX_BLOB_INDEX);
                final String s=ByteUtils.byteArrayToString(buffer, 0);
                return new Gson().fromJson(s, MessagePersistent.class);
            }
        }
        finally
        {
            if(cursor!=null)
                cursor.close();
        }
        return new MessagePersistent(message, message.getRead(), message.getStoredBody());
    }

    //delete from PSM messenger storage
    public boolean deletePersistent(Message message) {
        if(message==null)
            return false;
        int recs = this.database.delete(DbMainHelper.AUX_TABLE,
                DbMainHelper.AUX_MESSAGE_ID + "=" + message.getId(), null);
        if(recs==1)
            return true;
        return false;
    }


    @Override
    public boolean deleteAll(Context context)
    {
        database.beginTransaction();
        //delete from device messenger storage
        int recs=context.getContentResolver().delete(SMS_URI, null, null);
        //delete from PSM messenger storage
        int recsP = this.database.delete(DbMainHelper.AUX_TABLE, null, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs > 0 || recsP > 0)
            return true;
        return false;
    }

    @Override
    public boolean delete(Context context, String messageId)
    {
        database.beginTransaction();
        //delete from device messenger storage
        int recs=context.getContentResolver().delete(SMS_URI, MessageDAO._ID+" = "+messageId, null);
        //delete from PSM messenger storage
        int recsP = this.database.delete(DbMainHelper.AUX_TABLE,
                DbMainHelper.AUX_MESSAGE_ID+"="+ messageId, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs > 0 || recsP > 0)
            return true;
        return false;
    }

    public boolean deleteConversationMessage(Context context, String conversationId, String messageId)
    {
        boolean retValue=false;
        database.beginTransaction();
        //delete from device messenger storage
        int recs=context.getContentResolver().delete(Uri.parse(CONVERSATIONS_URL + conversationId), MessageDAO._ID+"=?", new String[]{messageId});
        //delete from PSM messenger storage
        int recsP = this.database.delete(DbMainHelper.AUX_TABLE, DbMainHelper.AUX_MESSAGE_ID+"="+ messageId, null);
        if(recs > 0 || recsP > 0)
            retValue=true;
        database.setTransactionSuccessful();
        database.endTransaction();
        return retValue;
    }

    @Override
    public boolean deleteGroup(Context context, String conversationId) {
        database.beginTransaction();
        //delete from device messenger storage
        int recs=context.getContentResolver().delete(Uri.parse(CONVERSATIONS_URL + conversationId), null, null);
        //delete from PSM messenger storage
        int recsP = this.database.delete(DbMainHelper.AUX_TABLE,
                DbMainHelper.AUX_CONVERSATION_ID+"="+ conversationId, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs > 0 || recsP > 0)
            return true;
        return false;
    }

    public synchronized boolean protectConversation(Context context, String conversationId) {
        Message message;
        boolean allProtected=true;
        Cursor cursor=null;
        try {
            cursor=this.getMmsConversationCursor(context, conversationId);
            while(cursor.moveToNext()) {
                message=this.getSmsMessage(context, cursor);
                if(message.isDraft())
                    continue;
                if(message.isProtected())
                    continue;
                if(!message.protect(context))
                    allProtected=false;
            }
        }
        catch(Exception ex) {
            Log.w(TAG, "Error while protecting conversation="+conversationId, ex);
            return false;
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return allProtected;
    }

    public synchronized boolean unprotectConversation(Context context, String conversationId) {
        Message message;
        boolean allUnprotected=true;
        Cursor cursor=null;
        try {
            cursor=this.getMmsSmsConversationCursor(context, conversationId);
            while(cursor.moveToNext()) {
                message=this.getSmsMessage(context, cursor);
                if(message.isProtected()) {
                    if(!message.unprotectSilent(context))
                        allUnprotected=false;
                }
            }
        }
        catch(Exception ex) {
            Log.w(TAG, "Error while clearing protection for conversation="+conversationId, ex);
            return false;
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return allUnprotected;
    }

    public Message getDraftMessage(Context context, String conversationId) {
        Message message;
        String[] columns= {TYPE, ADDRESS, DATE, BODY, _ID, READ, SEEN, STATUS, CONVERSATION_ID};
        Cursor cursor=null;
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns,
                    TYPE+"=? AND "+CONVERSATION_ID+"=?", new String[]{""+Message.MESSAGE_BOX_DRAFT, conversationId}, null);
            while(cursor!=null && cursor.moveToNext()) {
                message=this.getSmsMessage(context, cursor);
                if(message!=null)
                    return message;
            }
        }
        catch(Exception ex) {
            return null;
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return null;
    }

    public synchronized boolean resetMessages(Context context) {
        Message message=null;
        boolean allReset=true;
        String[] columns= {TYPE, ADDRESS, DATE, BODY, _ID, READ, SEEN, STATUS, CONVERSATION_ID};
        Cursor cursor=null;
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns, null, null, null);
            while(cursor!=null && cursor.moveToNext()) {
                message=this.getSmsMessage(context, cursor);
                if(message.isProtected()) {
                    if(!message.unprotectSilent(context))
                        allReset=false;
                }
                if(message.isCiphered()) {
                    if(!message.decipherSilent(context))
                        allReset=false;
                }
            }
        }
        catch(Exception ex) {
            if(message!=null)
                Log.w(TAG, "Error while reset message="+message.getId(), ex);
            else
                Log.w(TAG, "Error while reset messages", ex);
            return false;
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return allReset;
    }

    public synchronized Cursor getMessagesCursor(Context context) {
        String[] columns= {"*"};
        Cursor cursor=null;
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns, null, null, null);
            return cursor;
        }
        catch(Exception ex) {
            return cursor;
        }
    }

    public synchronized int countMessages(Context context) {
        int count=0;
        String[] columns= {"*"};
        Cursor cursor=null;
        try {
            cursor=context.getContentResolver().query(SMS_URI, columns, null, null, null);
            count=cursor.getCount();
        }
        catch(Exception ex) {
            Log.w(TAG, "Error while counting messages", ex);
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return count;
    }

    private Message getSmsMessage(Context context, Uri uri) {
        Message message=null;
        String[] cols= {"*"};
        Cursor cur = context.getContentResolver().query(uri, cols, null, null, null);
        if(cur.moveToFirst()) {
            message=this.getSmsMessage(context, cur);
        }
        if(cur!=null)
            cur.close();
        return message;
    }

    public MultimediaMessage getMmsMessage(Context context, Uri uri) {
        MultimediaMessage mmsMessage=null;
        String[] cols= {"*"};
        Cursor cursor = context.getContentResolver().query(uri, cols, null, null, null);
        if(cursor.moveToFirst()) {
            mmsMessage=this.getMmsMessage(context, cursor);
        }
        if(cursor!=null)
            cursor.close();
        return mmsMessage;
    }

    @Deprecated
    public static ArrayList<Message> getInboxMessages(Context context)
    {
        ArrayList<Message> messages=new ArrayList<Message>();
        Message message;
        Cursor c=null;
        String[] columns={_ID, ADDRESS, DATE, BODY, READ, SEEN};
        try
        {

            c=context.getContentResolver().query(SMS_INBOX_URI, columns, null, null, ADDRESS);
            if(c == null)
                return messages;
            for(boolean hasData=c.moveToFirst(); hasData; hasData=c.moveToNext())
            {
                final String id=c.getString(c.getColumnIndex(_ID));
                final String address=c.getString(c.getColumnIndex(ADDRESS));
                final String body=c.getString(c.getColumnIndex(BODY));
                final long date=c.getLong(c.getColumnIndex(DATE));
                final int read=c.getInt(c.getColumnIndex(READ));
                final int seen=c.getInt(c.getColumnIndex(SEEN));
//                long millis=Long.parseLong(date);
                message=new Message();
                message.setId(id);
                message.setAddress(address);
                message.setType(Message.MESSAGE_BOX_INBOX);
                message.setDate(date);
                message.setBody(body);
                message.setRead(read);
                message.setSeen(seen);
                messages.add(message);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error reading inbox messages", e);
        }
        finally {
            if(c!=null)
                c.close();
        }
        return messages;
    }

    @Deprecated
    public static ArrayList<Message> getSentMessages(Context context)
    {
        ArrayList<Message> messages=new ArrayList<Message>();
        Message message;
        Cursor c=null;
        String[] columns={_ID, ADDRESS, DATE, BODY};
        try
        {
            c=context.getContentResolver().query(SMS_SENT_URI, columns, null, null, ADDRESS);
            if(c == null)
                return messages;
            for(boolean hasData=c.moveToFirst(); hasData; hasData=c.moveToNext())
            {
                final String id=c.getString(c.getColumnIndex(_ID));
                final String address=c.getString(c.getColumnIndex(ADDRESS));
                final String body=c.getString(c.getColumnIndex(BODY));
                final long date=c.getLong(c.getColumnIndex(DATE));
//                long millis=Long.parseLong(date);
                message=new Message();
                message.setAddress(address);
                message.setType(Message.MESSAGE_BOX_SENT);
                message.setId(id);
                message.setDate(date);
                message.setBody(body);
                message.setRead(1);
                message.setSeen(1);
                messages.add(message);
            }
        }
        catch(Exception e) {
            Log.e(TAG, "Error reading sent messages", e);
        }
        finally {
            if(c!=null)
                c.close();
        }
        return messages;
    }

    /**
     * Calculates unread messages count in SMS inbox based on PSM data storage
     * @param context
     * @return int
     */
    public int getUnreadMessagesCount(Context context) {
        Cursor cursor=null;
        int unread=0;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.AUX_TABLE,
                    null,
                    DbMainHelper.AUX_READ + "=0",
                    null,
                    null,
                    null,
                    null
            );
            if(cursor==null)
                return 0;
            while(cursor.moveToNext())
                unread++;
        }
        finally {
            if(cursor!=null)
                cursor.close();
        }
        return unread;
    }

    /**
     * Calculates unread messages count in SMS inbox based on PSM data storage
     * @param context
     * @return int
     */
    public int getUnreadMessagesCount(Context context, String conversationId) {
        String[] selectionArgs={new Integer(0).toString(), conversationId};
        Cursor cursor=null;
        int unread=0;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.AUX_TABLE,
                    null,
                    DbMainHelper.AUX_READ + "=? and "+DbMainHelper.AUX_CONVERSATION_ID+"=?",
                    selectionArgs,
                    null,
                    null,
                    null
            );
            if(cursor==null)
                return 0;
            while(cursor.moveToNext())
                unread++;
        }
        finally
        {
            if(cursor!=null)
                cursor.close();
        }
        return unread;
    }

    /**
     *
     * @param context
     * @param sourcePhoneNumber //phone number as-is
     * @return null if not in contacts. array[0] - Display name, [1] - contactId
     */
    private String[] getContactNameByPhoneNumber(Context context, String sourcePhoneNumber) {
        String phoneNumber = PhoneNumberUtils.stripSeparators(sourcePhoneNumber);
        String contactInfo[] = null;
        if(phoneNumber==null || phoneNumber.trim().length()==0) {
            return contactInfo;
        }
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.LAST_TIME_CONTACTED};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, ContactDAO.LAST_CONTACT_DATE_DESC);
        if(cursor.moveToFirst()) {
            contactInfo = new String[2];
            contactInfo[0] = cursor.getString(0);
            contactInfo[1] = cursor.getString(1);
        }
        cursor.close();
        return contactInfo;
    }

    public String getConversationIdByPhoneNumber(Context context, String sourcePhoneNumber) {
        String threadId=null;
        String recipientIds;
        String address;
        String[] columns={"*"};
        //String[] columns={MessageDAO._ID, MessageDAO.RECIPIENT_IDS};
        if(sourcePhoneNumber==null || sourcePhoneNumber.trim().length()==0) {
            return null;
        }
        PhoneNumber phoneNumber = new PhoneNumber(sourcePhoneNumber);
        String selection=null;//MessageDAO.RECIPIENT_IDS+" like '%"+phoneNumber+"%'";
        Cursor cursor = context.getContentResolver().query(CONVERSATIONS_SIMPLE_URI, columns, selection, null, null);
        if(cursor==null)
            return null;
        while(cursor.moveToNext()) {
            //debugCursor(cursor);
            threadId = cursor.getString(0);
            recipientIds = cursor.getString(cursor.getColumnIndex(MessageDAO.RECIPIENT_IDS));
            address=this.getMainCanonicalAddress(context, recipientIds);
            if(phoneNumber.compareDefault(address))
                break;
/*            if(recipientIds.contains(" "))
                continue;
            else
                break;*/
            threadId=null;
        }
        cursor.close();
        return threadId;
    }

    public static void testTelephony(Context context) {
        Log.i(TAG, "Starting telephony test");
        final String className="android.provider.Telephony";
        Class clazz=null;
        try {
            clazz=Class.forName(className);
        }
        catch (ClassNotFoundException e) {
            Log.e(TAG, "Class not found", e);
            return;
        }
        Log.i(TAG, "Class="+clazz.getCanonicalName());
        Class[] clz=clazz.getDeclaredClasses();
        Log.i(TAG, "Declared classes="+clz.length);
        Class myClazz=null;
        for(int i=0; i < clz.length; i++) {
            Log.i(TAG, "Declared class="+clz[i].getCanonicalName());
            if(clz[i].getCanonicalName().equalsIgnoreCase("android.provider.Telephony.Threads")) {
                myClazz=clz[i];
            }
        }
        if(myClazz==null)
            return;
        Method[] mt=myClazz.getMethods();
        Log.i(TAG, mt.length+" - available public methods");
        for(int i=0; i < mt.length; i++) {
            Log.i(TAG, "Method="+mt[i].getName()+", return type="+mt[i].getReturnType().getName());
        }
        try {
            Method method=myClazz.getDeclaredMethod("getOrCreateThreadId", Context.class, String.class);
            Log.i(TAG, "Bingo! "+method.getName()+" is present");
            Long receiver=new Long(0);
            Object retValue=method.invoke(receiver, context, new String("123"));
            Log.i(TAG, "Allocated thread_id="+((Long )retValue).longValue());
        }
        catch(Exception e) {
            Log.i(TAG, "Can't find method");
        }
        mt=myClazz.getDeclaredMethods();
        Log.i(TAG, mt.length+" - available declared methods");
        for(int i=0; i < mt.length; i++) {
            Log.i(TAG, "Method="+mt[i].getName()+", return type="+mt[i].getReturnType().getName());
        }
    }

    private static Method reflectedMethod=null;

    public static String getOrCreateThreadId(Context context, String address) {
        final String defaultId="0";
        final String reflectedClassName="android.provider.Telephony.Threads";
        final String reflectedMethodName="getOrCreateThreadId";
        final String className="android.provider.Telephony";
        Class clazz, reflectedClass=null;
        if(reflectedMethod==null) { //first run only
            try {
                clazz=Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                Log.e(TAG, "Can't find Telephony class! Using default thread id");
                return defaultId;
            }
            Class[] clz=clazz.getDeclaredClasses();
            for(int i=0; i < clz.length; i++) {
                if(clz[i].getCanonicalName().equals(reflectedClassName)) {
                    reflectedClass=clz[i];
                    break;
                }
            }
            if(reflectedClass==null) {
                Log.e(TAG, "Can't find reflected class="+reflectedClassName);
                return defaultId;
            }
            try {
                reflectedMethod=reflectedClass.getDeclaredMethod(reflectedMethodName, Context.class, String.class);
                if(Me.DEBUG)
                    Log.i(TAG, "Reflected method "+reflectedMethod.toString()+" found");
            }
            catch(Exception e) {
                Log.e(TAG, "Can't find reflected method");
                reflectedMethod=null;
                return defaultId;
            }
        }
        try {
            Long receiver=new Long(0); //will be ignored
            Object retValue=reflectedMethod.invoke(receiver, context, address);
            if(retValue instanceof Long)
                return ((Long )retValue).toString();
            return retValue.toString();
        }
        catch(Exception e) {
            Log.e(TAG, "Error invoking reflected method"+reflectedMethodName);
            return defaultId;
        }
    }

    public static Long getOrCreateThreadId(Context context, Set<String> addresses) {
        final Long defaultId=0L;
        final String reflectedClassName="android.provider.Telephony.Threads";
        final String reflectedMethodName="getOrCreateThreadId";
        final String className="android.provider.Telephony";
        Class clazz, reflectedClass=null;
        if(reflectedMethod==null) { //first run only
            try {
                clazz=Class.forName(className);
            }
            catch (ClassNotFoundException e) {
                Log.e(TAG, "Can't find Telephony class! Using default thread id");
                return defaultId;
            }
            Class[] clz=clazz.getDeclaredClasses();
            for(int i=0; i < clz.length; i++) {
                if(clz[i].getCanonicalName().equals(reflectedClassName)) {
                    reflectedClass=clz[i];
                    break;
                }
            }
            if(reflectedClass==null) {
                Log.e(TAG, "Can't find reflected class="+reflectedClassName);
                return defaultId;
            }
            try {
                reflectedMethod=reflectedClass.getDeclaredMethod(reflectedMethodName, Context.class, Set.class);
                if(Me.DEBUG)
                    Log.i(TAG, "Reflected method "+reflectedMethod.toString()+" found");
            }
            catch(Exception e) {
                Log.e(TAG, "Can't find reflected method");
                reflectedMethod=null;
                return defaultId;
            }
        }
        try {
            Long receiver=new Long(0); //will be ignored
            Object retValue=reflectedMethod.invoke(receiver, context, addresses);
            if(retValue instanceof Long)
                return ((Long )retValue);
        }
        catch(Exception e) {
            Log.e(TAG, "Error invoking reflected method"+reflectedMethodName);
        }
        return defaultId;
    }

    public static String getRecipiendId(Context context, String address) {
        Cursor cursor=context.getContentResolver().query(Uri.parse(CANONICAL_ADDRESSES_URL), new String[] {"*"}, ADDRESS+"="+address, null, null);
        if(cursor.moveToFirst()) {
            return cursor.getString(0);
        }
        return "0";
    }

    @Deprecated
    public static String getOrCreateRecipientId(Context context, String address) {
        String recipientId="0";
        Cursor cursor=context.getContentResolver().query(Uri.parse(CANONICAL_ADDRESSES_URL), new String[] {"*"}, ADDRESS+"="+address, null, null);
        if(cursor.moveToFirst()) {
            recipientId=cursor.getString(0);
            if(Me.DEBUG)
                Log.i(TAG, "Reusing recipient id="+recipientId+" for address="+address);
            return recipientId;
        }
        else {
            ContentValues values = new ContentValues();
            values.put(ADDRESS, address);
            Uri uri=context.getContentResolver().insert(Uri.parse(CANONICAL_ADDRESSES_URL), values);
            if(uri!=null) {
                cursor=context.getContentResolver().query(uri, new String[] {"*"}, null, null, null);
                if(cursor.moveToFirst()) {
                    if(Me.DEBUG)
                        Log.i(TAG, "Created recipient id="+recipientId+" for address="+address);
                    recipientId=cursor.getString(0);
                    return recipientId;
                }
            }
        }
        if(Me.DEBUG)
            Log.w(TAG, "Default recipient id="+recipientId+" for address="+address);
        return "0";
    }

    public Uri insert(Context context, String recipient, String subject, byte[] imageBytes)
    {
        try
        {
            // Get thread id
            String thread_id = getOrCreateThreadId(context, recipient);
            Log.e(">>>>>>>", "Thread ID is " + thread_id);

            /*           // Create a dummy sms
            ContentValues dummyValues = new ContentValues();
            dummyValues.put("thread_id", thread_id);
            dummyValues.put("body", "Dummy SMS body.");
            Uri dummySms = context.getContentResolver().insert(Uri.parse("content://sms/sent"), dummyValues);
            */
            // Create a new message entry
            long now = System.currentTimeMillis();
            ContentValues mmsValues = new ContentValues();
            mmsValues.put(CONVERSATION_ID, thread_id);  //thread_id
            mmsValues.put(DATE, now/1000L); //date
            mmsValues.put(MMS_MESSAGE_BOX, Message.MESSAGE_BOX_OUTBOX); //msg_box mark as pending after sending it should read MESSAGE_BOX_SENT
            //mmsValues.put("m_id", System.currentTimeMillis());
            mmsValues.put(READ, 1); //read
            mmsValues.put(MMS_SUBJECT, subject);  //sub
            mmsValues.put(MMS_SUBJECT_CS, 106); //sub_cs
            mmsValues.put(MMS_CONTENT_TYPE, ContentType.MULTIPART_RELATED); //ct_t
            mmsValues.put(MMS_EXPIRY, imageBytes.length); //exp
            mmsValues.put(MMS_MESSAGE_CLASS, "personal"); //m_cls
            mmsValues.put(MMS_MESSAGE_TYPE, 128); // m_type 132 (RETRIEVE CONF) 130 (NOTIF IND) 128 (SEND REQ)
            mmsValues.put(MMS_VERSION, 19); //v
            mmsValues.put(MMS_PRIORITY, 129); //pri
            mmsValues.put(MMS_TRANSACTION_ID, "T"+ Long.toHexString(now)); //tr_id
            mmsValues.put(MMS_RESPONSE_STATUS, 128); //resp_st

            // Insert message
            Uri res = context.getContentResolver().insert(MMS_URI, mmsValues);
            String messageId = res.getLastPathSegment().trim();
            Log.e(">>>>>>>", "Message saved as " + res);

            // Create part
            createPart(context, messageId, imageBytes);

            // Create addresses
            saveMmsAddress(context, messageId, recipient, true);

            //res = Uri.parse(destUri + "/" + messageId);

            // Delete dummy sms
            //context.getContentResolver().delete(dummySms, null, null);

            return res;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public Uri createPart(Context context, String id, byte[] imageBytes) throws Exception
    {
        ContentValues mmsPartValue = new ContentValues();
        mmsPartValue.put(MMS_PART_MSG_ID, id); //mid
        mmsPartValue.put(MMS_PART_CONTENT_TYPE, "image/png"); //ct
        mmsPartValue.put(MMS_PART_CONTENT_ID, "<" + System.currentTimeMillis() + ">"); //cid
        Uri partUri=Uri.parse(MessageFormat.format(MMS_PART_URL, id));
        Uri res = context.getContentResolver().insert(partUri, mmsPartValue);
        Log.e(">>>>>>>", "Part uri is " + res.toString());

        // Add data to part
        OutputStream os = context.getContentResolver().openOutputStream(res);
        ByteArrayInputStream is = new ByteArrayInputStream(imageBytes);
        byte[] buffer = new byte[256];
        for (int len=0; (len=is.read(buffer)) != -1;)
        {
            os.write(buffer, 0, len);
        }
        os.close();
        is.close();

        return res;
    }

    public Uri saveMmsAddress(Context context, String id, String address, boolean to)
    {
        ContentValues addrValues = new ContentValues();
        addrValues.put(ADDRESS, address); //address
        addrValues.put(MMS_CHARSET, "106"); //charset utf-8
        addrValues.put(TYPE, (to)?151:137); //type to=151, from=137
        Uri addrUri=Uri.parse(MessageFormat.format(MMS_ADDRESS_URL, id));
        Uri res = context.getContentResolver().insert(addrUri, addrValues);
        if(Me.DEBUG)
            Log.i(TAG, "MMS address uri="+res);
        return res;
    }
}
