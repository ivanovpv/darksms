/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import ru.ivanovpv.gorets.psm.db.MessageDAO;

/**
 * Created by pivanov on 25.08.2014.
 */
public class HeadlessSmsSendService extends IntentService
{
    private static final String TAG=HeadlessSmsSendService.class.getName();

    public HeadlessSmsSendService() {
        super(HeadlessSmsSendService.class.getName());
        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(action)) {
            return;
        }

        Bundle extras = intent.getExtras();

        if (extras == null) {
            return;
        }

        String message = extras.getString(Intent.EXTRA_TEXT);
        Uri intentUri = intent.getData();
        String recipients = getRecipients(intentUri);

        // Exit if there is no destination
        if (TextUtils.isEmpty(recipients)) {
            return;
        }

        // Exit if there is no message
        if (TextUtils.isEmpty(message)) {
            return;
        }

        String[] destinations = TextUtils.split(recipients, ";");

        sendAndStoreTextMessage(getContentResolver(), destinations, message);
    }

    /**
     * get quick response recipients from URI
     */
    private String getRecipients(Uri uri) {
        String base = uri.getSchemeSpecificPart();
        int pos = base.indexOf('?');
        return (pos == -1) ? base : base.substring(0, pos);
    }

    /**
     * Send text message to recipients and store the message to SMS Content Provider
     *
     * @param contentResolver ContentResolver
     * @param destinations recipients of message
     * @param message message
     */
    private void sendAndStoreTextMessage(ContentResolver contentResolver, String[] destinations, String message) {
        SmsManager smsManager = SmsManager.getDefault();

        for (String destination : destinations) {
            smsManager.sendTextMessage(destination, null, message, null, null);

            ContentValues values = new ContentValues();
            values.put(MessageDAO.ADDRESS, destination);
            values.put(MessageDAO.BODY, message);

            Uri uri = contentResolver.insert(MessageDAO.SMS_SENT_URI, values);
        }
    }
}
