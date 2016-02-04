package ru.ivanovpv.gorets.psm;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 490 $
 *   $LastChangedDate: 2014-01-29 18:26:27 +0400 (Ср, 29 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/Constants.java $
 */

public final class Constants
{
    private static final String PSM_PREFIX=Me.class.getPackage().getName();
    public static final String ACTION_SEND = PSM_PREFIX+".action_send";
    public static final String ACTION_SMS_SENT = PSM_PREFIX+".action_sms_sent";
    public static final String ACTION_SMS_DELIVERED = PSM_PREFIX+".action_sms_delivered";
    public static final String ACTION_MESSAGE_RECEIVED= PSM_PREFIX+".action_message_received";
    public static final String ACTION_REFRESH_CONVERSATION= PSM_PREFIX+".action_refresh_conversation";
    public static final String ACTION_INTENT_FORWARD = PSM_PREFIX+".action_intent_forward";
    public static final String EXTRA_MESSAGE_ID = PSM_PREFIX+".message_id";
    public static final String EXTRA_MESSAGE_TYPE = PSM_PREFIX+".message_type";
    public static final String EXTRA_MESSAGE = PSM_PREFIX+".message";
    public static final String EXTRA_CONTACT = PSM_PREFIX+".contact";
    public static final String EXTRA_PHONE_NUMBER = PSM_PREFIX+".phone_number";
    public static final String EXTRA_IS_CIPHER = PSM_PREFIX+".is_cipher";
    public static final String EXTRA_PAGE = PSM_PREFIX+".page";
    public static final String EXTRA_FILTER=PSM_PREFIX+".filter";
    public static final String EXTRA_KEYS=PSM_PREFIX+".keys";
}
