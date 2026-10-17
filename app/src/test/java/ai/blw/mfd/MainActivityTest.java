package ai.blw.mfd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 30})
public final class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private WebView webView;
    private WebViewClient client;

    @Before
    public void setUp() {
        // The exported launcher must ignore arbitrary incoming URLs/extras.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://evil.example/"));
        intent.putExtra("url", "https://evil.example/");
        controller = Robolectric.buildActivity(MainActivity.class, intent).setup();
        activity = controller.get();
        webView = (WebView) ((FrameLayout) activity.findViewById(R.id.web_container)).getChildAt(0);
        client = shadowOf(webView).getWebViewClient();
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }

    @Test
    public void opensOnlyConfiguredPortalAndEnablesRequiredWebFeatures() {
        assertEquals(BuildConfig.START_URL, shadowOf(webView).getLastLoadedUrl());
        assertTrue(webView.getSettings().getJavaScriptEnabled());
        assertTrue(webView.getSettings().getDomStorageEnabled());
        assertFalse(webView.getSettings().getAllowFileAccess());
        assertFalse(webView.getSettings().getAllowContentAccess());
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, webView.getSettings().getMixedContentMode());
    }

    @Test
    public void containsMainFrameNavigationButAllowsSignIn() {
        assertFalse(client.shouldOverrideUrlLoading(webView, "https://login.microsoftonline.com/tenant/login"));
        assertTrue(client.shouldOverrideUrlLoading(webView, "https://evil.example/"));
        assertTrue(client.shouldOverrideUrlLoading(webView, "intent://external-app/"));
    }

    @Test
    public void failedMainFrameCannotRevealOldPageOnFinishedCallback() {
        client.onReceivedError(webView, request(true), null);
        client.onPageFinished(webView, BuildConfig.START_URL);
        assertEquals(View.VISIBLE, activity.findViewById(R.id.error_panel).getVisibility());
        assertEquals(View.INVISIBLE, activity.findViewById(R.id.web_container).getVisibility());
        assertEquals(View.INVISIBLE, activity.findViewById(R.id.progress).getVisibility());
    }

    @Test
    public void subresourceErrorsDoNotReplaceTheWholePage() {
        client.onReceivedError(webView, request(false), null);
        client.onReceivedHttpError(webView, request(false), response(404));
        assertEquals(View.GONE, activity.findViewById(R.id.error_panel).getVisibility());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.web_container).getVisibility());
    }

    @Test
    public void failedHttpResponseShowsStatusAndRecoversAtEntryPoint() {
        webView.loadUrl("https://login.microsoftonline.com/tenant/callback?code=synthetic-test-code");
        client.onReceivedHttpError(webView, request(true), response(503));
        assertTrue(((TextView) activity.findViewById(R.id.error_message)).getText().toString().contains("503"));
        activity.findViewById(R.id.retry).performClick();
        assertEquals(BuildConfig.START_URL, shadowOf(webView).getLastLoadedUrl());
        assertEquals(View.GONE, activity.findViewById(R.id.error_panel).getVisibility());
    }

    @Test
    public void pageStartedAlsoChecksDestinationsAndRedactsLocation() {
        client.onPageStarted(webView, "https://login.microsoftonline.com/tenant?code=synthetic-test-code", null);
        assertEquals("login.microsoftonline.com", ((TextView) activity.findViewById(R.id.location)).getText().toString());
        client.onPageStarted(webView, "https://evil.example/", null);
        assertEquals(View.VISIBLE, activity.findViewById(R.id.error_panel).getVisibility());
    }

    @Test
    @Config(sdk = 30)
    public void rendererCrashRequiresExplicitRecoveryAndCreatesANewWebView() {
        assertTrue(client.onRenderProcessGone(webView, null));
        assertEquals(0, ((FrameLayout) activity.findViewById(R.id.web_container)).getChildCount());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.error_panel).getVisibility());
        activity.findViewById(R.id.retry).performClick();
        WebView recovered = (WebView) ((FrameLayout) activity.findViewById(R.id.web_container)).getChildAt(0);
        assertNotNull(recovered);
        assertEquals(BuildConfig.START_URL, shadowOf(recovered).getLastLoadedUrl());
    }

    @Test
    public void certificateFailureIsCancelledAndHidesTheWebPage() {
        SslErrorHandler handler = ReflectionHelpers.callConstructor(SslErrorHandler.class);
        client.onReceivedSslError(webView, handler, null);
        assertTrue(shadowOf(handler).wasCancelCalled());
        assertFalse(shadowOf(handler).wasProceedCalled());
        assertEquals(View.VISIBLE, activity.findViewById(R.id.error_panel).getVisibility());
        assertEquals(View.INVISIBLE, activity.findViewById(R.id.web_container).getVisibility());
    }

    @Test
    @Config(sdk = 30)
    public void modernRequestCallbackAllowsConfiguredMainFrame() {
        assertFalse(client.shouldOverrideUrlLoading(webView, request(true)));
    }

    private static WebResourceRequest request(boolean mainFrame) {
        return new WebResourceRequest() {
            @Override public Uri getUrl() { return Uri.parse(BuildConfig.START_URL); }
            @Override public boolean isForMainFrame() { return mainFrame; }
            @Override public boolean isRedirect() { return false; }
            @Override public boolean hasGesture() { return false; }
            @Override public String getMethod() { return "GET"; }
            @Override public Map<String, String> getRequestHeaders() { return Collections.emptyMap(); }
        };
    }

    private static WebResourceResponse response(int status) {
        return new WebResourceResponse("text/html", "UTF-8", status, "Test response", Collections.emptyMap(), null);
    }
}
