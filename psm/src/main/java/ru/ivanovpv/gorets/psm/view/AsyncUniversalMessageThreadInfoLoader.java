package ru.ivanovpv.gorets.psm.view;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.Vector;

import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.persistent.Contact;

/**
 * Created by pivanov on 10.02.2015.
 */
public class AsyncUniversalMessageThreadInfoLoader extends AsyncTask<Void, Void, Vector<ContactInfo>> {
    MessageViewHolder messageViewHolder;
    Context context;
    Me me;
    DisplayImageOptions options;
    View view;

    AsyncUniversalMessageThreadInfoLoader(View view, MessageViewHolder messageViewHolder, Context context, Me me, DisplayImageOptions options) {
        this.messageViewHolder = messageViewHolder;
        this.context = context;
        this.me = me;
        this.options=options;
        this.view=view;
    }

    @Override
    protected Vector<ContactInfo> doInBackground(Void... params) {
//            getItem()
        synchronized (this) {
            Vector<ContactInfo> contactInfos;
            contactInfos = new Vector<ContactInfo>();
            if (messageViewHolder.recipientIds != null) { //invoked from threads
                Vector<String> recipients = me.getMessageDAO().getCanonicalAddresses(context, messageViewHolder.recipientIds);
                for (int i = 0; i < recipients.size(); i++) {
                    ContactInfo contactInfo = me.getContactDAO().getContactInfoByAddress(context, recipients.get(i));
                    contactInfos.add(contactInfo);
                }
            } else { //invoked from search
                for (int i = 0; i < messageViewHolder.addresses.size(); i++) {
                    ContactInfo contactInfo = me.getContactDAO().getContactInfoByAddress(context, messageViewHolder.addresses.get(i));
                    contactInfos.add(contactInfo);
                }
            }
            return contactInfos;
        }
    }

    @Override
    protected void onPostExecute(Vector<ContactInfo> contactInfos) {
        Uri uri;
        super.onPostExecute(contactInfos);
        messageViewHolder.addresses.clear();
        StringBuilder sb = new StringBuilder();
        if (contactInfos.size() > 1) {
            ImageLoader.getInstance().displayImage(null, messageViewHolder.icon, options);
            for (int i = 0; i < contactInfos.size(); i++) {
                sb.append(contactInfos.get(i).getShortInfo(context));
                messageViewHolder.addresses.add(contactInfos.get(i).phone);
                if (i < (contactInfos.size() - 1))
                    sb.append(", ");
            }
            messageViewHolder.respondent.setText(sb.toString());
        } else if(contactInfos.size()==1) {
            String id = contactInfos.get(0).id;
            if (TextUtils.isEmpty(id) || !TextUtils.isDigitsOnly(id))
                uri = null;
            else
                uri = Contact.getThumbnailUri(Long.parseLong(id));
            messageViewHolder.addresses.add(contactInfos.get(0).phone);
            ImageLoader.getInstance().displayImage(uri == null ? null : uri.toString(), messageViewHolder.icon, options);
            messageViewHolder.respondent.setText(contactInfos.get(0).getShortInfo(context));
        }
        else {
            messageViewHolder.respondent.setText("");
        }
        view.setTag(messageViewHolder);
    }
}
