package ai.blw.mfd;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final NavigationPolicy navigation = new NavigationPolicy(BuildConfig.NAVIGATION_HOSTS);
    private WebView webView;
    private FrameLayout webContainer;
    private View progress;
    private View errorPanel;
    private TextView errorMessage;
    private Button back;
    private boolean loadFailed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webContainer = findViewById(R.id.web_container);
        progress = findViewById(R.id.progress);
        errorPanel = findViewById(R.id.error_panel);
        errorMessage = findViewById(R.id.error_message);
        back = findViewById(R.id.back);
        back.setOnClickListener(view -> navigateBack());
        findViewById(R.id.home).setOnClickListener(view -> openHome());
        findViewById(R.id.reload).setOnClickListener(view -> {
            if (webView == null || loadFailed) {
                openHome();
            } else {
                webView.reload();
            }
        });
        findViewById(R.id.info).setOnClickListener(view -> showInfo());
        Button retry = findViewById(R.id.retry);
        retry.setText(getString(R.string.open_home, getString(R.string.app_name)));
        retry.setOnClickListener(view -> openHome());
        openHome();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private boolean createWebView() {
        // Some OEM images ship without a usable provider. Keep the native
        // recovery screen available instead of crashing the launcher app.
        try {
            webView = new WebView(this);
        } catch (RuntimeException missingProvider) {
            showError(getString(R.string.unavailable_webview));
            return false;
        }
        webContainer.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptCookie(true);
        // Cloudflare/Microsoft redirects are top-level navigations. Do not
        // enable third-party cookies unless an observed login flow requires it.
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.setWebViewClient(new PortalClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int percent) {
                progress.setVisibility(percent == 100 || loadFailed ? View.INVISIBLE : View.VISIBLE);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message result) {
                // Popups need their own navigation policy and lifecycle. Keep
                // this first version single-window and explain the limitation.
                showBlockedLink(getString(R.string.blocked_popup));
                return false;
            }
        });
        return true;
    }

    private void openHome() {
        if (webView == null && !createWebView()) {
            return;
        }
        if (!navigation.allows(BuildConfig.START_URL)) {
            showError(getString(R.string.blocked_link));
            return;
        }
        // Recovery always performs a fresh GET of the configured entry point.
        // Never replay a failed form submission or an OAuth callback.
        webView.stopLoading();
        beginLoad();
        webView.loadUrl(BuildConfig.START_URL);
    }

    private void beginLoad() {
        loadFailed = false;
        errorPanel.setVisibility(View.GONE);
        webContainer.setVisibility(View.VISIBLE);
        progress.setVisibility(View.VISIBLE);
        updateBack();
    }

    private void showError(String message) {
        loadFailed = true;
        webContainer.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.INVISIBLE);
        errorMessage.setText(message);
        errorPanel.setVisibility(View.VISIBLE);
        updateBack();
    }

    private void showBlockedLink(String message) {
        if (BuildConfig.DEBUG) {
            Log.i("MfdNavigation", message);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.blocked_title)
                .setMessage(message)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private void updateBack() {
        back.setEnabled(webView != null && webView.canGoBack());
    }

    private void navigateBack() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            navigateBack();
        } else {
            super.onBackPressed();
        }
    }

    // API 26+ is guarded below; older devices still report their user agent.
    @SuppressLint("WebViewApiAvailability")
    private void showInfo() {
        String provider = getString(R.string.provider_unknown);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && webView != null) {
            PackageInfo info = WebView.getCurrentWebViewPackage();
            if (info != null) {
                provider = info.packageName + " " + info.versionName;
            }
        }
        String userAgent = webView == null ? "" : webView.getSettings().getUserAgentString();
        TextView details = new TextView(this);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        details.setPadding(padding, padding, padding, padding);
        details.setTextIsSelectable(true);
        details.setText(getString(R.string.info_body, BuildConfig.VERSION_NAME,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT, provider, BuildConfig.START_URL, userAgent));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(details);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.info_title, getString(R.string.app_name)))
                .setView(scroll)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            webView.onPause();
            CookieManager.getInstance().flush();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        destroyWebView();
        super.onDestroy();
    }

    private void destroyWebView() {
        if (webView != null) {
            webContainer.removeView(webView);
            webView.destroy();
            webView = null;
        }
    }

    private final class PortalClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return checkNavigation(url, true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return checkNavigation(request.getUrl().toString(), request.isForMainFrame());
        }

        private boolean checkNavigation(String url, boolean mainFrame) {
            if (mainFrame ? navigation.allows(url) : NavigationPolicy.isHttps(url)) {
                return false;
            }
            if (mainFrame) {
                showBlockedLink(getString(R.string.blocked_destination, NavigationPolicy.destination(url)));
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // shouldOverrideUrlLoading does not cover every navigation (POSTs
            // in particular). Stop unexpected destinations here as well.
            if (!navigation.allows(url)) {
                view.stopLoading();
                showError(getString(R.string.blocked_destination, NavigationPolicy.destination(url)));
                return;
            }
            beginLoad();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.INVISIBLE);
            updateBack();
            // Do not reveal the web surface here: WebView also calls this after
            // failed loads, and an error must not leave old controls visible.
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame()) {
                showError(getString(R.string.network_error));
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
            if (request.isForMainFrame()) {
                showError(getString(R.string.http_error, response.getStatusCode()));
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            showError(getString(R.string.tls_error));
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            dontResend.sendToTarget();
            openHome();
        }

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            destroyWebView();
            showError(getString(R.string.renderer_error));
            return true;
        }
    }
}
