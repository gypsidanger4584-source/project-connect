package com.example.login_new.alogic;

import androidx.annotation.NonNull;

public class SearchResultItem {
    public enum ItemType { BUILDING, DEPARTMENT, SPACE }

    private final ItemType type;
    private final String displayName;
    private final String rawName;
    private final String buildingName;
    private final String markerIdName;

    public SearchResultItem(ItemType type, String displayName, String rawName, String buildingName, String markerIdName) {
        this.type = type;
        this.displayName = displayName;
        this.rawName = rawName;
        this.buildingName = buildingName;
        this.markerIdName = markerIdName;
    }

    public ItemType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRawName() {
        return rawName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getMarkerIdName() {
        return markerIdName;
    }
}
