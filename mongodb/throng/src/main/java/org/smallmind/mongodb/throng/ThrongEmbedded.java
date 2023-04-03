package org.smallmind.mongodb.throng;

import java.lang.reflect.InvocationTargetException;
import org.bson.codecs.configuration.CodecRegistry;

public class ThrongEmbedded {

  private final ThrongProperties throngProperties;

  public ThrongEmbedded (Class<?> embeddedClass, CodecRegistry codecRegistry)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    throngProperties = new ThrongProperties(embeddedClass, codecRegistry);
  }
}
