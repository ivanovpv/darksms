package ru.ivanovpv.gorets.psm.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.persistent.Purse;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 327 $
 *   $LastChangedDate: 2013-09-17 13:35:41 +0400 (Вт, 17 сен 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/service/ScreenStateBroadcastReceiver.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 06.07.12
 * Time: 9:33
 * To change this template use File | Settings | File Templates.
 */
public class ScreenStateBroadcastReceiver extends BroadcastReceiver
{
    private static String TAG=ScreenStateBroadcastReceiver.class.getName();

    public ScreenStateBroadcastReceiver()
    {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_SCREEN_OFF)) {
            Purse purse=Me.getMe().getMessagePurseDAO().get(context);
            if(purse.isComfortPINEnabled() && purse.isComfortPIN()) {
                purse.setComfortPINEnabled(false);
                Me.getMe().getMessagePurseDAO().save(context, purse);
            }
        }
    }
}
