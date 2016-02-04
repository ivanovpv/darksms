package ru.ivanovpv.gorets.psm.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import ru.ivanovpv.gorets.psm.ConversationActivity_;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Vector;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 470 $
 *   $LastChangedDate: 2014-01-14 06:40:37 +0400 (Вт, 14 янв 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/ConversationsCursorRowAdapter.java $
 */

public class ConversationsCursorRowAdapter extends SimpleCursorAdapter implements View.OnClickListener {
    private static final String TAG = ConversationsCursorRowAdapter.class.getName();

    private int layout;

    private int recipientIdsIndex;
    private int bodyIndex;
    private int dateIndex;
    private int conversationIdIndex;
    private int unreadIndex;
    private final SherlockFragmentActivity activity;
    private final LayoutInflater layoutInflater;
    private String match;
    private Me me;

    private DisplayImageOptions options;

    public ConversationsCursorRowAdapter(SherlockFragmentActivity activity, int layout, String match) {     // add param true for higthlite
        super(activity, layout, null,
                new String[] { MessageDAO.RECIPIENT_IDS, MessageDAO.SNIPPET, MessageDAO.DATE},
                new int[] { R.id.respondent, R.id.body, R.id.millis}, 0);
        this.activity =activity;
        me = Me.getMe();
        this.layout = layout;
        this.conversationIdIndex = 0; //cursor.getColumnIndex(MessageDAO._ID);
        this.recipientIdsIndex = 1; //cursor.getColumnIndex(MessageDAO.RECIPIENT_IDS);
        this.bodyIndex = 2; //cursor.getColumnIndex(MessageDAO.SNIPPET);
        this.dateIndex = 3; //cursor.getColumnIndex(MessageDAO.DATE);
        this.unreadIndex=4; //cursor.getColumnIndex(MessageDAO.UNPROCESSED_COUNT);
        this.match = match;
        this.notifyDataSetChanged();
        this.layoutInflater = LayoutInflater.from(activity);

        options = new DisplayImageOptions.Builder()
//                .showImageOnLoading(R.drawable.contact)
                .showImageForEmptyUri(R.drawable.contact)
                .showImageOnFail(R.drawable.contact)
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisc(true)
                .displayer(new RoundedBitmapDisplayer(20))
                .build();
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        checkCursorAfterSwapAndChange(cursor);
        return super.swapCursor(cursor);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        checkCursorAfterSwapAndChange(cursor);
        super.changeCursor(cursor);
    }

    private void checkCursorAfterSwapAndChange(Cursor cursor) {            // why?
        if(cursor!=null) {
            this.conversationIdIndex = cursor.getColumnIndex(MessageDAO._ID);
            this.recipientIdsIndex = cursor.getColumnIndex(MessageDAO.RECIPIENT_IDS);
            this.bodyIndex = cursor.getColumnIndex(MessageDAO.SNIPPET);
            this.dateIndex = cursor.getColumnIndex(MessageDAO.DATE);
            this.unreadIndex=cursor.getColumnIndex(MessageDAO.UNPROCESSED_COUNT);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = layoutInflater.inflate(layout, null);
        final MessageViewHolder viewHolder = new MessageViewHolder();
        viewHolder.isThreadView=true;
        viewHolder.respondent = (TextView)view.findViewById(R.id.respondent);
        viewHolder.body = (TextView)view.findViewById(R.id.body);
        viewHolder.date = (TextView)view.findViewById(R.id.millis);
        viewHolder.unreadMessages = (TextView) view.findViewById(R.id.unreadMessages);
        viewHolder.icon = (ImageView)view.findViewById(R.id.messageIcon);
        viewHolder.addresses=new ArrayList<String>();
        view.setTag(viewHolder);
        view.setOnClickListener(this);
        activity.registerForContextMenu(view);
        return view;
    }

    //MessageViewHolder viewHolder;
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        MessageViewHolder viewHolder=(MessageViewHolder )view.getTag();
        String body = cursor.getString(bodyIndex);
        body = checkBodyByProtocol(body);
        final long date = cursor.getLong(dateIndex);
        viewHolder.date.setText( DateFormat.getDateInstance().format(date));
        final String conversationId = cursor.getString(conversationIdIndex);
        viewHolder.conversationId = conversationId;
        final String recipientIds = cursor.getString(recipientIdsIndex);
        viewHolder.recipientIds = recipientIds;
        new AsyncUniversalMessageThreadInfoLoader(view, viewHolder, context, me, options).execute();

        final int unread = cursor.getInt(unreadIndex);
        if(body==null)
            body="";
        viewHolder.body.setText(body);
        if(unread > 0) {
            viewHolder.unreadMessages.setVisibility(View.VISIBLE);
            viewHolder.unreadMessages.setText(String.valueOf(unread));
        }
        else {
            viewHolder.unreadMessages.setVisibility(View.GONE);
            //viewHolder.unreadMessages.setText("1");
        }

    }

    private String checkBodyByProtocol(String body) {             // why?
        if(body==null)
            return "";
        if(Protocol.isScrambled(body))
            return activity.getString(R.string.protectedMessage);
        else if(Protocol.isCiphered(body))
            return activity.getString(R.string.cipherMessage);
        else if(Protocol.isInvitation(body))
            return activity.getString(R.string.invitationMessage);
        else if(Protocol.isAccept(body))
            return activity.getString(R.string.inviteAcceptMessage);
        return body;
    }

    public String getMatch() {
        return match;
    }

    public void setMatch(String match) {
        this.match = match;
    }

    public void onClick(View view) {
        final MessageViewHolder viewHolder=(MessageViewHolder ) view.getTag();
            if(Me.DEBUG) Log.i(TAG, "Selected: " + viewHolder.toString());
        final Intent intent = new Intent(mContext, ConversationActivity_.class);
        Vector<String> recipients = me.getMessageDAO().getCanonicalAddresses(activity, viewHolder.recipientIds);
        ArrayList<String> addresses=new ArrayList<>();
        for (int i = 0; i < recipients.size(); i++) {
            addresses.add(recipients.get(0));
        }
        intent.putExtra(MessageDAO.CONVERSATION_ID, viewHolder.conversationId);
        intent.putExtra(MessageDAO.ADDRESS, addresses); //array of addresses!
        this.activity.startActivity(intent);
    }

    /*private class AsyncContactId extends AsyncTask<Void, Void, String> {
        MessageViewHolder messageViewHolder;

        AsyncContactId(MessageViewHolder messageViewHolder) {
            this.messageViewHolder = messageViewHolder;
        }

        @Override
        protected String doInBackground(Void... params) {
            return getContactIdByNumber(messageViewHolder.address);
        }

        @Override
        protected void onPostExecute(String result) {
            final Uri uri;
            super.onPostExecute(result);
            if (!TextUtils.isDigitsOnly(result) || TextUtils.isEmpty(result))
                uri = null;
            else
                uri = Contact.getThumbnailUri(Long.parseLong(result));
            ImageLoader.getInstance().displayImage(uri == null? null:uri.toString(), messageViewHolder.icon, options);
        }

        public String getContactIdByNumber(String number) {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

            ContentResolver contentResolver = activity.getContentResolver();
            Cursor contactLookup = contentResolver.query(uri, new String[] {"*"}, null, null, null);
            String id = number;
            try {
                if (contactLookup != null && contactLookup.getCount() > 0) {
                    contactLookup.moveToNext();
                    id = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data._ID));
                }
            } finally {
                if (contactLookup != null) {
                    contactLookup.close();
                }
            }
            return id;
        }
    }

    private class AsyncContactName extends AsyncTask<Void, Void, String> {
        MessageViewHolder messageViewHolder;

        AsyncContactName(MessageViewHolder messageViewHolder) {
            this.messageViewHolder = messageViewHolder;
        }

        @Override
        protected String doInBackground(Void... params) {
            return getContactName(messageViewHolder.address);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            messageViewHolder.respondent.setText(result);
        }

        public String getContactName(final String phoneNumber) {
            String contactName=phoneNumber;
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            String[] projection = new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.LAST_TIME_CONTACTED};
            Cursor cursor = activity.getContentResolver().query(uri, projection, null, null, ContactDAO.LAST_CONTACT_DATE_DESC);
            if(cursor!=null && cursor.moveToFirst())
                contactName = cursor.getString(0);
            cursor.close();
            return contactName;
        }
    }*/
}
