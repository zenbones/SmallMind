package org.smallmind.cloud.transport.messaging;

import org.smallmind.quorum.pool.ConnectionPool;

public class MessagingConnectionDetails {

   public static final String SELECTION_PROPERTY = "Selection";
   public static final String EXCEPTION_PROPERTY = "Exception";

   private ConnectionPool contextPool;
   private String destinationName;
   private String connectionFactoryName;
   private String userName;
   private String password;
   private String serviceSelector;
   private int transmissionPoolSize;

   public MessagingConnectionDetails (ConnectionPool contextPool, String destinationName, String connectionFactoryName, String userName, String password, int transmissionPoolSize) {

      this(contextPool, destinationName, connectionFactoryName, userName, password, transmissionPoolSize, null);
   }

   public MessagingConnectionDetails (ConnectionPool contextPool, String destinationName, String connectionFactoryName, String userName, String password, int transmissionPoolSize, String serviceSelector) {

      this.contextPool = contextPool;
      this.destinationName = destinationName;
      this.connectionFactoryName = connectionFactoryName;
      this.userName = userName;
      this.password = password;
      this.transmissionPoolSize = transmissionPoolSize;
      this.serviceSelector = serviceSelector;
   }

   public ConnectionPool getContextPool () {

      return contextPool;
   }

   public String getDestinationName () {

      return destinationName;
   }

   public String getConnectionFactoryName () {

      return connectionFactoryName;
   }

   public String getUserName () {

      return userName;
   }

   public String getPassword () {

      return password;
   }

   public String getServiceSelector () {

      return serviceSelector;
   }

   public int getTransmissionPoolSize () {

      return transmissionPoolSize;
   }
}
