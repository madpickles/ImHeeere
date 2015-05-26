package org.madpickles.imheeere.endpoints;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.users.User;

import java.util.logging.Logger;

import javax.inject.Named;

@Api(name = "geofenceApi",
    version = "v1",
    namespace = @ApiNamespace(ownerDomain = "imheeere.madpickles.org",
                              ownerName = "imheeere.madpickles.org",
                              packagePath = ""),
    scopes = {"https://www.googleapis.com/auth/userinfo.email"},
    clientIds = {
        "172211572567-gmos2tsqvcftk7n96qe1jkou9kk07g3f.apps.googleusercontent.com",  // Android
        "172211572567-ji431ela2ppnd56pnoe7nkg1tbds2ib7.apps.googleusercontent.com",  // Web
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID
    },
    audiences = {"172211572567-ji431ela2ppnd56pnoe7nkg1tbds2ib7.apps.googleusercontent.com"})
public class GeofenceEndpoint {

  private static final Logger logger = Logger.getLogger(GeofenceEndpoint.class.getName());

    @ApiMethod(name = "sayHi")
    public MyBean sayHi(@Named("name") String name) {
        MyBean response = new MyBean();
        response.setData("Hi, " + name);
      logger.info(name);
        return response;
    }

  @ApiMethod(name = "sayHiAuthenticated", httpMethod = "post")
  public MyBean sayHiAuthenticated(User user, @Named("name") String name) {
    if (user == null) {
      logger.info("NULL user.");
      return null;
    }
    MyBean response = new MyBean();
    final String data = "Hi, " + name + ", User: " + user.toString();
    response.setData(data);
    logger.info(data);
    return response;
  }

}
