package org.smallmind.claxon.collector.indigenous.spring;

import java.util.function.Consumer;
import org.smallmind.claxon.collector.indigenous.IndigenousCollector;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class IndigenousCollectorFactoryBean implements FactoryBean<IndigenousCollector>, InitializingBean {

  private IndigenousCollector collector;
  private Consumer<String> messageConsumer;

  public void setMessageConsumer (Consumer<String> messageConsumer) {

    this.messageConsumer = messageConsumer;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return IndigenousCollector.class;
  }

  @Override
  public IndigenousCollector getObject () {

    return collector;
  }

  @Override
  public void afterPropertiesSet () {

    collector = new IndigenousCollector(messageConsumer);
  }
}
