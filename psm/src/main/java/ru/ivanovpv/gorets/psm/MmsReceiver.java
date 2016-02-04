package ru.ivanovpv.gorets.psm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.ArrayList;

import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.mms.MmsUtilsIntentService;
import ru.ivanovpv.gorets.psm.mms.pdu.GenericPdu;
import ru.ivanovpv.gorets.psm.mms.pdu.PduHeaders;
import ru.ivanovpv.gorets.psm.mms.pdu.PduParser;
import ru.ivanovpv.gorets.psm.mms.pdu.PduPersister;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessage;
import ru.ivanovpv.gorets.psm.widget.WidgetProvider;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 494 $
 *   $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/SmsReceiver.java $
 */

public class MmsReceiver extends BroadcastReceiver {
    private final static String TAG = MmsReceiver.class.getName();
    private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
    private static Me me;
    static Notification notificationPSM; //current notification (may be null)

    // Retrieve MMS
    public void onReceive(Context context, Intent intent) {

        if (me == null)
            me = Me.getMe();
        if(!me.isWriteMessagePermission()) {
            return;
        }
        //<action android:name="android.provider.Telephony.WAP_PUSH_RECEIVED" /> <!-- pre kitkat action -->
        //<action android:name="android.provider.Telephony.WAP_PUSH_DELIVER" /> <!-- kitkat action -->

        //if it is kitkat - ignore pre kitkat action
        if(intent.getAction().equalsIgnoreCase("android.provider.Telephony.WAP_PUSH_RECEIVED") && android.os.Build.VERSION.SDK_INT >= 19) {
            this.abortBroadcast();
            return;
        }
        //if it is not kitkat - ignore kitkat action
        if(intent.getAction().equalsIgnoreCase("android.provider.Telephony.WAP_PUSH_DELIVER") && android.os.Build.VERSION.SDK_INT < 19) {
            this.abortBroadcast();
            return;
        }
        this.abortBroadcast();
        if(Me.DEBUG)
            Log.i(TAG, "Bingo! MMS received!");
        Bundle bundle=intent.getExtras();
        if(bundle != null)
        {
            byte[] buffer=bundle.getByteArray("data");
            PduParser parser=new PduParser(buffer);
            GenericPdu pdu=parser.parse();
            if(pdu==null) {
                Log.e(TAG, "Invalid MMS pdu data!");
                return;
            }
            switch (pdu.getMessageType()) {
                case PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND:
                    PduPersister p=PduPersister.getPduPersister(context);
                    Uri uri=null;
                    try {
                        uri = p.persist(pdu, MessageDAO.MMS_INBOX_URI, true, false, null);
                    }
                    catch(Exception ex) {
                        Log.w(TAG, "Error while persisting PDU");
                    }
                    MultimediaMessage multimediaMessage=me.getMessageDAO().getMmsMessage(context, uri);
                    multimediaMessage.setSeen(1); ////mark message seen, so stock messenger couldn't arose notification
                    multimediaMessage.setRead(1); //mark message read, so stock messenger couldn't report it as unread
                    multimediaMessage.setProcessed(false); //mark message unread for our messenger
                    multimediaMessage=me.getMessageDAO().saveMms(context, multimediaMessage);
                    // start service to download mms attachments
                    MmsUtilsIntentService.startActionReceiveMms(context, multimediaMessage);
                    //update widget
                    WidgetProvider.forceWidgetUpdate(context);
                    updateNotification(context, multimediaMessage);
                break;
                case PduHeaders.MESSAGE_TYPE_DELIVERY_IND: //@todo handle delivery report
                    Log.v(TAG, "Received delivery report");
                    break;
                case PduHeaders.MESSAGE_TYPE_READ_ORIG_IND: //@todo handle read report
                    Log.v(TAG, "Received read report");
                    break;
                default:
                    Log.w(TAG, "Unknown pdu message type received=" + pdu.getMessageType());
                }
        }
    }

    private void updateNotification(Context context, MultimediaMessage mms) {
        RemoteViews messageView;
        Intent intent;
        PendingIntent contentIntent;
        String notificationText;
        String notificationTitle=context.getString(R.string.psm);
        NotificationManager notificationManager=me.getNotificationManager();
        notificationManager.cancelAll(); //cancel any pending notifications
        notificationPSM=null;
        int unreadMessages=me.getMessageDAO().getUnreadMessagesCount(context);
        if(unreadMessages==0)
            return;

        //preparing intent to view message
        RemoteViews notificationView = new RemoteViews(context.getPackageName(), R.layout.new_message_notification);
        notificationText=context.getString(R.string.multimediaMessage);
        if(unreadMessages==1) {
            messageView = new RemoteViews(context.getPackageName(), R.layout.new_message_notification_row);
            messageView.setTextViewText(R.id.address, me.getContactDAO().getContactTitle(context, mms.getAddress()));
            messageView.setTextViewText(R.id.body, notificationText);
            notificationView.addView(R.id.messages_notification_layout, messageView);
            intent = new Intent(context, ConversationActivity_.class);
            intent.setAction(Constants.ACTION_MESSAGE_RECEIVED);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(MessageDAO.CONVERSATION_ID, mms.getConversationId());
            intent.putExtra(MessageDAO._ID, mms.getId());
            ArrayList<String> addresses=new ArrayList<>();
            addresses.add(mms.getAddress());
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
        notificationPSM = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.psm)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setWhen(System.currentTimeMillis())
                .build();
        notificationPSM.flags=Notification.FLAG_AUTO_CANCEL;
        notificationPSM.sound=me.getMessageSoundUri();
        contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationPSM.contentIntent = contentIntent;
        notificationPSM.contentView = notificationView;
        notificationManager.notify(mms.hashCode(), notificationPSM);
    }
 }