package ai.blw.mfd;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Limits top-level navigation without preventing third-party HTTPS resources. */
final class NavigationPolicy {
    private final Set<String> hosts = new HashSet<>();

    NavigationPolicy(String[] allowedHosts) {
        for (String host : allowedHosts) {
            hosts.add(host.toLowerCase(Locale.ROOT));
        }
    }

    boolean allows(String url) {
        URI uri = parseHttps(url);
        return uri != null && hosts.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    static boolean isHttps(String url) {
        return parseHttps(url) != null;
    }

    static String host(String url) {
        URI uri = parseHttps(url);
        return uri == null ? "" : uri.getHost();
    }

    // Diagnostics show only the network origin, never OAuth paths or parameters.
    static String destination(String url) {
        try {
            URI uri = new URI(url == null ? "" : url);
            String scheme = uri.getScheme();
            if (scheme == null) {
                return "unrecognized URL";
            }
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    && uri.getHost() != null) {
                return scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost()
                        + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
            }
            return scheme.toLowerCase(Locale.ROOT) + ":";
        } catch (URISyntaxException invalidUrl) {
            return "unrecognized URL";
        }
    }

    private static URI parseHttps(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return null;
            }
            return uri;
        } catch (URISyntaxException invalidUrl) {
            return null;
        }
    }
}
