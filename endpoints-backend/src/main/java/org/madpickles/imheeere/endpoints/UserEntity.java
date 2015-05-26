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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

public class UserEntity {

  private static final Logger logger = Logger.getLogger(UserEntity.class.getName());

  private String id;
  private Set<String> geofenceIds;

  private static String generateId(final String email) throws NoSuchAlgorithmException {
    final MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.reset();
    md.update(email.getBytes(Charset.forName("UTF-8")));
    return DatatypeConverter.printHexBinary(md.digest());
  }

  public void setId(final String id) {
    this.id = id;
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
      geofenceIds = new HashSet<String>();
    }
    return geofenceIds;
  }

  public void setGeofenceIds(final Set<String> geofenceIds) {
    this.geofenceIds = geofenceIds;
  }

  public void put() {
    final Entity entity;
    try {
      entity = datastore.get(KeyFactory.createKey("UserEntity", id));
    } catch (EntityNotFoundException e) {
      logger.info("put: Entity not found for id: " + id);
      entity = new Entity("UserEntity", id);
    }
    entity.setProperty("geofenceIds", getGeofenceIds());
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
    userEntity.setId(id);

    final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    try {
      final Entity entity = datastore.get(KeyFactory.createKey("UserEntity", id));
      userEntity.setGeofenceIds((Set<String>) entity.getProperty("geofenceIds"));
    } catch (EntityNotFoundException e) {
      logger.info("get: Entity not found for id: " + id);
    }
    return userEntity;
  }

}
