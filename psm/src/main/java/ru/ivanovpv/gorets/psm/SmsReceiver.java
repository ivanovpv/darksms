package ru.ivanovpv.gorets.psm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.RemoteViews;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.protocol.*;
import ru.ivanovpv.gorets.psm.widget.WidgetProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 494 $
 *   $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/SmsReceiver.java $
 */

public class SmsReceiver extends BroadcastReceiver {
    private static final String PDUS = "pdus";
    private final static String TAG = SmsReceiver.class.getName();
    static Me me;
    static Notification notificationPSM; //current notification (may be null)

    @Override
    public void onReceive(Context context, Intent intent) {
        if (me == null)
            me = Me.getMe();
        if(!me.isWriteMessagePermission()) {
            return;
        }

        //<action android:name="android.provider.Telephony.SMS_RECEIVED" /> <!-- pre kitkat action -->
        //<action android:name="android.provider.Telephony.SMS_DELIVER" /> <!-- kitkat action -->

        //if it is kitkat - ignore pre kitkat action
        if(intent.getAction().equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED") && android.os.Build.VERSION.SDK_INT >= 19)
            return;
        //if it is not kitkat - ignore kitkat action
        if(intent.getAction().equalsIgnoreCase("android.provider.Telephony.SMS_DELIVER") && android.os.Build.VERSION.SDK_INT < 19)
            return;
        this.abortBroadcast();
        Map<String, String> msgs = retrieveMessages(intent);
        for (String address : msgs.keySet()) {
            String msg = msgs.get(address);
            if (Me.DEBUG)
                Log.i(TAG, "New sms received=" + msg);
            //saving message in phone
            Message message = Message.createIncomingMessage(address, msg, null);
            message.setSeen(1); ////mark message seen, so stock messenger couldn't arose notification
            message.setRead(1); //mark message read, so stock messenger couldn't report it as unread
            message.setProcessed(false); //mark message unread for our messenger
            message = me.getMessageDAO().save(context, message);
            SmsReceiver.updateNotification(context, message);
        }
        //update widget
        WidgetProvider.forceWidgetUpdate(context);
        //send broadcast about new message
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.ACTION_REFRESH_CONVERSATION));
        //context.sendOrderedBroadcast(new Intent(Constants.ACTION_MESSAGE_RECEIVED), null);
    }

    private Map<String, String> retrieveMessages(Intent intent) {
        Map<String, String> msg = null;
        SmsMessage[] msgs = null;
        Bundle bundle = intent.getExtras();
        if (bundle != null && bundle.containsKey(PDUS)) {
            Object[] pdus = (Object[]) bundle.get(PDUS);
            if (pdus != null) {
                int nbrOfpdus = pdus.length;
                msg = new HashMap<String, String>(nbrOfpdus);
                msgs = new SmsMessage[nbrOfpdus];
                // There can be multiple SMS from multiple senders, there can be a maximum of nbrOfpdus different senders
                // However, send long SMS of same sender in one message
                for (int i = 0; i < nbrOfpdus; i++) {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    String originatinAddress = msgs[i].getOriginatingAddress();
                    // Check if index with number exists
                    if (!msg.containsKey(originatinAddress)) {
                        // Index with number doesn't exist
                        // Save string into associative array with sender number as index
                        msg.put(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody());
                    } else {
                        // Number has been there, add content but consider that
                        // msg.get(originatinAddress) already contains sms:sndrNbr:previousparts of SMS,
                        // so just add the part of the current PDU
                        String previousparts = msg.get(originatinAddress);
                        String msgString = previousparts + msgs[i].getMessageBody();
                        msg.put(originatinAddress, msgString);
                    }
                }
            }
        }
        return msg;
    }

    /**
     * Yet another alternate way to get messages
     *
     * @param intent Intent
     * @return array of SmsMessage
     */
    private SmsMessage[] getMessages(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get(PDUS);
            /*Set<String> keys=bundle.keySet();
            for(String key:keys)
            {
                Log.i(TAG, "Bundle key="+key);
            }*/
            final SmsMessage[] messages = new SmsMessage[pdus.length];
            for (int i = 0; i < pdus.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                if (Me.DEBUG)
                    Log.i(TAG, "Sms received=" + messages[i].getDisplayMessageBody());
            }
            return messages;
        }
        return null;
    }

    public static void clearNotification() {
        me=Me.getMe();
        NotificationManager notificationManager=me.getNotificationManager();
        notificationManager.cancelAll(); //cancel any pending notifications
        notificationPSM=null;
    }

    private static void updateNotification(Context context, Message message) {
        RemoteViews messageView;
        Intent intent;
        PendingIntent contentIntent;
        if(me==null)
            me = Me.getMe();
        String body=message.getBody();
        String notificationText;
        String notificationTitle=context.getString(R.string.psm);
        NotificationManager notificationManager=me.getNotificationManager();
        notificationManager.cancelAll(); //cancel any pending notifications
        notificationPSM=null;
        int unreadMessages=me.getMessageDAO().getUnreadMessagesCount(context);
        if(unreadMessages==0)
            return;
//        if(unreadMessages == 1)
        { //if single message unread
            //preparing intent to view message
            RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.new_message_notification);
            Protocol protocol;
            try {
                byte[] sharedKey=me.getContactDAO().getSharedKey(context, message.getAddress(), message.getMillis());
                if(sharedKey!=null)
                    protocol = Protocol.parseProtocol(body, sharedKey);
                else {
                    protocol=new ProtocolPlain();
                    Log.w(TAG, "Can't find key for address: "+message.getAddress());
                }
            }
            catch (ProtocolException pe) {
                Log.w(TAG, "Error parsing message", pe);
                protocol=new ProtocolPlain();
            }
            if (protocol instanceof ProtocolSecure)
                notificationText=context.getString(R.string.secretSMSReceived);
            else if (protocol instanceof ProtocolInvite)
                notificationText=context.getString(R.string.invitationSMSReceived);
            else if (protocol instanceof ProtocolAccept)
                notificationText=context.getString(R.string.acceptReceived);
            else
                notificationText=message.getBody();
            if(unreadMessages==1) {
                messageView = new RemoteViews(context.getPackageName(), R.layout.new_message_notification_row);
                messageView.setTextViewText(R.id.address, me.getContactDAO().getContactTitle(context, message.getAddress()));
                messageView.setTextViewText(R.id.body, notificationText);
                notificationView.addView(R.id.messages_notification_layout, messageView);
                intent = new Intent(context, ConversationActivity_.class);
                intent.setAction(Constants.ACTION_MESSAGE_RECEIVED);
    //            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    //            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(MessageDAO.CONVERSATION_ID, message.getConversationId());
                intent.putExtra(MessageDAO._ID, message.getId());
                ArrayList<String> addresses=new ArrayList<>();
                addresses.add(message.getAddress());
                intent.putExtra(MessageDAO.ADDRESS, addresses);
            }
            else {
                messageView = new RemoteViews(context.getPackageName(), R.layout.unread_messages_notification);
                String text=unreadMessages+context.getString(R.string.unreadMessages);
                messageView.setTextViewText(R.id.body, text);
                notificationView.addView(R.id.messages_notification_layout, messageView);
                intent = new Intent(context, PSMActivity_.class);
                intent.setAction(Constants.ACTION_MESSAGE_RECEIVED);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra(Constants.EXTRA_PAGE, PSMActivity.PAGE_MESSAGES);
            }
            //notificationPSM = new Notification(R.drawable.psm, notificationText, System.currentTimeMillis());
            notificationPSM = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.psm)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setWhen(System.currentTimeMillis())
                    .build();
            notificationPSM.flags=Notification.FLAG_AUTO_CANCEL;
            notificationPSM.sound=me.getMessageSoundUri();
            contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            notificationPSM.setLatestEventInfo(context, "New message received!", contentIntent);
            notificationPSM.contentIntent = contentIntent;
            notificationPSM.contentView = notificationView;
            notificationManager.notify(message.getId().hashCode(), notificationPSM);
        }
    }
}