package org.smallmind.persistence.orm.spring.data.mongo;

import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

public class MongoDataMappingContext extends MongoMappingContext {

  public void addEntities (Class[] entityClasses) {

    for (Class<?> entityClass : entityClasses) {
      addPersistentEntity(entityClass);
    }
  }
}
