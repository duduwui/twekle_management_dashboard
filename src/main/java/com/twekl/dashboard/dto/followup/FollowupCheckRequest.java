package com.twekl.dashboard.dto.followup;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for completing or updating a Follow-up milestone check.
 * Dedicated followup domain DTO.
 */
public class FollowupCheckRequest {

    @JsonProperty("notes")
    @JsonAlias({"note"})
    private String notes = "";

    @JsonProperty("imageUrl")
    @JsonAlias({"photoUrl"})
    private String imageUrl = "";

    private String checkedBy;
    private String satisfaction;

    public FollowupCheckRequest() {}

    public String getNotes() { return notes != null ? notes : ""; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getNote() { return getNotes(); }
    public void setNote(String note) { this.notes = note; }

    public String getImageUrl() { return imageUrl != null ? imageUrl : ""; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPhotoUrl() { return getImageUrl(); }
    public void setPhotoUrl(String photoUrl) { this.imageUrl = photoUrl; }

    public String getCheckedBy() { return checkedBy; }
    public void setCheckedBy(String checkedBy) { this.checkedBy = checkedBy; }

    public String getSatisfaction() { return satisfaction; }
    public void setSatisfaction(String satisfaction) { this.satisfaction = satisfaction; }
}
