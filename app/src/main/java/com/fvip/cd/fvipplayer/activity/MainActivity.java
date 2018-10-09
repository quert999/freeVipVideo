package com.fvip.cd.fvipplayer.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fvip.cd.fvipplayer.R;
import com.fvip.cd.fvipplayer.adapter.ChannelListAdapter;
import com.fvip.cd.fvipplayer.adapter.PlatformListAdapter;
import com.fvip.cd.fvipplayer.bean.PlaylistBean;
import com.fvip.cd.fvipplayer.utils.ADFilterTool;
import com.fvip.cd.fvipplayer.utils.FileReaderUtils;
import com.fvip.cd.fvipplayer.utils.StringUtil;
import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cd on 2018/7/19.
 */

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView webView;
    private String platformVideoUrl = "";
    private DrawerLayout drawerLayout;
    private ListView lvLeft;
    private ListView lvRight;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }

    }

    private void init() {
        initView();
        initData();
    }

    private void initData() {
        String listData = FileReaderUtils.readFromAsset("urls.json", this);
        PlaylistBean bean = new Gson().fromJson(listData, PlaylistBean.class);
        List<PlaylistBean.PlatformlistBean> mLeftListData = bean.getPlatformlist();
        List<PlaylistBean.ListBean> mRightListData = bean.getList();
        initLeftMenu(mLeftListData);
        initRightMenu(mRightListData);
    }

    private void initView() {
        progressBar = findViewById(R.id.progressbar);
        drawerLayout = findViewById(R.id.dl_layout);
        webView = findViewById(R.id.webview);
        lvLeft = findViewById(R.id.lv_left);
        lvRight = findViewById(R.id.lv_right);
        drawerLayout = findViewById(R.id.dl_layout);

        initWebView();
    }

    private void initLeftMenu(final List<PlaylistBean.PlatformlistBean> mListData) {
        PlatformListAdapter mAdapter = new PlatformListAdapter(MainActivity.this, R.layout.platform_list_item, mListData);
        lvLeft.setDivider(null);
        lvLeft.setAdapter(mAdapter);
        lvLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (drawerLayout.isDrawerOpen(Gravity.START)) {//如果此时抽屉窗口打开，就给他关闭
                    drawerLayout.closeDrawer(Gravity.START);
                }
                if (position == mListData.size() - 1) {
                    webView.loadUrl("javascript:window.local_obj.tap2Parse();");
                    return;
                }
                loadUrl(mListData.get(position).getUrl());
                Toast.makeText(MainActivity.this, mListData.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initRightMenu(final List<PlaylistBean.ListBean> mListData) {
        ChannelListAdapter channelListAdapter = new ChannelListAdapter(MainActivity.this, R.layout.platform_list_item, mListData);
        lvRight.setDivider(null);
        lvRight.setAdapter(channelListAdapter);
        lvRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (drawerLayout.isDrawerOpen(Gravity.END)) {//如果此时抽屉窗口打开，就给他关闭
                    drawerLayout.closeDrawer(Gravity.END);
                }

                if (StringUtil.getCount(platformVideoUrl, "http") > 1) {
                    platformVideoUrl = platformVideoUrl.substring(platformVideoUrl.indexOf("=") + 1);
                }
                playVIP(mListData.get(position).getUrl(), platformVideoUrl);
                Toast.makeText(MainActivity.this, mListData.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initWebView() {
        WebSettings webSetting = webView.getSettings();
        webSetting.setDefaultTextEncodingName("utf-8");
        webSetting.setJavaScriptEnabled(true);  //必须保留
        webSetting.setDomStorageEnabled(true);//保留,否则无法播放优酷视频网页
        webView.setWebChromeClient(new MyWebChromeClient());//重写一下
        webView.setWebViewClient(new MyWebViewClient());
        webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        String url = "https://www.huya.com/";
        loadUrl(url);

    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
        Log.d(TAG, "loadUrl: " + url);
    }

    private void playVIP(String channelUrl, String url) {
        String furl = channelUrl + url;
        loadUrl(furl);
        Log.d(TAG, "playVIP====" + furl);
    }


    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                webView.loadUrl("javascript:window.local_obj.showSource('<head>'+" +
                        "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                webView.loadUrl("javascript:if(setVideoClick&&typeof(setVideoClick)==\"function\"){" +
                        "}else{" +
                        "function setVideoClick(){" +
                        "var videos = document.getElementsByTagName(\"video\");" +
                        "for (var i=0;i<videos.length;i++)" +
                        "{" +
                        "videos[i].addEventListener(\"play\",function(){" +
                        "this.pause();" +
                        "window.local_obj.goPlay(this.src);" +
                        "})" +
                        "}" +
                        "}" +
                        "setVideoClick();" +
                        "}");
                progressBar.setVisibility(View.GONE);//加载完网页进度条消失
            } else {
                progressBar.setVisibility(View.VISIBLE);//开始加载网页时显示进度条
                progressBar.setProgress(newProgress);//设置进度值
            }

        }


    }


    // 监听 所有点击的链接，如果拦截到我们需要的，就跳转到相对应的页面。
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "shouldOverrideUrlLoading: current page===" + url);
            platformVideoUrl = url;
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            view.getSettings().setJavaScriptEnabled(true);
//            super.onPageFinished(view, url);
            view.loadUrl("javascript:function setTop(){document.querySelector('.ad-footer').style.display=\"none\";}setTop();");
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            url = url.toLowerCase();
            if (!ADFilterTool.hasAd(MainActivity.this, url)) {
                return super.shouldInterceptRequest(view, url);
            } else {
                return new WebResourceResponse(null, null, null);
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //这是一个监听用的按键的方法，keyCode 监听用户的动作，如果是按了返回键，同时Webview要返回的话，WebView执行回退操作，因为mWebView.canGoBack()返回的是一个Boolean类型，所以我们把它返回为true
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (detailBack) {
            webView.goBack();
            detailBack = false;
        }
        lastHtml = null;
    }

    String lastHtml;
    boolean detailBack;

    class InJavaScriptLocalObj {
        @JavascriptInterface
        public void showSource(final String html) {
            if (html == null || html.equals(lastHtml)) return;
            lastHtml = html;
            Elements elements = Jsoup.parse(html).select("iframe");
            if (elements.size() > 0) {
                final String iframeSrc = elements.first().attr("src");
                if (iframeSrc.contains("?url=")) {
                    String url = iframeSrc.split("\\?url=")[1];
                    if (url != null) {
                        if (url.contains("m3u8")) {
                            Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                            intent.putExtra("videourl", url);
                            startActivity(intent);
                            detailBack = true;
                        } else {
                            if (!iframeSrc.startsWith("http")) return;
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        Document iframeDoc = Jsoup.connect(iframeSrc).get();
                                        Elements elementsIframe = iframeDoc.select("#player");
                                        if (elementsIframe.size() > 0) {
                                            String iframeSrcTrue = elementsIframe.first().attr("src");
                                            if (!isFinishing() && html.equals(lastHtml) && iframeSrcTrue != null && iframeSrcTrue.contains("?url=")) {
                                                String urlTrue = iframeSrcTrue.split("\\?url=")[1];
                                                if (urlTrue != null && urlTrue.contains("m3u8")) {
                                                    Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                                                    intent.putExtra("videourl", urlTrue);
                                                    startActivity(intent);
                                                    detailBack = true;
                                                }
                                            }
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                    }
                }
            }
        }

        @JavascriptInterface
        public void tap2Parse() {
            FileReaderUtils.checkFile();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.saveWebArchive(new File(FileReaderUtils.FILE_PATH).getAbsolutePath());
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String savedHTML = FileReaderUtils.readFromFile(FileReaderUtils.FILE_PATH);
                            savedHTML = savedHTML.replaceAll("=\n", "");
                            savedHTML = savedHTML.replaceAll("=3D", "=");
                            savedHTML = savedHTML.replaceAll("/=", "=");
                            savedHTML = savedHTML.replaceAll("\n", "");
                            Pattern pattern = Pattern.compile("<html([.\\s\\S]+?)</html>");
                            Matcher matcher = pattern.matcher(savedHTML);
                            while (matcher.find()) {
                                String html = matcher.group();
                                Elements videos = Jsoup.parse(html).select("video");
                                if (videos != null && videos.size() > 0) {
                                    String url = videos.first().attr("src");
                                    if (url != null && url.startsWith("http")) {
                                        Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                                        intent.putExtra("videourl", url);
                                        startActivity(intent);
                                        detailBack = true;
                                        return;
                                    }
                                }
                            }
                        }
                    }.start();
                }
            });
        }

        @JavascriptInterface
        public void goPlay(String url){
            if (url != null && url.startsWith("http")){
                Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                intent.putExtra("videourl", url);
                startActivity(intent);
            }
        }
    }
}
