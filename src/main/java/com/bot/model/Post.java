package com.bot.model;

public class Post {
    private int id;
    private String text;
    private byte[] image;

    public Post(int id, String text, byte[] image) {
        this.id = id;
        this.text = text;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public byte[] getImage() {
        return image;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}