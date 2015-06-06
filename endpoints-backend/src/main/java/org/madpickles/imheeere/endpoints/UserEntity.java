package org.madpickles.imheeere.endpoints;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

public class UserEntity {

  private static final Logger logger = Logger.getLogger(UserEntity.class.getName());
  private static final String USER_ENTITY = "UserEntity";
  private static final String GEOFENCE_IDS = "geofenceIds";

  private String id;  // The datastore Key's name/id.
  private Set<String> geofenceIds;

  private static String generateId(final String email) throws NoSuchAlgorithmException {
    final MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.reset();
    md.update(email.getBytes(Charset.forName("UTF-8")));
    return DatatypeConverter.printHexBinary(md.digest());
  }

  public String getId() {
    return id;
  }

  public void addGeofenceId(final String geofenceId) {
    final Set<String> geofenceIds = getGeofenceIds();
    geofenceIds.add(geofenceId);
  }

  public Set<String> getGeofenceIds() {
    if (geofenceIds == null) {
      geofenceIds = new HashSet<>();
    }
    return geofenceIds;
  }

  public void put() {
    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity entity;
    try {
      entity = datastore.get(KeyFactory.createKey(USER_ENTITY, id));
    } catch (EntityNotFoundException e) {
      logger.info("put: Entity not found for id: " + id);
      entity = new Entity(USER_ENTITY, id);
    }
    entity.setProperty(GEOFENCE_IDS, getGeofenceIds());
    datastore.put(entity);
  }

  public static UserEntity get(final String email) {
    final String id;
    try {
      id = generateId(email);
    } catch (NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, email, e);
      return null;
    }

    final UserEntity userEntity = new UserEntity();
    userEntity.id = id;

    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    try {
      final Entity entity = datastore.get(KeyFactory.createKey(USER_ENTITY, id));
      userEntity.geofenceIds = new HashSet((ArrayList<String>) entity.getProperty(GEOFENCE_IDS));
    } catch (EntityNotFoundException e) {
      logger.info("get: Entity not found for id: " + id);
    }
    return userEntity;
  }

}
