package ai.blw.mfd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NavigationPolicyTest {
    private final NavigationPolicy policy = new NavigationPolicy(new String[] {
            "rhib-control.blw.ai", "bluewater-ai.cloudflareaccess.com", "login.microsoftonline.com"
    });

    @Test
    public void permitsPortalAndObservedSignInRedirects() {
        assertTrue(policy.allows("https://rhib-control.blw.ai/"));
        assertTrue(policy.allows("https://RHIB-CONTROL.BLW.AI:443/status?view=helm#controls"));
        assertTrue(policy.allows("https://bluewater-ai.cloudflareaccess.com/cdn-cgi/access/login/rhib-control.blw.ai"));
        assertTrue(policy.allows("https://login.microsoftonline.com/example/oauth2/authorize?state=test"));
    }

    @Test
    public void rejectsHostConfusionCredentialsAndOtherOrigins() {
        String[] rejected = {
                "https://rhib-control.blw.ai.evil.example/",
                "https://evil.example/rhib-control.blw.ai",
                "https://rhib-control.blw.ai@evil.example/",
                "https://evil.example@rhib-control.blw.ai/",
                "https://rhib-control.blw.ai:8443/",
                "https://rhib-control.blw.ai./",
                "https://subdomain.rhib-control.blw.ai/",
                "https://rhib-control.blw.ai\\@evil.example/"
        };
        for (String url : rejected) {
            assertFalse(url, policy.allows(url));
        }
    }

    @Test
    public void rejectsNonHttpsAndMalformedInput() {
        String[] rejected = {
                null, "", "//rhib-control.blw.ai/", "https:", "https:///rhib-control.blw.ai",
                "http://rhib-control.blw.ai/", "file:///sdcard/page.html", "content://example/page",
                "javascript:alert(1)", "data:text/html,hello", "intent://example/#Intent;end",
                "https://rhib-control.blw.ai/invalid space"
        };
        for (String url : rejected) {
            assertFalse(url, policy.allows(url));
            assertFalse(url, NavigationPolicy.isHttps(url));
        }
    }

    @Test
    public void blockedDestinationOmitsSecretsAndOpaquePayloads() {
        assertEquals("https://example.com:8443", NavigationPolicy.destination(
                "https://test-user:test-password@example.com:8443/private?code=test-code#test-token"));
        assertEquals("intent:", NavigationPolicy.destination("intent://test-secret/#Intent;end"));
        assertEquals("data:", NavigationPolicy.destination("data:text/plain,test-secret"));
        assertEquals("unrecognized URL", NavigationPolicy.destination("invalid test-secret"));
        assertEquals("unrecognized URL", NavigationPolicy.destination(null));
    }

    @Test
    public void everyAppAllowsTheRequestedSitesAndObservedRhibRedirect() {
        NavigationPolicy compiled = new NavigationPolicy(BuildConfig.NAVIGATION_HOSTS);
        for (String host : new String[] {"rhib-control.blw.ai", "rhib.blw.ai",
                "dreadnought.blw.ai", "rhib-grafana.blw.ai"}) {
            assertTrue(host, compiled.allows("https://" + host + "/"));
            assertFalse(host, compiled.allows("http://" + host + "/"));
            assertFalse(host, compiled.allows("https://" + host + ".evil.example/"));
        }
    }

    @Test
    public void compiledPortalIsIncludedInItsOwnPolicy() {
        assertTrue(new NavigationPolicy(BuildConfig.NAVIGATION_HOSTS).allows(BuildConfig.START_URL));
    }
}
