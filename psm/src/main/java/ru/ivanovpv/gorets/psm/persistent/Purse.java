package ru.ivanovpv.gorets.psm.persistent;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/Purse.java $
 */

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;

public class Purse {
    public static final String MESSAGE_PURSE_ID="psms";

    private AtomicInteger quantity; //purchased quantity

    private boolean premium=true; //For Open Source version always true
    private long expirationTime=0L; //For Open Source version ethernal subscription
    private boolean comfortPIN=true; //comfort PIN purchased? For Open Source version always true
    private boolean comfortPINEnabled=false; //comfort PIN enabled?
    private boolean hiddenConversation=true; //hiddenConversation purchased? For Open Source version always true
    private boolean synced=true; //synced with Google Play data? For Open Source version always true

    private String payLoad; //not used so far

    public Purse(int quantity, String payLoad) {
        this.quantity=new AtomicInteger(quantity);
        this.payLoad=payLoad;
    }

    public Purse(int quantity) {
        this.quantity=new AtomicInteger(quantity);
        this.payLoad="";
    }

    public Purse() {
        this.quantity=new AtomicInteger();
        this.payLoad="";
    }

    public int get() {
        return quantity.get();
    }

    public void set(int quantity) {
        this.quantity.set(quantity);
    }

    public int add(int delta) {
        return this.quantity.addAndGet(delta);
    }

    public int consume() {
        if(!isPremium() || !isSubscribed()) //decrement only if it is not premium or subscribed
            return this.quantity.decrementAndGet();
        return this.quantity.get();
    }

    public String getPayLoad() {
        return payLoad;
    }

    public boolean isPremium() {
        return premium;
    }

    public boolean isSubscribed() {
        if(expirationTime==0)
            return true;
        if(System.currentTimeMillis() <= expirationTime)
            return true;
        return false;
    }
    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    /**
     * Checks availability of PSMS
     * @return boolean
     */
    public boolean isAny() {
        if(isPremium())
            return true;
        if(isSubscribed())
            return true;
        if(quantity.get()>0)
            return true;
        return false;
    }

    public void setExpirationByMonth(long buyingTime) {
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(buyingTime);
        calendar.add(Calendar.MONTH, 1);
        expirationTime=calendar.getTimeInMillis();
    }

    public void setExpirationByYear(long buyingTime) {
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(buyingTime);
        calendar.add(Calendar.YEAR, 1);
        expirationTime=calendar.getTimeInMillis();
    }

    public boolean isComfortPIN() {
        return comfortPIN;
    }

    public void setComfortPIN(boolean comfortPIN) {
        this.comfortPIN=comfortPIN;
    }

    public boolean isComfortPINEnabled() {
        return comfortPINEnabled;
    }

    public void setComfortPINEnabled(boolean comfortPINEnabled) {
        this.comfortPINEnabled=comfortPINEnabled;
    }

    public boolean isHiddenConversation()
    {
        return hiddenConversation;
    }

    public void setHiddenConversation(boolean hiddenConversation)
    {
        this.hiddenConversation = hiddenConversation;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced=synced;
    }
}
