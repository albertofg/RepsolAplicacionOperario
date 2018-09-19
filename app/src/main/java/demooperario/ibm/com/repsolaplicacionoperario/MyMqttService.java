package demooperario.ibm.com.repsolaplicacionoperario;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MyMqttService extends Service {

    IMqttClient client = null;
    String topic;
    static final int MSG_PUBLISH = 1;
    static final int EXIT = 2;
    String imei;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 60000;
    Location location; // location
    double longitude;
    double latitude;
    MqttConnectOptions options;
    String tcpAdress;
    String cliente;
    int notif=121212;

    @Override
    public void onCreate() {


        super.onCreate();
        try {
            TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            imei = tel.getDeviceId();
            Properties prop = new Properties();
            AssetManager assetManager = getBaseContext().getAssets();
            InputStream inputStream = assetManager.open("config.properties");
            prop.load(inputStream);
            topic = prop.getProperty("topic");
            cliente = "cliente"+imei;
            tcpAdress = "tcp://" + prop.getProperty("ip") + ":"
                    + prop.getProperty("port");

            client = new MqttClient(tcpAdress, cliente, null);
            client.setCallback(new Callback());
            options = new MqttConnectOptions();
            options.setCleanSession(true);
            client.connect(options);
            client.subscribe("CHAT");
            client.subscribe("MAPA");
            client.subscribe("FLASH");
            getGeoPosition();
        } catch (MqttException e) {
            e.printStackTrace();
        }catch (IOException e2) {
            e2.printStackTrace();
        }

    }

    public void chat(String mensaje) {
        String mess = imei + ";"+mensaje+";"+latitude+";"+longitude;
        byte[] b = mess.getBytes();
        MqttMessage message = new MqttMessage(b);
        message.setQos(1);
        message.setRetained(false);
        try {
            client.getTopic(topic).publish(message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }



    private class Callback implements MqttCallback {


        @Override
        public void connectionLost(Throwable throwable) {
            final AlertDialog.Builder dialog = new AlertDialog.Builder(MyMqttService.this);
            if (!client.isConnected()) {
                dialog.setTitle(getString(R.string.gps_advice_tittle));
                dialog.setMessage(getString(R.string.connection_losts));
                dialog.setNeutralButton(getString(R.string.but_rec), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            client.connect(options);
                            client.subscribe("CHAT");
                        } catch (MqttException e) {
                            e.printStackTrace();
                            Intent intent = new Intent("FINISH");
                            sendBroadcast(intent);
                        }

                    }
                });
                dialog.create();
                dialog.show();
            }
        }

        @Override
        public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
            String res = new String(mqttMessage.getPayload());
            String[] resultados = res.split(";");
            if(imei.equals(resultados[0])) {
                if(s.equals("CHAT")) {
                    Intent intent = new Intent("CHAT");
                    intent.putExtra("mensaje",resultados[1]);
                    sendBroadcast(intent);
                    sistemaNotificaciones();
                }
                else if(s.equals("FLASH")){
                    Intent action = new Intent(MyMqttService.this,FlashActivity.class);
                    startActivity(action);
                }else if(s.equals("MAPA")){
                    Intent action = new Intent(MyMqttService.this,MapActivity.class);
                    startActivity(action);
                }
            }


        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        }
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PUBLISH:
                    chat(msg.obj.toString());
                    break;
                case EXIT:
                    try {
                        client.unsubscribe("CHAT");
                        client.disconnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    private void getGeoPosition(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

        // getting GPS status
        boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        boolean isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        MyLocationListener locationListener1 = new MyLocationListener();

        if (isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,locationListener1 );

            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        }
        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled) {
            if (location == null) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener1);
                if (locationManager != null) {
                    location = locationManager
                            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                }
            }
        }
    }
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {

            Toast.makeText(
                    getBaseContext(),
                    "Location changed: Lat: " + loc.getLatitude() + " Lng: "
                            + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }

    private void sistemaNotificaciones(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MyMqttService.this);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setAutoCancel(true);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        mBuilder.setContentTitle("Nuevo Mensaje");
        mBuilder.setContentText("Mensaje Nuevo");
        mBuilder.setTicker("Alerta!");
        mBuilder.setVibrate(new long[] {100, 250, 100, 500});
        Intent notIntent =
                new Intent(getApplicationContext(), ChatActivity.class);
        notIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contIntent =
                PendingIntent.getActivity(
                  getApplicationContext(), 0, notIntent, 0);

        mBuilder.setContentIntent(contIntent);
        NotificationManager mNotificationMangaer = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationMangaer.notify(notif, mBuilder.build());
    }
}
