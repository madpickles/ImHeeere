package org.madpickles.imheeere.endpoints;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GeofenceEntityTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void testPut() {
    final GeofenceEntity entity = new GeofenceEntity();
    final Long id = entity.put();
    final GeofenceEntity geofenceEntity = GeofenceEntity.get(String.valueOf(id));
    Assert.assertNotNull(geofenceEntity);
    final String memberId = "foo";
    Assert.assertTrue(geofenceEntity.getMembers().isEmpty());
    geofenceEntity.addMember(memberId);
    final Long id1 = geofenceEntity.put();
    Assert.assertEquals(id.longValue(), id1.longValue());
    final GeofenceEntity geofenceEntity1 = GeofenceEntity.get(String.valueOf(id1));
    Assert.assertTrue(geofenceEntity1.getMembers().contains(memberId));
    Assert.assertEquals(1, geofenceEntity1.getMembers().size());
    Assert.assertEquals(id.toString(), geofenceEntity1.getShareableId());
  }

}
