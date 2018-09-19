package demooperario.ibm.com.repsolaplicacionoperario;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Location location;
    double longitude ,latitude;
    Button salir,chat,borrar;
    int dataSource=48;
    JSONObject object;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10, MIN_TIME_BW_UPDATES = 60000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);


        boolean isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if(!isGPSEnabled && !isNetworkEnabled){
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.gps_advice_tittle));
            dialog.setMessage(getString(R.string.gps_advice_text));
            dialog.setNeutralButton(getString(R.string.but_accept), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            });
            dialog.create();
            dialog.show();
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.gps_advice_tittle));
        dialog.setMessage(getString(R.string.vpn_advice_text));
        dialog.setNeutralButton(getString(R.string.but_accept), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        dialog.create();
        dialog.show();
        getGeoPosition();
        mandarInfoOperario();
        inicializacion();
    }
    @Override
    public void onClick(View v){
        int id= v.getId();
        switch (id){
            case R.id.salir:
                finish();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            case R.id.chat:
                Intent action = new Intent(MainActivity.this,ChatActivity.class);
                startActivity(action);
                break;
            case R.id.borrar:
                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("historialApp",Context.MODE_PRIVATE);
                SharedPreferences.Editor edit = sharedPreferences.edit();
                edit.clear();
                edit.commit();
                break;
        }
    }

    private void inicializacion(){
        chat = (Button)findViewById(R.id.chat);
        chat.setOnClickListener(this);
        salir = (Button)findViewById(R.id.salir);
        salir.setOnClickListener(this);
        borrar = (Button)findViewById(R.id.borrar);
        borrar.setOnClickListener(this);
    }

    private void mandarInfoOperario() {

        String fecha,imagenCode;
        object = new JSONObject();
        GregorianCalendar cal = new GregorianCalendar();
        java.text.SimpleDateFormat sdfMadrid = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        fecha= sdfMadrid.format(cal.getTime());
        fecha=fecha.replace(":",".").replace("+","").replace("T","-");
        fecha=fecha.substring(0,fecha.length()-1);

        TelephonyManager tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if(tel.getDeviceId().equals("862563032151155")) {
            Bitmap imagen = BitmapFactory.decodeResource(getResources(),R.mipmap.esther);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imagen.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] b = baos.toByteArray();
           imagenCode = Base64.encodeToString(b, Base64.DEFAULT);

            try {
                object.accumulate("NAME", "ESTHER");
                object.accumulate("APELLIDOS", "FERNANDEZ DIAZ");
                object.accumulate("IMAGEN", imagenCode);
                object.accumulate("IMEI"    , tel.getDeviceId());
                object.accumulate("LOCATION", "POINT(" + longitude + " " + latitude + ")");
                object.accumulate("CATEGORIA", "OPERARIO");
                object.accumulate("HORARIO","9:00-18:00");
                object.accumulate("SUPERIOR","Maria Martinez");
                object.accumulate("EMAIL","esther@repsol.com");
                object.accumulate("TURNO","Completa");
                object.accumulate("STARTDATETIME", fecha);
                object.accumulate("ENDDATETIME", fecha);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }else{
            try {
                Bitmap imagen = BitmapFactory.decodeResource(getResources(),R.mipmap.operario);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                imagen.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] b = baos.toByteArray();
                imagenCode = Base64.encodeToString(b, Base64.DEFAULT);
                object.accumulate("NAME", "JUAN");
                object.accumulate("APELLIDOS", "PEREZ ROBLES");
                object.accumulate("IMAGEN", imagenCode);
                object.accumulate("IMEI", tel.getDeviceId());
                object.accumulate("LOCATION", "POINT(" + longitude + " " + latitude + ")");
                object.accumulate("CATEGORIA", "OPERARIO");
                object.accumulate("HORARIO","9:00-18:00");
                object.accumulate("SUPERIOR","Maria Martinez");
                object.accumulate("EMAIL","juan@repsol.com");
                object.accumulate("TURNO","Completa");
                object.accumulate("STARTDATETIME", fecha);
                object.accumulate("ENDDATETIME", fecha);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }

        PostTask task = new PostTask();
        task.execute();
    }

    private class PostTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... data) {
            String authorize = Base64.encodeToString("sysadmin:z1ocpssc".getBytes(), Base64.DEFAULT).replace("\n", "");
            //String authorize = Base64.encodeToString("wpsadmin:z1ocpssc".getBytes(), Base64.DEFAULT).replace("\n", "");
            //String urlString = "https://iocdevsm.dcry.iccmop/ibm/ioc/api/data-injection-service/datablocks/39/dataitems";
            String urlString = "https://iocsms.dcry.iccmop/ibm/ioc/api/data-injection-service/datablocks/"+dataSource+"/dataitems";//viento
            //String urlString = "https://iocdevwe.dcry.iccmop/ibm/ioc/api/data-injection-service/datablocks/470/dataitems";
            HttpsURLConnection httpURLConnection=null;
            String msg="OK";
            try {


                URL url = new URL(urlString);
                trustAllHosts();
                httpURLConnection = (HttpsURLConnection)url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setHostnameVerifier(DO_NOT_VERIFY);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-Type", "application/json");
                httpURLConnection.setRequestProperty("IBM-Session-ID", "1");
                httpURLConnection.setRequestProperty("JSESSIONID", "1");
                httpURLConnection.setFixedLengthStreamingMode(object.toString().length());

                httpURLConnection.setRequestProperty("Authorization", "Basic " + authorize);
                httpURLConnection.connect();

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(object.toString());
                wr.flush();
                wr.close();
            }

            catch (IOException e4) {
                msg= e4.getMessage();
            }finally {

                if(httpURLConnection!=null){
                    httpURLConnection.disconnect();
                }
            }
            return msg;
        }

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

    final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * Trust every server - dont check for any certificate
     */
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }

            public void checkClientTrusted(X509Certificate[] chain,
                                           String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain,
                                           String authType){
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
