package org.madpickles.imheeere.endpoints;

import java.util.Set;

public class GeofenceBean {
  private double lat;
  private double lng;
  private String geofenceName;
  private String shareableId;

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLng() {
    return lng;
  }

  public void setLng(double lng) {
    this.lng = lng;
  }

  public String getGeofenceName() {
    return geofenceName;
  }

  public void setGeofenceName(String geofenceName) {
    this.geofenceName = geofenceName;
  }

  public String getShareableId() {
    return shareableId;
  }

  public void setShareableId(String shareableId) {
    this.shareableId = shareableId;
  }
}
