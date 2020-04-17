package org.smallmind.claxon.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;

public class ClaxonConfiguration {

  private Clock clock = SystemClock.instance();
  private Stint collectionStint = new Stint(1, TimeUnit.SECONDS);
  private Tag[] registryTags = new Tag[0];
  private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
  private Map<String, String> prefixMap = new HashMap<>();

  public Clock getClock () {

    return clock;
  }

  public void setClock (Clock clock) {

    this.clock = clock;
  }

  public Stint getCollectionStint () {

    return collectionStint;
  }

  public void setCollectionStint (Stint collectionStint) {

    this.collectionStint = collectionStint;
  }

  public Tag[] getRegistryTags () {

    return registryTags;
  }

  public void setRegistryTags (Tag[] registryTags) {

    this.registryTags = registryTags;
  }

  public TimeUnit getDefaultTimeUnit () {

    return defaultTimeUnit;
  }

  public void setDefaultTimeUnit (TimeUnit defaultTimeUnit) {

    this.defaultTimeUnit = defaultTimeUnit;
  }

  public Map<String, String> getPrefixMap () {

    return prefixMap;
  }

  public void setPrefixMap (Map<String, String> prefixMap) {

    this.prefixMap = prefixMap;
  }
}
