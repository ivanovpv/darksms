package ru.ivanovpv.gorets.psm.protocol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.persistent.Message;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 416 $
 *   $LastChangedDate: 2013-11-17 01:03:22 +0400 (Вс, 17 ноя 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/protocol/SentBroadcastReceiver.java $
 */

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 27.06.12
 * Time: 13:52
 * To change this template use File | Settings | File Templates.
 */
public class SentBroadcastReceiver extends BroadcastReceiver
{
    private static final String TAG=SentBroadcastReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Me.DEBUG)
            Log.i(TAG, "Sent receiver!");
        Message message=null;
        String id=intent.getStringExtra(Constants.EXTRA_MESSAGE_ID);
        if(id!=null)
            message=Me.getMe().getMessageDAO().get(context, id);
        switch (getResultCode())
        {
            case Activity.RESULT_OK:
                Toast.makeText(context, R.string.smsSent, Toast.LENGTH_SHORT).show();
                if(message!=null) {
                    message.setSentStatus(Message.STATUS_COMPLETE);
                    if(message.isCiphered())
                        Me.getMe().getMessagePurseDAO().get(context).consume();//purse consumed only when message was sent
                }
                break;
            case Activity.RESULT_CANCELED:
                Toast.makeText(context, R.string.smsCanceled, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(context, R.string.smsGenericFailure, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(context, R.string.smsNoService, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(context, R.string.smsNullPDU, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(context, R.string.smsRadioOff, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
                break;
            default:
                Toast.makeText(context, R.string.smsUnknowError, Toast.LENGTH_SHORT).show();
                if(message!=null)
                    message.setSentStatus(Message.STATUS_FAILED);
        }
        if(message!=null)
            Me.getMe().getMessageDAO().updateSentStatus(context, message);
    }
}
