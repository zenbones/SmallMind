package org.smallmind.mongodb.throng;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.bson.codecs.configuration.CodecRegistry;
import org.smallmind.nutsnbolts.reflection.FieldAccessor;
import org.smallmind.nutsnbolts.reflection.FieldUtility;

public class ThrongProperties extends HashMap<String, ThrongProperty> {

  public ThrongProperties (Class<?> entityClass, CodecRegistry codecRegistry)
    throws ThrongMappingException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

    for (FieldAccessor fieldAccessor : FieldUtility.getFieldAccessors(entityClass)) {

      Property propertyAnnotation;

      if ((propertyAnnotation = fieldAccessor.getField().getAnnotation(Property.class)) != null) {

        Codec codecAnnotation;
        org.bson.codecs.Codec<?> codec;

        if ((codecAnnotation = fieldAccessor.getField().getAnnotation(Codec.class)) != null) {
          codec = codecAnnotation.value().getConstructor().newInstance();
        } else if (fieldAccessor.getType().getAnnotation(Embedded.class) != null) {
          new ThrongEmbedded(fieldAccessor.getType(), codecRegistry);
          codec = null;
        } else if ((codec = codecRegistry.get(fieldAccessor.getType())) == null) {
          throw new ThrongMappingException("No known codec for field(%s) of type(%s) in entity(%s)", fieldAccessor.getName(), fieldAccessor.getType().getName(), entityClass.getName());
        }

        put(fieldAccessor.getName(), new ThrongProperty(fieldAccessor, codec, propertyAnnotation.value().isEmpty() ? fieldAccessor.getName() : propertyAnnotation.value()));
      }
    }
  }
}
