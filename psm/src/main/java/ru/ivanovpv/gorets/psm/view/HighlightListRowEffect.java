package ru.ivanovpv.gorets.psm.view;

/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com) and Egor Sarnavsky (egoretss@gmail.com) 2013.
 *   $Author: ivanovpv $
 *   $Rev: 246 $
 *   $LastChangedDate: 2013-06-19 10:58:01 +0400 (Ср, 19 июн 2013) $
 *   $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/view/HighlightListRowEffect.java $
 */

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import ru.ivanovpv.gorets.psm.R;

/**
 * Created with IntelliJ IDEA.
 * User: Gorets
 * Date: 08.02.13
 * Time: 21:09
 * To change this template use File | Settings | File Templates.
 */
public class HighlightListRowEffect implements View.OnTouchListener {
    public boolean onTouch(View view, MotionEvent motionEvent) {
/*        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.setBackgroundResource(R.drawable.select_row);
                break;
            case MotionEvent.ACTION_UP:
                view.setBackgroundColor(Color.TRANSPARENT);
                break;
            case MotionEvent.ACTION_CANCEL:
                view.setBackgroundColor(Color.TRANSPARENT);
                break;
        }  */

        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE){
            view.setBackgroundResource(R.drawable.select_row);
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            view.setBackgroundColor(Color.TRANSPARENT);
        }
        return false;
    }
}
