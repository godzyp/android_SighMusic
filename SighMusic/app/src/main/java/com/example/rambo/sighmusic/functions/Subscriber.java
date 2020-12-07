package com.example.rambo.sighmusic.functions;

//订阅器
public interface Subscriber<T> {
    void onComplete(T t);
    void onError(Exception e);
}
