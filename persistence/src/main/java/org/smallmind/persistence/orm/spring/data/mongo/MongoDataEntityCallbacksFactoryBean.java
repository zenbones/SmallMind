package org.smallmind.persistence.orm.spring.data.mongo;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class MongoDataEntityCallbacksFactoryBean implements FactoryBean<MongoDataEntityCallbacks>, InitializingBean {

  private MongoDataEntityCallbacks mongoDataEntityCallbacks;
  private MongoDataEntityCallback[] callbacks;

  public void setCallbacks (MongoDataEntityCallback[] callbacks) {

    this.callbacks = callbacks;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return MongoDataEntityCallbacks.class;
  }

  @Override
  public MongoDataEntityCallbacks getObject ()
    throws Exception {

    return mongoDataEntityCallbacks;
  }

  @Override
  public void afterPropertiesSet () {

    mongoDataEntityCallbacks = new MongoDataEntityCallbacks(callbacks);
  }
}
