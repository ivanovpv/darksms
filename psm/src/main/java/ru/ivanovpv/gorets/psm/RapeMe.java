package ru.ivanovpv.gorets.psm;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/RapeMe.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: egor.sarnavskiy
 * Date: 02.07.12
 * Time: 14:14
 * To change this template use File | Settings | File Templates.
 */
public class RapeMe {
    private static final String TAG=RapeMe.class.getName();
    public RapeMe() {}

    protected void launchMarket(Context context) {
        Uri uri = Uri.parse(context.getString(R.string.psmMarketUrl));
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            if(Me.DEBUG) {
                Log.i(TAG, context.getString(R.string.couldnt_launch_market));
                Toast.makeText(context, R.string.couldnt_launch_market, Toast.LENGTH_LONG).show();
            }
        }
    }
}
