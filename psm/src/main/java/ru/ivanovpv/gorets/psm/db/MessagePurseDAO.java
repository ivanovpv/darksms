package ru.ivanovpv.gorets.psm.db;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/MessagePurseDAO.java $
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.google.gson.Gson;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.cipher.CipherAnubis;
import ru.ivanovpv.gorets.psm.persistent.Purse;

public class MessagePurseDAO extends PurseDAO {
    private final static String TAG = MessagePurseDAO.class.getName();

    public MessagePurseDAO(final DbMainHelper dbHelper) {
        super(dbHelper);
    }

    public void init(Context context) {
        Purse purse=this.get(context);
        if(purse == null)
        {
            purse=new Purse();
            if(Me.FREE)
                purse.setPremium(true);
            this.save(context, purse);
        }
        else
        {
            if(Me.FREE && !purse.isPremium())
            {
                purse.setPremium(true);
                this.save(context, purse);
            }
        }
    }

    private ContentValues mapToContentValues(final Purse purse)
    {
        final Gson gson = new Gson();
        String s=gson.toJson(purse, Purse.class);
        byte[] buffer= ByteUtils.stringToByteArray(s);
        byte[] key=Me.getMe().getHashDAO().get().getKey();
        Cipher cipher=new CipherAnubis(key);
        byte[] buf=cipher.encryptBuffer(buffer);
        ContentValues cv=new ContentValues();
            cv.put(DbMainHelper.DATA_TYPE, DbMainHelper.TYPE_PURSE);
            cv.put(DbMainHelper.DATA_REF_ID, Purse.MESSAGE_PURSE_ID);
            cv.put(DbMainHelper.DATA_REF_ID2, (String )null);
            cv.put(DbMainHelper.DATA_BLOB, buf);
        return cv;
    }

    @Override
    public Purse save(Context context, Purse purse) {
        String[] selectionArgs={new Integer(DbMainHelper.TYPE_PURSE).toString(), Purse.MESSAGE_PURSE_ID};
        int recs = this.database.update(DbMainHelper.DATA_TABLE, this.mapToContentValues(purse),
                DbMainHelper.DATA_TYPE+"=? and "+ DbMainHelper.DATA_REF_ID+"=?", selectionArgs);
        if(recs==1)
            return purse;
        else
        {
            try {
                this.database.insert(DbMainHelper.DATA_TABLE, null, this.mapToContentValues(purse));
                return purse;
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't insert purse record!", e);
            }
            return null;
        }
    }

    @Override
    public boolean deleteGroup(Context context, String id) {
        return false;
    }

    public Purse get(Context context) {
        return super.get(context, Purse.MESSAGE_PURSE_ID);
    }

    public boolean delete(Context context) {
        return super.delete(context, Purse.MESSAGE_PURSE_ID);
    }

    public boolean isAny(Context context) {
        Purse purse=get(context);
        if(purse!=null)
            return purse.isAny();
        return false;
    }

    public boolean isComfortPin(Context context) {
        Purse purse=this.get(context);
        if(purse!=null)
            return purse.isComfortPIN();
        return false;
    }
}
