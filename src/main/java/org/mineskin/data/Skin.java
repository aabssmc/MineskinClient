package org.mineskin.data;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class Skin {
    public int id;
    public String uuid;
    public String name;
    public SkinData data;
    public long timestamp;
    @SerializedName("private")
    public boolean isPrivate;
    public int views;
    public int accountId;
    public DelayInfo delayInfo;
}
