package com.ori.proteinapplication;



public interface GeminiCallback {
    void onSuccess(String response);
    void onError(String error);
}