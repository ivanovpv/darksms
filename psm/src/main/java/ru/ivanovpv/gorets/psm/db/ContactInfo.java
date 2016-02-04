package ru.ivanovpv.gorets.psm.db;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 332 $
 *   $LastChangedDate: 2013-09-18 12:33:14 +0400 (Ср, 18 сен 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/ContactInfo.java $
 */

import android.content.Context;
import ru.ivanovpv.gorets.psm.R;

public class ContactInfo {
    public String id;
    public String key;
    public String name;
    public String phone;

    public ContactInfo() {
        this.name=this.id=this.key=null;
    }
    public ContactInfo(String name, String id, String key, String phone) {
        this.name=name;
        this.id=id;
        this.key=key;
        this.phone=phone;
    }

    public String getFullInfo(Context context) {
        if(isEmpty(this.phone) && isEmpty(this.name))
            return context.getString(R.string.anonymous);
        else if(isEmpty(this.name))
            return this.phone;
        else if(isEmpty(this.phone))
            return this.name;
        else
            return String.format("%s (%s)", this.name, this.phone);
    }

    public String getShortInfo(Context context) {
        if(isEmpty(this.phone) && isEmpty(this.name))
            return context.getString(R.string.anonymous);
        else if(isEmpty(this.name))
            return this.phone;
        return this.name;
    }

    private boolean isEmpty(String text) {
        if(text==null || text.trim().length()==0)
            return true;
        return false;
    }
}
