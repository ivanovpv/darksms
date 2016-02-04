package ru.ivanovpv.gorets.psm.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.ivanovpv.gorets.psm.*;
import ru.ivanovpv.gorets.psm.cipher.FingerPrint;
import ru.ivanovpv.gorets.psm.db.ContactInfo;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.mms.ContentType;
import ru.ivanovpv.gorets.psm.mms.MmsUtilsIntentService;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessage;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessagePart;
import ru.ivanovpv.gorets.psm.persistent.PhoneNumber;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.widget.WidgetProvider;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2012.
 *   $Author: ivanovpv $
 *   $Rev: 497 $
 *   $LastChangedDate: 2014-02-15 22:12:10 +0400 (Сб, 15 фев 2014) $
 */

@Deprecated
public class ConversationCursorRowAdapter extends CursorAdapter {
    private static final String TAG = ConversationCursorRowAdapter.class.getName();
    private int lastDate = new Date(0L).getDate();
    private final Cursor mmsSmsConversationCursor;
    protected ConversationActivity activity;
    private final LayoutInflater layoutInflater;
    private final String conversationId;
    private MessageDAO messageDAO;
    private Hashtable<String, Boolean> fired;
    //private byte[] sharedKey;

    private Me me;

    public ConversationCursorRowAdapter(ConversationActivity activity, String conversationId, String address) {
        super(activity, null, false);
        this.activity = activity;
        me = (Me) activity.getApplication();
        this.conversationId = conversationId;
        this.messageDAO = me.getMessageDAO();
        mmsSmsConversationCursor = messageDAO.getMmsSmsConversationCursor(activity, this.conversationId);
        this.changeCursor(mmsSmsConversationCursor);
        activity.startManagingCursor(mmsSmsConversationCursor);
        this.layoutInflater = LayoutInflater.from(activity);
        fired = new Hashtable<String, Boolean>();
    }

    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup viewGroup) {
        ViewGroup layout;
        ConversationViewHolder holder = new ConversationViewHolder();
        if (MessageDAO.isMms(cursor)) {
            layout = (ViewGroup) layoutInflater.inflate(R.layout.cloudy_mms_layout, null);
            final MultimediaMessage mmsMessage = messageDAO.getMmsMessage(context, cursor);
            View downloadButton = layout.findViewById(R.id.downloadButton);
            if (mmsMessage.isDownloaded())
                downloadButton.setVisibility(View.GONE);
            else {
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MmsUtilsIntentService.startActionReceiveMms(context, mmsMessage);
                    }
                });
            }
            holder.incomingBody = (LinearLayout) layout.findViewById(R.id.incomingBody);
            holder.sentBody = (LinearLayout) layout.findViewById(R.id.sentBody);
            //dealing with mms subject
            ViewGroup subjectIncoming = (ViewGroup) layoutInflater.inflate(R.layout.mms_subject_layout, null);
            holder.incomingBody.addView(subjectIncoming, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            ViewGroup subjectSent = (ViewGroup) layoutInflater.inflate(R.layout.mms_subject_layout, null);
            holder.sentBody.addView(subjectSent, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            holder.message = mmsMessage;
            List<MultimediaMessagePart> parts = mmsMessage.getParts();
            for (MultimediaMessagePart part : parts) {
                //dealing with mms images
                if (ContentType.isImageType(part.getContentType())) {
                    ViewGroup imageIncoming, imageSent;
                    imageIncoming = (ViewGroup) layoutInflater.inflate(R.layout.mms_image_layout, null);
                    holder.incomingBody.addView(imageIncoming, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    imageSent = (ViewGroup) layoutInflater.inflate(R.layout.mms_image_layout, null);
                    holder.sentBody.addView(imageSent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                } else if (ContentType.isAudioType(part.getContentType())) {
                    ViewGroup audioIncoming, audioSent;
                    audioIncoming = (ViewGroup) layoutInflater.inflate(R.layout.mms_audio_layout, null);
                    holder.incomingBody.addView(audioIncoming, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    audioSent = (ViewGroup) layoutInflater.inflate(R.layout.mms_audio_layout, null);
                    holder.sentBody.addView(audioSent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
            }
        } else {
            layout = (ViewGroup) layoutInflater.inflate(R.layout.cloudy_sms_layout, null);
            Message message = messageDAO.getSmsMessage(context, cursor);
            holder.incomingBody = null;
            holder.sentBody = null;
            holder.message = message;
        }
        //AbstractBaseDAO.debugCursor(cursor);
        holder.separatorText = (TextView) layout.findViewById(R.id.separatorText);
        holder.incomingText = (TextView) layout.findViewById(R.id.incomingText);
        holder.incomingTime = (TextView) layout.findViewById(R.id.incomingTime);
        holder.sentText = (TextView) layout.findViewById(R.id.sentText);
        holder.sentTime = (TextView) layout.findViewById(R.id.sentTime);
        holder.statusIcon = (ImageButton) layout.findViewById(R.id.sentStatusIcon);
        holder.progressBar = (ProgressBar) layout.findViewById(R.id.progressBarPendingStatus);
        holder.incoming = (ViewGroup) layout.findViewById(R.id.incoming);
        holder.sent = (ViewGroup) layout.findViewById(R.id.sent);
        layout.setTag(holder);
        return layout;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        Message message = null;
        ConversationViewHolder holder = (ConversationViewHolder) view.getTag();

        if (!cursor.isFirst()) {
            cursor.moveToPrevious();
            try {
                if (MessageDAO.isMms(cursor))
                    lastDate = messageDAO.getMmsMessage(context, cursor).getDate().getDate();
                else
                    lastDate = messageDAO.getSmsMessage(context, cursor).getDate().getDate();
                cursor.moveToNext();
            } catch (CursorIndexOutOfBoundsException e) {
                Log.e(TAG, e.toString());
            }
        } else
            lastDate = new Date(0L).getDate();
        if (MessageDAO.isMms(cursor)) {
            message = messageDAO.getMmsMessage(context, cursor);
            MultimediaMessage mms = (MultimediaMessage) message;
            List<MultimediaMessagePart> parts = mms.getParts();
            if (holder.incomingBody != null) {
                TextView tv = (TextView) holder.incomingBody.findViewById(R.id.mmsSubject);
                tv.setText(context.getString(R.string.subject) + mms.getSubject());
            }
            if (holder.sentBody != null) {
                TextView tv = (TextView) holder.sentBody.findViewById(R.id.mmsSubject);
                tv.setText(context.getString(R.string.subject) + mms.getSubject());
            }
            for (final MultimediaMessagePart part : parts) {
                final Uri partUri = Uri.parse("content://mms/part/" + part.getId());
                if (ContentType.isImageType(part.getContentType())) {
                    ImageView incomingImage, sentImage;
                    Bitmap bm = MessageDAO.getMmsDataAsImage(context, partUri);
                    if (holder.incomingBody != null) {
                        incomingImage = (ImageView) holder.incomingBody.findViewById(R.id.mmsImage);
                        incomingImage.setImageBitmap(bm);
                    }
                    if (holder.sentBody != null) {
                        sentImage = (ImageView) holder.sentBody.findViewById(R.id.mmsImage);
                        sentImage.setImageBitmap(bm);
                    }
                } else if (ContentType.isAudioType(part.getContentType())) {
                    if (holder.incomingBody != null) {
                        ImageButton incomingAudio = (ImageButton) holder.incomingBody.findViewById(R.id.mmsAudio);
                        incomingAudio.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(partUri, part.getContentType());
                                context.startActivity(intent);
                            }
                        });
                    }
                    if (holder.sentBody != null) {
                        ImageButton sentAudio = (ImageButton) holder.sentBody.findViewById(R.id.mmsAudio);
                        sentAudio.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(partUri, part.getContentType());
                                context.startActivity(intent);
                            }
                        });
                    }
                }
            }
        } else
            message = messageDAO.getSmsMessage(context, cursor);
        if (message != null) {
            if (message.isIncoming() && message.isInvitation() && !isDialogAlreadyFired(message)) {
                fired.put(message.getId(), true);
                performInvitation(context, message, holder);
            }
            if (message.isIncoming() && message.isAccept() && !isDialogAlreadyFired(message)) {
                fired.put(message.getId(), true);
                performAccept(context, message);
            }
            if (!message.isProcessed()) {  //shown message need to be marked as read
                message.setProcessed(true);
                me.getMessageDAO().save(context, message);
                SmsReceiver.clearNotification(); //clear notification (if any)
                WidgetProvider.forceWidgetUpdate(context);
            }
            if (message.getRead() == 0) {
                message.setRead(1);   //mark as read for stock messenger
                me.getMessageDAO().save(context, message);
            }
            if (message.isCiphered()) {
                message.setRealBody(message.getBody());
                message.setBody(context.getString(R.string.cipherMessage));
            }
            if (message.isSentSuccess()) {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.GONE);
            } else if (message.isDraft()) {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.GONE);
            } else if (message.isFailed()) {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.VISIBLE);
                holder.sentText.setTextColor(context.getResources().getColor(R.color.light_grey));
            } else if (message.isPending()) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.statusIcon.setVisibility(View.GONE);
            } else {
                holder.statusIcon.setVisibility(View.GONE);
            }
            if (!message.isProtected()) {
                holder.sentText.setText(message.getBody());
                holder.incomingText.setText(message.getBody());
            } else {
                holder.sentText.setText(R.string.protectedMessage);
                holder.incomingText.setText(R.string.protectedMessage);
            }

            if (!message.isBodyReal())
                holder.sentText.setAutoLinkMask(0);

            holder.incomingTime.setText(message.getTime(context));
            holder.sentTime.setText(message.getTime(context));
            if (message.getDate().getDate() != lastDate) {
                holder.separatorText.setVisibility(View.VISIBLE);
                holder.separatorText.setText(message.getDate(context));
            } else {
                holder.separatorText.setVisibility(View.GONE);
            }
            if (message.isIncoming()) {
                if (message instanceof MultimediaMessage) {
                    Log.i(TAG, "MMS id=" + message.getId());
                }
                holder.incoming.setVisibility(View.VISIBLE);
                holder.sent.setVisibility(View.GONE);
            } else {
                holder.incoming.setVisibility(View.GONE);
                holder.sent.setVisibility(View.VISIBLE);
            }
        }
        //lastDate = message.getDate().getDate();
    }

    private boolean isDialogAlreadyFired(Message message) {
        if (message == null || message.getId() == null)
            return false;
        Boolean isFired = fired.get(message.getId());
        if (isFired == null || !isFired)
            return false;
        return true;
    }

    private void performInvitation(Context context, Message message, ConversationViewHolder viewHolder) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.EXTRA_MESSAGE, message);
        activity.showDialog(R.layout.confirm_invitation_dialog, bundle);
        return;
    }

    private void performAccept(Context context, Message message) {
        PhoneNumber bingoNumber = null;
        String body = message.getBody();
        Protocol protocol;
        byte[] publicKey = null;
        try {
            protocol = Protocol.parseProtocol(body);
            publicKey = protocol.decodeBytes(message.getBody());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            new MessageBox(context, e);
            return;
        }
        ContactInfo contactInfo = me.getContactDAO().getContactInfoByAddress(context, message.getAddress());
        bingoNumber = me.getContactDAO().getPhoneNumber(context, message.getAddress());
        if (bingoNumber == null) {
            bingoNumber = new PhoneNumber(message.getAddress(), "", false);
        }
        bingoNumber.addPublicKey(publicKey, System.currentTimeMillis(), protocol.getKeyExchange().getType());
        me.getContactDAO().save(context, bingoNumber);
        message.setRealBody(message.getBody());
        FingerPrint fingerPrint = new FingerPrint(publicKey, protocol.getKeyExchange().getType());
        String msgText = context.getString(R.string.acceptReceived) + fingerPrint.toString();
        message.setBody(msgText);
        messageDAO.save(context, message);
        new MessageBox(context, context.getString(R.string.invitationAcceptMessageBox) + contactInfo.name);
        activity.updateConversationCursored(bingoNumber, false);
        return;
    }

}
