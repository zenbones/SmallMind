package org.smallmind.spark.wrapper.integration.spring;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ClassPathXmlSpringContextWrapperListener extends SpringContextWrapperListener {

   public static Class<? extends SpringContextWrapperListener> getImplementationClass () {

      return ClassPathXmlSpringContextWrapperListener.class;
   }

   public ConfigurableApplicationContext[] loadApplicationContexts (String[] args) {

      return new ConfigurableApplicationContext[] {new ClassPathXmlApplicationContext(args)};
   }
}