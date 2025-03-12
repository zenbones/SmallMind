package org.smallmind.web.jersey.json;

import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.web.jersey.spring.ResourceConfigExtension;

public class JsonProviderExtension extends ResourceConfigExtension {

  @Override
  public void apply (ResourceConfig resourceConfig) {

    resourceConfig.register(JsonProvider.class);
  }
}
