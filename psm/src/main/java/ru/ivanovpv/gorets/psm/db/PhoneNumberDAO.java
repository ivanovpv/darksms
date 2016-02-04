package ru.ivanovpv.gorets.psm.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 495 $
 *   $LastChangedDate: 2014-02-03 22:50:58 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/PhoneNumberDAO.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 03.02.14
 * Time: 13:29
 * To change this template use File | Settings | File Templates.
 */
public class PhoneNumberDAO extends AbstractBaseDAO<PhoneNumber>
{
    public final static String TAG=PhoneNumberDAO.class.getName();

    public PhoneNumberDAO(final DbMainHelper dbHelper) {
        super(dbHelper);
    }

    private ContentValues mapToContentValues(final PhoneNumber phoneNumber)
    {
        final Gson gson = new Gson();
        String s=gson.toJson(phoneNumber, PhoneNumber.class);
        byte[] buffer= ByteUtils.stringToByteArray(s);
        ContentValues cv=new ContentValues();
        cv.put(DbMainHelper.DATA_TYPE, DbMainHelper.TYPE_PHONE);
        cv.put(DbMainHelper.DATA_REF_ID, phoneNumber.getNormalAddress());
        cv.put(DbMainHelper.DATA_REF_ID2, phoneNumber.getContactKey());
        cv.put(DbMainHelper.DATA_BLOB, buffer);
        return cv;
    }

    @Override
    public PhoneNumber save(Context context, PhoneNumber phoneNumber)
    {
        if(!phoneNumber.isSaveable())
            return phoneNumber;
        int recs=0;
        database.beginTransaction();
        ContentValues cv=this.mapToContentValues(phoneNumber);
        if(phoneNumber.getId()!=null) {
            cv.put(DbMainHelper._ID, phoneNumber.getId());
            recs = this.database.update(DbMainHelper.DATA_TABLE, this.mapToContentValues(phoneNumber),
                DbMainHelper.DATA_TYPE + "=? and " + DbMainHelper.DATA_REF_ID + "=? and "+DbMainHelper._ID+"=?",
                new String[] {new Integer(DbMainHelper.TYPE_PHONE).toString(), phoneNumber.getNormalAddress(), phoneNumber.getId()});
        }
        if(recs!=1)
        {
            try {
                this.database.insert(DbMainHelper.DATA_TABLE, null, this.mapToContentValues(phoneNumber));
            } catch (SQLiteException e) {
                Log.e(TAG, "Can't insert record!", e);
            }
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return this.get(context, phoneNumber.getRawAddress());
    }

    @Override
    public ArrayList<PhoneNumber> getAll(Context context)
    {
        Cursor cursor=null;
        ArrayList<PhoneNumber> phoneNumbers=new ArrayList<PhoneNumber>();
        PhoneNumber phoneNumber;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=" + DbMainHelper.TYPE_PHONE,
                    null,
                    null,
                    null,
                    null
            );
            if(!cursor.moveToFirst())
                return phoneNumbers;
            do
            {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                final String s=ByteUtils.byteArrayToString(buffer, 0);
                phoneNumber=new Gson().fromJson(s, PhoneNumber.class);
                phoneNumbers.add(phoneNumber);
            }
            while(cursor.moveToNext());
        }
        finally
        {
            cursor.close();
        }
        return phoneNumbers;
    }

    @Override
    public ArrayList<PhoneNumber> getGroup(Context context, String contactKey)
    {
        if(contactKey==null)
            return this.getAll(context);
        Cursor cursor=null;
        ArrayList<PhoneNumber> phoneNumbers=new ArrayList<PhoneNumber>();
        PhoneNumber phoneNumber;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=? and " + DbMainHelper.DATA_REF_ID2+" =? ",
                    new String[] {new Integer(DbMainHelper.TYPE_PHONE).toString(), contactKey},
                    null,
                    null,
                    null
            );
            if(!cursor.moveToFirst())
                return phoneNumbers;
            do
            {
                byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
                final String s=ByteUtils.byteArrayToString(buffer, 0);
                phoneNumber=new Gson().fromJson(s, PhoneNumber.class);
                phoneNumbers.add(phoneNumber);
            }
            while(cursor.moveToNext());
        }
        finally
        {
            cursor.close();
        }
        return phoneNumbers;
    }

    @Override
    public PhoneNumber get(Context context, String address)
    {
        PhoneNumber phoneNumber=new PhoneNumber(address);
        address=phoneNumber.getNormalAddress();
        Cursor cursor=null;
        try
        {
            cursor=dbHelper.getReadableDatabase().query(DbMainHelper.DATA_TABLE,
                    null,
                    DbMainHelper.DATA_TYPE + "=? and " + DbMainHelper.DATA_REF_ID + " =? ",
                    new String[] {new Integer(DbMainHelper.TYPE_PHONE).toString(), address},
                    null,
                    null,
                    null
            );
            if(!cursor.moveToFirst())
                return phoneNumber;
            byte[] buffer=cursor.getBlob(DbMainHelper.DATA_BLOB_INDEX);
            final String s=ByteUtils.byteArrayToString(buffer, 0);
            phoneNumber=new Gson().fromJson(s, PhoneNumber.class);
        }
        finally
        {
            cursor.close();
        }
        return phoneNumber;
    }

    @Override
    public boolean deleteAll(Context context)
    {
        database.beginTransaction();
        int recs=database.delete(DbMainHelper.DATA_TABLE, DbMainHelper.DATA_TYPE+" = "+ DbMainHelper.TYPE_PHONE, null);
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs>=0)
            return true;
        return false;
    }

    @Override
    public boolean deleteGroup(Context context, String contactKey)
    {
        database.beginTransaction();
        int recs=database.delete(DbMainHelper.DATA_TABLE,
                DbMainHelper.DATA_TYPE+"=? and "+
                DbMainHelper.DATA_REF_ID2+ "=?",
                new String[] {new Integer(DbMainHelper.TYPE_PHONE).toString(), contactKey});
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs>=0)
            return true;
        return false;
    }

    @Override
    public boolean delete(Context context, String address)
    {
        database.beginTransaction();
        PhoneNumber phoneNumber=new PhoneNumber(address);
        address=phoneNumber.getNormalAddress();
        int recs=database.delete(DbMainHelper.DATA_TABLE,
                DbMainHelper.DATA_TYPE+"=? and "+
                DbMainHelper.DATA_REF_ID+ "=?",
                new String[] {new Integer(DbMainHelper.TYPE_PHONE).toString(), address});
        database.setTransactionSuccessful();
        database.endTransaction();
        if(recs>=0)
            return true;
        return false;
    }
}
