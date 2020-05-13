package org.smallmind.claxon.registry.spring;

import java.util.HashMap;
import java.util.Map;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Collector;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ClaxonRegistryFactoryBean implements FactoryBean<ClaxonRegistry>, InitializingBean, DisposableBean {

  private ClaxonRegistry registry;
  private ClaxonConfiguration configuration = new ClaxonConfiguration();
  private HashMap<String, Collector> collectorMap = new HashMap<>();

  public void setConfiguration (ClaxonConfiguration configuration) {

    this.configuration = configuration;
  }

  public void setCollectorMap (HashMap<String, Collector> collectorMap) {

    this.collectorMap = collectorMap;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return ClaxonRegistry.class;
  }

  @Override
  public ClaxonRegistry getObject () {

    return registry;
  }

  @Override
  public void destroy ()
    throws InterruptedException {

    if (registry != null) {
      registry.stop();
    }
  }

  @Override
  public void afterPropertiesSet () {

    registry = new ClaxonRegistry(configuration);

    for (Map.Entry<String, Collector> collectorEntry : collectorMap.entrySet()) {
      registry.bind(collectorEntry.getKey(), collectorEntry.getValue());
    }

    registry.asInstrumentRegistry();
  }
}
