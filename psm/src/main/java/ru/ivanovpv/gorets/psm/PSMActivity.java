package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v4.view.ViewPager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;

import com.viewpagerindicator.TitlePageIndicator;
import org.androidannotations.annotations.*;
import org.androidannotations.annotations.res.StringRes;

import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.fragments.*;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Purse;
import ru.ivanovpv.gorets.psm.view.ContactViewHolder;
import ru.ivanovpv.gorets.psm.view.MessageViewHolder;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 499 $
 *   $LastChangedDate: 2014-03-24 23:30:05 +0400 (Пн, 24 мар 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/PSMActivity.java $
 */

@EActivity(R.layout.main)
@OptionsMenu(R.menu.main_menu)
public class PSMActivity extends SherlockFragmentActivity implements Eula.OnEulaAction {

    private final static String TAG = PSMActivity.class.getName();

    private final static int NEW_CONTACT_REQUEST = 0;
    public static final int PAGE_MESSAGES = 0;
    public static final int PAGE_CONTACTS = 1;
    public static final int LAST_PAGE = PAGE_CONTACTS;
    public static final int PAGES_COUNT = 2;

    private MessageViewHolder messageViewHolder;
    private ContactViewHolder contactViewHolder;
    private FragmentPageAdapter fragmentPageAdapter;

    @StringRes(R.string.context_menu_settings)
    String contextMenuSettings;
    @StringRes(R.string.psm_store)
    String psmStore;

    private int defaultPage = PAGE_MESSAGES;

    @ViewById
    public ViewPager viewpager;
    @ViewById
    public TitlePageIndicator indicator;

    private MenuItem searchMenuItem;
    @App
    Me me;

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent");
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            defaultPage = extras.getInt(Constants.EXTRA_PAGE, PAGE_MESSAGES);
            if (BuildConfig.DEBUG)
                Log.i(TAG, "Default page=" + defaultPage);
        }
        viewpager.setCurrentItem(defaultPage);
        viewpager.invalidate();
        indicator.invalidate();
    }

    @AfterViews
    public void init() {
        Purse purse = Me.getMe().getMessagePurseDAO().get(this);
        defaultPage = me.psmSettings.defaultPage().get();
        Hash.initApplicationContext();

//        FlurryAgent.onStartSession(this, "QSWB7VWH4839FSM877Q6");
        Eula eula = new Eula(this);
        if (!eula.isAccepted()) {
            eula.showAndAsk(this, this);
            return;
        }
/*        if (!Me.KITKAT_SUPPORT && android.os.Build.VERSION.SDK_INT >= 19) {//Build.VERSION_CODES.KITKAT
            final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.psm)
                    .setMessage(R.string.incompatible)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PSMActivity.this.finish();
                        }
                    });
            builder.create().show();
        }*/
        handleDefaultSMSApplication();
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            defaultPage = extras.getInt(Constants.EXTRA_PAGE, PAGE_MESSAGES);
        }

        viewpager.setScrollbarFadingEnabled(true);
        viewpager.setSoundEffectsEnabled(true);

        fragmentPageAdapter = new FragmentPageAdapter(getSupportFragmentManager(), this);

        viewpager.setAdapter(fragmentPageAdapter);
        viewpager.setCurrentItem(defaultPage);

        indicator.setViewPager(viewpager);
        indicator.invalidate();
        indicator.setCurrentItem(defaultPage);
//        indicator.setOnPageChangeListener(onPageChangeListener);

        me.rateMe(this);
    }

/*    private ViewPager.SimpleOnPageChangeListener onPageChangeListener = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            String query=null;
            super.onPageSelected(position);
            switch(position) {
                case PAGE_MESSAGES:
                    if(queries[PAGE_MESSAGES]==null || queries[PAGE_MESSAGES].length()==0) {
                        searchMenuItem.collapseActionView();
                        searchMenuItem.setTitle(null);
                    }
                    else {
                        searchMenuItem.expandActionView();
                        searchMenuItem.setTitle(query);
                    }
                    break;
                case PAGE_CONTACTS:
                    if(queries[PAGE_CONTACTS]==null || queries[PAGE_CONTACTS].length()==0) {
                        searchMenuItem.collapseActionView();
                        searchMenuItem.setTitle(null);
                    }
                    else {
                        searchMenuItem.expandActionView();
                        searchMenuItem.setTitle(query);
                    }
                    break;

            }
            switchSearchQuery(query);
        }
    };
*/
    @Override
    public void onStop() {
        super.onStop();
/*        if(Me.DEBUG)
            FlurryAgent.onEndSession(this);*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        me.psmSettings.defaultPage().put(indicator.getCurrentItem());
        me.setLogged(false);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem=menu.findItem(R.id.menu_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        if (searchView!=null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(true);
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    switchSearchQuery(null);
                    return true;
                }
            });
        }
        searchMenuItem.setOnActionExpandListener(new OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                switchSearchQuery(null);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                switchSearchQuery(null);
                return true;
            }
        });
        SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String query) {
                switchSearchQuery(query);
                return true;
            }

            public boolean onQueryTextSubmit(String query) {
                switchSearchQuery(query);
                return true;
            }
        };
        if(searchView!=null)
            searchView.setOnQueryTextListener(queryTextListener);
        return super.onCreateOptionsMenu(menu);
    }

    private void setTag(String query) {
        switch (indicator.getCurrentItem()) {
            case PAGE_MESSAGES:
                indicator.setTag(R.layout.messages_layout, query);
                break;
            case PAGE_CONTACTS:
                indicator.setTag(R.layout.contacts_layout, query);
                break;
        }
    }

    private String getTag() {
        switch (indicator.getCurrentItem()) {
            case PAGE_MESSAGES:
                return (String )indicator.getTag(R.layout.messages_layout);
            case PAGE_CONTACTS:
                return (String )indicator.getTag(R.layout.contacts_layout);
        }
        return null;
    }

    private void switchSearchQuery(String query) {
        if (fragmentPageAdapter == null)
            return;
        final Bundle bundle = new Bundle();
        switch (indicator.getCurrentItem()) {
            case PAGE_MESSAGES:
                MessagesFragments_ mf = (MessagesFragments_) fragmentPageAdapter.getItem(PAGE_MESSAGES);
                if(mf.getActivity()==null)
                    return;
                if(query==null || query.trim().length()==0)
                    mf.getLoaderManager().restartLoader(MessagesFragments.ALL_MESSAGES_LOADER, bundle, mf);
                else {
                    bundle.putString(Constants.EXTRA_FILTER, query);
                    mf.getLoaderManager().restartLoader(MessagesFragments.SEARCH_MESSAGES_LOADER, bundle, mf);
                }
                break;
            case PAGE_CONTACTS:
                ContactsFragments_ cf = (ContactsFragments_) fragmentPageAdapter.getItem(PAGE_CONTACTS);
                if(cf.getActivity()==null)
                    return;
                if(query==null || query.trim().length()==0)
                    cf.getLoaderManager().restartLoader(ContactsFragments.INIT_CONTACTS_LOADER, bundle, cf);
                else {
                    bundle.putString(Constants.EXTRA_FILTER, query);
                    cf.getLoaderManager().restartLoader(ContactsFragments.FILTER_CONTACTS_LOADER, bundle, cf);
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newSmsMenuId:
                startActivity(new Intent(this, SMSActivity_.class));
                return true;
            case R.id.newContactMenuId:
                Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                intent.putExtra(ContactsContract.Intents.Insert.NAME, this.getString(R.string.anonymous));
                startActivityForResult(intent, NEW_CONTACT_REQUEST);
                return true;
            case R.id.psmStoreId:
                Toast.makeText(this, this.getString(R.string.itsFree), Toast.LENGTH_LONG).show();
                return true;
            case R.id.contextMenuSettingsId:
                showSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case R.string.rateApp:
                String message = this.getString(R.string.dontaskanymore);
                SpannableStringBuilder ssb = new SpannableStringBuilder(message);
                StyleSpan span = new StyleSpan(Typeface.ITALIC);
                RelativeSizeSpan span1 = new RelativeSizeSpan(0.75f);
                ssb.setSpan(span, 0, message.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                ssb.setSpan(span1, 0, message.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                final SpannableStringBuilder[] items = {ssb};
                final boolean checked[] = new boolean[]{false};
                builder.setCancelable(false)
                        .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                me.psmSettings.rateApp().put(!isChecked);
                            }
                        })
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                RapeMe rapeMe = new RapeMe();
                                rapeMe.launchMarket(PSMActivity.this);
                            }
                        })
                        .setNegativeButton(R.string.later, null);
                builder.setTitle(this.getString(R.string.rateApp));
                return builder.create();
            default:
                return null;
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        switch (indicator.getCurrentItem()) {
            case PAGE_CONTACTS:
                contactViewHolder = (ContactViewHolder) view.getTag();
                messageViewHolder = null;
                getMenuInflater().inflate(R.menu.contact_contextmenu, contextMenu);
                break;
            case PAGE_MESSAGES:
                messageViewHolder = (MessageViewHolder) view.getTag();
                contactViewHolder = null;
                if(messageViewHolder.isThreadView)
                    getMenuInflater().inflate(R.menu.messages_thread_contextmenu, contextMenu);
                else
                    getMenuInflater().inflate(R.menu.message_single_contextmenu, contextMenu);
                if(messageViewHolder.addresses==null || messageViewHolder.addresses.size() > 1) {
                    contextMenu.removeItem(R.id.viewContact);
                    contextMenu.removeItem(R.id.call);
                }
                else {
                    final ContactInfo contactInfo = me.getContactDAO().getContactInfoByAddress(this, messageViewHolder.addresses.get(0));
                    if (contactInfo.key == null) //not contact, so delete contact related menu
                        contextMenu.removeItem(R.id.viewContact);
                }
                break;
            default:
                super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.viewContact: //called either from contact or messages pages
                final Intent intent = new Intent(this, PhoneContactActivity_.class);
                if (messageViewHolder != null && messageViewHolder.addresses!=null && messageViewHolder.addresses.size()==1) {
                    final ContactInfo contactInfo = me.getContactDAO().getContactInfoByAddress(this, messageViewHolder.addresses.get(0));
                    intent.putExtra(ContactsContract.Contacts.LOOKUP_KEY, contactInfo.key);
                }
                if (contactViewHolder != null)
                    intent.putExtra(ContactsContract.Contacts.LOOKUP_KEY, contactViewHolder.key);
                startActivity(intent);
                break;
            case R.id.call: //called only from messages page
                if(messageViewHolder!=null && messageViewHolder.addresses!=null && messageViewHolder.addresses.size() > 0) {
                    final Intent intentCall = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + messageViewHolder.addresses.get(0)));
                    startActivity(intentCall);
                }
                break;
            case R.id.deleteContact: //called only from contacts page
                final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteContact)
                        .setMessage(R.string.deleteContactWarning)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                me.getContactDAO().delete(PSMActivity.this, contactViewHolder.key);
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                builder.create().show();
                break;
            case R.id.deleteThread: //called only from messages page
                final AlertDialog.Builder builder_thread = new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteThread)
                        .setMessage(R.string.deleteThreadWarning)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                me.getMessageDAO().deleteGroup(PSMActivity.this, messageViewHolder.conversationId);
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                builder_thread.create().show();
                break;
            case R.id.protectThread: //called only from messages page
                final AlertDialog.Builder builder_protect = new AlertDialog.Builder(this)
                        .setTitle(R.string.protectThread)
                        .setMessage(R.string.protectThreadWarning)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new ProtectThreadTask(PSMActivity.this, true).execute(messageViewHolder.conversationId);
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                builder_protect.create().show();
                break;
            case R.id.unProtectThread: //called only from messages page
                final AlertDialog.Builder builder_unprotect = new AlertDialog.Builder(this)
                        .setTitle(R.string.unProtectThread)
                        .setMessage(R.string.unProtectThreadWarning)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Purse purse = Me.getMe().getMessagePurseDAO().get(PSMActivity.this);
                                if (me.getHashDAO().get().isEnabled() && !purse.isComfortPINEnabled()) {
                                    final CheckPINDialog checkPINDialog = new CheckPINDialog(PSMActivity.this, CheckPINDialog.MODE_DOESNT_MATTER);
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
                                            new ProtectThreadTask(PSMActivity.this, false).execute(messageViewHolder.conversationId);
                                        }
                                    });
                                    checkPINDialog.show();
                                } else {
                                    new ProtectThreadTask(PSMActivity.this, false).execute(messageViewHolder.conversationId);
                                }
                            }
                        }).setNegativeButton(R.string.no, null);
                builder_unprotect.create().show();
                break;
            case R.id.deleteMessage: //called only from messages page (in search mode)
                final AlertDialog.Builder builder_delete = new AlertDialog.Builder(this)
                        .setTitle(R.string.delete)
                        .setMessage("Are you sure delete this message?")
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                me.getMessageDAO().delete(PSMActivity.this, messageViewHolder.messageId);
                            }
                        })
                        .setNegativeButton(R.string.no, null);
                builder_delete.create().show();
                break;
            default: //unknown
                Log.w(TAG, "Unknown context menu item =" + item.getItemId());
                return false;
        }
        return true; //item processed
    }

    private void showSettings() {
        this.closeOptionsMenu();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SettingsActivity.LOGIN_ENABLED);   /**/
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case NEW_CONTACT_REQUEST:
                if (requestCode == RESULT_OK) {
                    defaultPage = PAGE_CONTACTS;
                    viewpager.setCurrentItem(defaultPage);
                    viewpager.invalidate();
                    indicator.invalidate();
                }
                break;
        }
    }

    public void onEulaAgreedTo() {
        this.finish();
        this.startActivity(new Intent(this, PSMActivity_.class));
    }

    public void onEulaRefuseTo() {
        this.finish();
        me.exit();
    }

    private void handleDefaultSMSApplication() {
        final String myPackageName = getPackageName();
        if (android.os.Build.VERSION.SDK_INT < 19) {
            me.setWriteMessagePermission(true);
            return;
        }
        if (!Me.KITKAT_SUPPORT && android.os.Build.VERSION.SDK_INT >= 19) {
            me.setWriteMessagePermission(false);
            return;
        }
/*        if(!me.psmSettings.checkDefaultSMSApplication().get())
            return;*/
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            Log.i(TAG, "PSM is not default SMS application");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.changeSMSApplication)
                    .setMessage(R.string.usePSMAsDefault)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
                            startActivity(intent);
                            me.setWriteMessagePermission(true);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            me.setWriteMessagePermission(false);
                            new MessageBox(PSMActivity.this, "Application can't be used to send messages");
                        }
                    });
            builder.create().show();
        } else {
            // App is the default.
            me.setWriteMessagePermission(true);
        }
    }

    private class ProtectThreadTask extends AsyncTask<Object, Integer, Boolean> {
        Context context;
        ProgressDialog progressDialog;
        boolean protect;
        String conversationId;

        protected ProtectThreadTask(Context context, boolean protect) {
            super();
            this.context = context;
            this.protect = protect;
            progressDialog = new ProgressDialog(context);
            progressDialog.setOwnerActivity(PSMActivity.this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            if (protect)
                progressDialog.setTitle(R.string.protectThread);
            else
                progressDialog.setTitle(R.string.unProtectThread);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Object... arg) {
            conversationId = (String) arg[0];
            boolean result;
            if (protect)
                result = Me.getMe().getMessageDAO().protectConversation(context, conversationId);
            else
                result = Me.getMe().getMessageDAO().unprotectConversation(context, conversationId);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            if (!result && protect) {
                new AlertDialog.Builder(PSMActivity.this)
                        .setTitle(R.string.protectThread)
                        .setMessage(R.string.errorProtecting)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                return;
                            }
                        }).show();
            }
            if (!result && !protect) {
                new AlertDialog.Builder(PSMActivity.this)
                        .setTitle(R.string.protectThread)
                        .setMessage(R.string.errorUnProtecting)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                                return;
                            }
                        }).show();
            }
        }
    }
}

