package ru.ivanovpv.gorets.psm.view;

import android.app.Activity;
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

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Vector;

import ru.ivanovpv.gorets.psm.Constants;
import ru.ivanovpv.gorets.psm.ConversationActivity_;
import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.db.MessageDAO;
import ru.ivanovpv.gorets.psm.persistent.Message;
import ru.ivanovpv.gorets.psm.persistent.MultimediaMessage;
import ru.ivanovpv.gorets.psm.protocol.Protocol;
import ru.ivanovpv.gorets.psm.MessageDetailsActivity_;

/**
 * Created by pivanov on 29.12.2014.
 */
public class MessageSearchCursorRowAdapter extends SimpleCursorAdapter implements View.OnClickListener {
    private static final String TAG = MessageSearchCursorRowAdapter.class.getName();
    private Activity activity;
    private Me me;
    private int layout;
    private final LayoutInflater layoutInflater;
    private int messageIdIndex;
    private int conversationIdIndex;
    private int addressIndex;
    private int bodyIndex;
    private int dateIndex;
    private DisplayImageOptions options;
    private MessageDAO messageDAO;


    //Column #0=_id
    //Column #1=thread_id
    //Column #2=address
    //Column #3=body
    //Column #4=date

    public MessageSearchCursorRowAdapter(Activity activity, int layout) {     // add param true for higthlite
        super(activity, layout, null,
                new String[] { MessageDAO.ADDRESS, MessageDAO.BODY, MessageDAO.DATE},
                new int[] { R.id.respondent, R.id.body, R.id.millis}, 0);
        this.activity =activity;
        me = Me.getMe();
        messageDAO=me.getMessageDAO();
        this.layout = layout;
        this.messageIdIndex=0; //cursor.getColumnIndex(MessageDAO._ID);
        this.conversationIdIndex = 1; //cursor.getColumnIndex(MessageDAO.CONVERSATION_ID);
        this.addressIndex = 2; //cursor.getColumnIndex(MessageDAO.ADDRESS);
        this.bodyIndex = 3; //cursor.getColumnIndex(MessageDAO.BODY);
        this.dateIndex = 4; //cursor.getColumnIndex(MessageDAO.DATE);
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
        findColumns(cursor);
        return super.swapCursor(cursor);
    }

    @Override
    public void changeCursor(Cursor cursor) {
        findColumns(cursor);
        super.changeCursor(cursor);
    }

    private void findColumns(Cursor cursor) {
        if(cursor!=null) {
            this.messageIdIndex = cursor.getColumnIndex(MessageDAO._ID);
            this.conversationIdIndex = cursor.getColumnIndex(MessageDAO.CONVERSATION_ID);
            this.addressIndex = cursor.getColumnIndex(MessageDAO.ADDRESS);
            this.bodyIndex = cursor.getColumnIndex(MessageDAO.BODY);
            this.dateIndex = cursor.getColumnIndex(MessageDAO.DATE);
        }
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = layoutInflater.inflate(layout, null);
        final MessageViewHolder viewHolder = new MessageViewHolder();
        viewHolder.isThreadView=false;
        viewHolder.respondent = (TextView)view.findViewById(R.id.respondent);
        viewHolder.body = (TextView)view.findViewById(R.id.body);
        viewHolder.date = (TextView)view.findViewById(R.id.millis);
        viewHolder.icon = (ImageView)view.findViewById(R.id.messageIcon);
        view.setTag(viewHolder);
        view.setOnClickListener(this);
        activity.registerForContextMenu(view);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewGroup viewGroup;
        MessageViewHolder viewHolder=(MessageViewHolder )view.getTag();
        viewHolder.messageId=cursor.getString(messageIdIndex);
        String body = cursor.getString(bodyIndex);
        body = checkBodyByProtocol(body);
        long date = cursor.getLong(dateIndex);
        viewHolder.conversationId = cursor.getString(conversationIdIndex);
        viewHolder.addresses = new ArrayList<String>();
        viewHolder.addresses.add(cursor.getString(addressIndex));

        //checking whether it's sms or mms
        Message message=messageDAO.getSmsMessage(context, viewHolder.conversationId, viewHolder.messageId);
        if(message==null) { //means mms
            MultimediaMessage multimediaMessage=messageDAO.getMmsMessage(context, viewHolder.conversationId, viewHolder.messageId);
            //viewHolder.address=multimediaMessage.getAddress();
            date*=1000L;
            if(multimediaMessage.isIncoming()) {
                viewGroup = (ViewGroup) view.findViewById(R.id.sentBalls);
                viewGroup.setVisibility(View.GONE);
                viewGroup = (ViewGroup) view.findViewById(R.id.inputBalls);
                viewGroup.setVisibility(View.VISIBLE);
            }
            else {
                viewGroup = (ViewGroup) view.findViewById(R.id.inputBalls);
                viewGroup.setVisibility(View.GONE);
                viewGroup = (ViewGroup) view.findViewById(R.id.sentBalls);
                viewGroup.setVisibility(View.VISIBLE);
            }
            multimediaMessage.computeAddress(context);
            viewHolder.addresses=new ArrayList<String>();
            viewHolder.addresses.add(multimediaMessage.getAddress());
        }
        else {
            viewHolder.addresses=new ArrayList<String>();
            viewHolder.addresses.add(message.getAddress());
            if(message.isIncoming()) {
                viewGroup = (ViewGroup) view.findViewById(R.id.sentBalls);
                viewGroup.setVisibility(View.GONE);
                viewGroup = (ViewGroup) view.findViewById(R.id.inputBalls);
                viewGroup.setVisibility(View.VISIBLE);
            }
            else {
                viewGroup = (ViewGroup) view.findViewById(R.id.inputBalls);
                viewGroup.setVisibility(View.GONE);
                viewGroup = (ViewGroup) view.findViewById(R.id.sentBalls);
                viewGroup.setVisibility(View.VISIBLE);
            }
        }
        viewHolder.date.setText(DateFormat.getDateTimeInstance().format(date));
        new AsyncUniversalMessageThreadInfoLoader(view, viewHolder, context, me, options).execute();

        if(body==null)
            body="";
        viewHolder.body.setText(body);
//        viewHolder.unreadMessages.setText("");

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

    public void onClick(View view) {
        final MessageViewHolder viewHolder=(MessageViewHolder ) view.getTag();
        if(Me.DEBUG)
            Log.i(TAG, "Selected: " + viewHolder.toString());
        final Intent intent = new Intent(mContext, ConversationActivity_.class);
        intent.putExtra(MessageDAO.CONVERSATION_ID, viewHolder.conversationId);
        intent.putExtra(MessageDAO.ADDRESS, viewHolder.addresses); //array of addresses!
        this.activity.startActivity(intent);
    }

}
