package ru.ivanovpv.gorets.psm.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.persistent.Contact;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds;


/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 495 $
 *   $LastChangedDate: 2014-02-03 22:50:58 +0400 (Пн, 03 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/db/ContactDAO.java $
 */

public class ContactDAO extends AbstractBaseDAO<Contact>
{
    private final static String TAG = ContactDAO.class.getName();
    Type phoneListType = new TypeToken<List<PhoneNumber>>(){}.getClass();
    public static final String LAST_CONTACT_DATE_DESC = "last_time_contacted desc";
    private PhoneNumberDAO phoneNumberDAO;
    private static final String[] PHONE_NUMBER_PROJECTION = new String[] {
            ContactsContract.Contacts._ID,                 // 0
            ContactsContract.Contacts.LOOKUP_KEY,          // 1
            ContactsContract.Contacts.HAS_PHONE_NUMBER,    // 2
            ContactsContract.Contacts.DISPLAY_NAME,        // 3
            ContactsContract.CommonDataKinds.Phone.NUMBER, // 4
            ContactsContract.CommonDataKinds.Phone.TYPE,   // 5
            ContactsContract.CommonDataKinds.Phone.LABEL   // 6
    };
    private static int api=-1;

    public ContactDAO(final DbMainHelper dbHelper)
    {
        super(dbHelper);
        phoneNumberDAO=new PhoneNumberDAO(dbHelper);
        //userPicsCache=new UserPicsCache();
    }

/*    private ContentValues mapToContentValues(final Contact contact)
    {
        final Gson gson = new Gson();
        String s=gson.toJson(contact, Contact.class);
        byte[] buffer=ByteUtils.stringToByteArray(s);
        ContentValues cv=new ContentValues();
            cv.put(DbMainHelper.DATA_TYPE, DbMainHelper.TYPE_CONTACT);
            cv.put(DbMainHelper.DATA_REF_ID, contact.getKey());
            cv.put(DbMainHelper.DATA_BLOB, buffer);
        return cv;
    }*/

    @Override
    public Contact save(final Context context, final Contact contact)
    {
        for(PhoneNumber phoneNumber:contact.getPhoneNumbers()) {
            phoneNumberDAO.save(context, phoneNumber);
        }
        return contact;
    }

    @Override
    public Contact get(Context context, String key)
    {
        int primary;
        PhoneNumber phoneNumber;
        String id="";
        Contact contact = new Contact();
        Cursor cursor;

        if(key==null || key.trim().length()==0)
            return null;
        Uri lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key);
        Uri res = ContactsContract.Contacts.lookupContact(context.getContentResolver(), lookupUri);
        //getting contact name
        cursor=context.getContentResolver().query(res, null, null, null, null);
        if(cursor.moveToFirst())
        {
            String name=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            key=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
            id=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            contact.setContactName(name);
            contact.setKey(key);
            contact.setId(id);
        }
        cursor.close();

        //getting phones
        cursor=context.getContentResolver().query(
                CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.Contacts.LOOKUP_KEY + " = ?",
                new String[]{key},
                null);
        while(cursor.moveToNext())
        {
            String phone=cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
            primary=cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.IS_PRIMARY));
            String label= ContactDAO.getPhoneLabel(context, cursor);
            phoneNumber=phoneNumberDAO.get(context, phone);
            if(phoneNumber==null)
                phoneNumber=new PhoneNumber(phone, label, (primary==1)?true:false);
            contact.addPhoneNumber(phoneNumber);
        }
        cursor.close();
        return contact;
    }

    public Loader<Cursor> getContactsCursorLoader(Context context, String where) {
        Uri uri;
        if(where==null || where.length()==0) {
            uri=ContactsContract.Contacts.CONTENT_URI;
        }
        else {
            uri=Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, where);
        }
        return new CursorLoader(context, uri, new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.LOOKUP_KEY
        }, ContactsContract.Contacts.HAS_PHONE_NUMBER+">?", new String[] {"0"}, "2 asc");
    }

    @Deprecated
    public Cursor getAdapterCursor(Context context, String where) {
        String selection;
        if(where==null || where.length()==0)
            selection=ContactsContract.Contacts.HAS_PHONE_NUMBER+">0";
        else
            selection=ContactsContract.Contacts.DISPLAY_NAME +
                    " like '%"+where+"%' COLLATE NOCASE and "+ ContactsContract.Contacts.HAS_PHONE_NUMBER+">0";
        return context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, new String[] {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts.LOOKUP_KEY
        }, selection, null, "2 asc");
    }

    //need to be rewritten - now it's just placeholder, id has to be contacts group id
    @Override
    public ArrayList<Contact> getGroup(Context context, String id)
    {
        ArrayList<Contact> contacts=new ArrayList<Contact>();
        contacts.add(get(context, id));
        return contacts;
    }

    @Override
    @Deprecated
    public ArrayList<Contact> getAll(Context context)
    {
        PhoneNumber phoneNumber;
        ArrayList<Contact> contacts=new ArrayList<Contact>();
        Contact contact;
        Cursor cursor=context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if(cursor.getCount() > 0)
        {
            while(cursor.moveToNext())
            {
                contact=new Contact();
                String key=null;
                final String id=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                contact.setId(id);
                String name=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                key=cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                contact.setContactName(name);
                String phone=cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
                int primary=cursor.getInt(cursor.getColumnIndex(ContactsContract.Data.IS_PRIMARY));
                String label= ContactDAO.getPhoneLabel(context, cursor);
                phoneNumber=phoneNumberDAO.get(context, phone);
                if(phoneNumber==null)
                    phoneNumber=new PhoneNumber(phone, label, (primary==1)?true:false);
                contact.addPhoneNumber(phoneNumber);
            }
        }
        return contacts;
    }


    @Override
    public boolean deleteAll(Context context) {
        return false;
    }

    @Override
    public boolean deleteGroup(Context context, String id) {
        return false;
    }

    @Override
    public boolean delete(Context context, String key)
    {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, key);
        Cursor cursor=context.getContentResolver().query(uri, null, null, null, null);
        while(cursor.moveToNext())
        {
            String phone=cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.NUMBER));
            phoneNumberDAO.delete(context, phone);
        }
        int recs=cr.delete(uri, null, null);
        if(recs >= 1)
            return true;  //removed both from phone database and PSM storage
        return false;
    }

    private static String getPhoneLabel(Context context, Cursor cursor) {
        int type=cursor.getInt(cursor.getColumnIndex(CommonDataKinds.Phone.TYPE));
        String customLabel=cursor.getString(cursor.getColumnIndex(CommonDataKinds.Phone.LABEL));
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, customLabel).toString();
    }

    public static List<String> contactsListToString(final ArrayList<Contact> contacts) {
        final List<String> list = new ArrayList<String>();
        for (Contact contact: contacts) {
            list.add(contact.getContactName().toString());
        }
        return list;
    }

    private ContactInfo lookupContactInfo(Context context, String phoneNumber) {
        ContactInfo contactInfo=null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.LOOKUP_KEY,
                ContactsContract.PhoneLookup.LAST_TIME_CONTACTED};

        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, ContactDAO.LAST_CONTACT_DATE_DESC);
        if(cursor!=null && cursor.moveToFirst()) {
            contactInfo = new ContactInfo();
            contactInfo.name = cursor.getString(0);
            contactInfo.id = cursor.getString(1);
            contactInfo.key = cursor.getString(2);
            contactInfo.phone=phoneNumber;
        }
        if(cursor!=null)
            cursor.close();
        return contactInfo;
    }

    /**
     *
     * @param context
     * @param address //phone number as-is
     * @return null if not in contacts. array[0] - Display name, [1] - contactId
     */
    public ContactInfo getContactInfoByAddress(Context context, String address) {
        ContactInfo contactInfo;
        if(TextUtils.isEmpty(address)) {
            return new ContactInfo(context.getString(R.string.anonymous), null, null, address);
        }
        if(!PhoneNumber.isPhoneNumber(address))
            return new ContactInfo(address, null, null, null);
        ArrayList<String> phoneVersion=new ArrayList<String>();
        contactInfo=new ContactInfo("", null, null, address);
        String phoneNumber = PhoneNumberUtils.stripSeparators(address);
        if(TextUtils.isEmpty(phoneNumber)) {
            return contactInfo;
        }
        phoneVersion.add(phoneNumber);
        String fPhoneNumber=PhoneNumberUtils.formatNumber(address);
        int index=fPhoneNumber.lastIndexOf('-');
        if(index==-1)
            index=fPhoneNumber.lastIndexOf('.');
        String localPhone=fPhoneNumber.substring(index+1);
        if(localPhone!=null || localPhone.length()!=0) {
            phoneVersion.add(localPhone);
        }
        ContactInfo cInfo;
        for(String phone:phoneVersion) {
            cInfo=lookupContactInfo(context, phone);
            if(cInfo!=null)
                return cInfo;
        }
        return contactInfo;
    }

    /**
     *
     * @param context
     * @param sourcePhoneNumber //phone number as-is
     * @return null if not in contacts. array[0] - Display name, [1] - contactId
     */
    @Deprecated
    public String[] getContactNameByPhoneNumber2(Context context, String sourcePhoneNumber) {
        String phoneNumber = PhoneNumberUtils.stripSeparators(sourcePhoneNumber);
        String contactInfo[] = {null, phoneNumber};
        if(phoneNumber==null)
            return contactInfo;
        String[] projection = new String[] {
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.STARRED,
                ContactsContract.Contacts.CONTACT_STATUS,
                ContactsContract.Contacts.CONTACT_PRESENCE
        };
        String selection = "PHONE_NUMBERS_EQUAL(" +
                ContactsContract.CommonDataKinds.Phone.NUMBER +
                ",?) AND " +
                ContactsContract.Data.MIMETYPE + "='" +
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
        String selectionArgs[] = new String[] { phoneNumber };
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
        if(cursor!=null && cursor.moveToFirst()) {
            contactInfo = new String[2];
            contactInfo[0] = cursor.getString(0);
            contactInfo[1] = cursor.getString(1);
        }
        cursor.close();
        return contactInfo;
    }

    public String getContactTitle(Context context, String address) {
        ContactInfo contactInfo=Me.getMe().getContactDAO().getContactInfoByAddress(context, address);
        return contactInfo.getFullInfo(context);
    }

    public PhoneNumber getPhoneNumber(Context context, String address) {
        if(address==null)
            return null;
        return phoneNumberDAO.get(context, address);
    }

    public byte[] getSharedKey(Context context, String address, long time) {
        PhoneNumber phoneNumber=phoneNumberDAO.get(context, address);
        if(phoneNumber!=null)
            return phoneNumber.getSharedKey(time);
        return null;
    }

    public PhoneNumber save(Context context, PhoneNumber phoneNumber) {
        return phoneNumberDAO.save(context, phoneNumber);
    }

}
