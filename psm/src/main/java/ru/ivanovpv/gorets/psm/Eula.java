package ru.ivanovpv.gorets.psm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 416 $
 *   $LastChangedDate: 2013-11-17 01:03:22 +0400 (Вс, 17 ноя 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/Eula.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 06.07.12
 * Time: 11:05
 * To change this template use File | Settings | File Templates.
 */
public final class Eula
{
    private static final String PREFERENCE_EULA_ACCEPTED="eula.accepted";
    private Me me;

    public Eula(Context context) {
        me = Me.getMe();
    }
    /**
     * callback to let the activity know when the user has accepted the EULA.
     */
    public static interface OnEulaAction
    {

        /**
         * Called when the user has accepted the eula and the dialog closes.
         */
        void onEulaAgreedTo();
        void onEulaRefuseTo();
    }

    /**
     * Displays the EULA if necessary. This method should be called from the onCreate()
     * method of your main Activity.
     *
     * @param context current context
     * @param responder listener to respond about results of EULA acceptance
     */
    public void showAndAsk(final Context context, final OnEulaAction responder)
    {
        final AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.eula_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                accept();
                responder.onEulaAgreedTo();
            }
        });
        builder.setNegativeButton(R.string.eula_refuse, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                refuse();
                responder.onEulaRefuseTo();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            public void onCancel(DialogInterface dialog)
            {
                refuse();
                responder.onEulaRefuseTo();
            }
        });
        builder.setMessage(readEula(context));
        builder.create().show();
    }

    static void show(final Context context)
    {
        final AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(R.string.eula_title);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                return;
            }
        });
        builder.setMessage(readEula(context));
        builder.create().show();
    }

    private void accept()
    {
        me.psmSettings.eulaAccepted().put(true);
    }

    public boolean isAccepted()
    {
        return me.psmSettings.eulaAccepted().getOr(false);
    }

    private void refuse()
    {
        me.psmSettings.eulaAccepted().put(false);
    }

    private static CharSequence readEula(Context context)
    {
        BufferedReader in=null;
        try
        {
            String eula_asset=context.getString(R.string.eula_asset);
            in=new BufferedReader(new InputStreamReader(context.getAssets().open(eula_asset)));
            String line;
            StringBuilder buffer=new StringBuilder();
            while((line=in.readLine()) != null) buffer.append(line).append('\n');
            return buffer;
        }
        catch(IOException e)
        {
            return "";
        }
        finally
        {
            closeStream(in);
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private static void closeStream(Closeable stream)
    {
        if(stream != null)
        {
            try
            {
                stream.close();
            }
            catch(IOException e)
            {
                // Ignore
            }
        }
    }
}

