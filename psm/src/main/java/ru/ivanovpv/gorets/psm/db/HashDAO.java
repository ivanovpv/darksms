package ru.ivanovpv.gorets.psm.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.persistent.Hash;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 430 $
 *   $LastChangedDate: 2013-11-22 12:10:16 +0400 (Пт, 22 ноя 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/HashDAO.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: Pavel
 * Date: 06.06.12
 * Time: 20:12
 * To change this template use File | Settings | File Templates.
 */
public class HashDAO extends AbstractBaseDAO<Hash>
{
    public final static String TAG=HashDAO.class.getName();
    Type hashListType = new TypeToken<List<Hash>>(){}.getClass();

    public HashDAO(final DbMainHelper dbHelper)
    {
        super(dbHelper);
    }

    private ContentValues mapToContentValues(final Hash hash)
    {
        final Gson gson = new Gson();
        String s=gson.toJson(hash, Hash.class);
        byte[] buffer= ByteUtils.stringToByteArray(s);
        ContentValues cv=new ContentValues();
        cv.put(DbMainHelper.DATA_TYPE, DbMainHelper.TYPE_HASH);
        cv.put(DbMainHelper.DATA_REF_ID, hash.getId());
        cv.put(DbMainHelper.DATA_BLOB, buffer);
        return cv;
    }

    @Override
    public Hash save(Context context, Hash hash)
    {
        return this.save(hash);
    }

    public Hash save(Hash hash)
    {
        int recs = this.database.update(DbMainHelper.DATA_TABLE, this.mapToContentValues(hash),
                DbMainHelper.DATA_REF_ID+"=?",
                new String[] {hash.getId()});
        if(recs==1)
            return hash;
        else
        {
            try {
                long id = this.database.insert(DbMainHelper.DATA_TABLE, null, this.mapToContentValues(hash));
                return hash;
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't insert record!", e);
            }
            return null;
        }
    }

    /**
     * Gets default hash (1st one)
     * @return Hash
     */
    public Hash get() {
        Cursor cursor=null;
        Hash hash;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=" + DbMainHelper.TYPE_HASH,
                    null,
                    null,
                    null,
                    null
            );
            if(!cursor.moveToFirst()) {
                hash=new Hash();
                this.save(hash);
            }
            else {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                final String s=ByteUtils.byteArrayToString(buffer, 0);
                hash=new Gson().fromJson(s, Hash.class);
            }
        }
        finally
        {
            cursor.close();
        }
        return hash;
    }

    @Override
    public ArrayList<Hash> getAll(Context context)
    {
        Cursor cursor=null;
        ArrayList<Hash> hashes=new ArrayList<Hash>();
        Hash hash;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=" + DbMainHelper.TYPE_HASH,
                    null,
                    null,
                    null,
                    null
            );
            if(!cursor.moveToFirst())
                return hashes;
            do
            {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                final String s=ByteUtils.byteArrayToString(buffer, 0);
                hash=new Gson().fromJson(s, Hash.class);
                hashes.add(hash);
            }
            while(cursor.moveToNext());
        }
        finally
        {
            cursor.close();
        }
        return hashes;
    }

    @Override
    public ArrayList<Hash> getGroup(Context context, String groupId)
    {
        return getAll(context);
    }

    @Override
    public Hash get(Context context, String id)
    {
        return null;
    }

    @Override
    public boolean deleteAll(Context context)
    {
        return false;
    }

    @Override
    public boolean deleteGroup(Context context, String id) {
        return false;
    }

    @Override
    public boolean delete(Context context, String id)
    {
        return false;
    }
}
