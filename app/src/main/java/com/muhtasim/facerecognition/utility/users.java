package com.muhtasim.facerecognition.utility;

public class users {


    private String id;
    private String title;
    private Float distance;
    private String extra;
    private String time;

    public users() {
    }

    public users(String id, String title, Float distance) {
        this.id = id;
        this.title = title;
        this.distance = distance;
        this.extra = null;
        this.time = null;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Float getDistance() {
        return distance;
    }

    public String getExtra() {
        return this.extra;
    }

    public void setExtra(String extra) {

        this.extra = extra;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {

        this.time = time;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (id != null) {
            resultString += "[" + id + "] ";
        }

        if (title != null) {
            resultString += title + " ";
        }

        if (distance != null) {
            resultString += String.format("(%.1f%%) ", distance * 100.0f);
        }

        return resultString.trim();
    }

}
