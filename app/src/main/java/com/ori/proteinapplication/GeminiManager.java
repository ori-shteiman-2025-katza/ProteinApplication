package com.ori.proteinapplication;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.ImagePart;
import com.google.ai.client.generativeai.type.Part;
import com.google.ai.client.generativeai.type.TextPart;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class GeminiManager {

    // API_KEY: https://ai.google.dev/gemini-api/docs/api-key
    // for git: get ignore
    private static final String API_KEY = "AIzaSyBWQPhUFZS1YY3RCdyaGrPLG7Eb0rUuko4";
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    private static GeminiManager instance;
    private GenerativeModel gemini;  //class from google SDK
    private Handler mainHandler;

    private GeminiManager() {
        // Constructor is private to prevent instantiation from outside
        // init
        gemini = new GenerativeModel(
                "gemini-2.0-flash-lite",  // model
                API_KEY                             // API_KEY
        );
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void sendMessage(String prompt, GeminiCallback callback) {
        // Initiate the request to generate content from the Gemini AI model.
        // This is an asynchronous operation.
        gemini.generateContent(prompt,
                // Create an anonymous inner class that implements the Continuation interface.
                // This is used to handle the asynchronous result of the generateContent operation.
                new Continuation<GenerateContentResponse>() {
                    @NotNull
                    @Override
                    public CoroutineContext getContext() {
                        return EmptyCoroutineContext.INSTANCE;
                    }

                    @Override
                    public void resumeWith(@NotNull Object result) {
                        // Check if the operation was successful or if it failed.
                        if (result instanceof Result.Failure) {
                            // The operation failed.
                            // Call the onError method of the callback to notify the caller of the error.
                            callback.onError(((Result.Failure)result).exception.getMessage());
                        } else {
                            // The operation was successful.
                            // Extract the text from the GenerateContentResponse.
                            // Call the onSuccess method of the callback to notify the caller of the successful result.
                            callback.onSuccess(((GenerateContentResponse) result).getText());
                        }
                    }
                });
    }

    public void sendMessageWithPhoto(String prompt, Bitmap photo, GeminiCallback callback) {
        List<Part> parts = new ArrayList<>();
        parts.add(new TextPart(prompt));
        parts.add(new ImagePart(photo));

        Content[] content = new Content[1];
        content[0] = new Content(parts);

        // Set up timeout
        Runnable timeoutRunnable = () -> {
            if (callback != null) {
                callback.onError("Request timed out");
            }
        };
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        try {
            gemini.generateContent(content, new Continuation<GenerateContentResponse>() {
                @NotNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NotNull Object result) {
                    // Remove timeout callback
                    mainHandler.removeCallbacks(timeoutRunnable);

                    if (callback == null) {
                        return; // Callback was cleared (activity destroyed)
                    }

                    try {
                        if (result instanceof Result.Failure) {
                            String errorMessage = ((Result.Failure) result).exception.getMessage();
                            callback.onError(errorMessage != null ? errorMessage : "Unknown error occurred");
                        } else {
                            String response = ((GenerateContentResponse) result).getText();
                            if (response != null && !response.isEmpty()) {
                                callback.onSuccess(response);
                            } else {
                                callback.onError("Empty response from Gemini");
                            }
                        }
                    } catch (Exception e) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            mainHandler.removeCallbacks(timeoutRunnable);
            if (callback != null) {
                callback.onError("Error sending request: " + e.getMessage());
            }
        }
    }
}