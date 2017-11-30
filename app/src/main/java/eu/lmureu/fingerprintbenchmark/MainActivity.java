/* Copyright (C) 2017 Lorenzo Mureu */

package eu.lmureu.fingerprintbenchmark;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    protected Handler handler = new Handler();

    private FingerprintManager fpm;
    private long count = 0;
    private long runningTotalMillis = 0;
    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fpm = (FingerprintManager) this.getApplicationContext().getSystemService(Context.FINGERPRINT_SERVICE);
        result = (TextView) findViewById(R.id.resultID);

        for(int i : InputDevice.getDeviceIds()) {
            InputDevice dev = InputDevice.getDevice(i);
            if(dev.supportsSource(dev.SOURCE_TOUCHPAD)) {
                Log.d("fingerprintbenchmark", String.format("id:%d, name:%s, desc:%s", i, dev.getName(), dev.getDescriptor()));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        benchmark_do();
    }

    protected void benchmark_do(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(fpm != null && fpm.isHardwareDetected() && fpm.hasEnrolledFingerprints()){
            handler.start();
            fpm.authenticate(null, null, 0, handler, null);
        }
    }

    protected class Handler extends FingerprintManager.AuthenticationCallback implements Runnable {
        private long endTime, startTime;

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            endTime = System.nanoTime();
            runOnUiThread(this);
            //handler.reset();
        }

        protected void reset() {
            endTime = 0;
            startTime = 0;
        }

        protected void start() {
            startTime = System.nanoTime();
            endTime = 0;
        }

        @Override
        public void run() {
            success(duration());
        }

        private boolean isCompleted() {
            return isStarted() && endTime > startTime;
        }

        private boolean isStarted() {
            return startTime > 0;
        }

        private long duration() {
            return endTime - startTime;
        }
    }

    protected void success(long duration) {
        count ++;
        duration *= 1e-6;
        runningTotalMillis += duration;
        result.setText(String.format("count:%d; last:%d ms, avg:%d ms", count, duration, runningTotalMillis/count));
        benchmark_do();
    }
}
