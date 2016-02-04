/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.utils;

import android.content.Context;
import ru.ivanovpv.gorets.psm.persistent.Message;

import java.util.ArrayList;

/**
 * Created by Gorets on 18.05.14.
 */
public class MessageUtils {

    public static void copyMessage(Message message, Context context) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(message.getBody());
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(message.getDisplayName(), message.getBody());
            clipboard.setPrimaryClip(clip);
        }
    }

    public void setAddress(ArrayList<Message> messages, String address) {
        for (Message message : messages) {
            if (address == null) {
                address = message.getAddress();
                break; //no need to scan all messages, since address is the same everywhere
            }
        }
    }
}
