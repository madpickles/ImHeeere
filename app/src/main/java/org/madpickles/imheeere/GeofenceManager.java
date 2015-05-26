package org.madpickles.imheeere;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.appspot.im_heeere.geofenceApi.GeofenceApi;
import com.appspot.im_heeere.geofenceApi.model.MyBean;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeofenceManager implements ResultCallback {

  private static final String TAG = "GoByCar";
  private static final String SAVED_IDS_KEY = "SAVED_IDS";
  public static final String GEOFENCE_ID_PREFIX = "org.madpickles.imheeere.geofenceids.";
  private final Set<String> geofenceIds;
  private final PendingIntent geofencePendingIntent;
  private final GoogleApiClient googleApiClient;
  private final ArrayAdapter geofenceListAdapter;
  private final SharedPreferences preferences;
  private final Context context;

  public GeofenceManager(final Context context, final GoogleApiClient googleApiClient) {
    Log.d(TAG, "GeofenceManager");
    this.context = context;
    this.googleApiClient = googleApiClient;
    preferences = context.getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, context.MODE_PRIVATE);
    // Make a copy: http://stackoverflow.com/questions/21396358/sharedpreferences-putstringset-doesnt-work
    geofenceIds = new HashSet<String>(preferences.getStringSet(SAVED_IDS_KEY, new HashSet<String>()));
    final ListView geofenceList = (ListView) ((Activity) context).findViewById(R.id.geofence_list);
    geofenceListAdapter = new ArrayAdapter(context, R.layout.geofence_list_item, new ArrayList<String>(geofenceIds));
    geofenceList.setAdapter(geofenceListAdapter);
    final AdapterView.OnItemLongClickListener geofenceLongClickHandler = new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "parent: " + parent.toString() + ", view: " + view.toString() + ", position: " + position + ", id: " + id);
        remove(geofenceListAdapter.getItem(position).toString());
        return true;
      }
    };
    geofenceList.setOnItemLongClickListener(geofenceLongClickHandler);

    Log.d(TAG, geofenceIds.toString());
    // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
    // calling addGeofences() and removeGeofences().
    final Intent intent = new Intent(context, GeofenceTransitionBroadcastReceiver.class);
    geofencePendingIntent =
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public void add(final String geofenceName, final String lat, final String lng) {
    final String id = geofenceName + ": " + lat + ", " + lng;
    final Geofence geofence = new Geofence.Builder()
        .setRequestId(GEOFENCE_ID_PREFIX + id)
            // Is anything less than 100m any more accurate?
        .setCircularRegion(Double.parseDouble(lat), Double.parseDouble(lng), 100)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)  // Is this really "never"?
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
        .build();
    final List<Geofence> geofencesToAdd = new ArrayList<Geofence>();
    geofencesToAdd.add(geofence);
    final GeofencingRequest addGeofencingRequestReceiver = new GeofencingRequest.Builder()
        .addGeofences(geofencesToAdd)
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .build();
    LocationServices.GeofencingApi.addGeofences(
        googleApiClient, addGeofencingRequestReceiver, geofencePendingIntent)
        .setResultCallback(this);
    if (!geofenceIds.contains(id)) {
      geofenceIds.add(id);
      geofenceListAdapter.add(id);
      geofenceListAdapter.sort(String.CASE_INSENSITIVE_ORDER);
    }
    Log.d(TAG, "Geofence added: " + id);
  }

  public void remove(final String id) {
    final List<String> geofenceIdsToRemove = new ArrayList<String>();
    geofenceIdsToRemove.add(GEOFENCE_ID_PREFIX + id);
    // Since the same intent is used for all geofences, remove by id.
    LocationServices.GeofencingApi.removeGeofences(googleApiClient, geofenceIdsToRemove)
        .setResultCallback(this);
    geofenceIds.remove(id);
    geofenceListAdapter.remove(id);
    Log.d(TAG, "Geofence removed: " + id);
  }

  @Override
  public void onResult(Result result) {
    // Anything worth doing here?
    Log.d(TAG, "result: " + result.toString());
  }

  public void saveState() {
    Log.d(TAG, "saveState");
    final SharedPreferences.Editor editor = preferences.edit();
    editor.putStringSet(SAVED_IDS_KEY, geofenceIds);
    if (!editor.commit()) {
      Log.e(TAG, "Unable to commit preferences: " + SAVED_IDS_KEY + " = " + geofenceIds.toString());
    }
    Log.d(TAG, geofenceIds.toString() + ", " + preferences.getStringSet(SAVED_IDS_KEY, new HashSet<String>()));
    final ConnectivityManager connMgr = (ConnectivityManager)
        this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.isConnected()) {
      new TestAsyncTask().execute();
    } else {
      Log.d(TAG, "no network or not connected.");
    }
  }

  // TODO: Make endpoint API call to retrieve geofences
  // Also create way to add and delete geofences.
  private class TestAsyncTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      final GeofenceApi geofenceApiService = ((MainActivity) context).getGeofenceApiService();
      if (geofenceApiService == null) {
        Log.d(TAG, "not initialized yet.");
        // Service not initialized yet.
        return null;
      }
      try {
        final MyBean response = geofenceApiService.sayHi("foo").execute();
        Log.d(TAG, "sayHi: " + response.toString());
      } catch (IOException e) {
        Log.d(TAG, "sayHi: ", e);
      }
      try {
        final MyBean response = geofenceApiService.sayHiAuthenticated("bar").execute();
        Log.d(TAG, "sayHiAuthenticated: " + response.toString());
      } catch (IOException e) {
        Log.d(TAG, "sayHiAuthenticated: ", e);
      }
      return null;
    }
  }

}
