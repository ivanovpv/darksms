package ru.ivanovpv.gorets.psm.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import ru.ivanovpv.gorets.psm.Me;
import ru.ivanovpv.gorets.psm.PSMActivity_;
import ru.ivanovpv.gorets.psm.R;

public class WidgetUpdateService extends Service {
    private static final String TAG = WidgetUpdateService.class.getName();

    public WidgetUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context=this.getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        //get number of unread messages
        int unreadMessages=Me.getMe().getMessageDAO().getUnreadMessagesCount(context);
        Log.i(TAG, "Unread messages=" + unreadMessages);
        for (int i=0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];

            //update unread messages
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.psm_widget_layout);
            if(unreadMessages > 0) {
                remoteViews.setViewVisibility(R.id.unreadMessages, View.VISIBLE);
                remoteViews.setTextViewText(R.id.unreadMessages, String.valueOf(unreadMessages));
            }
            else  //hide unread messages textView
                remoteViews.setViewVisibility(R.id.unreadMessages, View.GONE);

            // Register an onClickListener
            Intent newIntent = new Intent(context, PSMActivity_.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, newIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.psmWidget, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }

}
