package org.madpickles.imheeere.endpoints;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserEntityTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private static final String TEST_EMAIL = "foo@bar.com";
  private static final String TEST_ID = "0C7E6A405862E402EB76A70F8A26FC732D07C32931E9FAE9AB1582911D2E8A3B";

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testGetNewEntity() throws NoSuchAlgorithmException {
    final UserEntity entity = UserEntity.get(TEST_EMAIL);
    assertEquals(TEST_ID, entity.getId());
    assertEquals(0, entity.getGeofenceIds().size());
  }

  @Test
  public void testGetExistingEntity() throws NoSuchAlgorithmException {
    final String geofenceId0 = "geofenceId0";
    final String geofenceId1 = "geofenceId1";
    final UserEntity entity = UserEntity.get(TEST_EMAIL);
    entity.addGeofenceId(geofenceId0);
    entity.addGeofenceId(geofenceId1);
    entity.put();

    final UserEntity entityFromDatastore = UserEntity.get(TEST_EMAIL);
    assertEquals(TEST_ID, entityFromDatastore.getId());
    final Set<String> geofenceIds = entityFromDatastore.getGeofenceIds();
    assertEquals(2, geofenceIds.size());
    assertTrue(geofenceIds.contains(geofenceId0));
    assertTrue(geofenceIds.contains(geofenceId1));
  }
}
