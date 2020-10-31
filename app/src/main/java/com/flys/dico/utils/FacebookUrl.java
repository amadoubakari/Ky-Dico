package com.flys.dico.utils;

public class FacebookUrl {
    private String baseUrl;
    private String params;

    public FacebookUrl() {
    }

    public FacebookUrl(String baseUrl, String params) {
        this.baseUrl = baseUrl;
        this.params = params;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}
