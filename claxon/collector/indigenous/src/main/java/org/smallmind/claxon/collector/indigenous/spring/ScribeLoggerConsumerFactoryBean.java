package org.smallmind.claxon.collector.indigenous.spring;

import org.smallmind.claxon.collector.indigenous.ScribeLoggerConsumer;
import org.smallmind.scribe.pen.Level;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ScribeLoggerConsumerFactoryBean implements FactoryBean<ScribeLoggerConsumer>, InitializingBean {

  private ScribeLoggerConsumer scribeLoggerConsumer;
  private Class<?> caller;
  private Level level = Level.DEBUG;

  public void setCaller (Class<?> caller) {

    this.caller = caller;
  }

  public void setLevel (Level level) {

    this.level = level;
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  @Override
  public Class<?> getObjectType () {

    return ScribeLoggerConsumer.class;
  }

  @Override
  public ScribeLoggerConsumer getObject () {

    return scribeLoggerConsumer;
  }

  @Override
  public void afterPropertiesSet () {

    scribeLoggerConsumer = new ScribeLoggerConsumer(caller, level);
  }
}
