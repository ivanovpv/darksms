/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/ConversationViewHolder.java $
 */

package ru.ivanovpv.gorets.psm.view;

import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.ivanovpv.gorets.psm.persistent.Message;

import java.io.Serializable;

/**
 * Helper class nothing special
 */
public class ConversationViewHolder {
    public boolean isSms;
    public TextView separatorText;
    public TextView incomingText;
    public TextView sentText;
    public TextView incomingTime;
    public TextView sentTime;
    public TextView incomingType;
    public TextView sentType;
    public ImageButton statusIcon;
    public ProgressBar progressBar;
    public ViewGroup incoming;
    public ViewGroup sent;
    public ViewGroup incomingBody; //for mms
    public ViewGroup sentBody; //for mms
    public Message message;
}
