package org.smallmind.instrument.micrometer;

public class Tag {

  private String key;
  private String value;

  public Tag (String key, String value) {

    this.key = key;
    this.value = value;
  }

  public String getKey () {

    return key;
  }

  public String getValue () {

    return value;
  }

  @Override
  public String toString () {

    return new StringBuilder(key).append('=').append(value).toString();
  }

  @Override
  public int hashCode () {

    return (key.hashCode() * 31) + value.hashCode();
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Tag) && ((Tag)obj).getKey().equals(key) && ((Tag)obj).getValue().equals(value);
  }
}
