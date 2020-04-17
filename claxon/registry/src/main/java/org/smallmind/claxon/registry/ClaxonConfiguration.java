package org.smallmind.claxon.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;
import org.smallmind.nutsnbolts.util.DotNotation;

public class ClaxonConfiguration {

  private Clock clock = SystemClock.instance();
  private Stint collectionStint = new Stint(1, TimeUnit.SECONDS);
  private Tag[] registryTags = new Tag[0];
  private TimeUnit defaultTimeUnit = TimeUnit.MILLISECONDS;
  private Map<DotNotation, String> prefixMap = new HashMap<>();
  private String defaultPrefix;

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

  public Map<DotNotation, String> getPrefixMap () {

    return prefixMap;
  }

  public void setPrefixMap (Map<DotNotation, String> prefixMap) {

    this.prefixMap = prefixMap;
  }

  public String getDefaultPrefix () {

    return defaultPrefix;
  }

  public void setDefaultPrefix (String defaultPrefix) {

    this.defaultPrefix = defaultPrefix;
  }
}
