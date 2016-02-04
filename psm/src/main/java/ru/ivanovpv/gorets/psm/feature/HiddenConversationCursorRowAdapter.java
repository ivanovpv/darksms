/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com)
 * and Oleksandr Lashchenko (gsorron@gmail.com) 2012-2013. All Rights Reserved.
 *    $Author: $
 *    $Rev: $
 *    $LastChangedDate:  $
 *    $URL: $
 */

package ru.ivanovpv.gorets.psm.feature;

import android.content.Context;
import android.database.Cursor;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import ru.ivanovpv.gorets.psm.ConversationActivity;
import ru.ivanovpv.gorets.psm.R;
import ru.ivanovpv.gorets.psm.view.NewConversationCursorRowAdapter;

/**
 * Created by Gorets on 14.05.14.
 */
public class HiddenConversationCursorRowAdapter extends NewConversationCursorRowAdapter {

    public HiddenConversationCursorRowAdapter(ConversationActivity activity, Cursor cursor) {
        super(activity, cursor);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        super.bindView(view, context, cursor);
    }

    @Override
    public View newView(final Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = super.newView(context, cursor, viewGroup);
        Animation animationUtils = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
        final TextView sentBodyTextHide = (TextView) view.findViewById(R.id.sentText);
        final TextView incomingBodyTextHide = (TextView) view.findViewById(R.id.incomingText);
        sentBodyTextHide.setAnimation(animationUtils);
        sentBodyTextHide.setVisibility(View.INVISIBLE);

        incomingBodyTextHide.setAnimation(animationUtils);
        incomingBodyTextHide.setVisibility(View.INVISIBLE);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Animation animationUtils = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
                        v.setAnimation(animationUtils);
                        sentBodyTextHide.setVisibility(View.VISIBLE);
                        incomingBodyTextHide.setAnimation(animationUtils);
                        incomingBodyTextHide.setVisibility(View.VISIBLE);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_OUTSIDE:
                    case MotionEvent.ACTION_MOVE:
                        Animation animationUtilsOut = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
                        sentBodyTextHide.setAnimation(animationUtilsOut);
                        sentBodyTextHide.setVisibility(View.INVISIBLE);
                        incomingBodyTextHide.setAnimation(animationUtilsOut);
                        incomingBodyTextHide.setVisibility(View.INVISIBLE);
                        break;
                    default:
                }
                return false;
            }
        });
        return view;
    }

}
