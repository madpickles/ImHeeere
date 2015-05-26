package org.madpickles.imheeere;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionBroadcastReceiver extends BroadcastReceiver {
  private static final String TAG = "GoByBlimp";
  public GeofenceTransitionBroadcastReceiver() {
    super();
    Log.d(TAG, "BroadcaseReceiver created");
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    Log.d(TAG, "onHandleIntent: " + intent.toString());
    Intent localIntent = new Intent("GeofenceTransitionBroadcastReceiverIntent");
    GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
    if (geofencingEvent.hasError()) {
      String text = "ERROR: " + geofencingEvent.getErrorCode();
      Log.e(TAG, text);
      localIntent.putExtra("status", text);
    } else {
      List<String> triggeringGeofencesIdsList = new ArrayList<String>();
      for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
        triggeringGeofencesIdsList.add(geofence.getRequestId());
      }
      String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);
      int geofenceTransition = geofencingEvent.getGeofenceTransition();
      String text = "";
      transmitTransition(context, geofenceTransition, triggeringGeofencesIdsList);
      if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
        text = "Entering: " +
            triggeringGeofencesIdsString.replace(GeofenceManager.GEOFENCE_ID_PREFIX, "");
      } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
        text = "Exiting: " +
            triggeringGeofencesIdsString.replace(GeofenceManager.GEOFENCE_ID_PREFIX, "");
      } else {
        text = "Invalid: " +
            triggeringGeofencesIdsString.replace(GeofenceManager.GEOFENCE_ID_PREFIX, "");
      }
      Log.d(TAG, text);
      localIntent.putExtra("status", text);
    }
    NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_plusone_tall_off_client)
            .setContentTitle("Geofence BR Notification")
            .setContentText(localIntent.getStringExtra("status"));
    ((NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE)).notify(
        1235, mBuilder.build());
  }

  private void transmitTransition(final Context context, final int geofenceTransition,
                                  final List<String> triggeringGeofencesIdsList) {
    final ConnectivityManager connMgr = (ConnectivityManager)
        context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    if (networkInfo != null && networkInfo.isConnected()) {
      StringBuilder sb = new StringBuilder();
      sb.append("t=");
      sb.append(geofenceTransition);
      for (final String id : triggeringGeofencesIdsList) {
        sb.append("&gid=");
        sb.append(id);
      }
      new TransmitTransition().execute(sb.toString());
    } else {
      Log.d(TAG, "no network or not connected.");
    }
  }

  // TODO: Make endpoint API call to store transition.
  private class TransmitTransition extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
      try {
        final URL url = new URL("http://www.madpickles.org/geofence?" + params[0]);
        Log.d(TAG, "url: " + url.toString());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(10000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();
        final int response = connection.getResponseCode();
        Log.d(TAG, "response code: " + response);
      } catch (Exception e) {
        Log.e(TAG, "Fail doInBackground", e);
      }
      return null;
    }
  }

}
