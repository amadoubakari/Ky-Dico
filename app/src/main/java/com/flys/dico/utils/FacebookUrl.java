package com.flys.dico.utils;

public class FacebookUrl {
    private String baseUrl;
    private String hash;
    private String ext;

    public FacebookUrl() {
    }

    public FacebookUrl(String baseUrl, String hash) {
        this.baseUrl = baseUrl;
        this.hash = hash;
    }

    public FacebookUrl(String baseUrl, String hash, String ext) {
        this.baseUrl = baseUrl;
        this.hash = hash;
        this.ext = ext;
    }


    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    @Override
    public String toString() {
        return "FacebookUrl{" +
                "baseUrl='" + baseUrl + '\'' +
                ", params='" + hash + '\'' +
                ", extraParams='" + ext + '\'' +
                '}';
    }
}
