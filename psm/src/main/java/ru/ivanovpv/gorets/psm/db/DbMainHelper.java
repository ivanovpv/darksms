package ru.ivanovpv.gorets.psm.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import com.google.gson.Gson;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.persistent.Hash;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 485 $
 *   $LastChangedDate: 2014-01-27 15:48:59 +0400 (Пн, 27 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/DbMainHelper.java $
 */

public class DbMainHelper extends SQLiteOpenHelper implements BaseColumns
{
    private final static String TAG = DbMainHelper.class.getName();

    private static final int DB_VERSION=6;
    static final String DB_NAME="psm.db";

    //DATA table field
    public static final String DATA_TABLE="DATA";
    private static final String DATA_INDEX="IDX_"+DATA_TABLE;
    public static final String DATA_ID=BaseColumns._ID;
    public static final String DATA_TYPE="DATA_TYPE";
    public static final String DATA_REF_ID="DATA_REF_ID";
    public static final String DATA_REF_ID2="DATA_REF_ID2";
    public static final String DATA_BLOB="DATA_BLOB";
    //fields enumeration in DATA table
    protected static final int DATA_ID_INDEX=0;
    protected static final int DATA_TYPE_INDEX=1;
    protected static final int DATA_REF_ID_INDEX=2;
    protected static final int DATA_REF_ID2_INDEX=3;
    protected static final int DATA_BLOB_INDEX=4;
    //field names in DATA table
    public static final String[] DATA_COLUMNS={DATA_ID, DATA_TYPE, DATA_REF_ID, DATA_REF_ID2, DATA_BLOB};

    //data types
    public static final int TYPE_HASH=0;
    public static final int TYPE_PHONE=1;
    public static final int TYPE_PURSE=2;

    //AUX table field
    public static final String AUX_TABLE="AUX";
    private static final String AUX_INDEX="IDX_"+DATA_TABLE;
    public static final String AUX_ID=BaseColumns._ID;
    public static final String AUX_MESSAGE_ID="message_id";
    public static final String AUX_CONVERSATION_ID="conversation_id";
    public static final String AUX_READ="read";
    public static final String AUX_BLOB="aux_blob";
    //fields enumeration in AUX table
    public static final int AUX_ID_INDEX=0;
    public static final int AUX_MESSAGE_ID_INDEX=1;
    public static final int AUX_CONVERSATION_ID_INDEX=2;
    public static final int AUX_READ_INDEX=3;
    public static final int AUX_BLOB_INDEX=4;

    //field names in AUX table
    public static final String[] AUX_COLUMNS={AUX_ID, AUX_MESSAGE_ID, AUX_CONVERSATION_ID, AUX_READ, AUX_BLOB};

    private static final String CREATE_DATA_TABLE="create table if not exists \n"
            + DATA_TABLE +" ("
            + DATA_ID + " integer primary key autoincrement, "
            + DATA_TYPE + " integer, "   //data type (see above)
            + DATA_REF_ID + " text, "    //contains reference to external ids, like: contacts and messages
            + DATA_REF_ID2 + " text, "   //contains supplemental reference to external ids e.g. conversationId for specific message
            + DATA_BLOB + " blob)";      //encrypted content (as encrypted json string of Hash, Contact or Message)
    private static final String CREATE_DATA_INDEX="create index if not exists\n"
            + DATA_INDEX+" on "+ DATA_TABLE +"(\n"
            + DATA_REF_ID +" asc, "
            + DATA_REF_ID2 +" asc, "
            + DATA_TYPE +" asc\n)";

    private static final String CREATE_AUX_TABLE="create table if not exists \n"
            + AUX_TABLE +" ("
            + AUX_ID + " integer primary key autoincrement, "
            + AUX_MESSAGE_ID + " text, "       //message id
            + AUX_CONVERSATION_ID + " text, "  //message conversation id
            + AUX_READ + " integer, "          //message read=1, unread=0
            + AUX_BLOB + " blob)";             //aux message body (lemon text, reserved for future use)

    private static final String CREATE_AUX_INDEX="create index if not exists\n"
            + AUX_INDEX+" on "+ AUX_TABLE +"(\n"
            + AUX_MESSAGE_ID +" asc, "
            + AUX_CONVERSATION_ID +" asc, "
            + AUX_READ +" asc\n)";

    public DbMainHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database)
    {
        database.beginTransaction();
        if(Me.DEBUG)
            Log.d(TAG, "Creating database");
        try
        {
            database.execSQL(CREATE_DATA_TABLE);
            database.execSQL(CREATE_DATA_INDEX);
            database.execSQL(CREATE_AUX_TABLE);
            database.execSQL(CREATE_AUX_INDEX);
            database.setTransactionSuccessful();
        }
        catch(Exception e)
        {
            Log.d(TAG, "Can't create PSM database", e);
        }
        database.endTransaction();
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        if(oldVersion==6) {
            //delete old installation id
            Me.getMe().psmSettings.installationId().put("");
            //put deviceSalt from preferences to database
            Cursor cursor=database.query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=" + DbMainHelper.TYPE_HASH,
                    null,
                    null,
                    null,
                    null
            );
            if(cursor!=null && cursor.moveToFirst()) {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                String s= ByteUtils.byteArrayToString(buffer, 0);
                Hash hash=new Gson().fromJson(s, Hash.class); //old formatted hash
                String passwordHash=hash.getHash();
                String deviceSalt=Me.getMe().psmSettings.deviceSalt().get();
                Me.getMe().psmSettings.deviceSalt().put("");
                hash=new Hash("", deviceSalt, hash.getId()); //ignore old hash (disable pin check)
                final Gson gson = new Gson();
                s=gson.toJson(hash, Hash.class);
                buffer= ByteUtils.stringToByteArray(s);
                ContentValues cv=new ContentValues();
                cv.put(DbMainHelper.DATA_TYPE, DbMainHelper.TYPE_HASH);
                cv.put(DbMainHelper.DATA_REF_ID, hash.getId());
                cv.put(DbMainHelper.DATA_BLOB, buffer);
                int recs = database.update(DbMainHelper.DATA_TABLE, cv, DbMainHelper.DATA_REF_ID+"=?", new String[] {hash.getId()});
                if(recs==1)
                    Log.i(TAG, "Database correctly reformatted from version="+oldVersion+" to version="+newVersion);
                else
                    Log.w(TAG, "Couldn't reformat database from version="+oldVersion+" to version="+newVersion);
            }
        }
        else if (oldVersion < 6) {
            database.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + DATA_INDEX);
            database.execSQL("DROP TABLE IF EXISTS " + AUX_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + AUX_INDEX);
            onCreate(database);
        }
        else {
            database.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + DATA_INDEX);
            database.execSQL("DROP TABLE IF EXISTS " + AUX_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + AUX_INDEX);
            onCreate(database);
        }
    }

    public void reset() {
        SQLiteDatabase database=this.getWritableDatabase();
        if(database!=null) {
            database.execSQL("DROP TABLE IF EXISTS " + DATA_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + DATA_INDEX);
            database.execSQL("DROP TABLE IF EXISTS " + AUX_TABLE);
            database.execSQL("DROP INDEX IF EXISTS " + AUX_INDEX);
            onCreate(database);
        }
    }
}
