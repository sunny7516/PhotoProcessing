package com.example.tacademy.photoprocessing;

import android.app.Application;

import com.miguelbcr.ui.rx_paparazzo.RxPaparazzo;

/**
 * Created by Tacademy on 2017-01-19.
 */

public class CameraApplication extends Application {

    @Override public void onCreate() {
        super.onCreate();
        RxPaparazzo.register(this);
    }
}