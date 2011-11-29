package org.smallmind.persistence.cache.ehcache;

import net.sf.ehcache.config.CacheConfiguration;

public enum TransactionalMode {

  OFF, LOCAL, XA;

  public CacheConfiguration.TransactionalMode asConfiguration () {

    switch (this) {
      case OFF:
        return CacheConfiguration.TransactionalMode.OFF;
      case XA:
        return CacheConfiguration.TransactionalMode.XA;
      default:
        throw new UnsupportedOperationException(this.name());
    }
  }

  public String asString () {

    switch (this) {
      case LOCAL:
        return "local";
      default:
        throw new UnsupportedOperationException(this.name());
    }
  }
}
