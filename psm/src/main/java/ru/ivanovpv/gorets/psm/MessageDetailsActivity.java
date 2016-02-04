/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Alexander Laschenko 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 494 $
 *    $LastChangedDate: 2014-02-03 17:06:16 +0400 (Пн, 03 фев 2014) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/MessageDetailsActivity.java $
 */

package ru.ivanovpv.gorets.psm;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.App;
import org.androidannotations.annotations.EActivity;
import ru.ivanovpv.gorets.psm.cipher.Cipher;
import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.Purse;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolException;

@EActivity(R.layout.message_details_layout)
public class MessageDetailsActivity extends SherlockActivity
{
    private static final String TAG=MessageDetailsActivity.class.getName();

    @App
    Me me;

    private TextView transportTextView, addressTextView, dateTextView, bodyTextView, storedBodyTextView, typeTextView;

    private Message message=null;

    @AfterViews
	public void init() {
        Long id=null;
        ViewGroup group;
        boolean isSms=true;

        Bundle extras=this.getIntent().getExtras();
        if(extras!=null) {
            id = extras.getLong(Constants.EXTRA_MESSAGE_ID);
            isSms = extras.getBoolean(Constants.EXTRA_MESSAGE_TYPE, true);
        }
        if(id!=null)
            message=me.getMessageDAO().getMessageByRowId(this, id, isSms);
        if(id==null || message==null) {
            this.finish();
            return;
        }

        //address
        if(!message.isIncoming()) {
            group=(ViewGroup )findViewById(R.id.fromRow);
            addressTextView=(TextView)findViewById(R.id.whom);
        }
        else {
            group=(ViewGroup )findViewById(R.id.whomRow);
            addressTextView=(TextView)findViewById(R.id.from);
        }
        group.setVisibility(View.GONE);
        ContactInfo contactInfo=me.getContactDAO().getContactInfoByAddress(this, message.getAddress());
        addressTextView.setText(contactInfo.getFullInfo(this));

        //transport
        transportTextView=(TextView )findViewById(R.id.transport);
        if(message.isSms())
            transportTextView.setText(R.string.sms);
        else if(message.isMms())
            transportTextView.setText(R.string.mms);

        //date
        dateTextView=(TextView )findViewById(R.id.date);
        dateTextView.setText(message.getFullDate(this));

        //stored body
        if(message.isBodyReal() && message.isPlain()) {
            group=(ViewGroup )findViewById(R.id.storedBodyRow);
            group.setVisibility(View.GONE);
        } else {
            storedBodyTextView=(TextView )findViewById(R.id.storedBody);
            if(!message.isBodyReal())
                storedBodyTextView.setText(message.getMessagePersistent().getStoredBody());
            else
                storedBodyTextView.setText(message.getBody());
        }
        String storedBody=message.getStoredBody();

        //body and type
        bodyTextView=(TextView )findViewById(R.id.body);
        typeTextView=(TextView )findViewById(R.id.type);
        if(Protocol.isAccept(storedBody)) {
            typeTextView.setText(R.string.inviteAcceptMessage);
            bodyTextView.setText(message.getBody());
        }
        else if(Protocol.isInvitation(storedBody)) {
            typeTextView.setText(R.string.invitationMessage);
            bodyTextView.setText(message.getBody());
        }
        else if(Protocol.isCiphered(storedBody)) {
            String typeText=this.getString(R.string.cipherMessage)+" - ";
            String[] s=this.getResources().getStringArray(R.array.privacyLevelEntries);
            switch(Protocol.extractCipherType(storedBody)) {
                case Cipher.CIPHER_DES:
                    typeText=typeText+s[0];
                    break;
                case Cipher.CIPHER_RIJNDAEL:
                    typeText=typeText+s[1];
                    break;
                case Cipher.CIPHER_ANUBIS:
                    typeText=typeText+s[2];
                default:
            }
            typeTextView.setText(typeText);
            Purse purse=Me.getMe().getMessagePurseDAO().get(this);
            if(me.getHashDAO().get().isEnabled() && !purse.isComfortPINEnabled()) {
                final CheckPINDialog checkPINDialog=new CheckPINDialog(this, CheckPINDialog.MODE_DOESNT_MATTER);
                checkPINDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        MessageDetailsActivity.this.finish();
                    }
                });
                checkPINDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(checkPINDialog.isCanceled())
                            return;
                        String storedBody=message.getStoredBody();
                        if(Protocol.isCiphered(storedBody))
                            bodyTextView.setText(MessageDetailsActivity.this.decipher(storedBody));
                        else
                            bodyTextView.setText(MessageDetailsActivity.this.unProtect(storedBody));
                    }
                });
                checkPINDialog.show();
                return;
            }
            else
                bodyTextView.setText(this.decipher(storedBody));
        }
        else if(Protocol.isScrambled(storedBody)) {
            typeTextView.setText(R.string.protectedMessage);
            Purse purse=Me.getMe().getMessagePurseDAO().get(this);
            if(me.getHashDAO().get().isEnabled() && !purse.isComfortPINEnabled()) {
                final CheckPINDialog checkPINDialog=new CheckPINDialog(this, CheckPINDialog.MODE_DOESNT_MATTER);
                checkPINDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        MessageDetailsActivity.this.finish();
                    }
                });
                checkPINDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if(checkPINDialog.isCanceled())
                            return;
                        String storedBody=message.getStoredBody();
                        if(Protocol.isCiphered(storedBody))
                            bodyTextView.setText(MessageDetailsActivity.this.decipher(storedBody));
                        else
                            bodyTextView.setText(MessageDetailsActivity.this.unProtect(storedBody));
                    }
                });
                checkPINDialog.show();
                return;
            }
            else
                bodyTextView.setText(this.unProtect(storedBody));
        }
        else {
            bodyTextView.setText(message.getBody());
            if(message.isBodyReal())
                typeTextView.setText(R.string.plainMessage);
            else
                typeTextView.setText(R.string.lemonMessage);
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

    private String decipher(String text) {
        String s=text;
        Protocol protocol;
        if(!Protocol.isCiphered(text))
            return text;
        byte[] sharedKey=me.getContactDAO().getSharedKey(this, message.getAddress(), message.getMillis());
        if(sharedKey!=null) {
            try {
                protocol=Protocol.parseProtocol(text, sharedKey);
                s=protocol.decodeString(text);
            } catch (ProtocolException e) {
                if(Me.DEBUG)
                    Log.e(TAG, "Error decrypting message", e);
                new MessageBox(this, this.getString(R.string.cantDecrypt));
            }
        }
        else {
            new MessageBox(this, this.getString(R.string.dontHavePublicKey));
        }
        return s;
    }

    private String unProtect(String text) {
        String s=text;
        if(!Protocol.isScrambled(text))
            return text;
        try {
            Protocol protocol=Protocol.parseProtocol(text);
            s=protocol.decodeString(text);
        } catch (Exception e) {
            if(Me.DEBUG)
                Log.e(TAG, "Error decrypting message", e);
            new MessageBox(this, this.getString(R.string.cantUnprotect));
        }
        return s;
    }

}
