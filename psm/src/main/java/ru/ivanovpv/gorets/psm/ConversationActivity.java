package ru.ivanovpv.gorets.psm;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import ru.ivanovpv.gorets.psm.db.ContactDAO;
import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.feature.HiddenConversationCursorRowAdapter;
import ru.ivanovpv.gorets.psm.persistent.*;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.service.SmsSendIntentService;
import ru.ivanovpv.gorets.psm.utils.MessageUtils;
import ru.ivanovpv.gorets.psm.view.ConversationViewHolder;
import ru.ivanovpv.gorets.psm.view.InviteConversationButton;
import ru.ivanovpv.gorets.psm.view.NewConversationCursorRowAdapter;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 497 $
 *   $LastChangedDate: 2014-02-15 22:12:10 +0400 (Сб, 15 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/ConversationActivity.java $
 */

@EActivity(R.layout.conversation_layout)
@OptionsMenu(R.menu.conversation_menu)
public class ConversationActivity extends SherlockFragmentActivity implements
        AdapterView.OnItemClickListener, Eula.OnEulaAction, LoaderManager.LoaderCallbacks<Cursor> {
    static final int NEW_CONTACT_REQUEST = 0;

    public static final int VISIBLE_MESSAGES_LOADER = 0;
    public static final int HIDDEN_MESSAGES_LOADER = 1;

    private static String TAG = ConversationActivity.class.getName();

    private static final int MENU_CONTEXT_OPEN = Menu.FIRST + 0;
    private static final int MENU_CONTEXT_PROTECT = Menu.FIRST + 1;
    private static final int MENU_CONTEXT_UNPROTECT = Menu.FIRST + 2;
    private static final int MENU_CONTEXT_FORWARD = Menu.FIRST + 3;
    private static final int MENU_CONTEXT_COPY = Menu.FIRST + 4;
    private static final int MENU_CONTEXT_DELETE = Menu.FIRST + 5;

    private String conversationId;
    private Message currentMessage;
    private ArrayList<String> addresses=new ArrayList<String>();
    private String contactId;
    private String contactKey;
    private Contact contact;
    private PhoneNumber phoneNumber;
    boolean isSecret = false;


    @ViewById
    ListView conversationList;

    private RefreshConversationReceiver refreshConversationReceiver;
    //private IntentFilter filter = ;

    @ViewById
    EditText editMessage;
    @ViewById
    ImageButton sendMessage;
    @ViewById
    InviteConversationButton inviteConversationButton;
    @ViewById
    View buttonsLayout;
    @Pref
    PsmSettings_ settings;
    @App
    Me me;

    private NewConversationCursorRowAdapter newConversationCursorRowAdapter;
    private HiddenConversationCursorRowAdapter hiddenConversationCursorRowAdapter;

    @AfterViews
    public void init() {
        newConversationCursorRowAdapter=new NewConversationCursorRowAdapter(this, null);
        hiddenConversationCursorRowAdapter=new HiddenConversationCursorRowAdapter(this, null);
        Eula eula = new Eula(this);
        if (!eula.isAccepted()) {
            eula.showAndAsk(this, this);
            return;
        }
        if(Build.VERSION.SDK_INT >=16) {
            LayoutTransition layoutTransition = new LayoutTransition();
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
            conversationList.setLayoutTransition(layoutTransition);
        }

        conversationList.setOnItemClickListener(this);

        registerForContextMenu(conversationList);
        conversationList.setOnCreateContextMenuListener(this);

        final ContactDAO contactDAO = me.getContactDAO();

        this.processIntentData(this.getIntent());
        ViewGroup sendLayout = (ViewGroup) findViewById(R.id.sendLayout);
        if (addresses.size() > 1  || (addresses.size() > 0 && !PhoneNumber.isPhoneNumber(addresses.get(0)))) {
            sendLayout.setVisibility(View.GONE);
            buttonsLayout.setVisibility(View.GONE);
            phoneNumber=null;
        }
        getSupportLoaderManager().initLoader(VISIBLE_MESSAGES_LOADER, null, this);

        //setting default phonenumber
        if(addresses.size()==1) {
            phoneNumber = contactDAO.getPhoneNumber(this, addresses.get(0));
            if (phoneNumber == null)
                phoneNumber = new PhoneNumber(addresses.get(0));
            ContactInfo contactInfo = contactDAO.getContactInfoByAddress(this, addresses.get(0));
            contactId = contactInfo.id;
            contactKey = contactInfo.key;
            if(contactId!=null)
                contact = contactDAO.get(this, contactInfo.key);
        }

        //setting appropriate title showing contact name or it's address
        StringBuilder sb=new StringBuilder();
        for(int i=0; i < addresses.size(); i++) {
            ContactInfo contactInfo = contactDAO.getContactInfoByAddress(this, addresses.get(0));
            if (contactInfo.id != null) {
                sb.append(contactInfo.getFullInfo(this));
            } else {
                sb.append(addresses.get(i));
            }
            if(i < (addresses.size()-1))
                sb.append(", ");
        }
        setTitle(sb.toString());

        if (!me.isWriteMessagePermission()) {
            sendMessage.setEnabled(false);
            editMessage.setHint(getString(R.string.cantSendMessage));
            inviteConversationButton.setEnabled(false);
        }
        refreshConversationReceiver = new RefreshConversationReceiver(this, phoneNumber);
        LocalBroadcastManager.getInstance(this).registerReceiver(refreshConversationReceiver,
                new IntentFilter(Constants.ACTION_REFRESH_CONVERSATION));

        //registerReceiver(refreshConversationReceiver, filter);
        updateConversationCursored(phoneNumber, true);
        if(Build.VERSION.SDK_INT >=11) {
            android.app.ActionBar actionBar = getActionBar();
            if(actionBar!=null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
        conversationList.setStackFromBottom(true);
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    public void callAction(View view) {
        if (view.getId() == R.id.callButton) {
            final Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + addresses.get(0)));
            this.startActivity(intent);
        }
    }

    public void hiddenConversationAction(View view) {
        Purse purse = Me.getMe().getMessagePurseDAO().get(this);

        /*if (!purse.isHiddenConversation()) { //secret conference not purchased, yet - so start buying it
            Intent intent = new Intent(this, BuySKUActivity.class);
            intent.putExtra("sku", BillingUtilsService.SKU_HIDDEN_CONVERSATION);
            startActivityForResult(intent, Math.abs(BillingUtilsService.SKU_HIDDEN_CONVERSATION.hashCode()));
        } else*/
        {
            if (!isSecret) {
                isSecret = true;
                getSupportLoaderManager().restartLoader(HIDDEN_MESSAGES_LOADER, null, this);

            } else {
                isSecret = false;
                getSupportLoaderManager().restartLoader(VISIBLE_MESSAGES_LOADER, null, this);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Me.DEBUG)
            Log.i(TAG, "onNewIntent!");
        this.onNewIntentThis(intent);
    }

    private void onNewIntentThis(Intent intent) {
        final ContactDAO contactDAO = me.getContactDAO();
        this.processIntentData(intent);

        //setting default phonenumber
        if(addresses.size()==1) {
            phoneNumber = contactDAO.getPhoneNumber(this, addresses.get(0));
            if (phoneNumber == null)
                phoneNumber = new PhoneNumber(addresses.get(0));
            ContactInfo contactInfo = contactDAO.getContactInfoByAddress(this, addresses.get(0));
            contactId = contactInfo.id;
            contactKey = contactInfo.key;
            if(contactId!=null)
                contact = contactDAO.get(this, contactInfo.key);
        }

        //setting appropriate title showing contact name or it's address
        StringBuilder sb=new StringBuilder();
        for(int i=0; i < addresses.size(); i++) {
            ContactInfo contactInfo = contactDAO.getContactInfoByAddress(this, addresses.get(i));
            if (contactInfo.id != null) {
                sb.append(contactInfo.getFullInfo(this));
            } else {
                sb.append(addresses.get(i));
            }
            if(i < (addresses.size()-1))
                sb.append(", ");
        }
        setTitle(sb.toString());
        this.updateConversationCursored(phoneNumber, true);
    }

    public void updateConversationCursored(PhoneNumber phoneNumber, boolean reset) {
        this.phoneNumber = phoneNumber;
        if (newConversationCursorRowAdapter == null || reset) {
            newConversationCursorRowAdapter = new NewConversationCursorRowAdapter(this, null);

            conversationList.setAdapter(newConversationCursorRowAdapter);
        } else
            newConversationCursorRowAdapter.notifyDataSetChanged();
    /*    conversationList.setStackFromBottom(true);
        conversationList.post(new Runnable() {
            public void run() {
                conversationList.setSelection(newConversationCursorRowAdapter.getCount() - 1);
            }
        });*/
        inviteConversationButton.init(this, phoneNumber);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Me.DEBUG)
            Log.i(TAG, "Intercepting activity result");
        if (requestCode == NEW_CONTACT_REQUEST) {
            if (resultCode == RESULT_OK) {
                this.updateConversationCursored(phoneNumber, false);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (refreshConversationReceiver != null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshConversationReceiver);
    }

    @Override
    public void onBackPressed() {
        if (Me.DEBUG)
            Log.i(TAG, "onBack!");
        String draftText = editMessage.getText().toString().trim();
        editMessage.setText("");
        if (draftText != null && draftText.length() > 0 && me.isWriteMessagePermission() && (addresses!=null && addresses.size()>0)) //saving text as draft
        {
            Message message = Message.createSendMessage(addresses.get(0), draftText, null);
            message.setSentStatus(Message.STATUS_NONE);
            message.setDeliveredStatus(Message.STATUS_NONE);
            message.setType(Message.MESSAGE_BOX_DRAFT);
            me.getMessageDAO().save(this, message);
        }
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        ActivityManager.RunningTaskInfo rti = list.get(0);
        if (rti != null) {
            ComponentName cn = rti.baseActivity;
            if (cn.getShortClassName().contains("PSMActivity")) { //if activity in stack go back
                super.onBackPressed();
                return;
            }
        }
        //if no in stack then create new one
        Intent intent = new Intent(this, PSMActivity_.class);
        intent.putExtra(Constants.EXTRA_PAGE, PSMActivity.PAGE_MESSAGES); //show messages page 1st
        this.startActivity(intent);
        this.finish();
        return;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle bundle) {
        super.onPrepareDialog(id, dialog);
        if (dialog instanceof ConfirmInvitationDialog) {
            Message message = (Message) bundle.getSerializable(Constants.EXTRA_MESSAGE);
            ((ConfirmInvitationDialog) dialog).onPrepare(this, message);
        }
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle bundle) {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case R.layout.confirm_invitation_dialog:
                Message message = (Message) bundle.getSerializable(Constants.EXTRA_MESSAGE);
                dialog = new ConfirmInvitationDialog(this, message);
                return dialog;
            case R.layout.action_item:
                CharSequence[] itemsListFull = {this.getString(R.string.call), this.getString(R.string.contact), this.getString(R.string.invite)};
                CharSequence[] itemsListShort = {this.getString(R.string.call)};
                CharSequence[] itemsList;
                if (phoneNumber != null)
                    itemsList = itemsListFull;
                else
                    itemsList = itemsListShort;
                builder.setItems(itemsList, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        switch (i) {
                            case 0:
                                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + addresses.get(0)));
                                startActivity(intent);
                                break;
                            case 1:
                                if (contactId == null)
                                    break;
                                intent = new Intent(ConversationActivity.this, PhoneContactActivity_.class);
                                intent.putExtra(ContactsContract.Contacts.LOOKUP_KEY, contactKey);
                                startActivity(intent);
                                break;
                            case 2:
                                if (phoneNumber != null && phoneNumber.isPresentKey()) {
                                    new AlertDialog.Builder(ConversationActivity.this)
                                            .setTitle(ConversationActivity.this.getString(R.string.alreadyInvited))
                                            .setMessage(ConversationActivity.this.getString(R.string.inviteAgain))
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    ConversationActivity.this.performInviteAction();
                                                }
                                            })
                                            .setNegativeButton(R.string.no, null).show();
                                }
                                break;
                        }
                    }
                });
                return builder.create();
        }
        return dialog;
    }

    private void performInviteAction() {
        if (!settings.inviteWarning().get()) {
            new AlertDialog.Builder(this)
                    .setTitle(this.getString(R.string.inviteWarningTitle))
                    .setMessage(this.getString(R.string.inviteWarning1) + "\n" + this.getString(R.string.inviteWarning2))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Message message = Protocol.sendInvitation(ConversationActivity.this, phoneNumber);
                            ConversationActivity.this.setConversationId(message.getConversationId());
                            ConversationActivity.this.updateConversationCursored(phoneNumber, false);
                        }
                    })
                    .setNegativeButton(R.string.no, null).show();
        } else {
            Message message = Protocol.sendInvitation(this, phoneNumber);
            this.setConversationId(message.getConversationId());
            //conversation id may be changed if it is new conversation over empty ("0" based)
            this.updateConversationCursored(this.phoneNumber, false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        if (contactId == null)
            menu.removeItem(R.id.viewContactMenuId);
            //menu.add(Menu.NONE, MENU_OPTIONS_CONTACT, Menu.NONE, this.getString(R.string.viewContact)).setIcon(R.drawable.contact);
        if (addresses.size() > 1 || (addresses.size() > 0 && !PhoneNumber.isPhoneNumber(addresses.get(0)))) {
            menu.removeItem(R.id.callMenuId);
            menu.removeItem(R.id.newContactMenuId);
            menu.removeItem(R.id.inviteMenuId);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                return true;
            case R.id.callMenuId:
                intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + addresses.get(0)));
                startActivity(intent);
                return true;
            case R.id.viewContactMenuId:
                intent = new Intent(this, PhoneContactActivity_.class);
                intent.putExtra(ContactsContract.Contacts.LOOKUP_KEY, contactKey);
                startActivity(intent);
                return true;
            case R.id.newContactMenuId:
                intent = new Intent(Intent.ACTION_INSERT);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, addresses.get(0));
                intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                this.startActivityForResult(intent, NEW_CONTACT_REQUEST);
                return true;
            case R.id.inviteMenuId:
                if (phoneNumber != null && phoneNumber.isPresentKey()) {
                    new AlertDialog.Builder(ConversationActivity.this)
                            .setTitle(ConversationActivity.this.getString(R.string.alreadyInvited))
                            .setMessage(ConversationActivity.this.getString(R.string.inviteAgain))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ConversationActivity.this.performInviteAction();
                                }
                            })
                            .setNegativeButton(R.string.no, null).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) newConversationCursorRowAdapter.getItem(info.position);
        if(MessageDAO.isMms(cursor))
            currentMessage=me.getMessageDAO().getMmsMessage(this, cursor);
        else
            currentMessage=me.getMessageDAO().getSmsMessage(this, cursor);
        //info.id contains table's rowid
//        ConversationViewHolder holder = (ConversationViewHolder) view.getTag(R.layout.cloudy_mms_layout);
        //currentMessage = me.getMessageDAO().getMessageByRowId(this, info.id, holder.isSms);
        menu.add(Menu.NONE, MENU_CONTEXT_OPEN, Menu.NONE, R.string.open);
        if (!currentMessage.isProtected() && !currentMessage.isCiphered() && currentMessage.isSms())
            menu.add(Menu.NONE, MENU_CONTEXT_PROTECT, Menu.NONE, R.string.protect);
        if (currentMessage.isProtected())
            menu.add(Menu.NONE, MENU_CONTEXT_UNPROTECT, Menu.NONE, R.string.unprotect);
        menu.add(Menu.NONE, MENU_CONTEXT_COPY, Menu.NONE, R.string.copy);
        if(currentMessage.isSms())
            menu.add(Menu.NONE, MENU_CONTEXT_FORWARD, Menu.NONE, R.string.forward);
        menu.add(Menu.NONE, MENU_CONTEXT_DELETE, Menu.NONE, R.string.delete);

    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        Intent intent;
        Message message;

        switch (item.getItemId()) {
            case MENU_CONTEXT_OPEN:
                intent = new Intent(this, MessageDetailsActivity_.class);
                intent.putExtra(Constants.EXTRA_MESSAGE_ID, new Long(currentMessage.getId()));
                intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, currentMessage.isSms());
                startActivity(intent);
                break;
            case MENU_CONTEXT_PROTECT:
                message = me.getMessageDAO().get(this, currentMessage.getId());
                if (message != null)
                    message.protect(this);
                break;
            case MENU_CONTEXT_UNPROTECT:
                Purse purse = Me.getMe().getMessagePurseDAO().get(this);
                if (me.getHashDAO().get().isEnabled() && !purse.isComfortPINEnabled()) {
                    final CheckPINDialog checkPINDialog = new CheckPINDialog(this, CheckPINDialog.MODE_DOESNT_MATTER);
                    checkPINDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            return;
                        }
                    });
                    checkPINDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (checkPINDialog.isCanceled())
                                return;
                            Message msg = me.getMessageDAO().get(ConversationActivity.this, currentMessage.getId());
                            if (msg != null)
                                msg.unprotect(ConversationActivity.this);
                        }
                    });
                    checkPINDialog.show();
                    break;
                } else {
                    message = me.getMessageDAO().get(this, currentMessage.getId());
                    if (message != null)
                        message.unprotect(this);
                }
                break;
            case MENU_CONTEXT_COPY:
                MessageUtils.copyMessage(me.getMessageDAO().get(this, currentMessage.getId()), this);
                break;
            case MENU_CONTEXT_FORWARD:
                final Message msg = me.getMessageDAO().get(this, currentMessage.getId());
                final Intent forwardIntent = new Intent(ConversationActivity.this, SMSActivity_.class);
                if (msg.isCiphered() || msg.isProtected()) {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle(R.string.forward)
                            .setMessage(R.string.forwardWarning)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Purse purse = Me.getMe().getMessagePurseDAO().get(ConversationActivity.this);
                                    if (me.getHashDAO().get().isEnabled() && !purse.isComfortPINEnabled()) {
                                        final CheckPINDialog checkPINDialog = new CheckPINDialog(ConversationActivity.this, CheckPINDialog.MODE_DOESNT_MATTER);
                                        checkPINDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                return;
                                            }
                                        });
                                        checkPINDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                            @Override
                                            public void onDismiss(DialogInterface dialog) {
                                                if (checkPINDialog.isCanceled())
                                                    return;
                                                if (msg.isProtected())
                                                    forwardIntent.putExtra("sms_body", msg.getUnprotected(ConversationActivity.this));
                                                else
                                                    forwardIntent.putExtra("sms_body", msg.getDecrypted(ConversationActivity.this));
                                                startActivity(forwardIntent);
                                                return;
                                            }
                                        });
                                        checkPINDialog.show();
                                        return;
                                    } else {
                                        if (msg.isProtected())
                                            forwardIntent.putExtra("sms_body", msg.getUnprotected(ConversationActivity.this));
                                        else
                                            forwardIntent.putExtra("sms_body", msg.getDecrypted(ConversationActivity.this));
                                    }
                                    startActivity(forwardIntent);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .setNeutralButton(R.string.sendAsIs, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(ConversationActivity.this, SMSActivity_.class);
                                    intent.putExtra("sms_body", msg.getBody());
                                    startActivity(intent);
                                }
                            });
                    builder.create().show();
                }
                else {
                    forwardIntent.putExtra("sms_body", msg.getBody());
                    startActivity(forwardIntent);
                }
                break;
            case MENU_CONTEXT_DELETE:
                me.getMessageDAO().delete(this, currentMessage.getId());
                break;
            default:
                return false;
        }
        return true;
    }

    public void sendMessageAction(View view) {
        Message message;
        Me me = (Me) this.getApplication();
        final String text = editMessage.getText().toString();
        if (text.length() > 0) {
            message = Message.createSendMessage(addresses.get(0), text, null);
            if (inviteConversationButton.isEncryptState()) {
                if (!me.getMessagePurseDAO().isAny(this)) {
                    new MessageBox(this, this.getString(R.string.balanceZero));
                    return;
                }
                SmsSendIntentService.startActionSend(this, message, phoneNumber, true);
            } else
                SmsSendIntentService.startActionSend(this, message, phoneNumber, false);
            this.conversationId = message.getConversationId(); //conversation id may be changed if it is new conversation over empty ("0" based)
            this.updateConversationCursored(phoneNumber, false);
            //Log.i(TAG, message.toString());
            editMessage.setText("");
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

    }

    public void setConversationId(final String conversationId) {
        this.conversationId = conversationId;
    }

    private void processIntentData(Intent intent) {
        String singleAddress;
        if (intent == null)
            return;
        if (Intent.ACTION_SENDTO.equals(intent.getAction()) || Intent.ACTION_VIEW.equals(intent.getAction())) {

            //in the data i'll find the number of the destination
            singleAddress = intent.getDataString();
            singleAddress = URLDecoder.decode(singleAddress);
            //clear the string
            singleAddress = singleAddress.replace("-", "").replace("smsto:", "").replace("sms:", "");
            String body = intent.getStringExtra("sms_body");
            if (body != null)
                editMessage.setText(body);
            addresses.add(singleAddress);
        } else if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            //in the data i'll find the content of the message
            String message = intent.getStringExtra(Intent.EXTRA_TEXT);
            //clear the string
            editMessage.setText(message);
        } else {
            String body = intent.getStringExtra("sms_body");
            if (body != null)
                editMessage.setText(body);
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            conversationId = bundle.getString(MessageDAO.CONVERSATION_ID);
            addresses = bundle.getStringArrayList(MessageDAO.ADDRESS);
            if(addresses==null)
                addresses=new ArrayList<String>();
            String body = bundle.getString(MessageDAO.BODY);
            editMessage.setText(body);
        }
        if (conversationId != null) { //put draft message in editor
            Message message = Me.getMe().getMessageDAO().getDraftMessage(this, conversationId);
            if (message != null) {
                editMessage.setText(message.getBody());
                editMessage.setSelection(message.getBody().length());
                Me.getMe().getMessageDAO().deleteConversationMessage(this, message.getConversationId(), message.getId());
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if(Me.DEBUG)
            Log.i(TAG, "Item clicked id="+id+", position="+position);
        ConversationViewHolder holder=(ConversationViewHolder )view.getTag(R.layout.cloudy_mms_layout);
        Intent intent = new Intent(this, MessageDetailsActivity_.class);
        intent.putExtra(Constants.EXTRA_MESSAGE_ID, id);
        intent.putExtra(Constants.EXTRA_MESSAGE_TYPE, holder.isSms);
        startActivity(intent);
    }

    public void onEulaAgreedTo() {
        this.finish();
        this.startActivity(new Intent(this, ConversationActivity_.class));
    }

    public void onEulaRefuseTo() {
        this.finish();
        me.exit();
    }

    /*@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }
*/
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        switch (loaderId) {
            case VISIBLE_MESSAGES_LOADER:
                conversationList.setAdapter(newConversationCursorRowAdapter);
                Loader<Cursor> cursorLoader=Me.getMe().getMessageDAO().getConversationCursorLoader(this, conversationId);
                return cursorLoader;
            case HIDDEN_MESSAGES_LOADER:
                conversationList.setAdapter(hiddenConversationCursorRowAdapter);
                return Me.getMe().getMessageDAO().getConversationCursorLoader(this, conversationId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        switch(cursorLoader.getId()) {
            case VISIBLE_MESSAGES_LOADER:
                newConversationCursorRowAdapter.swapCursor(cursor);
                return;
            case HIDDEN_MESSAGES_LOADER:
                hiddenConversationCursorRowAdapter.swapCursor(cursor);
                return;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        switch(cursorLoader.getId()) {
            case VISIBLE_MESSAGES_LOADER:
                newConversationCursorRowAdapter.changeCursor(null);
                return;
            case HIDDEN_MESSAGES_LOADER:
                hiddenConversationCursorRowAdapter.changeCursor(null);
                return;
        }
    }

    //resend message by clicking on status icon
    public void resendMessage(View view) {
        Message message=(Message )view.getTag(R.id.sentStatusIcon);
        if(message==null)
            return;
        ConversationViewHolder holder=(ConversationViewHolder )view.getTag(R.id.cloudyMessageLayout);
        if(holder==null)
            return;
        holder.progressBar.setVisibility(View.VISIBLE);
        //Toast.makeText(this, "Resending message", Toast.LENGTH_SHORT).show();
        if (inviteConversationButton.isEncryptState()) {
            if (!me.getMessagePurseDAO().isAny(this)) {
                new MessageBox(this, this.getString(R.string.balanceZero));
                return;
            }
            SmsSendIntentService.startActionSend(this, message, phoneNumber, true);
        } else
            SmsSendIntentService.startActionSend(this, message, phoneNumber, false);
        this.conversationId = message.getConversationId(); //conversation id may be changed if it is new conversation over empty ("0" based)
        newConversationCursorRowAdapter.notifyDataSetChanged();
    }

    class RefreshConversationReceiver extends BroadcastReceiver {
        private final String TAG = RefreshConversationReceiver.class.getName();
        ConversationActivity activity;
        PhoneNumber phoneNumber;

        public RefreshConversationReceiver(ConversationActivity activity, PhoneNumber phoneNumber) {
            this.activity = activity;
            this.phoneNumber = phoneNumber;
        }

        public void onReceive(Context context, Intent intent) {
            if (Me.DEBUG)
                Log.i(TAG, "Refresh conversation receiver!");
            activity.updateConversationCursored(this.phoneNumber, false);
        }
    }
}

