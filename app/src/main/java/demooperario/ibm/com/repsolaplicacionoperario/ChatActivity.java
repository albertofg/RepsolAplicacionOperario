package demooperario.ibm.com.repsolaplicacionoperario;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    Button salir,enviar;
    EditText text;
    LinearLayout linealView;
    String conversacion="";
    Location location;
    double longitude ,latitude;
    private BroadcastReceiver _refreshReceiver = new MyReceiver();
    SharedPreferences sharedPreferences;
    Messenger mService = null;
    boolean mBound;

    ScrollView scroll;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent svc = new Intent(this, MyMqttService.class);
        bindService(svc, mConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter("CHAT");
        filter.addAction("FINISH");
        this.registerReceiver(_refreshReceiver, filter);
        inicializacion();
    }
    @Override
    public void onClick(View v){
        int id= v.getId();
        Message msg;
        switch (id){
            case R.id.salir:
                if (!mBound) return;
                msg = Message.obtain(null, MyMqttService.EXIT, "bye");
                try {
                    mService.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                text.setText("");
                finish();
                break;
            case R.id.enviar:
                if(text.getText().toString().length()!=0){
                    publicar();
                    if (!mBound) return;
                    msg = Message.obtain(null, MyMqttService.MSG_PUBLISH, text.getText().toString());
                    try {
                        mService.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    text.setText("");
                }
                break;
            case R.id.text:
                text.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(text, InputMethodManager.SHOW_IMPLICIT);
                break;

        }
    }

    private void inicializacion(){
        sharedPreferences = getApplicationContext().getSharedPreferences("historialApp",Context.MODE_PRIVATE);
        text = (EditText)findViewById(R.id.text);
        enviar = (Button)findViewById(R.id.enviar);
        enviar.setOnClickListener(this);
        salir = (Button)findViewById(R.id.salir);
        salir.setOnClickListener(this);
        scroll = (ScrollView)findViewById(R.id.scrollView);
        linealView = (LinearLayout)findViewById(R.id.lineal);
        String historial=sharedPreferences.getString("historial",null);
        if(historial!=null)
            procesarHistorial(historial);

    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            mBound = false;
        }
    };

    private void publicar(){
        String[] palabras = text.getText().toString().split(" ");
        int longi=0;
        String fin="";
        for(String word :palabras){
            fin+=" "+word;
            longi+=1+word.length();
            if(longi>24){
                fin+="\n";
                longi=0;
            }
        }
        if(palabras.length!=1)
            fin+="\n";

        TextView aux;
        aux = new TextView(this);
        aux.setText("Yo: " +fin);
        aux.setTextColor(getResources().getColor(R.color.colorBlack));
        aux.setGravity(Gravity.LEFT);
        aux.setTextSize(17);
        aux.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        aux.setPadding(30,0,0,0);
        linealView.addView(aux);
        conversacion +="Yo:";
        conversacion += text.getText().toString();
        conversacion +=";;;;";
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("historial",conversacion);
        edit.commit();

    }

    private void recibir(String mensaje){

        String[] palabras = mensaje.split(" ");
        int longi=0;
        String fin="";
        for(String word :palabras){
            fin+=" "+word;
            longi+=1+word.length();
            if(longi>24){
                fin+="\n";
                longi=0;
            }
        }
        if(palabras.length!=1)
            fin+="\n";

        TextView aux;
        aux = new TextView(this);
        aux.setText("Operador: " +fin);
        aux.setTextColor(getResources().getColor(R.color.colorBlack));
        aux.setGravity(Gravity.LEFT);
        aux.setTextSize(17);
        aux.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        aux.setPadding(380,0,0,0);
        linealView.addView(aux);
        conversacion +="Contacto:";
        conversacion += fin;
        conversacion +=";;;;";
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("historial",conversacion);
        edit.commit();
        scroll.fullScroll(View.FOCUS_DOWN);
        scroll.scrollTo(0, scroll.getBottom());
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("CHAT")) {
                recibir(intent.getStringExtra("mensaje"));
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle guardarEstado) {
        super.onSaveInstanceState(guardarEstado);
        guardarEstado.putString("mensajes", text.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle recEstado) {
        super.onRestoreInstanceState(recEstado);
        text.setText(recEstado.getString("mensajes"));
    }

    private void procesarHistorial(String historial){
        String[] textos = historial.split(";;;;");
        for(String texto:textos){
            if(texto.contains("Yo:")){
                String[] palabras = texto.split(" ");
                int longi=0;
                String fin="";
                for(String word :palabras){
                    fin+=" "+word;
                    longi+=1+word.length();
                    if(longi>24){
                        fin+="\n";
                        longi=0;
                    }
                }

                TextView aux;
                aux = new TextView(this);
                aux.setText(fin);
                aux.setTextColor(getResources().getColor(R.color.colorBlack));
                aux.setGravity(Gravity.LEFT);
                aux.setTextSize(17);
                aux.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                aux.setPadding(30,0,0,0);
                linealView.addView(aux);
                conversacion +=texto;
                conversacion +=";;;;";
            }else if(texto.contains("Contacto:")){
                String[] palabras = texto.split(" ");
                int longi=0;
                String fin="";
                for(String word :palabras){
                    fin+=" "+word;
                    longi+=1+word.length();
                    if(longi>24){
                        fin+="\n";
                        longi=0;
                    }
                }
                TextView aux;
                aux = new TextView(this);
                aux.setText(fin);
                aux.setTextColor(getResources().getColor(R.color.colorBlack));
                aux.setGravity(Gravity.LEFT);
                aux.setTextSize(17);
                aux.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                aux.setPadding(380,0,0,0);
                linealView.addView(aux);
                conversacion +=texto;
                conversacion +=";;;;";
            }

        }
        scroll.fullScroll(View.FOCUS_DOWN);
        scroll.scrollTo(0, scroll.getBottom());

    }
 }
