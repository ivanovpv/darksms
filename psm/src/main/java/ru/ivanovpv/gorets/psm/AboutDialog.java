/*
 * Copyright (c) Ivanov Pavel (ivanovpv@gmail.com), Egor Sarnavsky (egoretss@gmail.com) and Alexander Laschenko 2012-2013. All Rights Reserved.
 *    $Author: ivanovpv $
 *    $Rev: 408 $
 *    $LastChangedDate: 2013-11-07 11:47:05 +0400 (Чт, 07 ноя 2013) $
 *    $URL: https://subversion.assembla.com/svn/ivanovpv/trunk/src/ru/ivanovpv/gorets/psm/AboutDialog.java $
 */

package ru.ivanovpv.gorets.psm;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

public class AboutDialog extends Dialog implements View.OnClickListener
{
    private Activity activity;
    private boolean isMore;

    public AboutDialog(Activity activity) {
        super(activity);
        this.activity=activity;
        isMore=true;
    }

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        this.setContentView(R.layout.about_dialog);
        TextView tv=(TextView)findViewById(R.id.version);
        String versionInfo;
        versionInfo=activity.getString(R.string.versionPSM)+Me.getVersionName(activity);
        if(Me.DEBUG)
            versionInfo=versionInfo+"D";
        if(Me.FREE)
            versionInfo=versionInfo+"F";
        if(Me.ENABLE_ACRA)
            versionInfo=versionInfo+"A";
        if(Me.TEST)
            versionInfo=versionInfo+"T";
        if(Me.MILITARY)
            versionInfo=versionInfo+"M";
        if(Me.ENABLE_MARKET)
            versionInfo=versionInfo+"P";
        tv.setText(versionInfo);
        this.setTitle(R.string.about);
		Button button = (Button) findViewById(R.id.ok);
		button.setOnClickListener(this);
        /*button=(Button) findViewById(R.id.showEula);
        button.setOnClickListener(this);*/
        button=(Button )findViewById(R.id.other);
        button.setOnClickListener(this);
        this.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.psm);
	}

    @Override
    public void onStart() {
        TextView tv=(TextView ) findViewById(R.id.more);
        tv.setOnClickListener(this);
        inflateMoreOrLess();
    }

	public void onClick(View v) {
        if(v.getId()==R.id.more)
        {
            isMore=!isMore;
            inflateMoreOrLess();
            return;
        }
        /*else if(v.getId()==R.id.showEula)
            Eula.show(activity);*/
        else if(v.getId()==R.id.other)
        {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(activity.getString(R.string.otherProducts)));
            activity.startActivity(marketIntent);
            return;
        }
        activity=null;
        this.dismiss();
	}

    private void inflateMoreOrLess() {
        TableRow tr=(TableRow)findViewById(R.id.creditsRow);
        TextView tv=(TextView ) findViewById(R.id.more);
        tr.removeAllViews();
        if(!isMore)
        {
            WebView webView=new WebView(activity);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(activity.getString(R.string.creditsURL));
            tr.addView(webView);
/*            TextView moreTV=new TextView(activity);
            StringBuilder sb=new StringBuilder();
            try
            {
                InputStreamReader isr=new InputStreamReader(activity.getResources().getAssets().open("FullCredits.html"));
                int ch;
                do
                {
                    ch=isr.read();
                    if(ch!=-1)
                        sb.append((char )ch);
                }
                while(ch!=-1);
                isr.close();
            }
            catch(Exception e)
            {
                Log.v(TAG, "Error reading FullCredits.html", e);
            }
//            moreTV.setAutoLinkMask(Linkify.WEB_URLS);
            moreTV.setWidth(LinearLayout.LayoutParams.FILL_PARENT);
            moreTV.setText(Html.fromHtml(sb.toString()));
            tr.addView(moreTV);*/
            tv.setText(Html.fromHtml(activity.getString(R.string.lessURL)));
        }
        else
        {
            tv.setText(Html.fromHtml(activity.getString(R.string.moreURL)));
        }
        return;
    }
}
