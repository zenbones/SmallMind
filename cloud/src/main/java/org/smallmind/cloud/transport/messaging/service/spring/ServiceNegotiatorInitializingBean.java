package org.smallmind.cloud.transport.messaging.service.spring;

import java.util.LinkedList;
import java.util.List;
import javax.jms.JMSException;
import javax.naming.NamingException;
import org.smallmind.cloud.transport.messaging.MessagingConnectionDetails;
import org.smallmind.cloud.transport.messaging.MessagingReceiver;
import org.smallmind.cloud.transport.messaging.service.ServiceEndpoint;
import org.smallmind.cloud.transport.messaging.service.ServiceTarget;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ServiceNegotiatorInitializingBean implements InitializingBean, DisposableBean {

   private final LinkedList<MessagingReceiver> messagingReceiverList = new LinkedList<MessagingReceiver>();

   private ConnectionPool javaEnvironmentPool;
   private List<ServiceEndpoint> serviceEndpointList;
   private String jmsUser;
   private String jmsCredentials;
   private String destinationEnvPath;
   private String factoryEnvPath;
   private boolean closed = false;

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

   public void setServiceEndpointList (List<ServiceEndpoint> serviceEndpointList) {

      this.serviceEndpointList = serviceEndpointList;
   }

   public void afterPropertiesSet ()
      throws NoSuchMethodException, NamingException, JMSException, ConnectionPoolException {

      MessagingConnectionDetails connectionDetails;

      for (ServiceEndpoint serviceEndpoint : serviceEndpointList) {
         connectionDetails = new MessagingConnectionDetails(javaEnvironmentPool, destinationEnvPath, factoryEnvPath, jmsUser, jmsCredentials, 0, serviceEndpoint.getServiceSelector());
         messagingReceiverList.add(new MessagingReceiver(new ServiceTarget(serviceEndpoint), connectionDetails));
      }
   }

   public synchronized void close () {

      if (!closed) {
         closed = true;

         for (MessagingReceiver messagingReceiver : messagingReceiverList) {
            messagingReceiver.close();
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