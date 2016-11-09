package camilo.android.ubicacion;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOGTAG = "android-localizacion";
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int PETICION_PERMISO_LOCALIZACION = 101;
    private static final int PETICION_CONFIG_UBICACION = 201;
    public static final long UPDATE_INTERVAL = 2000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;
    private GoogleApiClient apiClient;
    private TextView lblLatitud;
    private TextView lblLongitud;
    private LocationRequest mLocationRequest;
    private boolean canGetLocation = false;
    private static final String sharedPrefName="preferencias_BoApp";
    private SharedPreferences sharedPref;
    private Double longitud,latitud;

    private IntentFilter filter = new IntentFilter();
    private ProgressReceiver rcv = new ProgressReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("EMPIEZA", " :onCreate");
        setContentView(R.layout.activity_main);
        loadDefaultValues();
        comprobarConfiguracion();
        //registeReceiver();
        Log.d("TERMINA", " :onCreate");
    }

    private void comprobarPermisos() {
        Log.d("EMPIEZA", " :comprobarPermiso");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        } else {
            Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT);
            ejecutarServicioUbicacion();
        }
        Log.d("TERMINA", " :comprobarPermiso");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d("EMPIEZA", " : onRequestPermissionsResult");
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT);
                ejecutarServicioUbicacion();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    //Show permission explanation dialog...
                } else {
                    //Never ask again selected, or device policy prohibits the app from having that permission.
                    //So, disable that feature, or fall back to another situation...
                }
            }
        }
        Log.d("TERMINA", " : onRequestPermissionsResult");
    }



    private synchronized void comprobarConfiguracion() {
        Log.d("EMPIEZA", " : comprobarConf");
        apiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();


        apiClient.connect();
        Log.d("TERMINA", " : comprobarConf");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("EMPIEZA", " : onActivityResult");
        switch (requestCode) {
            case PETICION_CONFIG_UBICACION:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //Ejecuto el servicio
                        canGetLocation = true;
                        comprobarPermisos();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(LOGTAG, "El usuario no ha realizado los cambios de configuración necesarios");
                        canGetLocation = false;
                        comprobarPermisos();
                        break;
                }
                break;
        }
        Log.d("TERMINA", " : onActivityResult");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("EMPIEZA", " : onConnected");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(UPDATE_FASTEST_INTERVAL);

        LocationSettingsRequest locSettingsRequest =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(mLocationRequest)
                        .build();

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(
                        apiClient, locSettingsRequest);
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        Log.i(LOGTAG, "Configuración correcta");
                        //Se ejecuta el servicio
                        canGetLocation = true;
                        comprobarPermisos();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            Log.i(LOGTAG, "Se requiere actuación del usuario");
                            status.startResolutionForResult(MainActivity.this, PETICION_CONFIG_UBICACION);
                            canGetLocation = false;
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(LOGTAG, "Error al intentar solucionar configuración de ubicación");
                            canGetLocation = false;
                        }

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(LOGTAG, "No se puede cumplir la configuración de ubicación necesaria");
                        canGetLocation = false;
                        break;
                }
            }
        });
        Log.d("TERMINA", " : onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOGTAG, "Conexión de API suspendida");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(LOGTAG, "Conexión de API fallida");
    }

    private void ejecutarServicioUbicacion() {
        Intent msgIntent = new Intent(MainActivity.this, Ubicacion.class);
        msgIntent.putExtra("stopservice", false);
        msgIntent.putExtra("canGetLocation", canGetLocation);
        startService(msgIntent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveDefaultValues();
        /*Intent msgIntent = new Intent(MainActivity.this, Ubicacion.class);
        msgIntent.putExtra("stopservice", true);
        msgIntent.putExtra("canGetLocation",canGetLocation);
        if(stopService(msgIntent)) Log.d(LOGTAG,"Servicio parado");
        else Log.d(LOGTAG,"Servicio no parado");*/
        unregisterReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDefaultValues();
        /*Intent msgIntent = new Intent(MainActivity.this, Ubicacion.class);
        msgIntent.putExtra("stopservice", false);
        msgIntent.putExtra("canGetLocation",canGetLocation);
        String nom=startService(msgIntent).toShortString();
        if(nom!=null) Log.d(LOGTAG,"Servicio iniciado ("+nom+")");
        else Log.d(LOGTAG,"Servicio no iniciado");*/
        registeReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveDefaultValues();
        //unregisterReceiver();
    }

    public class ProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Ubicacion.ACTION_PROGRESO)) {
                longitud = intent.getDoubleExtra("longitud", 0);
                latitud = intent.getDoubleExtra("latitud", 0);
                updateUI();
            } else if (intent.getAction().equals(Ubicacion.ACTION_FIN)) {
                Toast.makeText(MainActivity.this, "Tarea finalizada!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registeReceiver() {
        filter.addAction(Ubicacion.ACTION_PROGRESO);
        filter.addAction(Ubicacion.ACTION_FIN);
        registerReceiver(rcv, filter);
    }

    private void unregisterReceiver() {
        filter.addAction(Ubicacion.ACTION_PROGRESO);
        filter.addAction(Ubicacion.ACTION_FIN);
        unregisterReceiver(rcv);
    }

    private void loadDefaultValues(){
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        latitud = Double.valueOf(sharedPref.getString("latitud","37.2582"));
        longitud = Double.valueOf(sharedPref.getString("longitud","-6.9293"));
        updateUI();
    }

    private void saveDefaultValues(){
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("latitud",latitud.toString());
        editor.putString("longitud",longitud.toString());
        editor.commit();
    }

    private void updateUI(){
        lblLatitud = (TextView) findViewById(R.id.lblLatitud);
        lblLongitud = (TextView) findViewById(R.id.lblLongitud);
        lblLatitud.setText("Latitud: " + latitud.toString());
        lblLongitud.setText("Longitud: " + longitud.toString());
    }
}
