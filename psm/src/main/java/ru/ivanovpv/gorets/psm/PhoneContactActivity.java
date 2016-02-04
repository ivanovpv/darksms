package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.androidannotations.annotations.*;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import ru.ivanovpv.gorets.psm.cipher.FingerPrint;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.persistent.Contact;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.protocol.Protocol;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 496 $
 *   $LastChangedDate: 2014-02-05 13:42:06 +0400 (Ср, 05 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/PhoneContactActivity.java $
 */

@EActivity(R.layout.contact_layout)
public class PhoneContactActivity extends SherlockFragmentActivity implements AdapterView.OnItemClickListener {
    private final static String TAG = PhoneContactActivity.class.getName();
    private final static int EDIT_CONTACT_REQUEST = 0;
    private PhoneListAdapter listAdapter;

    @ViewById
    TextView contactName;
    @ViewById
    ListView phonesList;

    @App
    Me me;

    private DisplayImageOptions options = new DisplayImageOptions.Builder()
            .showImageOnFail(R.drawable.contact)
            .showImageForEmptyUri(R.drawable.contact)
            .resetViewBeforeLoading(true)
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .displayer(new RoundedBitmapDisplayer(20))
            .build();


    Contact contact;
    String contactKey;

    @AfterViews
    public void init() {
        Bundle bundle = this.getIntent().getExtras();

        phonesList.setOnItemClickListener(this);
        if (bundle != null) {
            contactKey = bundle.getString(ContactsContract.Contacts.LOOKUP_KEY);
            if (Me.DEBUG)
                Log.i(TAG, "Contact=" + contactKey);
            update();
        }
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void contactEditAction(View view) {
        Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/" + contact.getId()));
        this.startActivityForResult(intent, EDIT_CONTACT_REQUEST);
    }

    private void update() {
        contact = me.getContactDAO().get(this, contactKey);
        if(contact==null) {
            this.finish();
            return;
        }
        contactName.setText(contact.getContactName());
        ImageView imageView = (ImageView) findViewById(R.id.contactPicture);
        ImageLoader.getInstance().displayImage(contact.getPhotoUri(), imageView, options);
        listAdapter = new PhoneListAdapter(this, contact);
        phonesList.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
        phonesList.invalidate();
    }

    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        if (reqCode == EDIT_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            this.update();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, KeysInfoActivity.class);
        intent.putExtra(Constants.EXTRA_CONTACT, contact);
        PhoneNumber phoneNumber = (PhoneNumber) listAdapter.getItem(position);
        intent.putExtra(Constants.EXTRA_PHONE_NUMBER, phoneNumber);
        this.startActivity(intent);
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Contact id=" + contact.getId() + ", Phone #=" + phoneNumber.getRawAddress());
            Hashtable<Long, FingerPrint> fingers = phoneNumber.getKeysFingerPrints();
            Log.i(TAG, "Keys attached=" + fingers.size());
            for (long time : fingers.keySet()) {
                Log.i(TAG, "Date=" + new Date(time).toString() + ", fingerprint=" + fingers.get(time));
            }
        }
    }
}

class PhoneListAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
    private static final String TAG = PhoneListAdapter.class.getName();
    private final Context context;
    private ArrayList<PhoneNumber> values;
    private final Contact contact;

    public PhoneListAdapter(Context context, Contact contact) {
        this.context = context;
        this.contact = contact;
        this.values = contact.getPhoneNumbers();
        if (BuildConfig.DEBUG) {
            for (PhoneNumber phoneNumber : values) {
                Log.i(TAG, "Phone #" + phoneNumber.getRawAddress());
            }
        }
    }

    public void update() {
        this.values = contact.getPhoneNumbers();
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int position) {
        return values.get(position);
    }

    @Override
    public long getItemId(int position) {
        return ((Object) (values.get(position))).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View viewGroup = inflater.inflate(R.layout.contact_phone_layout, parent, false);
        viewGroup.setScrollContainer(true);
        final PhoneNumber phoneNumber = values.get(position);
        TextView textView = (TextView) viewGroup.findViewById(R.id.phoneType);
        textView.setText(phoneNumber.getType());
        textView = (TextView) viewGroup.findViewById(R.id.address);
        textView.setText(phoneNumber.getRawAddress());

        ImageButton call = (ImageButton) viewGroup.findViewById(R.id.callButton);
        call.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    final Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber.getRawAddress()));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "Error parsing phone #", e);
                }
            }
        });

        ImageButton sms = (ImageButton) viewGroup.findViewById(R.id.messageButton);
        sms.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String conversationId = Me.getMe().getMessageDAO().getConversationIdByPhoneNumber(context, phoneNumber.getRawAddress());
                final Intent intent = new Intent(context, ConversationActivity_.class);
                intent.putExtra(MessageDAO.CONVERSATION_ID, conversationId);
                ArrayList<String> addresses=new ArrayList<String>();
                addresses.add(phoneNumber.getRawAddress());
                intent.putExtra(MessageDAO.ADDRESS, addresses);
                context.startActivity(intent);
            }
        });

        if (phoneNumber.getRawAddress().length() != 0) {
            ImageButton invite = (ImageButton) viewGroup.findViewById(R.id.invite);
            final CheckBox alwaysProtect = (CheckBox) viewGroup.findViewById(R.id.alwaysProtect);
            if (!phoneNumber.isPresentKey()) {     // is there any public key for this number?
                alwaysProtect.setVisibility(View.GONE);
                invite.setOnClickListener(new View.OnClickListener() {
                    public void onClick(final View view) {
                        // check flag from settings, if flag - "never show" true - send sms without notifications
                        if (!Me.getMe().psmSettings.inviteWarning().get()) {
                            new AlertDialog.Builder(view.getContext())
                                    .setTitle(view.getContext().getString(R.string.inviteWarningTitle))
                                    .setMessage(view.getContext().getString(R.string.inviteWarning1) + "\n" + view.getContext().getString(R.string.inviteWarning2))
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            Protocol.sendInvitation(view.getContext(), phoneNumber);
                                        }
                                    })
                                    .setNegativeButton(R.string.no, null).show();
                        } else {
                            Protocol.sendInvitation(context, phoneNumber);
                        }
                    }
                });
            } else {
                invite.setVisibility(View.GONE);
                alwaysProtect.setChecked(phoneNumber.isAlwaysProtect());
                alwaysProtect.setOnCheckedChangeListener(this);
                alwaysProtect.setTag(phoneNumber);
            }
        }
        return viewGroup;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return (values.get(position) != null) ? true : false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        PhoneNumber phoneNumber = null;
        if (buttonView.getTag() != null && buttonView.getTag() instanceof PhoneNumber) {
            phoneNumber = (PhoneNumber) buttonView.getTag();
        }
        if (phoneNumber != null) {
            phoneNumber.setAlwaysProtect(isChecked);
            if (phoneNumber.isSaveable() && isChecked)
                Me.getMe().getContactDAO().save(context, phoneNumber); //
            if (!isChecked)
                Me.getMe().getContactDAO().save(context, phoneNumber); //just update
        }
    }
}