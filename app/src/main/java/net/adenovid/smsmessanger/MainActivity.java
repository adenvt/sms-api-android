package net.adenovid.smsmessanger;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static java.util.Locale.US;

public class MainActivity extends AppCompatActivity implements MqttCallback {
    @BindView(R.id.log) TextView log;
    @BindView(R.id.button) Button btn;

    private static final String URL = "tcp://iot.eclipse.org:1883";
    private static final String CID = "AD3123456";
    private static final String TOPIC = "sendSMS";
    private static final String KEY = "123456789";
    private static final int REQUEST_CODE = 123;

    private MqttAndroidClient mqtt;
    private MqttConnectOptions options;
    private DisconnectedBufferOptions bufferOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initClient();
        initPermission();
    }

    private void initClient() {
        mqtt = new MqttAndroidClient(this, URL, CID);
        mqtt.setCallback(this);

        options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);

        bufferOptions = new DisconnectedBufferOptions();
        bufferOptions.setBufferEnabled(true);
        bufferOptions.setBufferSize(100);
        bufferOptions.setPersistBuffer(false);
        bufferOptions.setDeleteOldestMessages(false);
    }

    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Can send SMS now", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void subscribeTopic() {
        try {
            mqtt.subscribe(TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    logging("Success to subscribe: " + TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logging("Failed to subscribe");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        logging("Incoming topic: " + topic);

        JSONObject payload = new JSONObject(new String(message.getPayload()));

        String no = payload.getString("no");
        String msg = payload.getString("message");
        String key = payload.getString("key");

        if (key.equals(KEY))
            sendingSMS(no, msg);
    }

    private void sendingSMS(String no, String message) {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(no, null, message, null, null);

        logging("Send SMS to: " + no);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @OnClick(R.id.button)
    protected void onClick() {
        if (!mqtt.isConnected())
            connect();
        else
            disconnect();
    }

    private void connect() {
        try {
            btn.setEnabled(false);
            mqtt.connect(options, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mqtt.setBufferOpts(bufferOptions);

                    subscribeTopic();

                    btn.setEnabled(true);
                    btn.setText(R.string.disconnect);

                    logging("Connected to: " + URL);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    btn.setEnabled(true);

                    logging("Failed to connecting: " + URL);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            mqtt.disconnect(null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    btn.setText(R.string.connect);

                    logging("Disconnected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    logging("Failed to disconnecting");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void logging(String msg) {
        log.append(String.format(US, "%s\n", msg));
    }
}
