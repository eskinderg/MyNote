package com.example.drawer.ui.notes;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class Note implements Serializable {
    @SerializedName("header")
    @Expose
    private String header;

    @SerializedName("id")
    @Expose
    private Number id;

    @SerializedName("userId")
    @Expose
    private String userId;

    @SerializedName("colour")
    @Expose
    private String colour;

    @SerializedName("height")
    @Expose
    private Number height;

    @SerializedName("width")
    @Expose
    private Number width;

    @SerializedName("left")
    @Expose
    private Number left;

    @SerializedName("top")
    @Expose
    private Number top;

    @SerializedName("selection")
    @Expose
    private String selection;

    @SerializedName("archived")
    @Expose
    private Boolean archived;

    @SerializedName("active")
    @Expose
    private Boolean active;

    @SerializedName("spellCheck")
    @Expose
    private Boolean spellCheck;

    @SerializedName("pinOrder")
    @Expose
    private String pinOrder;

    @SerializedName("dateCreated")
    @Expose
    private String dateCreated;

    @SerializedName("dateModified")
    @Expose
    private String dateModified;

    @SerializedName("dateArchived")
    @Expose
    private String dateArchived;

    @SerializedName("owner")
    @Expose
    private String owner;

    @SerializedName("text")
    @Expose
    private String text;

    public Note() {
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean value) {
        this.archived = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getDateModified() {
        return dateModified;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public Number getId() {
        return id;
    }

    public String getDateArchived() {
        return this.dateArchived;
    }
}
