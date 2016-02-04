package ru.ivanovpv.gorets.psm.view;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.ConversationActivity;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.MessageBox;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.SmsReceiver;
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

/**
 * Created by pivanov on 30.12.2014.
 */
public class NewConversationCursorRowAdapter extends CursorAdapter {
    private final static String TAG=NewConversationCursorRowAdapter.class.getName();
    private LayoutInflater layoutInflater;
    protected ConversationActivity activity;
    private Me me;
    private MessageDAO messageDAO;
    private int lastDate = new Date(0L).getDate();
    private Hashtable<String, Boolean> fired;

    public NewConversationCursorRowAdapter(ConversationActivity activity, Cursor cursor) {
        super(activity, cursor, false);
        this.activity=activity;
        layoutInflater=(LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        me=Me.getMe();
        messageDAO= me.getMessageDAO();
        fired=new Hashtable<String, Boolean>();
    }

    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup layout;

        ConversationViewHolder holder = new ConversationViewHolder();
        layout = (ViewGroup) layoutInflater.inflate(R.layout.cloudy_mms_layout, null);
        holder.incomingBody = (LinearLayout) layout.findViewById(R.id.incomingBody);
        holder.sentBody = (LinearLayout) layout.findViewById(R.id.sentBody);
        holder.separatorText = (TextView) layout.findViewById(R.id.separatorText);
        holder.incomingText = (TextView) layout.findViewById(R.id.incomingText);
        holder.incomingTime = (TextView) layout.findViewById(R.id.incomingTime);
        holder.sentText = (TextView) layout.findViewById(R.id.sentText);
        holder.sentTime = (TextView) layout.findViewById(R.id.sentTime);
        holder.incomingType = (TextView) layout.findViewById(R.id.incomingType);
        holder.sentType = (TextView) layout.findViewById(R.id.sentType);
        holder.statusIcon = (ImageButton) layout.findViewById(R.id.sentStatusIcon);
        holder.progressBar = (ProgressBar) layout.findViewById(R.id.progressBarPendingStatus);
        holder.incoming = (ViewGroup) layout.findViewById(R.id.incoming);
        holder.sent = (ViewGroup) layout.findViewById(R.id.sent);
        if (MessageDAO.isMms(cursor))
        {
            ViewGroup rootView;
            holder.isSms=false;
            final MultimediaMessage mmsMessage = messageDAO.getMmsMessage(context, cursor);
            if(mmsMessage.isIncoming())
                rootView=holder.incomingBody;
            else
                rootView=holder.sentBody;
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
            List<MultimediaMessagePart> parts = mmsMessage.getParts();
            for (MultimediaMessagePart part : parts) {
                //dealing with mms images
                if (ContentType.isImageType(part.getContentType())) {
                    ViewGroup imageLayout;
                    imageLayout = (ViewGroup) layoutInflater.inflate(R.layout.mms_image_layout, rootView);
                } else if (ContentType.isAudioType(part.getContentType())) {
                    ViewGroup audioLayout;
                    audioLayout = (ViewGroup) layoutInflater.inflate(R.layout.mms_audio_layout, rootView);
                }
            }
        }
        layout.setTag(R.layout.cloudy_mms_layout, holder);
        return layout;
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        Message message = null;
        View tmpView;
        ViewGroup viewGroup=(ViewGroup )view;
        ViewGroup tmpViewGroup;
        ConversationViewHolder holder = (ConversationViewHolder) view.getTag(R.layout.cloudy_mms_layout);
        //deal with date
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
            holder.isSms=false;
            message = messageDAO.getMmsMessage(context, cursor);
            MultimediaMessage mms = (MultimediaMessage) message;
            String body=mms.getBody();
            List<MultimediaMessagePart> parts = mms.getParts();
            if(mms.isIncoming()) {
                holder.sent.setVisibility(View.GONE);
                holder.incoming.setVisibility(View.VISIBLE);
                TextView tv = (TextView) holder.incomingBody.findViewById(R.id.incomingSubject);
                tv.setText(context.getString(R.string.subject) + mms.getSubject());
                tv = (TextView) holder.incomingBody.findViewById(R.id.incomingText);
                tv.setText(mms.getBody());
                View button=holder.incomingBody.findViewById(R.id.downloadButton);
                if(mms.isDownloaded())
                    button.setVisibility(View.GONE);
                else
                    button.setVisibility(View.VISIBLE);
                holder.incomingType.setText(R.string.mms);
            }
            else {
                holder.sent.setVisibility(View.VISIBLE);
                holder.incoming.setVisibility(View.GONE);
                TextView tv = (TextView) holder.sentBody.findViewById(R.id.sentSubject);
                tv.setText(context.getString(R.string.subject) + mms.getSubject());
                tv = (TextView) holder.sentBody.findViewById(R.id.sentText);
                tv.setText(mms.getBody());
                holder.sentType.setText(R.string.mms);
            }
            //inflate any possible mms parts
            boolean partActive=false;
            ImageView imageView=null;
            ImageButton audioButton=null;
            for (final MultimediaMessagePart part : parts) {
                final Uri partUri = Uri.parse("content://mms/part/" + part.getId());
                if (ContentType.isImageType(part.getContentType())) {
                    partActive=true;
                    Bitmap bm = MessageDAO.getMmsDataAsImage(context, partUri);
                    if (mms.isIncoming()) {
                        imageView=(ImageView )holder.incomingBody.findViewById(R.id.mmsImage);
                        if(imageView==null) {
                            ViewGroup imageGroup = (ViewGroup) layoutInflater.inflate(R.layout.mms_image_layout, holder.incomingBody);
                            imageView = (ImageView) imageGroup.findViewById(R.id.mmsImage);
                        }
                    }
                    else {
                        imageView=(ImageView )holder.sentBody.findViewById(R.id.mmsImage);
                        if(imageView==null) {
                            ViewGroup imageGroup = (ViewGroup) layoutInflater.inflate(R.layout.mms_image_layout, holder.sentBody);
                            imageView = (ImageView) imageGroup.findViewById(R.id.mmsImage);
                        }
                    }
                    imageView.setImageBitmap(bm);
                }
                else if (ContentType.isAudioType(part.getContentType())) {
                    partActive=true;
                    if (mms.isIncoming()) {
                        audioButton = (ImageButton) holder.incomingBody.findViewById(R.id.mmsAudio);
                        audioButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent();
                                intent.setAction(android.content.Intent.ACTION_VIEW);
                                intent.setDataAndType(partUri, part.getContentType());
                                context.startActivity(intent);
                            }
                        });
                    }
                    else {
                        audioButton = (ImageButton) holder.sentBody.findViewById(R.id.mmsAudio);
                        audioButton.setOnClickListener(new View.OnClickListener() {
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
            if(partActive) {
                showLayout(holder.incoming, R.id.partLayout);
                showLayout(holder.sent, R.id.partLayout);
            }
            else {
                hideLayout(holder.incoming, R.id.partLayout);
                hideLayout(holder.sent, R.id.partLayout);
            }

        }
        else {
            holder.isSms=true;
            hideLayout(holder.incoming, R.id.partLayout);
            hideLayout(holder.sent, R.id.partLayout);
            message = messageDAO.getSmsMessage(context, cursor);
            if(message.isIncoming()) {
                view = viewGroup.findViewById(R.id.sent);
                holder.incomingType.setText(R.string.sms);
            }
            else {
                view = viewGroup.findViewById(R.id.incoming);
                holder.sentType.setText(R.string.sms);
            }
            view.setVisibility(View.GONE);
            /*tmpView=holder.incomingBody.findViewById(R.id.mmsImage);
            holder.incomingBody.removeView(tmpView);
            tmpView=holder.sentBody.findViewById(R.id.mmsImage);
            holder.sentBody.removeView(tmpView);
            if(message.isIncoming())
                ((ViewGroup )view).removeView(holder.sent);
            else
                ((ViewGroup )view).removeView(holder.incoming);*/
            tmpView=viewGroup.findViewById(R.id.incomingSubject);
            tmpView.setVisibility(View.GONE);
            tmpView=viewGroup.findViewById(R.id.sentSubject);
            tmpView.setVisibility(View.GONE);
            tmpView=viewGroup.findViewById(R.id.incomingSubject);
            tmpView.setVisibility(View.GONE);
            tmpView = viewGroup.findViewById(R.id.downloadButton);
            tmpView.setVisibility(View.GONE);
        }

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
                WidgetProvider.forceWidgetUpdate(context);
                SmsReceiver.clearNotification(); //clear notification (if any)
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
                holder.statusIcon.setTag(R.id.sentStatusIcon, null);
            } else if (message.isDraft()) {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.GONE);
                holder.statusIcon.setTag(R.id.sentStatusIcon, null);
            } else if (message.isFailed()) {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.VISIBLE);
                holder.statusIcon.setTag(R.id.sentStatusIcon, message);
                holder.statusIcon.setTag(R.id.cloudyMessageLayout, holder);
            } else if (message.isPending()) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.statusIcon.setVisibility(View.GONE);
                holder.statusIcon.setTag(R.id.sentStatusIcon, null);
            } else {
                holder.progressBar.setVisibility(View.GONE);
                holder.statusIcon.setVisibility(View.GONE);
                holder.statusIcon.setTag(R.id.sentStatusIcon, null);
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

    private ImageView setOrAddImage(ViewGroup layout, int resourceId, int id, Bitmap bitmap) {
//        layout.list
        ImageView imageView=(ImageView )layout.findViewById(id);
        if(imageView==null) {
            ViewGroup imageGroup = (ViewGroup) layoutInflater.inflate(resourceId, layout);
            //layout.addView(imageIncoming, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            imageView = (ImageView) imageGroup.findViewById(id);
        }
        imageView.setImageBitmap(bitmap);
        if(Me.DEBUG) {
            for(int i=0; i < layout.getChildCount(); i++) {
                View view=layout.getChildAt(i);
                Log.i(TAG, "Image adder child #"+i+", view="+view);
            }
        }
        return imageView;
    }

    private void hideLayout(ViewGroup layout, int resourceId) {
        if(layout==null)
            return;
        ViewGroup viewGroup=(ViewGroup )layout.findViewById(resourceId);
        if(viewGroup!=null)
            viewGroup.setVisibility(View.GONE);
    }

    private void showLayout(ViewGroup layout, int resourceId) {
        if(layout==null)
            return;
        ViewGroup viewGroup=(ViewGroup )layout.findViewById(resourceId);
        if(viewGroup!=null)
            viewGroup.setVisibility(View.VISIBLE);
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
        //activity.updateConversationCursored(bingoNumber, false);
        return;
    }

    @Override
    public Cursor swapCursor(Cursor cursor) {
        return super.swapCursor(cursor);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
    }

}
