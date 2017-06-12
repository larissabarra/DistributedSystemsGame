package cefetmg.br.sd;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import cefetmg.br.sd.services.FailureControllerService;
import cefetmg.br.sd.services.P2P.P2PStorageService;

import static cefetmg.br.sd.services.P2P.P2PPeerNode.CONTINGENCY_OPTIONAL;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int REQUEST_COARSE_PERMISSION = 1;
    public static final int REQUEST_FINE_PERMISSION = 2;
    public static String SERVER_IP = "";
    public static String P2P_NODEID = "";
    public static boolean P2P_FIRSTNODE = false;
    public static int P2P_PEERCONTINGENCY;
    public static String P2P_PEERNODE = "";
    public static String MQTT_BROKE_ADDRESS = "";
    public static final String MQTT_CLIENT_TOPIC = "coordenadas";
    public static final String MQTT_CLIENT_NAME = "AndroidClient";
    public static final int MQTT_CLIENT_QOS = 2;

    GoogleApiClient mGoogleApiClient = null;

    Intent mIntentFailureService = null;
    Intent mIntentP2PStorageService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SERVER_IP = getIntent().getStringExtra("SERVER_IP");
        P2P_NODEID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        P2P_FIRSTNODE = getIntent().getExtras().getBoolean("P2P_FIRSTNODE", true);
        P2P_PEERNODE = getIntent().getExtras().getString("P2P_PEERNODE", "");
        P2P_PEERCONTINGENCY = getIntent().getExtras().getInt("P2P_PEERCONTINGENCY", CONTINGENCY_OPTIONAL);
        MQTT_BROKE_ADDRESS = "tcp://" + SERVER_IP + ":1883";
        //201.17.158.170
        setContentView(R.layout.activity_main);
        checkPermissions();
    }

    @Override
    protected void onResume() {
        initializeP2PStorageService();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        stopP2PStorageService();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
    }

    public void getLocation() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
            Toast.makeText(this, "Atualizando localização...", Toast.LENGTH_SHORT).show();
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                String currentPosition = String.valueOf(mLastLocation.getLatitude()) + ", " + String.valueOf(mLastLocation.getLongitude());
                TextView coordinatesText = (TextView) findViewById(R.id.coordinatesText);
                coordinatesText.setText(currentPosition);
                String jsonMessage = "{ \"sensor\": \"%s\", \"location\": \"%s\" }";
                jsonMessage = String.format(jsonMessage, MQTT_CLIENT_NAME, currentPosition);
                sendMqttMessage(jsonMessage);
            }
        }
    }

    public void onClickRefreshLocation(View view) {
        getLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermissions();
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, "Falha de conexão", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Conexão suspensa", Toast.LENGTH_LONG).show();
    }

    private void sendMqttMessage(String message) {
        try {
            MemoryPersistence mMqttPersistence = new MemoryPersistence();
            MqttClient mqttClient = new MqttClient(MQTT_BROKE_ADDRESS, MQTT_CLIENT_NAME, mMqttPersistence);
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(true);
            mqttClient.connect(mqttConnectOptions);
            Log.d("SD", "MQTT Conectado");
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(MQTT_CLIENT_QOS);
            mqttClient.publish(MQTT_CLIENT_TOPIC, mqttMessage);
            mqttClient.disconnect();
            Log.d("SD", "MQTT Desconectado");
        } catch (MqttException e) {
            Log.e("SD", "Falha de MQTT: " + e.getMessage());
            Toast.makeText(this, "Falha de MQTT: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(MainActivity.this, getString(R.string.denied_coarse_permission), Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_PERMISSION);
            }
        } else if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(MainActivity.this, getString(R.string.denied_fine_permission), Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_PERMISSION);
            }
        } else {
            if (mGoogleApiClient == null) {
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API)
                        .build();
            }
            mGoogleApiClient.connect();
        }
    }

    private void initializeFailureController() {
        mIntentFailureService = new Intent(this, FailureControllerService.class);
        startService(mIntentFailureService);
    }

    private void initializeP2PStorageService() {
        mIntentP2PStorageService = new Intent(this, P2PStorageService.class);
        mIntentP2PStorageService.putExtra("P2P_NODEID", P2P_NODEID);
        mIntentP2PStorageService.putExtra("P2P_FIRSTNODE", P2P_FIRSTNODE);
        mIntentP2PStorageService.putExtra("P2P_PEERNODE", P2P_PEERNODE);
        mIntentP2PStorageService.putExtra("P2P_PEERCONTINGENCY", P2P_PEERCONTINGENCY);
        startService(mIntentP2PStorageService);
    }

    private void stopP2PStorageService() {
        stopService(mIntentP2PStorageService);
    }

}
