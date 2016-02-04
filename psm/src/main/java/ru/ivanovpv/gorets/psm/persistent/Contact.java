package ru.ivanovpv.gorets.psm.persistent;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;

import java.io.Serializable;
import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 442 $
 *   $LastChangedDate: 2013-12-08 14:29:27 +0400 (Вс, 08 дек 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/persistent/Contact.java $
 */

public class Contact implements Serializable
{
    private final static String TAG = Contact.class.getName();

    transient private String id;
    private String key; //lookup key
    private ArrayList<PhoneNumber> phoneNumbers;

    transient Uri person;
    transient private String contactName;
    transient boolean thumbDefined=false;
    transient private String thumbnailUri=null; //low-res photo
    transient boolean photoDefined=false;
    transient private String photoUri=null;   //hi-res photo

    public Contact() {
        this.phoneNumbers=new ArrayList<PhoneNumber>();
    }

    public PhoneNumber getPrimaryPhoneNumber() {
        for(PhoneNumber phoneNumber:phoneNumbers)
            if(phoneNumber.isPrimary())
                return phoneNumber;
        return null;
    }

    public boolean isSaveable() {
        if(phoneNumbers==null || phoneNumbers.size()==0)
            return false;
        for(PhoneNumber phoneNumber:phoneNumbers) {
            if(phoneNumber.isSaveable())
                return true;
        }
        return false;
    }

    public void setContactName(final String contactName) {
        this.contactName=contactName;
    }

    public String getContactName()
    {
        return this.contactName;
    }

    public void setId(final String id)
    {
        this.id=id;
        person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(id));
        /*this.thumbnailUri= Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString();
        this.photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.DISPLAY_PHOTO).toString();*/
    }

    public String getThumbnailUri() {
        if(!thumbDefined) {
            this.thumbnailUri= Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY).toString();
            thumbDefined=true;
        }
        return thumbnailUri;
    }

    public String getPhotoUri() {
        if(!photoDefined) {
            this.photoUri = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.DISPLAY_PHOTO).toString();
            photoDefined=true;
        }
        return photoUri;
    }

    public static Uri getPhotoUri(long contactId) {
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.PHOTO);
    }

    public static Uri getThumbnailUri(long contactId) {
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public static Uri getThumbnailUri(String contactId) {
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(contactId));
        return Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    public String getId()
    {
        return this.id;
    }

    public void addPhoneNumber(final PhoneNumber number)
    {
        this.phoneNumbers.add(number);
    }

    public ArrayList<PhoneNumber> getPhoneNumbers()
    {
        return phoneNumbers;
    }

    public PhoneNumber getPhoneNumber(String address) {
        if(address==null)
            return null;
        for(PhoneNumber phoneNumber:phoneNumbers) {
            if(phoneNumber.compareDefault(address))
                return phoneNumber;
        }
        return null;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key=key;
    }
}
