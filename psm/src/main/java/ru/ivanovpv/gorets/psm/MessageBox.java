package ru.ivanovpv.gorets.psm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Log;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 353 $
 *   $LastChangedDate: 2013-10-01 17:47:07 +0400 (Вт, 01 окт 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/MessageBox.java $
 */

public class MessageBox implements DialogInterface.OnClickListener {
    private AlertDialog.Builder adb;
    private static final String TAG=MessageBox.class.getName();

    public MessageBox(Context context, String message)
    {
        if(Me.DEBUG)
            Log.i(TAG, "Showing message box="+message);
        adb=new AlertDialog.Builder(context);
        adb.setMessage(message);
        adb.setPositiveButton(R.string.ok, this);
        adb.setIcon(R.drawable.psm);
        adb.show();
    }

    public MessageBox(Context context, int resId)
    {
        String message=context.getString(resId);
        if(Me.DEBUG)
            Log.i(TAG, "Showing message box="+message);
        adb=new AlertDialog.Builder(context);
        adb.setMessage(context.getString(resId));
        adb.setPositiveButton(R.string.ok, this);
        adb.setIcon(R.drawable.psm);
        adb.show();
    }

    public MessageBox(Context context, Exception ex) {
        String message=context.getString(R.string.error)+ex.getLocalizedMessage();
        if(Me.DEBUG)
            Log.i(TAG, "Showing error message box="+message);
        adb=new AlertDialog.Builder(context);
        adb.setMessage(context.getString(R.string.error)+ex.getLocalizedMessage());
        adb.setPositiveButton(R.string.ok, this);
        adb.setIcon(R.drawable.exclamation_mark);
        adb.show();
    }


    public void onClick(DialogInterface dialogInterface, int i)
    {
    }
}
