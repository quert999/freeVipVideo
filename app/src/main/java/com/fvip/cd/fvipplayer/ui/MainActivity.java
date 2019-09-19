package com.fvip.cd.fvipplayer.ui;

import android.Manifest;
import android.arch.lifecycle.Lifecycle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.fvip.cd.fvipplayer.R;
import com.fvip.cd.fvipplayer.adapter.ChannelListAdapter;
import com.fvip.cd.fvipplayer.adapter.PlatformListAdapter;
import com.fvip.cd.fvipplayer.bean.PlaylistBean;
import com.google.gson.Gson;
import com.uber.autodispose.AutoDispose;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by cd on 2018/7/19.
 */

public class MainActivity extends AppCompatActivity {
    private MaterialDialog pd;
    String copyRightTheftUrl = "https://660e.com/?url=";
    String copyRightTheftDomain = "https://660e.com";
    private static final String TAG = "MainActivity";
    private WebView webView;
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

        MaterialDialog.Builder builder = new MaterialDialog.Builder(this);
        builder.progress(true, 0);
        builder.canceledOnTouchOutside(false);
        builder.cancelable(false);
        builder.content("请稍候");
        pd = builder.build();
    }

    private void initData() {
        String listData = readFromAsset("urls.json");
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
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //获取剪贴板管理器
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    // 创建普通字符型ClipData
                    ClipData mClipData = ClipData.newPlainText("Label", webView.getUrl());
                    // 将ClipData内容放到系统剪贴板里。
                    cm.setPrimaryClip(mClipData);
                    showToast("网址已复制到剪切板中");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

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
                String title = webView.getTitle();
                if (position == mListData.size()-2) {
                    String url = copyRightTheftUrl + webView.getUrl();
                    pd.show();
                    Observable.just(title + "`" + url)
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String u) throws Exception {
                                    String t = u.split("`")[0];
                                    u = u.split("`")[1];
                                    Document document = Jsoup.parse(new URL(u), 15000);
                                    Element element = document.getElementsByTag("iframe").first();
                                    if (element == null) {
                                        return "error";
                                    }
                                    String url = element.attr("src");
                                    if (!url.startsWith("/")){
                                        document = Jsoup.parse(new URL(url),15000);
                                        return "error";
                                    }
                                    document = Jsoup.parse(new URL(copyRightTheftDomain + url), 15000);
                                    Matcher matcher = Pattern.compile("var vid = '(.+)'").matcher(document.toString());
                                    if (matcher.find()) {
                                        String m3u8 = matcher.group(1);
                                        return t + "`" + m3u8;
                                    }
                                    return "error";
                                }
                            })
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(AutoDispose.<String>autoDisposable(AndroidLifecycleScopeProvider.from(MainActivity.this, Lifecycle.Event.ON_DESTROY)))
                            .subscribe(new Consumer<String>() {
                                @Override
                                public void accept(String s) throws Exception {
                                    pd.dismiss();
                                    if ("error".equals(s)) {
                                        showToast("未找到播放源");
                                    } else {
                                        String u = s.split("`")[1];
                                        String t = s.split("`")[0];
                                        Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                                        intent.putExtra("url", u);
                                        intent.putExtra("title", t);
                                        startActivity(intent);
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    pd.dismiss();
                                    showToast("网络连接不稳定，请重试");
                                }
                            });
                    return;
                }

                if (position == mListData.size()-1) {
                    webView.loadUrl("javascript:HTMLOUT.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
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

                copyRightTheftUrl = mListData.get(position).getUrl();
                copyRightTheftDomain = copyRightTheftUrl.substring(0,copyRightTheftUrl.lastIndexOf("/"));
                webView.loadUrl(copyRightTheftUrl + webView.getUrl());
                Toast.makeText(MainActivity.this, mListData.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        try {
            String appCachePath = getApplication().getApplicationContext().getCacheDir().getAbsolutePath();
            webView.getSettings().setAppCachePath(appCachePath);
        } catch (Exception e) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Method method = webView.getSettings().getClass().getDeclaredMethod("setMixedContentMode", int.class);
                method.setAccessible(true);
                method.invoke(webView.getSettings(), WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setUseWideViewPort(getIntent().getBooleanExtra("isSupportZoom", true));
        webView.getSettings().setAllowContentAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webView.getSettings().setDisplayZoomControls(false);
            webView.getSettings().setSupportZoom(getIntent().getBooleanExtra("isSupportZoom", true));
            webView.getSettings().setBuiltInZoomControls(true);
        }

        webView.setWebChromeClient(new MyWebChromeClient());//重写一下
        webView.setWebViewClient(new MyWebViewClient());
        webView.addJavascriptInterface(new CustomScriptInterface(), "HTMLOUT");

        String url = "https://v.qq.com/";
        loadUrl(url);

    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
    }

    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
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
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return super.shouldInterceptRequest(view, url);
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
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pd.isShowing()) {
            pd.dismiss();
        }
    }


    public String readFromAsset(String path) {
        InputStreamReader reader = null;
        StringWriter writer = new StringWriter();
        try {
            reader = new InputStreamReader(getAssets().open(path));
            //将输入流写入输出流
            char[] buffer = new char[1024];
            int n;
            while (-1 != (n = reader.read(buffer))) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        //返回转换结果
        return writer.toString();
    }

    class CustomScriptInterface {
        @JavascriptInterface
        public void processHTML(final String html) {
            if (pd.isShowing()) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFinishing()) return;
                    pd.show();
                    Observable.just(html)
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    String url;
                                    Document document = Jsoup.parse(s);
                                    Element video = document.getElementsByTag("video").first();
                                    if (video != null){
                                        url = video.attr("src");
                                        if (url.startsWith("//")){
                                            url = "http:" + url;
                                        }
                                        return url;
                                    }
                                    url = document.getElementsByTag("iframe").first().attr("src");
                                    // 有些视频可以直接获取到这种格式，else里走这种格式 https://www.heimijx.com/jx/api/?url=https://film.sohu.com/album/999486.html
                                    // 示例：/m3u8.php?url=https://cn3.78love.cn/hls/20181127/895b494e08a7630d7a66d7d8a584d275/1543270542/index.m3u8
                                    if (url.startsWith("/m3u8.php?url=")){
                                        url = url.replace("/m3u8.php?url=","");
                                        return url;
                                    }else{
                                        return "error`" + url;
                                    }

                                }
                            })
                            .subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .as(AutoDispose.<String>autoDisposable(AndroidLifecycleScopeProvider.from(MainActivity.this, Lifecycle.Event.ON_DESTROY)))
                            .subscribe(new Consumer<String>() {
                                @Override
                                public void accept(String s) throws Exception {
                                    pd.dismiss();
                                    if (s.contains("error`")){
                                        showToast("需要跳转iframe内网页再获取一次");
                                        webView.loadUrl(s.split("`")[1]);
                                    }else {
                                        Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                                        intent.putExtra("url", s);
                                        startActivity(intent);
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    pd.dismiss();
                                }
                            });
                }
            });
        }
    }
}
