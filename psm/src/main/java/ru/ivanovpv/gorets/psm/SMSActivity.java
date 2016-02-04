package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.*;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.service.SmsSendIntentService;
import ru.ivanovpv.gorets.psm.view.InviteConversationButton;

import java.net.URLDecoder;
import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 497 $
 *   $LastChangedDate: 2014-02-15 22:12:10 +0400 (Сб, 15 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/SMSActivity.java $
 */

@EActivity(R.layout.sms_layout)
public class SMSActivity extends SherlockFragmentActivity implements Eula.OnEulaAction, View.OnClickListener {
    static final String TAG = SMSActivity.class.getName();
    private static final int NUMBER_TAG=R.string.phone;

    public static final String[] PROJECTION = new String[] {
            ContactsContract.Contacts._ID,                 // 0
            ContactsContract.Contacts.LOOKUP_KEY,          // 1
//            ContactsContract.Contacts.HAS_PHONE_NUMBER,    // 2
            ContactsContract.Contacts.DISPLAY_NAME,        // 2
            ContactsContract.CommonDataKinds.Phone.NUMBER, // 3
            ContactsContract.CommonDataKinds.Phone.TYPE,   // 4
            ContactsContract.CommonDataKinds.Phone.LABEL   // 5
    };

    @ViewById
    public AutoCompleteTextView recipient; //single address entered by user
    @ViewById(R.id.smsBody)
    public EditText smsBody;
    @ViewById
    public InviteConversationButton inviteConversationButton;
    @ViewById
    public ViewGroup recipientsList; //recipients list entered by user into recipient field
    @ViewById
    public ImageButton sendSms;
    @App
    Me me;

    PhoneNumber phoneNumber; //number sent through Intent
    Message message;
    ContentResolver content;

    @AfterViews
    public void init() {
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Eula eula = new Eula(this);
        if(!eula.isAccepted()) {
            eula.showAndAsk(this, this);
            return;
        }

        inviteConversationButton.setOnClickListener(this);
        if(!me.isWriteMessagePermission()) {
            sendSms.setEnabled(false);
            smsBody.setHint(R.string.cantSendMessage);
            inviteConversationButton.setEnabled(false);
        }
        processIntentData(this.getIntent());
        this.setTitle(R.string.sendSMS);
//        recipient.setFocusable(true);
        if(phoneNumber!=null) {
            recipient.setText(phoneNumber.getRawAddress());
            inviteConversationButton.init(this, phoneNumber);
        }
        else
            inviteConversationButton.init(this);
        content = getContentResolver();
        Cursor cursor = content.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, null, null, null);
//        String[] columns=cursor.getColumnNames();
        ContactListAdapter adapter = new ContactListAdapter(this, cursor);
        recipient.setThreshold(0);
        recipient.setAdapter(adapter);
        recipient.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Cursor cursor = content.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PROJECTION, ContactsContract.Contacts._ID + "=" + id, null, null);
                if (cursor.moveToFirst()) {
                    Button button = new Button(SMSActivity.this);
                    String name = cursor.getString(2); //display name
                    String number = cursor.getString(3); //number
                    button.setText(name + " (" + number + ")");
                    PhoneNumber phoneNumber=Me.getMe().getContactDAO().getPhoneNumber(SMSActivity.this, number);
                    if(phoneNumber==null)
                        phoneNumber=new PhoneNumber(number, "", false);
                    button.setTag(NUMBER_TAG, phoneNumber);
                    recipientsList.addView(button, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    inviteConversationButton.add(phoneNumber);
                    button.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            recipientsList.removeView(view);
                            inviteConversationButton.init(SMSActivity.this, SMSActivity.this.getPhoneNumbers(), true);
                        }
                    });
                }
                recipient.setText("");
//                Toast.makeText(SMSActivity.this, id+" selected", Toast.LENGTH_LONG).show();
            }
        });

        if (message != null) {
            recipient.setText(message.getAddress());
            smsBody.setText(message.getBody());
        }
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *
     * @param intent
     * @return true if redirected to new Activity, otherwise false
     */

    private void processIntentData(Intent intent)
    {
        if (intent==null)
            return;
        String destinationNumber=null;
        String body=null;
        if (Intent.ACTION_SENDTO.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) {
            //in the data i'll find the number of the destination
            destinationNumber = intent.getDataString();
            destinationNumber = URLDecoder.decode(destinationNumber);
            //clear the string
            destinationNumber = destinationNumber.replace("-", "")
                    .replace("smsto:", "")
                    .replace("sms:", "");
            //and set fields
            recipient.setText(destinationNumber);
            body=intent.getStringExtra("sms_body");
            if(body!=null)
                smsBody.setText(body);
        }
        else if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            //in the data i'll find the content of the message
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            //clear the string
            smsBody.setText(message);
        }
        else {
            body=intent.getStringExtra("sms_body");
            if(body!=null)
                smsBody.setText(body);
        }
        if(destinationNumber!=null && destinationNumber.trim().length()>0) {
            String conversationId=me.getMessageDAO().getConversationIdByPhoneNumber(this, destinationNumber);
            final Intent newIntent=new Intent(this, ConversationActivity_.class);
            newIntent.putExtra(MessageDAO.CONVERSATION_ID, conversationId);
            ArrayList<String> addresses=new ArrayList<>();
            addresses.add(destinationNumber);
            newIntent.putExtra(MessageDAO.ADDRESS, addresses);
            newIntent.putExtra(MessageDAO.BODY, body);
            this.startActivity(newIntent);
            this.finish();
            return;
        }
        if(intent.getExtras()==null) {
            this.phoneNumber=null;
            return;
        }
        this.phoneNumber = (PhoneNumber) getIntent().getExtras().getSerializable(Constants.EXTRA_PHONE_NUMBER);
        String messageId = intent.getStringExtra(Constants.EXTRA_MESSAGE_ID);
        if(messageId!=null)
            message=me.getMessageDAO().get(this, messageId);
        else {
            if(phoneNumber!=null)
                message=Message.createSendMessage(phoneNumber.getRawAddress(), body, null);
            else
                message=Message.createSendMessage(destinationNumber, body, null);
        }
        return;
    }

    public void sendSmsAction(View v) {
        boolean encrypt;
        ArrayList<PhoneNumber> phoneNumbers=this.getPhoneNumbers();
        if(phoneNumbers.size()==0 || smsBody.getText().length()==0) {
            Toast.makeText(this, this.getString(R.string.emptyMessage), Toast.LENGTH_SHORT).show();
            return;
        }
        for(PhoneNumber phoneNumber:phoneNumbers) {
            if(inviteConversationButton.isEncryptState()) {
                if(!me.getMessagePurseDAO().isAny(this)) {
                    new MessageBox(this, this.getString(R.string.balanceZero));
                    return;
                }
                encrypt=true;
            } else
                encrypt=false;
            Message message=Message.createSendMessage(phoneNumber.getRawAddress(), smsBody.getText().toString(), null);
            SmsSendIntentService.startActionSend(this, message, phoneNumber, encrypt);
            //Toast.makeText(this, this.getString(R.string.messageTo) + phoneNumber.getRawAddress() + this.getString(R.string.messageSent), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

/*    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_OPTIONS_PROTECT, Menu.NONE, getString(R.string.protect)).setIcon(
                getResources().getDrawable(android.R.drawable.ic_lock_idle_lock));
        menu.add(Menu.NONE, MENU_OPTIONS_DELETE, Menu.NONE, getString(R.string.delete)).setIcon(
                getResources().getDrawable(android.R.drawable.ic_delete));
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_OPTIONS_PROTECT:
                scrambleMessage();
                return true;
            case MENU_OPTIONS_DELETE:
                return true;
        }
        return false;
    }*/

    private ArrayList<PhoneNumber> getPhoneNumbers() {
        ArrayList<PhoneNumber> phoneNumbers=new ArrayList<PhoneNumber>();
        if(phoneNumber!=null)
            phoneNumbers.add(phoneNumber);
        String address=recipient.getText().toString().trim();
        if(PhoneNumberUtils.isGlobalPhoneNumber(address)) {
            PhoneNumber phoneNumber=Me.getMe().getContactDAO().getPhoneNumber(SMSActivity.this, address);
            if(phoneNumber==null)
                phoneNumber=new PhoneNumber(address, "", true);
            phoneNumbers.add(phoneNumber);
        }
        int size=recipientsList.getChildCount();
        for(int i=0; i < size; i++) {
            View view=recipientsList.getChildAt(i);
            if(view.getTag(NUMBER_TAG) instanceof PhoneNumber)
                phoneNumbers.add((PhoneNumber )view.getTag(NUMBER_TAG));
        }
        return phoneNumbers;
    }

    public void onEulaAgreedTo() {
        this.finish();
        this.startActivity(new Intent(this, ConversationActivity_.class));
    }

    public void onEulaRefuseTo() {
        this.finish();
        me.exit();
    }

    @Override
    public void onClick(View v) {
        inviteConversationButton.init(this, this.getPhoneNumbers(), false);
        inviteConversationButton.onClick(v);
    }
}

class ContactListAdapter extends CursorAdapter implements Filterable {
    private static final String TAG=ContactListAdapter.class.getName();
    private ContentResolver mCR;
    private Context context;

    public ContactListAdapter(Context context, Cursor c) {
        super(context, c);
        this.context=context;
        mCR = context.getContentResolver();
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewGroup viewGroup=(ViewGroup )view;
        TextView textMain = (TextView) viewGroup.findViewById(R.id.textMain);
        TextView textAdditional = (TextView) viewGroup.findViewById(R.id.textAdditional);
        textMain.setText(this.getMainLine(cursor));
        textAdditional.setText(this.getAdditionalLine(cursor));
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
/*        final TextView view = (TextView) inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        view.setText(cursor.getString(1));*/
        ViewGroup viewGroup=(ViewGroup )inflater.inflate(R.layout.contacts_dropdown_item, parent, false);
        TextView textMain = (TextView) viewGroup.findViewById(R.id.textMain);
        TextView textAdditional = (TextView) viewGroup.findViewById(R.id.textAdditional);
        return viewGroup;
    }

    @Override
    public String convertToString(Cursor cursor) {
        return cursor.getString(1)+" ("+cursor.getString(1)+")";
        /*StringBuilder sb=new StringBuilder();
        sb.append(cursor.getString(1)).append(" (").append(cursor.getString(2)).append(")");
        return sb.toString();*/
    }

    private String getMainLine(Cursor cursor) {
        return cursor.getString(2); //display_name
    }

    private String getAdditionalLine(Cursor cursor) {
        StringBuilder sb=new StringBuilder();
        sb.append(this.getLabel(cursor));
        sb.append(": ");
        sb.append(cursor.getString(3)); //number
        return sb.toString();
    }

    private String getLabel(Cursor cursor) {
        int type=cursor.getInt(4); //type
        String customLabel=cursor.getString(5); //label
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), type, customLabel).toString();
    }

    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        if (getFilterQueryProvider() != null) {
            return getFilterQueryProvider().runQuery(constraint);
        }
        if(constraint==null || constraint.toString().trim().length()==0) {
            mCR.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    SMSActivity.PROJECTION,
                    null,
                    null,
                    ContactsContract.Contacts.DISPLAY_NAME);        }
        if(Me.DEBUG)
            Log.i(TAG, "Filtering = "+constraint);
        return mCR.query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI, constraint.toString()),
                SMSActivity.PROJECTION,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME);
    }

}