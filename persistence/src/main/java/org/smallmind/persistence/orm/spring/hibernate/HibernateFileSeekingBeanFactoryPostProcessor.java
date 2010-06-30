package org.smallmind.persistence.orm.spring.hibernate;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.HashMap;
import org.smallmind.persistence.orm.DataSource;
import org.smallmind.persistence.orm.hibernate.HibernateDao;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

public class HibernateFileSeekingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

   private static final HashMap<String, HashMap<Class, UrlResource>> HBM_DATA_SOURCE_MAP = new HashMap<String, HashMap<Class, UrlResource>>();
   private static final UrlResource[] NO_RESOURCES = new UrlResource[0];

   public static Resource[] getHibernateResources () {

      return getHibernateResources(null);
   }

   public static Resource[] getHibernateResources (String dataSourceKey) {

      UrlResource[] hbmResources;
      HashMap<Class, UrlResource> hbmResourceMap;

      if ((hbmResourceMap = HBM_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
         return NO_RESOURCES;
      }

      hbmResources = new UrlResource[hbmResourceMap.size()];
      hbmResourceMap.values().toArray(hbmResources);

      return hbmResources;
   }

   public void postProcessBeanFactory (ConfigurableListableBeanFactory configurableListableBeanFactory)
      throws BeansException {

      Class<?> beanClass;
      Class persistentClass;
      Annotation dataSourceAnnotation;
      HashMap<Class, UrlResource> hbmResourceMap;
      URL hbmURL;
      String dataSourceKey = null;
      String packageRemnant;
      String hbmFileName;

      for (String beanName : configurableListableBeanFactory.getBeanDefinitionNames()) {
         if ((beanClass = configurableListableBeanFactory.getType(beanName)) != null) {
            if (HibernateDao.class.isAssignableFrom(beanClass)) {
               if ((dataSourceAnnotation = beanClass.getAnnotation(DataSource.class)) != null) {
                  dataSourceKey = ((DataSource)dataSourceAnnotation).value();
               }

               if ((hbmResourceMap = HBM_DATA_SOURCE_MAP.get(dataSourceKey)) == null) {
                  HBM_DATA_SOURCE_MAP.put(dataSourceKey, hbmResourceMap = new HashMap<Class, UrlResource>());
               }

               int lastSlashIndex;

               try {
                  persistentClass = (Class)((ParameterizedType)beanClass.getMethod("getManagedClass").getGenericReturnType()).getActualTypeArguments()[0];

                  while ((persistentClass != null) && (!hbmResourceMap.containsKey(persistentClass))) {
                     packageRemnant = persistentClass.getPackage().getName().replace('.', '/');
                     hbmFileName = persistentClass.getSimpleName() + ".hbm.xml";
                     do {
                        if ((hbmURL = configurableListableBeanFactory.getBeanClassLoader().getResource((packageRemnant.length() > 0) ? packageRemnant + '/' + hbmFileName : hbmFileName)) != null) {
                           hbmResourceMap.put(persistentClass, new UrlResource(hbmURL));
                           break;
                        }

                        packageRemnant = packageRemnant.length() > 0 ? packageRemnant.substring(0, (lastSlashIndex = packageRemnant.lastIndexOf('/')) >= 0 ? lastSlashIndex : 0) : null;
                     } while (packageRemnant != null);

                     persistentClass = persistentClass.getSuperclass();
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