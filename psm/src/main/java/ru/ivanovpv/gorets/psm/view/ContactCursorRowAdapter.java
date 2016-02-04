package ru.ivanovpv.gorets.psm.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.PhoneContactActivity_;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.persistent.Contact;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 490 $
 *   $LastChangedDate: 2014-01-29 18:26:27 +0400 (Ср, 29 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/ContactCursorRowAdapter.java $
 */

public class ContactCursorRowAdapter extends SimpleCursorAdapter implements View.OnClickListener {
    private static final String TAG = ContactCursorRowAdapter.class.getName();
    private Activity activity;
    private int layout;
    //private final Cursor cursor;
    private int contactNameIndex;
    private int idIndex;
    private int keyIndex;
    private final LayoutInflater layoutInflater;
    private String match;

    public boolean isScrolling = false;

    private DisplayImageOptions options;

    public ContactCursorRowAdapter (Activity activity, int layout, Cursor cursor, String match) {
        super(activity, layout, cursor, new String[] { ContactsContract.Contacts.DISPLAY_NAME}, new int[] { R.id.contactName }, 0);
        this.activity = activity;
        this.layout = layout;
        this.contactNameIndex = 1;//cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        this.idIndex = 0;//cursor.getColumnIndex(ContactsContract.Contacts._ID);
        this.keyIndex=3;
        this.layoutInflater = LayoutInflater.from(activity);
        this.match = match;

        options = new DisplayImageOptions.Builder()
                .showStubImage(R.drawable.contact)
                //.showImageOnLoading(R.drawable.contact_unknown)
                .showImageForEmptyUri(R.drawable.contact)
                .showImageOnFail(R.drawable.contact)
//                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();

    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        if(cursor!=null) {
            this.contactNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            this.idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        }
        return super.swapCursor(cursor);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        if(cursor!=null) {
            this.contactNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
            this.idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
        }
        super.changeCursor(cursor);
    }

    public void setMatch(final String match) {
        this.match = match;
    }

    @Override
    public View newView(Context context, Cursor c, ViewGroup parent) {
        View view = layoutInflater.inflate(layout, null);
        ContactViewHolder contactViewHolder= new ContactViewHolder();
            contactViewHolder.contactName = (TextView) view.findViewById(R.id.contactName);
            contactViewHolder.picture = (ImageView) view.findViewById(R.id.contactPicture);
        view.setTag(contactViewHolder);
        view.setOnClickListener(this);
        activity.registerForContextMenu(view);
        return view;
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (!isScrolling) {
            ContactViewHolder contactViewHolder= (ContactViewHolder) view.getTag();
            String contactId = this.getCursor().getString(idIndex);
            String key=this.getCursor().getString(keyIndex);
            contactViewHolder.id = contactId;
            contactViewHolder.key=key;

            String name = this.getCursor().getString(contactNameIndex);
            contactViewHolder.contactName.setText(Me.highlightSelectedText(name, match), TextView.BufferType.SPANNABLE);

            if(TextUtils.isEmpty(contactId))
                contactViewHolder.picture.setImageResource(R.drawable.contact_unknown);
            //Contact contact=Me.getMe().getContactDAO().get(context, key);
            String thumbnailUri = Contact.getThumbnailUri(contactId).toString();
            ImageLoader.getInstance().displayImage(thumbnailUri, contactViewHolder.picture, options);
        }
    }

    public void onClick(View view) {
        ContactViewHolder contactViewHolder=(ContactViewHolder) view.getTag();
        Intent intent=new Intent(activity, PhoneContactActivity_.class);
            intent.putExtra(ContactsContract.Contacts.LOOKUP_KEY, contactViewHolder.key);
        activity.startActivity(intent);
    }
}
