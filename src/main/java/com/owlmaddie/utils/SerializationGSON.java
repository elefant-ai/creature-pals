package com.owlmaddie.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SerializationGSON {
    public static final Gson GSON = new GsonBuilder()
        .excludeFieldsWithoutExposeAnnotation()
        .create();
}