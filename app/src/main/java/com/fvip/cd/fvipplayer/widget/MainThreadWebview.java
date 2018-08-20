package com.fvip.cd.fvipplayer.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.webkit.WebView;

public class MainThreadWebview extends WebView{
    Handler handler = new Handler();
    public MainThreadWebview(Context context) {
        super(context);
    }

    public MainThreadWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MainThreadWebview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void loadUrl(final String url) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                MainThreadWebview.super.loadUrl(url);
            }
        });
    }
}
