package com.android.xzy.hcsr505;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 *
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String TAG = "xzy";

    public static final String PIR_PIN = "GPIO2_IO03";

    public TextView mTextView;

    public SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
            Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView) findViewById(R.id.pir_sensor);
        PeripheralManager pioService = PeripheralManager.getInstance();
        try {
            Gpio pirGpio = pioService.openGpio(PIR_PIN);

            pirGpio.setDirection(Gpio.DIRECTION_IN);
            // Enable edge trigger events for both falling and rising edges. This will make it a toggle button.
            pirGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Register an event callback.
            pirGpio.registerGpioCallback(mSetLEDCallback);

        } catch (IOException e) {
            Log.i("xzy", "error => " + e.toString());
            e.printStackTrace();
        }

    }

    private GpioCallback mSetLEDCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                Log.i(TAG, "GPIO callback -->" + gpio.getValue());
                long time = System.currentTimeMillis();
                String d = mDateFormat.format(new Date(time));

                mTextView.setText(mTextView.getText() + "\n" + d + " ==>   " + gpio.getValue());
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
            // Return true to keep callback active.
            return true;
        }
    };
}
