package org.smallmind.persistence.orm.spring.hibernate;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import org.smallmind.persistence.orm.DataSource;
import org.smallmind.persistence.orm.hibernate.HibernateDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class HibernateAnnotationSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

   private static final HashMap<String, HashSet<Class>> ANNOTATED_CLASS_DATA_SOURCE_MAP = new HashMap<String, HashSet<Class>>();

   private static final Class[] NO_CLASSES = new Class[0];

   private Class<? extends Annotation>[] markedAnnotations;

   public static Class[] getAnnotatedClasses () {

      return getAnnotatedClasses(null);
   }

   public static Class[] getAnnotatedClasses (String dataSourceKey) {

      Class[] annotatedClasses;
      HashSet<Class> annotatedClassSet;

      if ((annotatedClassSet = ANNOTATED_CLASS_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
         return NO_CLASSES;
      }

      annotatedClasses = new Class[annotatedClassSet.size()];
      annotatedClassSet.toArray(annotatedClasses);

      return annotatedClasses;
   }

   public void setMarkedAnnotations (Class<? extends Annotation>[] markedAnnotations) {

      this.markedAnnotations = markedAnnotations;
   }

   private boolean hasMarkedAnnotation (Class persistentClass) {

      for (Class<? extends Annotation> markedAnnotation : markedAnnotations) {
         if (persistentClass.isAnnotationPresent(markedAnnotation)) {

            return true;
         }
      }

      return false;
   }

   public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
      throws BeansException {

      Class<?> beanClass;
      Class persistentClass;
      Annotation dataSourceAnnotation;
      HashSet<Class> annotatedClassSet;
      String dataSourceKey = null;

      for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
         if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
            if (HibernateDao.class.isAssignableFrom(beanClass)) {
               if ((dataSourceAnnotation = beanClass.getAnnotation(DataSource.class)) != null) {
                  dataSourceKey = ((DataSource)dataSourceAnnotation).value();
               }

               if ((annotatedClassSet = ANNOTATED_CLASS_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
                  ANNOTATED_CLASS_DATA_SOURCE_MAP.put(dataSourceKey, annotatedClassSet = new HashSet<Class>());
               }

               try {
                  persistentClass = (Class)((ParameterizedType)beanClass.getMethod("getManagedClass").getGenericReturnType()).getActualTypeArguments()[0];
                  if (hasMarkedAnnotation(persistentClass)) {
                     annotatedClassSet.add(persistentClass);
                  }
               }
               catch (NoSuchMethodException noSuchMethodException) {
                  throw new FatalBeanException("HibernateDao classes are expected to contain the method getManagedClass()", noSuchMethodException);
               }
            }
         }
      }
   }
}