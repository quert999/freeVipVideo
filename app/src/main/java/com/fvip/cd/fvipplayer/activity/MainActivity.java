package com.fvip.cd.fvipplayer.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
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
import com.fvip.cd.fvipplayer.utils.StringUtil;
import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cd on 2018/7/19.
 */

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebView webView,webViewIframe;
    private String url = "https://v.qq.com/";
    private List<PlaylistBean.PlatformlistBean> mLeftListData = new ArrayList<>();
    private List<PlaylistBean.ListBean> mRightListData = new ArrayList<>();
    private String platformVideoUrl = "";
    private WebSettings webSetting;
    private DrawerLayout drawerLayout;
    private ListView lvLeft;
    private ListView lvRight;
    private PlatformListAdapter mAdapter;
    private ChannelListAdapter channelListAdapter;
    private String furl;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        initView();
        initData();
    }

    private void initData() {
        PlaylistBean bean = new Gson().fromJson(listData, PlaylistBean.class);
        mLeftListData = bean.getPlatformlist();
        mRightListData = bean.getList();
        initLeftMenu(mLeftListData);
        initRightMenu(mRightListData);
    }

    private void initView() {
        progressBar = findViewById(R.id.progressbar);
        drawerLayout = findViewById(R.id.dl_layout);
        webView = findViewById(R.id.webview);
        webViewIframe = findViewById(R.id.webview_iframe);
        lvLeft = findViewById(R.id.lv_left);
        lvRight = findViewById(R.id.lv_right);
        drawerLayout = findViewById(R.id.dl_layout);

        initWebView();
    }

    private void initLeftMenu(final List<PlaylistBean.PlatformlistBean> mListData) {
        mAdapter = new PlatformListAdapter(MainActivity.this, R.layout.platform_list_item, mListData);
        lvLeft.setDivider(null);
        lvLeft.setAdapter(mAdapter);
        lvLeft.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {//如果此时抽屉窗口打开，就给他关闭
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }
                if (position == mListData.size() - 1){
//                    String cmd = "javascript:alert(document.getElementsByTagName('iframe')[0].innerHTML);";
//                    webView.loadUrl(cmd);
                    webView.loadUrl("javascript:window.local_obj.tap2Parse('<head>'+" +
                            "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    return;
                }
                loadUrl(mListData.get(position).getUrl());
                Toast.makeText(MainActivity.this, mListData.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initRightMenu(final List<PlaylistBean.ListBean> mListData) {
        channelListAdapter = new ChannelListAdapter(MainActivity.this, R.layout.platform_list_item, mListData);
        lvRight.setDivider(null);
        lvRight.setAdapter(channelListAdapter);
        lvRight.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {//如果此时抽屉窗口打开，就给他关闭
                    drawerLayout.closeDrawer(Gravity.RIGHT);
                }

                if (StringUtil.getCount(platformVideoUrl, "http") > 1) {
                    platformVideoUrl = platformVideoUrl.substring(platformVideoUrl.indexOf("=") + 1);
                }
                playVIP(mListData.get(position).getUrl(), platformVideoUrl);
                Toast.makeText(MainActivity.this, mListData.get(position).getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR_MR1)
    private void initWebView() {
        webSetting = webView.getSettings();
        webSetting.setDefaultTextEncodingName("utf-8");
        webSetting.setJavaScriptEnabled(true);  //必须保留
        webSetting.setDomStorageEnabled(true);//保留,否则无法播放优酷视频网页
        webView.setWebChromeClient(new MyWebChromeClient());//重写一下
        webView.setWebViewClient(new MyWebViewClient());
        webView.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
        loadUrl(url);


        WebSettings settings = webViewIframe.getSettings();
        settings.setDefaultTextEncodingName("utf-8");
        settings.setJavaScriptEnabled(true);  //必须保留
        settings.setDomStorageEnabled(true);//保留,否则无法播放优酷视频网页
        webViewIframe.setWebChromeClient(new WebChromeClient());//重写一下
        webViewIframe.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) return;
                        webViewIframe.loadUrl("javascript:window.local_obj.iframeParse('<head>'+" +
                                "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                    }
                },2000);
            }
        });
        webViewIframe.addJavascriptInterface(new InJavaScriptLocalObj(), "local_obj");
    }

    private void loadUrl(String url) {
        webView.loadUrl(url);
        Log.d(TAG, "loadUrl: " + url);
    }

    private void playVIP(String channelUrl, String url) {
        furl = channelUrl + url;
        loadUrl(furl);
        Log.d(TAG, "playVIP====" + furl);
    }


    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                webView.loadUrl("javascript:window.local_obj.showSource('<head>'+" +
                        "document.getElementsByTagName('html')[0].innerHTML+'</head>');");
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
//            Toast.makeText(MainActivity.this, "shouldOverrideUrlLoading: ==" + url, Toast.LENGTH_SHORT).show();
//这里进行url拦截

 /*            if (url != null && url.contains("vip")) {
                Toast.makeText(MainActivity.this, "url 拦截", Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, "url 拦截", Toast.LENGTH_SHORT).show();
                return true;
            }
*/
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


    String listData = "{\n" +
            "\"platformlist\": [\n" +
            "{\n" +
            "\"name\": \"腾讯视频\",\n" +
            "\"url\": \"https://v.qq.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"爱奇艺\",\n" +
            "\"url\": \"http://www.iqiyi.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"芒果\",\n" +
            "\"url\": \"https://www.mgtv.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"优酷\",\n" +
            "\"url\": \"https://www.youku.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"搜狐视频\",\n" +
            "\"url\": \"https://tv.sohu.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"乐视视频\",\n" +
            "\"url\": \"https://www.le.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"52影院\",\n" +
            "\"url\": \"http://www.52xsba.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4080新视觉影院\",\n" +
            "\"url\": \"http://www.yy4080.com/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"电影天堂\",\n" +
            "\"url\": \"http://www.btbtdy.net/\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"手动解析\",\n" +
            "\"url\": \"\"\n" +
            "}\n" +
            "],\n" +
            "\"list\": [\n" +
            "{\n" +
            "\"name\": \"原地址\",\n" +
            "\"url\": \"\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-8\",\n" +
            "\"url\": \"http://api.visaok.net/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-9\",\n" +
            "\"url\": \"http://api.xyingyu.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-10\",\n" +
            "\"url\": \"http://api.greatchina56.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-11\",\n" +
            "\"url\": \"http://jx.618g.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-12\",\n" +
            "\"url\": \"http://api.baiyug.vip/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-14\",\n" +
            "\"url\": \"http://api.xyingyu.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-15\",\n" +
            "\"url\": \"http://api.greatchina56.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-16\",\n" +
            "\"url\": \"http://api.baiyug.vip/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-17\",\n" +
            "\"url\": \"http://api.visaok.net/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-18\",\n" +
            "\"url\": \"http://jx.618g.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-22\",\n" +
            "\"url\": \"http://www.82190555.com/index/qqvod.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-24\",\n" +
            "\"url\": \"http://www.82190555.com/index/qqvod.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4.21-2\",\n" +
            "\"url\": \"http://qtv.soshane.com/ko.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4.21-3\",\n" +
            "\"url\": \"https://yooomm.com/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4.21-4\",\n" +
            "\"url\": \"http://www.82190555.com/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4.21-6\",\n" +
            "\"url\": \"http://www.85105052.com/admin.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"高端解析\",\n" +
            "\"url\": \"http://jx.vgoodapi.com/jx.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"六六视频\",\n" +
            "\"url\": \"http://qtv.soshane.com/ko.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"超清接口1_0\",\n" +
            "\"url\": \"http://www.52jiexi.com/tong.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"超清接口1_1\",\n" +
            "\"url\": \"http://www.52jiexi.com/yun.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"超清接口2\",\n" +
            "\"url\": \"http://jiexi.92fz.cn/player/vip.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"品优解析\",\n" +
            "\"url\": \"http://api.pucms.com/xnflv/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"无名小站\",\n" +
            "\"url\": \"http://www.82190555.com/index/qqvod.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"腾讯可用，百域阁视频\",\n" +
            "\"url\": \"http://api.baiyug.cn/vip/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"腾讯可用，线路三(云解析)\",\n" +
            "\"url\": \"http://jiexi.92fz.cn/player/vip.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"腾讯可用，金桥解析\",\n" +
            "\"url\": \"http://jqaaa.com/jx.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"线路四（腾讯暂不可用）\",\n" +
            "\"url\": \"http://api.nepian.com/ckparse/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"线路五\",\n" +
            "\"url\": \"http://aikan-tv.com/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"花园影视（可能无效）\",\n" +
            "\"url\": \"http://j.zz22x.com/jx/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"花园影视1\",\n" +
            "\"url\": \"http://j.88gc.net/jx/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"线路一(乐乐视频解析)\",\n" +
            "\"url\": \"http://www.662820.com/xnflv/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"1717ty\",\n" +
            "\"url\": \"http://1717ty.duapp.com/jx/ty.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"速度牛\",\n" +
            "\"url\": \"http://api.wlzhan.com/sudu/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"1\",\n" +
            "\"url\": \"http://17kyun.com/api.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"6\",\n" +
            "\"url\": \"http://014670.cn/jx/ty.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"8\",\n" +
            "\"url\": \"http://tv.x-99.cn/api/wnapi.php?id=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"10\",\n" +
            "\"url\": \"http://7cyd.com/vip/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"表哥解析\",\n" +
            "\"url\": \"http://jx.biaoge.tv/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"万能接口3\",\n" +
            "\"url\": \"http://vip.jlsprh.com/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"万能接口4\",\n" +
            "\"url\": \"https://api.daidaitv.com/index/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"万能接口6\",\n" +
            "\"url\": \"http://wwwhe1.177kdy.cn/4.php?pass=1&url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-5\",\n" +
            "\"url\": \"http://www.ckplayer.tv/kuku/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-6\",\n" +
            "\"url\": \"http://api.lvcha2017.cn/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-7\",\n" +
            "\"url\": \"http://www.aktv.men/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-13\",\n" +
            "\"url\": \"http://jx.reclose.cn/jx.php/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-19\",\n" +
            "\"url\": \"http://yun.baiyug.cn/vip/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-23\",\n" +
            "\"url\": \"http://api.baiyug.cn/vip/index.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-25\",\n" +
            "\"url\": \"http://2gty.com/apiurl/yun.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-26\",\n" +
            "\"url\": \"http://v.2gty.com/apiurl/yun.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"4.21-5\",\n" +
            "\"url\": \"http://jiexi.92fz.cn/player/vip.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"爱跟影院\",\n" +
            "\"url\": \"http://2gty.com/apiurl/yun.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-1\",\n" +
            "\"url\": \"http://www.82190555.com/index/qqvod.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-2\",\n" +
            "\"url\": \"http://jiexi.92fz.cn/player/vip.php?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-3\",\n" +
            "\"url\": \"http://api.wlzhan.com/sudu/?url=\"\n" +
            "},\n" +
            "{\n" +
            "\"name\": \"5月-4\",\n" +
            "\"url\": \"http://beaacc.com/api.php?url=\"\n" +
            "}\n" +
            "]\n" +
            "}";

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
                if (iframeSrc.contains("?url=")){
                    String url = iframeSrc.split("\\?url=")[1];
                    if (url != null){
                        if (url.contains("m3u8")){
                            Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                            intent.putExtra("videourl", url);
                            startActivity(intent);
                            detailBack = true;
                        }else {
                            if (!iframeSrc.startsWith("http")) return;
                            new Thread(){
                                @Override
                                public void run() {
                                    try {
                                        Document iframeDoc = Jsoup.connect(iframeSrc).get();
                                        Elements elementsIframe = iframeDoc.select("#player");
                                        if (elementsIframe.size() > 0){
                                            String iframeSrcTrue = elementsIframe.first().attr("src");
                                            if (!isFinishing() && html.equals(lastHtml) && iframeSrcTrue != null && iframeSrcTrue.contains("?url=")){
                                                String urlTrue = iframeSrcTrue.split("\\?url=")[1];
                                                if (urlTrue != null && urlTrue.contains("m3u8")){
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
        public void tap2Parse(String html){
            Elements elements = Jsoup.parse(html).select("video");
            if (elements.size() > 0){
                String url = elements.first().attr("src");
                if (url != null && url.startsWith("http")){
                    Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                    intent.putExtra("videourl", url);
                    startActivity(intent);
                    detailBack = true;
                }
            }else{
                Elements iframeElements = Jsoup.parse(html).select("iframe");
                if (iframeElements.size() > 0){
                    String iframeSrc = iframeElements.first().attr("src");
                    if (iframeSrc == null) return;
                    if (!iframeSrc.startsWith("http")) iframeSrc = "http://" + iframeSrc;
                    webViewIframe.loadUrl(iframeSrc);
                }
            }
        }

        @JavascriptInterface
        public void iframeParse(String html){
            Elements elements = Jsoup.parse(html).select("video");
            if (elements.size() > 0){
                String url = elements.first().attr("src");
                if (url != null && url.startsWith("http")){
                    Intent intent = new Intent(MainActivity.this, VideoDetailActivity.class);
                    intent.putExtra("videourl", url);
                    startActivity(intent);
                    detailBack = true;
                }
            }
        }
    }

}
