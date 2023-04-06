package org.smallmind.mongodb.throng.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import org.bson.BsonDocument;
import org.smallmind.mongodb.throng.event.PostLoad;
import org.smallmind.mongodb.throng.event.PostPersist;
import org.smallmind.mongodb.throng.event.PreLoad;
import org.smallmind.mongodb.throng.event.PrePersist;

public class ThrongEvents<T> {

  private final LinkedList<Method> preLoadMethods = new LinkedList<>();
  private final LinkedList<Method> postLoadMethods = new LinkedList<>();
  private final LinkedList<Method> prePersistMethods = new LinkedList<>();
  private final LinkedList<Method> postPersistMethods = new LinkedList<>();

  public ThrongEvents (Class<T> entityClass) {

    for (Method method : entityClass.getMethods()) {
      if (method.getAnnotation(PreLoad.class) != null) {
        preLoadMethods.add(method);
      }
      if (method.getAnnotation(PostLoad.class) != null) {
        postLoadMethods.add(method);
      }
      if (method.getAnnotation(PrePersist.class) != null) {
        prePersistMethods.add(method);
      }
      if (method.getAnnotation(PostPersist.class) != null) {
        postPersistMethods.add(method);
      }
    }
  }

  public void executePreLoad (T value, BsonDocument bsonDocument)
    throws InvocationTargetException, IllegalAccessException {

    for (Method method : preLoadMethods) {
      method.invoke(value, bsonDocument);
    }
  }

  public void executePostLoad (T value)
    throws InvocationTargetException, IllegalAccessException {

    for (Method method : preLoadMethods) {
      method.invoke(value);
    }
  }

  public void executePrePersist (T value)
    throws InvocationTargetException, IllegalAccessException {

    for (Method method : prePersistMethods) {
      method.invoke(value);
    }
  }

  public void executePostPersist (T value, BsonDocument bsonDocument)
    throws InvocationTargetException, IllegalAccessException {

    for (Method method : prePersistMethods) {
      method.invoke(value, bsonDocument);
    }
  }
}
