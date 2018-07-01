package com.android.xzy.hcsr505;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import com.android.xzy.hcsr505.aliyun.AliyunIOTSignUtil;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

    //Aliyun IOT service
    public static String productKey = "a1ao3jWu96i";

    public static String deviceName = "human_detector_001";

    public static String deviceSecret = "g4vSp0nQWCJpQFFl1Bh0AVNRWFEEyQOk";

    private static String pubTopic = "/sys/" + productKey + "/" + deviceName
            + "/thing/event/property/post";

    private static final String payloadJson
            = "{\"id\":%s,\"params\":{\"hasHuman\": %s},\"method\":\"thing.event.property.post\"}";

    private MqttClient mqttClient = null;

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
        initAliyunIoTClient();

    }

    private GpioCallback mSetLEDCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                Log.i(TAG, "GPIO callback -->" + gpio.getValue());
                long time = System.currentTimeMillis();
                String d = mDateFormat.format(new Date(time));

                mTextView.setText(mTextView.getText() + "\n" + d + " ==>   " + gpio.getValue());

                postDeviceProperties(gpio.getValue());

            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
            // Return true to keep callback active.
            return true;
        }
    };


    private void initAliyunIoTClient() {

        try {
            String clientId = "androidthings" + System.currentTimeMillis();

            Map<String, String> params = new HashMap<String, String>(16);
            params.put("productKey", productKey);
            params.put("deviceName", deviceName);
            params.put("clientId", clientId);
            String timestamp = String.valueOf(System.currentTimeMillis());
            params.put("timestamp", timestamp);

            // cn-shanghai
            String targetServer = "tcp://" + productKey
                    + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

            String mqttclientId = clientId + "|securemode=3,signmethod=hmacsha1,timestamp="
                    + timestamp + "|";
            String mqttUsername = deviceName + "&" + productKey;
            String mqttPassword = AliyunIOTSignUtil.sign(params, deviceSecret, "hmacsha1");

            connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword);

        } catch (Exception e) {
            Log.e(TAG, "initAliyunIoTClient error " + e.getMessage(), e);
        }
    }

    public void connectMqtt(String url, String clientId, String mqttUsername, String mqttPassword)
            throws Exception {

        MemoryPersistence persistence = new MemoryPersistence();
        mqttClient = new MqttClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        // MQTT 3.1.1
        connOpts.setMqttVersion(4);
        connOpts.setAutomaticReconnect(true);
        connOpts.setCleanSession(true);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(60);

        mqttClient.connect(connOpts);
        Log.d(TAG, "connected " + url);

    }


    private void postDeviceProperties(final boolean status) {

        try {

            //上报数据

            String payload = String.format(payloadJson, String.valueOf(System.currentTimeMillis()),
                    status == true ? 1 : 0);

            MqttMessage message = new MqttMessage(payload.getBytes("utf-8"));
            message.setQos(1);

            if (mqttClient == null) {
                initAliyunIoTClient();
            } else {
                mqttClient.publish(pubTopic, message);
                Log.d(TAG, "publish topic=" + pubTopic + ",payload=" + payload);
            }

        } catch (Exception e) {
            Log.e(TAG, "postDeviceProperties error " + e.getMessage(), e);
        }
    }
}
