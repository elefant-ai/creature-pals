package com.owlmaddie.player2.auth;

public class OAuthData {
    String deviceCode;
    String userCode;
    String verificationUri;
    String verificationUriComplete;

    int interval;

    public OAuthData(String deviceCode, String userCode, String verificationUri,
            int interval, String verificationUriComplete) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.interval = interval;
        this.verificationUriComplete = verificationUriComplete;
    }
}
