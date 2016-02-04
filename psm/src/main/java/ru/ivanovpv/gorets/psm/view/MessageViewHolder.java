/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/MessageViewHolder.java $
 */

package ru.ivanovpv.gorets.psm.view;

import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: pivanov
 * Date: 22.11.13
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
public final class MessageViewHolder
{
    //selector
    public boolean isThreadView;
    //fields
    public String messageId;
    public String conversationId;
    public String recipientIds;
    public ArrayList<String> addresses;
    //views
    public TextView respondent;
    public TextView body;
    public TextView date;
    public ImageView icon;
    public TextView unreadMessages;

    public MessageViewHolder() {
        addresses=new ArrayList<>();
        isThreadView=true;
    }
}
