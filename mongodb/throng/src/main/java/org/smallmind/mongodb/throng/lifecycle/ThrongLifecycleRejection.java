package org.smallmind.mongodb.throng.lifecycle;

import java.lang.reflect.Method;
import org.smallmind.mongodb.throng.ThrongMappingException;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostPersist;
import org.smallmind.mongodb.throng.lifecycle.annotation.PreLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PrePersist;

public class ThrongLifecycleRejection {

  public static void reject (Class<?> embeddedClass)
    throws ThrongMappingException {

    for (Method method : embeddedClass.getMethods()) {
      if (method.getAnnotation(PreLoad.class) != null) {
        throw new ThrongMappingException("Lifecycle annotations may only be used within @Entity classes");
      }
      if (method.getAnnotation(PostLoad.class) != null) {
        throw new ThrongMappingException("Lifecycle annotations may only be used within @Entity classes");
      }
      if (method.getAnnotation(PrePersist.class) != null) {
        throw new ThrongMappingException("Lifecycle annotations may only be used within @Entity classes");
      }
      if (method.getAnnotation(PostPersist.class) != null) {
        throw new ThrongMappingException("Lifecycle annotations may only be used within @Entity classes");
      }
    }
  }
}
