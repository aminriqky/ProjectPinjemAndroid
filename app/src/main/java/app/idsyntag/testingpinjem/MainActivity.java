package app.idsyntag.testingpinjem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    WebView mWebView;
    private SwipeRefreshLayout refreshLayout;
    String URL = "http://testingpinjem.idsyntag.com/";
    ProgressBar bar;

    @SuppressLint({"SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWebView = (WebView) findViewById(R.id.web_view);

        bar=(ProgressBar) findViewById(R.id.progress);

        if (Build.VERSION.SDK_INT >= 19) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        mWebView.getSettings().setAllowFileAccess( true );
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setAppCacheEnabled( true );
        mWebView.getSettings().setAppCacheMaxSize( 8 * 1024 * 1024 );
        mWebView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        mWebView.getSettings().setCacheMode( WebSettings.LOAD_DEFAULT );
        mWebView.getSettings().setDatabaseEnabled(true);
        if ( !isNetworkAvailable() )  //offline
            mWebView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ONLY );

        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                //ProgressBar akan Terlihat saat Halaman Dimuat
                bar.setVisibility(View.VISIBLE);
                bar.setProgress(newProgress);
                if(newProgress == 100){
                    //ProgressBar akan Menghilang setelah Valuenya mencapai 100%
                    bar.setVisibility(View.GONE);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.loadUrl(URL);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        refreshLayout.setOnRefreshListener(this);
    }

    @Override
    public void onRefresh() {
        mWebView.reload();
        refreshLayout.setRefreshing(false);
    }

    //method ini perlu ditambahkan agar saat halaman web diload tetap diload di aplikasi, tidak diload di webbrowseer
    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            view.loadUrl(url);
            return true;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            final String mime = URLConnection.guessContentTypeFromName(url);
            if (mime == null || !mime.startsWith("image")) {
                return super.shouldInterceptRequest(view, url);
            }
            try {
                final Bitmap image = Glide.with(view).asBitmap().diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).load(url).submit().get();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (mime.endsWith("jpeg")) {
                    image.compress(Bitmap.CompressFormat.JPEG, 30, out);
                } else if (mime.endsWith("png")) {
                    image.compress(Bitmap.CompressFormat.PNG, 30, out);
                } else {
                    return super.shouldInterceptRequest(view, url);
                }
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                return new WebResourceResponse(mime, "UTF-8", in);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("Unable to load image", String.valueOf(e));
                return super.shouldInterceptRequest(view, url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(MainActivity.this, "Error! " + description, Toast.LENGTH_SHORT).show();
            //Clearing the WebView
            try {
                view.stopLoading();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                view.clearView();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (view.canGoBack()) {
                view.goBack();
            }
            String ErrorPagePath = "file:///android_asset/error.html";
            view.loadUrl(ErrorPagePath);

            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }

    //tambahkan method ini agar saat tombol back ditekan, tidak keluar dari aplikasi tapi kembali ke halaman sebelumnya
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()){
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE );
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void onDestroy() {
        super.onDestroy();
        mWebView.removeJavascriptInterface("xx");
        mWebView.getSettings().setJavaScriptEnabled(false);
        mWebView.loadUrl("about:blank");
        mWebView.loadDataWithBaseURL(null, "", "text/html", "uft-8", null);
        ViewParent parent = mWebView.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(mWebView);
        }
        mWebView.clearHistory();
        mWebView.clearView();
        mWebView.removeAllViews();
        mWebView.destroy();
    }
}