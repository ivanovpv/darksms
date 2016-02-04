package ru.ivanovpv.gorets.psm.view;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 465 $
 *   $LastChangedDate: 2014-01-12 21:01:28 +0400 (Вс, 12 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/ConversationsCursor.java $
 */

import android.content.ContentResolver;
import android.content.Context;
import android.database.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.db.DbMainHelper;
import ru.ivanovpv.gorets.psm.db.MessageDAO;

public class ConversationsCursor extends AbstractCursor {
    private static final String TAG = ConversationsCursor.class.getName();

    public final static String UNPROCESSED_MESSAGES_COUNT = MessageDAO.UNPROCESSED_COUNT;
    private Cursor mainCursor;
    //private Cursor auxCursor;
    private String[] mainColumns;
    private Me me;

    public ConversationsCursor(Context context, String where) {
        super();
        me = Me.getMe();
        mainCursor = me.getMessageDAO().getConversationsCursor(context, where);
    }

    public ConversationsCursor(Context context, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        super();
        me = Me.getMe();
        mainCursor = context.getContentResolver().query(MessageDAO.CONVERSATIONS_SIMPLE_URI, projection, selection, selectionArgs, sortOrder);

    }

    protected ConversationsCursor(Cursor cursor) {
        super();
        mainCursor = cursor;
    }

    @Override
    public int getColumnCount() {
        return mainCursor.getColumnCount() + 1;
    }

    @Override
    public int getCount() {
        int mainCount = mainCursor.getCount();
        return mainCount;
    }

/*    @Override
    public int getType(int column) {
        if(column < mainColumns.length)
            return mainCursor.getType(column);
        return FIELD_TYPE_INTEGER;
    }*/

    @Override
    public String[] getColumnNames() {
        mainColumns = mainCursor.getColumnNames();
        String[] columns = new String[mainColumns.length + 1];
        for (int i = 0; i < mainColumns.length; i++)
            columns[i] = mainColumns[i];
        columns[mainColumns.length] = UNPROCESSED_MESSAGES_COUNT;
        return columns;
    }

    @Override
    public int getColumnIndex(String columnName) {
        String[] columnNames=this.getColumnNames();
        for(int i=0; i < columnNames.length; i++) {
            if(columnNames[i].equalsIgnoreCase(columnName))
                return i;
        }
        return -1;
    }

    private int getAuxCount() {
        Cursor auxCursor = null;
        String[] columns = {DbMainHelper.AUX_ID, DbMainHelper.AUX_MESSAGE_ID, DbMainHelper.AUX_CONVERSATION_ID, DbMainHelper.AUX_READ};
        int count;

        if(Me.DEBUG)
            Log.i(TAG, "Querying aux count");
        String conversationId = mainCursor.getString(mainCursor.getColumnIndex(MessageDAO._ID));
        String[] vals = {conversationId, "0"};
        try {
            auxCursor = me.getDbHelper().getReadableDatabase().query(DbMainHelper.AUX_TABLE,
                    columns,
                    DbMainHelper.AUX_CONVERSATION_ID + "=? and " + DbMainHelper.AUX_READ + "=?",
                    vals,
                    null,
                    null,
                    null
            );
            if (auxCursor == null)
                count = 0;
            else
                count = auxCursor.getCount();
        } catch (Exception ex) {
            if(Me.DEBUG)
                Log.w(TAG, "Ignoring error while trying to read unread messages", ex);
            count = 0;
        } finally {
            if (auxCursor != null)
                auxCursor.close();
        }
        if(Me.DEBUG)
            Log.i(TAG, "Unread messages for conversation=" + conversationId + " = " + count);
        return count;
    }

    @Override
    public boolean requery() {
        return mainCursor.requery();
    }

    @Override
    public String getString(int i) {
        if (i < 0)
            return "";
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getString(i);
        return "";
    }

    @Override
    public short getShort(int i) {
        if (i < 0)
            return 0;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getShort(i);
        return (short) getAuxCount();
    }

    @Override
    public int getInt(int i) {
        if (i < 0)
            return 0;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getInt(i);
        return (int) getAuxCount();
    }

    @Override
    public long getLong(int i) {
        if (i < 0)
            return 0;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getLong(i);
        return (long) getAuxCount();
    }

    @Override
    public float getFloat(int i) {
        if (i < 0)
            return 0;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getFloat(i);
        return (float) getAuxCount();
    }

    @Override
    public double getDouble(int i) {
        if (i < 0)
            return 0;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.getDouble(i);
        return (double) getAuxCount();
    }

    @Override
    public boolean isNull(int i) {
        if (i < 0)
            return true;
        mainCursor.moveToPosition(mPos);
        if (i < mainColumns.length)
            return mainCursor.isNull(i);
        int count = getAuxCount();
        if (count == 0)
            return true;
        return false;
    }

    @Override
    public void close() {
        if (mainCursor != null)
            mainCursor.close();
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {
        mainCursor.registerContentObserver(contentObserver);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver datasetObserver) {
        mainCursor.registerDataSetObserver(datasetObserver);
    }

    @Override
    public Bundle respond(Bundle extras) {
        return mainCursor.respond(extras);
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri notifyUri) {
        mainCursor.setNotificationUri(cr, notifyUri);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mainCursor.unregisterContentObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mainCursor.unregisterDataSetObserver(observer);
    }

/*    @Override
    public boolean onMove (int oldPosition, int newPosition) {
        return mainCursor.onMove(oldPosition, newPosition);
    }*/

    @Override
    public boolean isClosed() {
        if (mainCursor != null)
            return mainCursor.isClosed();
        return true;
    }

    /*@Override
    public Uri getNotificationUri() {
        return mainCursor.getNotificationUri();
    } */

    @Override
    public Bundle getExtras() {
        return mainCursor.getExtras();
    }

    @Override
    public void deactivate() {
        mainCursor.deactivate();
    }

    @Override
    public CursorWindow getWindow() {
        return null;
    }

    @Override
    public void fillWindow(int position, CursorWindow window) {
        if (position < 0 || position > getCount()) {
            return;
        }
        window.acquireReference();
        try {
            int oldpos = mPos;
            mPos = position - 1;
            window.clear();
            window.setStartPosition(position);
            int columnNum = this.getColumnCount();
            window.setNumColumns(columnNum);
            while (mainCursor.moveToNext() && window.allocRow()) {
                for (int i = 0; i < columnNum; i++) {
                    String field = this.getString(i);
                    if (field != null) {
                        if (!window.putString(field, mPos, i)) {
                            window.freeLastRow();
                            break;
                        }
                    } else {
                        if (!window.putNull(mPos, i)) {
                            window.freeLastRow();
                            break;
                        }
                    }
                }
            }

            mPos = oldpos;
        } catch (IllegalStateException e) {
            // simply ignore it
        } finally {
            window.releaseReference();
        }
    }
}
