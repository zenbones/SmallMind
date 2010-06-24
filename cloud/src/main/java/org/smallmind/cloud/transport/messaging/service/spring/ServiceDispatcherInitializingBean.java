package org.smallmind.cloud.transport.messaging.service.spring;

import java.util.HashMap;
import java.util.List;
import javax.jms.JMSException;
import javax.naming.NamingException;
import org.smallmind.cloud.transport.messaging.MessagingConnectionDetails;
import org.smallmind.cloud.transport.messaging.MessagingTransmitter;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceDispatcherInitializingBean implements InitializingBean, DisposableBean {

   private static final HashMap<String, MessagingTransmitter> MESSAGING_TRANSMITTER_MAP = new HashMap<String, MessagingTransmitter>();

   private ConnectionPool javaEnvironmentPool;
   private List<String> serviceSelectorList;
   private String jmsUser;
   private String jmsCredentials;
   private String destinationEnvPath;
   private String factoryEnvPath;
   private boolean closed = false;
   private int transmissionPoolSize;

   public static MessagingTransmitter getMessagingTransmitter (String serviceSelector) {

      return MESSAGING_TRANSMITTER_MAP.get(serviceSelector);
   }

   public void setJmsUser (String jmsUser) {

      this.jmsUser = jmsUser;
   }

   public void setJmsCredentials (String jmsCredentials) {

      this.jmsCredentials = jmsCredentials;
   }

   public void setJavaEnvironmentPool (ConnectionPool javaEnvironmentPool) {

      this.javaEnvironmentPool = javaEnvironmentPool;
   }

   public void setDestinationEnvPath (String destinationEnvPath) {

      this.destinationEnvPath = destinationEnvPath;
   }

   public void setFactoryEnvPath (String factoryEnvPath) {

      this.factoryEnvPath = factoryEnvPath;
   }

   public void setTransmissionPoolSize (int transmissionPoolSize) {

      this.transmissionPoolSize = transmissionPoolSize;
   }

   public void setServiceSelectorList (List<String> serviceSelectorList) {

      this.serviceSelectorList = serviceSelectorList;
   }

   public void afterPropertiesSet ()
      throws NoSuchMethodException, NamingException, JMSException, ConnectionPoolException {

      MessagingConnectionDetails connectionDetails;

      for (String serviceSelector : serviceSelectorList) {
         connectionDetails = new MessagingConnectionDetails(javaEnvironmentPool, destinationEnvPath, factoryEnvPath, jmsUser, jmsCredentials, transmissionPoolSize, serviceSelector);
         MESSAGING_TRANSMITTER_MAP.put(serviceSelector, new MessagingTransmitter(connectionDetails));
      }
   }

   public synchronized void close () {

      if (!closed) {
         closed = true;

         for (MessagingTransmitter messagingTransmitter : MESSAGING_TRANSMITTER_MAP.values()) {
            try {
               messagingTransmitter.close();
            }
            catch (JMSException jmsException) {
               LoggerManager.getLogger(ServiceDispatcherInitializingBean.class).error(jmsException);
            }
         }
      }
   }

   public void destroy () {

      close();
   }

   public void finalize () {

      close();
   }
}
