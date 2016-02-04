package ru.ivanovpv.gorets.psm;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import ru.ivanovpv.gorets.psm.cipher.ByteUtils;
import ru.ivanovpv.gorets.psm.cipher.FingerPrint;
import ru.ivanovpv.gorets.psm.cipher.KeyExchange;
import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.persistent.Contact;
import ru.ivanovpv.gorets.psm.persistent.Hash;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.protocol.ProtocolAccept;
import ru.ivanovpv.gorets.psm.protocol.ProtocolInvite;
import ru.ivanovpv.gorets.psm.service.SmsSendIntentService;

import java.util.ArrayList;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 497 $
 *   $LastChangedDate: 2014-02-15 22:12:10 +0400 (Сб, 15 фев 2014) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/ConfirmInvitationDialog.java $
 */

public class ConfirmInvitationDialog extends Dialog implements View.OnClickListener {
    private final static String TAG=ConfirmInvitationDialog.class.getName();
    private Message message;
    private ConversationActivity activity;

    private Me me;
    public ConfirmInvitationDialog(ConversationActivity activity, Message message) {
        super(activity);
        this.message=message;
        this.activity=activity;
        me = Me.getMe();
    }

    public void onPrepare(ConversationActivity activity, Message message) {
        this.activity=activity;
        this.message=message;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.confirm_invitation_dialog);
        this.setTitle(R.string.confirmInvitationTitle);
        final Button yesButton = (Button) findViewById(R.id.yes);
        final Button noButton = (Button) findViewById(R.id.no);
        final Button laterButton = (Button) findViewById(R.id.later);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);
        laterButton.setOnClickListener(this);
    }

    @Override
    public void onStart()
    {
        TextView tv=(TextView )findViewById(R.id.confirmMessage);
        StringBuilder sb=new StringBuilder();
        sb.append(activity.getString(R.string.invitationDetected));
        sb.append(message.getAddress());
        sb.append('\n').append(activity.getString(R.string.doYouAcceptInvitation));
        tv.setText(sb.toString());

    }

    public void onClick(View view) {
        PhoneNumber bingoNumber=null;
        Protocol protocolAccept;
        ProtocolInvite protocolInvite;
        byte[] publicKey;
        String msgText;
        if(view.getId()==R.id.later) {
            this.cancel();
            return;
        }
        //parsing received key
        try {
            protocolInvite=(ProtocolInvite )Protocol.parseProtocol(message.getBody());
            if(Me.TEST)
                publicKey=new byte[] {0,1,0,1,0,1,0,1};
            else
                publicKey=protocolInvite.decodeBytes(message.getBody());
        }
        catch(Exception pe) {
            Log.e(TAG, "Error parsing invitation", pe);
            new MessageBox(activity,activity.getString(R.string.invitationCorrupted));
            this.cancel();
            return;
        }
        FingerPrint receivedFingerPrint=new FingerPrint(publicKey, protocolInvite.getKeyExchange().getType());
        if(Me.DEBUG)
            Log.i(TAG, "Public key received="+ ByteUtils.bytesToHex(publicKey));
        if(view.getId()==R.id.yes) {
            //saving received key
            KeyExchange keyExchange=Hash.getDefaultKeyExchange(Me.getMe().getHashDAO().get().getKey());
            byte[] key=keyExchange.getPublicKey();
            bingoNumber=me.getContactDAO().getPhoneNumber(activity, message.getAddress());
            if(bingoNumber==null) {
                bingoNumber=new PhoneNumber(message.getAddress(), "", false);
            }
            bingoNumber.addPublicKey(publicKey, System.currentTimeMillis(), protocolInvite.getKeyExchange().getType());
            bingoNumber=me.getContactDAO().save(activity, bingoNumber);
            msgText=activity.getString(R.string.invitationReceived)+receivedFingerPrint.toString();
            message.setBody(msgText);
            me.getMessageDAO().save(activity, message);
            //send my public key
            FingerPrint myFingerPrint=new FingerPrint(key, keyExchange.getType());
            if(Me.DEBUG)
                Log.i(TAG, "Accept and send public key="+myFingerPrint.toString());
            protocolAccept=new ProtocolAccept(keyExchange);
            String body=protocolAccept.encodeBytes(key);
            msgText=activity.getString(R.string.invitationAccepted)+myFingerPrint.toString();
            Message acceptMessage=Message.createSendMessage(message.getAddress(), msgText, body);
            SmsSendIntentService.startActionSend(activity, acceptMessage, bingoNumber, false);
/*            acceptMessage.setSentStatus(Message.STATUS_PENDING);
            acceptMessage.setDeliveredStatus(Message.STATUS_PENDING);
            acceptMessage=me.getMessageDAO().save(activity, acceptMessage);
            protocolAccept.sendRawMessage(activity, acceptMessage.getAddress(), body, acceptMessage.getId());*/

            activity.updateConversationCursored(bingoNumber, false);
        }
        else if(view.getId()==R.id.no) {
            msgText=activity.getString(R.string.invitationIgnored)+receivedFingerPrint.toString();
            message.setBody(msgText);
            me.getMessageDAO().save(activity, message);
        }
        this.dismiss();
    }
}
