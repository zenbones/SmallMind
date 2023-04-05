package org.smallmind.mongodb.throng.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.ThrongRuntimeException;
import org.smallmind.mongodb.throng.annotation.Polymorphic;

public class ThrongPolymorphicMultiplexer<T> {

  private final HashMap<String, ThrongPropertiesCodec<?>> polymorphicCodecMap = new HashMap<>();
  private final Class<T> entityClass;
  private final String key;

  public ThrongPolymorphicMultiplexer (Class<T> entityClass, Polymorphic polymorphic, CodecRegistry codecRegistry, HashMap<Class<?>, ThrongEmbeddedCodec<?>> embeddedReferenceMap, boolean storeNulls)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    this.entityClass = entityClass;

    key = polymorphic.key().isEmpty() ? "java/object" : polymorphic.key();

    for (Class<?> polymorphicClass : polymorphic.value()) {
      if (!polymorphicClass.isAssignableFrom(entityClass)) {
        throw new ThrongMappingException("The declared polymorphic class(%s) is not assignable from the declaring type(%s)", polymorphicClass.getName(), entityClass.getName());
      } else {
        polymorphicCodecMap.put(polymorphicClass.getName(), new ThrongPropertiesCodec<>(new ThrongProperties<>(polymorphicClass, codecRegistry, embeddedReferenceMap, storeNulls)));
      }
    }
  }

  public Class<T> getEntityClass () {

    return entityClass;
  }

  public String getKey () {

    return key;
  }

  public ThrongPropertiesCodec<?> getCodec (String polymorphicClassName) {

    ThrongPropertiesCodec<?> polymorphicCodec;

    if ((polymorphicCodec = polymorphicCodecMap.get(polymorphicClassName)) == null) {
      throw new ThrongRuntimeException("No known codec for polymorphic key(%s) of entity type(%s)", polymorphicClassName, entityClass.getName());
    } else {

      return polymorphicCodec;
    }
  }
}
