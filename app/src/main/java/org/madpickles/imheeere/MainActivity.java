package org.madpickles.imheeere;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.appspot.im_heeere.geofenceApi.GeofenceApi;
import com.appspot.im_heeere.geofenceApi.model.MyBean;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import java.io.IOException;


public class MainActivity extends ActionBarActivity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  public static final String SHARED_PREFERENCES_NAME = "org.madpickles.imheeere.preferences";

  private static final String TAG = "GoDogGo";
  private static final String PREFERRED_ACCOUNT_NAME = "PREFERRED_ACCOUNT_NAME";
  private static final int REQUEST_ACCOUNT_PICKER = 2;
  private GeofenceManager geofenceManager;
  private GoogleApiClient googleApiClient;
  private Location lastLocation;
  private LocationRequest locationRequest;
  private GeofenceApi geofenceApiService;
  private GoogleAccountCredential credential;

  public GeofenceApi getGeofenceApiService() {
    return geofenceApiService;
  }

  public void addGeofence(View view) {
    final EditText geofenceName = (EditText) findViewById(R.id.geofence_name);
    final EditText lat = (EditText) findViewById(R.id.latitude);
    final EditText lng = (EditText) findViewById(R.id.longitude);
    geofenceManager.add(geofenceName.getText().toString(), lat.getText().toString(),
        lng.getText().toString());
  }

  public void currentLatLng(View view) {
    Log.d(TAG, "currentLatLng");
    final Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    final double lat;
    final double lng;
    if (location == null) {
      lat = 0;
      lng = 0;
    } else {
      lat = location.getLatitude();
      lng = location.getLongitude();
    }
    ((EditText) findViewById(R.id.latitude)).setText(Double.toString(lat));
    ((EditText) findViewById(R.id.longitude)).setText(Double.toString(lng));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate");
    super.onCreate(savedInstanceState);

    final SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_NAME, this.MODE_PRIVATE);
    // Must be same as WEB_ID in endpoint backend.
    this.credential = GoogleAccountCredential.usingAudience(this,
        "server:client_id:172211572567-ji431ela2ppnd56pnoe7nkg1tbds2ib7.apps.googleusercontent.com");
    final String accountName = preferences.getString(PREFERRED_ACCOUNT_NAME, null);
    Log.d(TAG, "accountName: " + accountName);
    if (accountName == null) {
      startActivityForResult(credential.newChooseAccountIntent(), 2);
    } else {
      buildGeofenceApiService(accountName);
    }

    setContentView(R.layout.activity_main);
    buildGoogleApiClient();
    geofenceManager = new GeofenceManager(this, googleApiClient);
  }

  @Override
  public void onConnected(Bundle bundle) {
    Log.d(TAG, "onConnected");
  }

  @Override
  protected void onStart() {
    super.onStart();
    Log.d(TAG, "onStart");
    googleApiClient.connect();
    Log.d(TAG, "Connected GoogleApiClient");
  }

  @Override
  protected void onPause() {
    super.onPause();
    Log.d(TAG, "onPause");
    geofenceManager.saveState();
    if (googleApiClient != null && googleApiClient.isConnected()) {
      googleApiClient.disconnect();
      Log.d(TAG, "Disconnected GoogleApiClient");
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "onStop");
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.i(TAG, "connctionSuspended: " + i);

  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e(TAG, "onConnectionFailed connectionResult: " + connectionResult.toString());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_ACCOUNT_PICKER:
        if (data != null && data.getExtras() != null) {
          final String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
          Log.d(TAG, "choosing accountName: " + accountName);
          if (accountName != null) {
            final SharedPreferences preferences = getSharedPreferences(SHARED_PREFERENCES_NAME, this.MODE_PRIVATE);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PREFERRED_ACCOUNT_NAME, accountName);
            editor.commit();
            buildGeofenceApiService(accountName);
          }
        }
        break;
    }
  }

  private synchronized void buildGoogleApiClient() {
    googleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(LocationServices.API)
        .build();
    Log.d(TAG, "googleApiClient built.");
  }

  private synchronized void buildGeofenceApiService(final String accountName) {
    credential.setSelectedAccountName(accountName);
    GeofenceApi.Builder builder = new GeofenceApi.Builder(AndroidHttp.newCompatibleTransport(),
        new AndroidJsonFactory(), credential)
        // options for running against local devappserver
        // - 10.0.2.2 is localhost's IP address in Android emulator
        // - turn off compression when running against local devappserver
        // .setRootUrl("http://10.0.2.2:8080/_ah/api/")
        .setRootUrl("https://im-heeere.appspot.com/_ah/api/")
        .setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
          @Override
          public void initialize(AbstractGoogleClientRequest<?> abstractGoogleClientRequest) throws IOException {
            abstractGoogleClientRequest.setDisableGZipContent(true);
          }
        });
    // end options for devappserver
    geofenceApiService = builder.build();
  }
}
