package org.smallmind.cloud.transport.messaging.service.spring;

import org.smallmind.cloud.transport.messaging.MessagingTransmitter;
import org.smallmind.cloud.transport.messaging.service.ServiceException;
import org.springframework.beans.factory.FactoryBean;

public class ServiceDispatcherFactoryBean implements FactoryBean {

   private String serviceSelector;

   public void setServiceSelector (String serviceSelector) {

      this.serviceSelector = serviceSelector;
   }

   public Object getObject ()
      throws ServiceException {

      MessagingTransmitter messagingTransmitter;

      if ((messagingTransmitter = ServiceDispatcherInitializingBean.getMessagingTransmitter(serviceSelector)) == null) {
         throw new ServiceException("No %s configured for selector(%s)", MessagingTransmitter.class.getSimpleName(), serviceSelector);
      }

      return messagingTransmitter;
   }

   public Class getObjectType () {

      return MessagingTransmitter.class;
   }

   public boolean isSingleton () {

      return true;
   }
}
