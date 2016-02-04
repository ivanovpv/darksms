package ru.ivanovpv.gorets.psm.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import com.google.i18n.phonenumbers.Phonenumber;
import ru.ivanovpv.gorets.psm.Me;

import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 494 $
 *   $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/AbstractBaseDAO.java $
 */

public abstract class AbstractBaseDAO<E>
{
    private final static String TAG = AbstractBaseDAO.class.getName();
    protected DbMainHelper dbHelper;

    public abstract E save(Context context, final E entity);

    public abstract ArrayList<E> getAll(Context context);
    public abstract ArrayList<E> getGroup(Context context, String groupId);
    public abstract E get(Context context, String id);
    public abstract boolean deleteAll(Context context);
    public abstract boolean deleteGroup(Context context, String id);
    public abstract boolean delete(Context context, String id);
    protected SQLiteDatabase database;

    protected AbstractBaseDAO(final DbMainHelper dbHelper)
    {
        this.dbHelper=dbHelper;
        this.openDatabase();
    }

    public static void debugCursor(Cursor cursor)
    {
        int index;
        String field;
        try {
            String[] names = cursor.getColumnNames();
            for (int i = 0; i < names.length; i++) {
                index = cursor.getColumnIndex(names[i]);
                if (index >= 0)
                    field = cursor.getString(index);
                else
                    field = null;
                Log.i(TAG, "Column #" + i + "=" + names[i] + ", value=" + field);
            }
        }
        catch(Exception e) {
            debugCursorFields(cursor);
        }
    }

    public static void debugCursorFields(Cursor cursor)
    {
        String[] names=cursor.getColumnNames();
        for(int i=0; i < names.length; i++)
        {
            Log.i(TAG, "Column #" + i + "=" + names[i]);
        }
    }


    public void openDatabase() {

        try {
            if (this.database == null) {
                    this.database = dbHelper.getWritableDatabase();
            }
        } catch (SQLiteException e) {
            this.database = dbHelper.getReadableDatabase();
        }
    }

    public void closeDatabase() {
        this.database.close();
        if(Me.DEBUG)
            Log.i(TAG, "Closing PSM database");
    }

}
