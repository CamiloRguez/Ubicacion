package camilo.android.ubicacion;

import android.Manifest;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Bo_1 on 09/11/2016.
 */

public class Ubicacion extends IntentService implements GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    public static final String TAG = "ubicacion-service";
    public static final String ACTION_PROGRESO = "net.sgoliver.intent.action.PROGRESO";
    public static final String ACTION_FIN = "net.sgoliver.intent.action.FIN";
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private boolean canGetLocation;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public Ubicacion() {
        super("Ubicacion");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("EMPIEZA: ", "onCreate");
        Log.e("TERMINA: ", "onCreate");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("EMPIEZA: ", "onHandleInternet");
        canGetLocation = intent.getBooleanExtra("canGetLocation", false);
        boolean stopService = true;
        if (intent != null) {
            stopService = intent.getBooleanExtra("stopservice", false);
        }

        if (stopService) {
            Log.d(TAG,"Servicio detenido");
            stopLocationUpdates();
        } else {
            Log.d(TAG,"Servicio iniciado");
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            mGoogleApiClient.connect();
        }
        Log.e("TERMINA: ", "onHandleIntent");
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.e("EMPIEZA: ", "onDestroy");
        super.onDestroy();
        Log.d("TERMINA: ", "onDestroy");
    }

    public void stopLocationUpdates() {
        Log.e("EMPIEZA: ", "stopLocationUpdates");
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        } else {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            if (mGoogleApiClient.isConnected())
                mGoogleApiClient.disconnect();
        }
        Log.d("TERMINA: ", "stopLocationUpdates");
    }

    private void startLocationUpdates() {
        Log.e("EMPIEZA: ", "startLocationUpdates");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Ojo: estamos suponiendo que ya tenemos concedido el permiso.
            //Sería recomendable implementar la posible petición en caso de no tenerlo.

            Log.i(TAG, "Inicio de recepción de ubicaciones");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, Ubicacion.this);
        }
        Log.d("TERMINA: ", "startLocationUpdates");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        //Se ha producido un error que no se puede resolver automáticamente
        //y la conexión con los Google Play Services no se ha establecido.

        Log.e(TAG, "Error grave al conectar con Google Play Services");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("EMPIEZA: ", "onConnected");
        // TODO Auto-generated method stub
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(MainActivity.UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(MainActivity.UPDATE_FASTEST_INTERVAL);

        // Obtenemos la última ubicación al ser la primera vez
        getLastLocation();
        //if(canGetLocation==true)
            startLocationUpdates();
        Log.d("TERMINA: ", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Se ha interrumpido la conexión con Google Play Services

        Log.e(TAG, "Se ha interrumpido la conexión con Google Play Services");
    }

    private void updateValores(Location loc) {
        Log.e("EMPIEZA: ", "updateValores");
        if (loc != null) {
            //Comunicamos el progreso
            Intent bcIntent = new Intent();
            bcIntent.setAction(ACTION_PROGRESO);
            bcIntent.putExtra("longitud", loc.getLongitude());
            bcIntent.putExtra("latitud", loc.getLatitude());
            sendBroadcast(bcIntent);
        } else {
            Log.e(TAG, "Variable 'loc' es nula");
        }
        Log.d("TERMINA: ", "updateValores");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("EMPIEZA: ", "onLocationChanged");
        Log.i(TAG, "Enviada nueva ubicación!");

        //Mostramos la nueva ubicación recibida
        mLastLocation = location;
        updateValores(location);
        Log.d("TERMINA: ", "onLocationChanged");
    }

    private void getLastLocation() {
        Log.e("EMPIEZA: ", "getLastLocation");
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            updateValores(mLastLocation);
        }
        Log.d("TERMINA: ", "getLastLocation");
    }

}
