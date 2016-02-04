package ru.ivanovpv.gorets.psm.db;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/PurseDAO.java $
 */

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.google.gson.Gson;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.cipher.CipherAnubis;
import ru.ivanovpv.gorets.psm.persistent.Purse;

import java.util.ArrayList;

public abstract class PurseDAO extends AbstractBaseDAO<Purse> {
    private final static String TAG = PurseDAO.class.getName();

    protected PurseDAO(final DbMainHelper dbHelper)
    {
        super(dbHelper);
    }

    @Override
    public ArrayList<Purse> getAll(Context context) {
        Cursor cursor=null;
        Cipher cipher=new CipherAnubis(Me.getMe().getHashDAO().get().getKey());
        ArrayList<Purse> purses=new ArrayList<Purse>();
        Purse purse;
        try {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=" + DbMainHelper.TYPE_PURSE,
                    null,
                    null,
                    null,
                    null
            );
            if(cursor==null)
                return null;
            while(cursor.moveToNext())  {
                try
                {
                    byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                    byte[] buf=cipher.decryptBuffer(buffer);
                    final String s=ByteUtils.byteArrayToString(buf, 0);
                    purse=new Gson().fromJson(s, Purse.class);
                    purses.add(purse);
                }
                catch(Exception ex) {
                    Log.w(TAG, "Can't decrypt cached purse data, assuming empty purse");
                    purses.add(new Purse(0));
                }
            }
        }
        finally
        {
            if(cursor!=null)
                cursor.close();
        }
        return purses;
    }

    @Override
    public ArrayList<Purse> getGroup(Context context, String groupId) {
        Cursor cursor=null;
        Cipher cipher=new CipherAnubis(Me.getMe().getHashDAO().get().getKey());
        String[] selectionArgs={new Integer(DbMainHelper.TYPE_PURSE).toString(), groupId};
        ArrayList<Purse> purses=new ArrayList<Purse>();
        Purse purse;
        try {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=? and " + DbMainHelper.DATA_REF_ID2 + "=?",
                    selectionArgs,
                    null,
                    null,
                    null
            );
            if(cursor==null)
                return null;
            while(cursor.moveToNext())  {
                try
                {
                    byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                    byte[] buf=cipher.decryptBuffer(buffer);
                    final String s=ByteUtils.byteArrayToString(buf, 0);
                    purse=new Gson().fromJson(s, Purse.class);
                    purses.add(purse);
                }
                catch(Exception ex) {
                    Log.w(TAG, "Can't decrypt cached purse data, assuming empty purse");
                    purses.add(new Purse(0));
                }
            }
        }
        finally
        {
            if(cursor!=null)
                cursor.close();
        }
        return purses;
    }

    @Override
    public Purse get(Context context, String id) {
        Cursor cursor=null;
        Cipher cipher=new CipherAnubis(Me.getMe().getHashDAO().get().getKey());
        String[] selectionArgs={new Integer(DbMainHelper.TYPE_PURSE).toString(), id};
        try {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=? and " + DbMainHelper.DATA_REF_ID + "=?",
                    selectionArgs,
                    null,
                    null,
                    null
            );
            if(cursor==null)
                return null;
            if(cursor.moveToFirst())  {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                byte[] buf=cipher.decryptBuffer(buffer);
                final String s=ByteUtils.byteArrayToString(buf, 0);
                return new Gson().fromJson(s, Purse.class);
            }
        }
        catch(Exception ex) {
            Log.w(TAG, "Can't decrypt cached purse data! Assuming empty purse");
            return new Purse(0);
        }
        finally
        {
            if(cursor!=null)
                cursor.close();
        }
        return null;
    }

    @Override
    public boolean deleteAll(Context context) {
        int recs=database.delete(DbMainHelper.DATA_TABLE, DbMainHelper.DATA_TYPE+" = "+ DbMainHelper.TYPE_PURSE, null);
        if(recs>=0)
            return true;
        return false;
    }

    @Override
    public boolean delete(Context context, String id) {
        String[] selectionArgs={new Integer(DbMainHelper.TYPE_PURSE).toString(), id};
        int recs=database.delete(DbMainHelper.DATA_TABLE,
                DbMainHelper.DATA_TYPE+"=? and "+
                DbMainHelper.DATA_REF_ID+ "=?", selectionArgs);
        if(recs>=1)
            return true;
        return false;
    }

}
