package org.smallmind.persistence.cache.ehcache;

public enum MemoryUnit {

  KILOBYTES("k"), MEGABYTES("m"), GIGABYTES("g"), TERABYTES("t");

  private String code;

  private MemoryUnit (String code) {

    this.code = code;
  }

  public String getCode () {

    return code;
  }
}
