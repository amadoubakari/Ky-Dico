package com.flys.dico.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookProfile  implements Serializable {
    private String id;
    private String name;
    private String email;
    private Picture picture;

    public FacebookProfile() {
    }

    public FacebookProfile(String id, String name, String email, Picture picture) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Picture getPicture() {
        return picture;
    }

    public void setPicture(Picture picture) {
        this.picture = picture;
    }


    @Override
    public String toString() {
        return "FacebookProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", picture=" + picture +
                '}';
    }
}