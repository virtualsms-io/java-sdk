package io.virtualsms.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Manual browser-registration session result. Only {@code start} is in scope
 * for v2.0.0 (the drive/stop/viewer tools are gated behind an invite-only
 * beta flag and out of scope — see the SDK spec appendix).
 */
public final class Sessions {

    private Sessions() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimelineEvent {
        public String at;
        public String event;
        public String detail;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BrowserSessionResult {
        public String id;
        public String status;
        public String serviceName;
        public String countryCode;
        public String deviceMode;
        public Boolean withProxy;
        // The backend returns only viewer_url (our own proxied live-viewer
        // link); it never sends a raw upstream debug URL. Never synthesize one.
        public String viewerUrl;
        public String targetUrl;
        public String orderId;
        public String phoneNumber;
        public List<TimelineEvent> timeline;
    }
}
