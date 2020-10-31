package com.flys.dico.utils;

public class FacebookUrl {
    private String baseUrl;
    private String params;
    private String extraParams;

    public FacebookUrl() {
    }

    public FacebookUrl(String baseUrl, String params) {
        this.baseUrl = baseUrl;
        this.params = params;
    }

    public FacebookUrl(String baseUrl, String params, String extraParams) {
        this.baseUrl = baseUrl;
        this.params = params;
        this.extraParams = extraParams;
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

    public String getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(String extraParams) {
        this.extraParams = extraParams;
    }

    @Override
    public String toString() {
        return "FacebookUrl{" +
                "baseUrl='" + baseUrl + '\'' +
                ", params='" + params + '\'' +
                ", extraParams='" + extraParams + '\'' +
                '}';
    }
}
