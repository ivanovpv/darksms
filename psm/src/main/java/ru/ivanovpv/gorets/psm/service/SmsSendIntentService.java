package ru.ivanovpv.gorets.psm.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.MessageBox;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.cipher.FingerPrint;
import ru.ivanovpv.gorets.psm.nativelib.NativeRandom;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolPlain;
import ru.ivanovpv.gorets.psm.protocol.ProtocolSecure;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class SmsSendIntentService extends IntentService {
    private static final String TAG=SmsSendIntentService.class.getName();

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionSend(Context context, Message message, PhoneNumber phoneNumber, Boolean isCipher) {
        Protocol protocol;
        String body;
        message.setSentStatus(Message.STATUS_PENDING);
        message.setDeliveredStatus(Message.STATUS_PENDING);
        message = Me.getMe().getMessageDAO().save(context, message);
        if (isCipher) {
            NativeRandom r = new NativeRandom();
            int sessionIndex = r.getInt(Protocol.SESSION_SIZE);
            byte[] sessionKey = phoneNumber.getSessionKey(System.currentTimeMillis(), sessionIndex);
            if (Me.DEBUG)
                Log.i(TAG, "Session key=" + new FingerPrint(sessionKey).toString());
            Cipher cipher = Hash.getDefaultMessagingCipher(sessionKey);
            protocol = new ProtocolSecure(cipher, sessionIndex);
        } else
            protocol = new ProtocolPlain();
        if(!message.isBodyReal())
            body=message.getStoredBody();
        else
            body=message.getBody();
        message.setBody(protocol.encodeString(body));
        //message = Me.getMe().getMessageDAO().save(context, message);
        Intent intent = new Intent(context, SmsSendIntentService.class);
        intent.setAction(Constants.ACTION_SEND);
        intent.putExtra(Constants.EXTRA_MESSAGE, message);
        context.startService(intent);
    }

    public SmsSendIntentService() {
        super("SmsSendIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (Constants.ACTION_SEND.equals(action)) {
                final Message message = (Message )intent.getSerializableExtra(Constants.EXTRA_MESSAGE);
                sendMessage(this, message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Constants.ACTION_REFRESH_CONVERSATION));
            }
        }
    }

    private void sendMessage(Context context, Message message)
    {
        ArrayList<String> parts;
        if(!PhoneNumber.isPhoneNumber(message.getAddress())) {
            Log.w(TAG, message.toString() + " - can't be used for SMS");
        }
        SmsManager sms = SmsManager.getDefault();
        message.setSentStatus(Message.STATUS_PENDING);
        message.setDeliveredStatus(Message.STATUS_PENDING);
/*        if(message.getBody()==null)
            message.setBody(this.encodeBytes(message.getBody()));
        else
            message.setBody(this.encodeString(message.getBody()));*/
        parts=sms.divideMessage(message.getBody());
        Intent intentSent=new Intent(Constants.ACTION_SMS_SENT);
        intentSent.putExtra(Constants.EXTRA_MESSAGE_ID, message.getId());
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, intentSent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentDelivered=new Intent(Constants.ACTION_SMS_DELIVERED);
        intentSent.putExtra(Constants.EXTRA_MESSAGE_ID, message.getId());
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, intentDelivered, PendingIntent.FLAG_CANCEL_CURRENT);

        ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
        for(int i=0; i < parts.size(); i++)
        {
            sentPIs.add(sentPI);
            deliveredPIs.add(deliveredPI);
        }
        message=Me.getMe().getMessageDAO().save(context, message);
        sms.sendMultipartTextMessage(message.getAddress(), null, parts, sentPIs, deliveredPIs);
        if(Me.DEBUG)
            Log.i(TAG, "Send message=" + message.getBody());
    }

    /**
     * @param context
     * @param phoneNumber
     * @param body
     */
    public static void sendRawMessage(Context context, String phoneNumber, String body, String id)
    {
        SmsManager sms = SmsManager.getDefault();
        ArrayList<String> parts=sms.divideMessage(body);

        Intent intentSent=new Intent(Constants.ACTION_SMS_SENT);
        intentSent.putExtra(Constants.EXTRA_MESSAGE_ID, id);
        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, intentSent, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentDelivered=new Intent(Constants.ACTION_SMS_DELIVERED);
        intentDelivered.putExtra(Constants.EXTRA_MESSAGE_ID, id);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, intentDelivered, PendingIntent.FLAG_CANCEL_CURRENT);

        ArrayList<PendingIntent> sentPIs = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPIs = new ArrayList<PendingIntent>();
        for(int i=0; i < parts.size(); i++)
        {
            sentPIs.add(sentPI);
            deliveredPIs.add(deliveredPI);
        }
        sms.sendMultipartTextMessage(phoneNumber, null, parts, sentPIs, deliveredPIs);
        if(Me.DEBUG)
            Log.i(TAG, "Send message=" + body);
    }
}
