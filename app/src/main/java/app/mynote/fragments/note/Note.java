package app.mynote.fragments.note;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.sql.Timestamp;

import app.mynote.core.utils.AppDate;
import app.mynote.core.utils.AppTimestamp;

public class Note implements Serializable {
    @SerializedName("header")
    @Expose
    private String header;

    @SerializedName("id")
    @Expose
    private String id;

    @SerializedName("userId")
    @Expose
    private String userId;

    @SerializedName("colour")
    @Expose
    private String colour;



    @SerializedName("selection")
    @Expose
    private String selection;

    @SerializedName("archived")
    @Expose
    private boolean archived;

    @SerializedName("pinned")
    @Expose
    private boolean pinned;

    @SerializedName("favorite")
    @Expose
    private boolean favorite;

    @SerializedName("active")
    @Expose
    private boolean active = true;

    @SerializedName("spellCheck")
    @Expose
    private boolean spellCheck;

    @SerializedName("pinOrder")
    @Expose
    private Timestamp pinOrder;

    @SerializedName("dateCreated")
    @Expose
    private Timestamp dateCreated;

    @SerializedName("dateModified")
    @Expose
    private Timestamp dateModified;

    @SerializedName("dateArchived")
    @Expose
    private Timestamp dateArchived;

    @SerializedName("dateSync")
    @Expose
    private String dateSync;

    @SerializedName("owner")
    @Expose
    private String owner;

    @SerializedName("text")
    @Expose
    private String text;

    public Note() {
        this.pinOrder = AppTimestamp.convertStringToTimestamp(AppDate.Now());
        this.dateModified = AppTimestamp.convertStringToTimestamp(AppDate.Now());
        this.dateArchived = AppTimestamp.convertStringToTimestamp(AppDate.Now());
    }

    private boolean sync = true;

    public boolean getIsSync() {
        return sync;
    }

    public void setIsSync(boolean value){
        this.sync = value;
    }

    public String getDateSync() {
        return dateSync;
    }

    public void setDateSync(String dateSync) {
        this.dateSync = dateSync;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean getArchived() {
        return this.archived;
    }

    public void setArchived(boolean value) {
        this.archived = value;
    }

    public Timestamp getDateArchived() {
        return this.dateArchived;
    }

    public void setDateArchived(Timestamp dateArchived) {
        this.dateArchived = dateArchived;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public void setFavorite(boolean value) {
        this.favorite = value;
    }

    public String getText() {
        return text==null ? "" : text;
    }

    public void setText(String text) {
        this.text = text;

    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Timestamp getDateModified() {
        return dateModified;
    }

    public void setDateModified(Timestamp dateModified) {
        this.dateModified = dateModified;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSelection() {
        return this.selection;
    }

    public void setSelection(String selection) {
        this.selection = selection;
    }

    public boolean getPinned() {
        return this.pinned;
    }

    public Timestamp getPinOrder() {
        return this.pinOrder;
    }

    public void setPinOrder(Timestamp pinOrder) {
        this.pinOrder = pinOrder;
    }

    public boolean isPinned() {
        return this.pinned;
    }

    public void setPinned(boolean value) {
        this.pinned = value;
    }

    public boolean getSpellCheck() {
        return this.spellCheck;
    }

    public void setSpellCheck(Boolean spellCheck) {
        this.spellCheck = spellCheck;
    }

    public boolean getActive() {
        return this.active;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
