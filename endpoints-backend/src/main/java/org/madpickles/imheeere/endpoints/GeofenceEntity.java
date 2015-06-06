package org.madpickles.imheeere.endpoints;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nullable;

public class GeofenceEntity {

  private static final Logger logger = Logger.getLogger(GeofenceEntity.class.getName());
  private static final String GEOFENCE_ENTITY = "GeofenceEntity";
  private static final String LAT = "lat";
  private static final String LNG = "lng";
  private static final String GEOFENCE_NAME = "geofenceName";
  private static final String MEMBERS = "members";
  private static final String LAST_UPDATED = "lastUpdated";
  private static final String SHAREABLE_ID = "shareableId";

  private double lat;
  private double lng;
  private String geofenceName;
  private Set<String> members;
  private Date lastUpdated;
  private String shareableId;

  public GeofenceEntity() {}

  public GeofenceEntity(final double lat, final double lng, final String geofenceName) {
    this.lat = lat;
    this.lng = lng;
    this.geofenceName = geofenceName;
  }

  public void addMember(final String member) {
    members.add(member);
  }

  public Set<String> getMembers() {
    return members;
  }

  public String getShareableId() { return shareableId; }

  public void setFromEndpoint(final double lat, final double lng, final String geofenceName) {
    this.lat = lat;
    this.lng = lng;
    this.geofenceName = geofenceName;
  }

  public Long put() {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity;
    if (shareableId == null) {
      entity = new Entity(GEOFENCE_ENTITY);
    } else {
      final Query query = new Query(GEOFENCE_ENTITY);
      query.setFilter(Query.FilterOperator.EQUAL.of(SHAREABLE_ID, shareableId));
      final Iterator<Entity> entities = datastore.prepare(query).asIterator();
      if (entities.hasNext()) {
        entity = entities.next();
      } else {
        logger.severe("put: shareableId not found: " + shareableId);
        return null;
      }
    }

    entity.setProperty(LAT, lat);
    entity.setProperty(LNG, lng);
    entity.setProperty(GEOFENCE_NAME, geofenceName);
    entity.setProperty(MEMBERS, members);
    entity.setProperty(LAST_UPDATED, new Date());
    entity.setProperty(SHAREABLE_ID, shareableId);

    // TODO: Transaction
    final long id = datastore.put(entity).getId();
    // If this is a creation of an entity, then need to generate the shareable id and update.
    if (shareableId == null) {
      shareableId = generateShareableId(id);
      entity.setProperty(SHAREABLE_ID, shareableId);
      datastore.put(entity);
    }
    return id;
  }

  private String generateShareableId(final long id) {
    // 62^6 = 56.8B
    // TODO: Convert to 6 character string.
    return String.valueOf(id);
  }

  public static GeofenceEntity get(final @Nullable String shareableId) {
    final GeofenceEntity geofenceEntity = new GeofenceEntity();
    if (shareableId == null || shareableId.equals("")) {
      return geofenceEntity;
    }
    final Query query = new Query(GEOFENCE_ENTITY);
    query.setFilter(Query.FilterOperator.EQUAL.of(SHAREABLE_ID, shareableId));
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    final Iterator<Entity> entities = datastore.prepare(query).asIterator();
    if (entities.hasNext()) {
      final Entity entity = entities.next();
      geofenceEntity.lat = (double) entity.getProperty(LAT);
      geofenceEntity.lng = (double) entity.getProperty(LNG);
      geofenceEntity.geofenceName = (String) entity.getProperty(GEOFENCE_NAME);
      final ArrayList<String> members = (ArrayList<String>) entity.getProperty(MEMBERS);
      if (members != null) {
        geofenceEntity.members = new HashSet<>(members);
      }
      geofenceEntity.lastUpdated = (Date) entity.getProperty(LAST_UPDATED);
      geofenceEntity.shareableId = (String) entity.getProperty(SHAREABLE_ID);
      return geofenceEntity;
    }
    return geofenceEntity;
  }

  @Override
  public String toString() {
    return lat + ", " + lng + ", " + geofenceName + ", " + Arrays.toString(members.toArray()) + ", " + shareableId;
  }

  public GeofenceBean toBean() {
    final GeofenceBean geofenceBean = new GeofenceBean();
    geofenceBean.setGeofenceName(geofenceName);
    geofenceBean.setLat(lat);
    geofenceBean.setLng(lng);
    geofenceBean.setShareableId(shareableId);
    return geofenceBean;
  }
}
