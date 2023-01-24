package org.smallmind.persistence.orm.spring.data.mongo;

import java.util.Collections;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

public class MongoDataConverter extends MappingMongoConverter {

  public MongoDataConverter (MongoDatabaseFactory factory, boolean ensureIndexes, Class... entityClasses) {

    super(new DefaultDbRefResolver(factory), createMappingContext(ensureIndexes));

    setCustomConversions(conversions);
    setCodecRegistryProvider(factory);

    afterPropertiesSet();
  }

  private static MongoDataMappingContext createMappingContext (boolean ensureIndexes, Class... entityClasses) {

    MongoCustomConversions conversions = new MongoCustomConversions(Collections.emptyList());
    MongoDataMappingContext mappingContext = new MongoDataMappingContext();

    mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
    mappingContext.setAutoIndexCreation(ensureIndexes);
    mappingContext.addEntities(entityClasses);

    mappingContext.afterPropertiesSet();

    return mappingContext;
  }
}
