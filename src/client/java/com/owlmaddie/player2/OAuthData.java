package com.owlmaddie.player2;

public class OAuthData {
    String deviceCode;
    String userCode;
    String verificationUri;
    int interval;

    public OAuthData(String deviceCode, String userCode, String verificationUri,
            int interval) {
        this.deviceCode = deviceCode;
        this.userCode = userCode;
        this.verificationUri = verificationUri;
        this.interval = interval;
    }
}
