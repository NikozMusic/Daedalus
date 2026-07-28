package net.zmods.daedalus.module;

import com.google.gson.annotations.SerializedName;

public class ModuleMetadata {
    @SerializedName("data")
    public Data data;

    @SerializedName("info")
    public Info info;

    public static class Data {
        public String id;
    }

    public static class Info {
        public String name;
        public String description;
        public String author;
    }
}