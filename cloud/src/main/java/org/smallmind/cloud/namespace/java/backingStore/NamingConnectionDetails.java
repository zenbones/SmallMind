package org.smallmind.cloud.namespace.java.backingStore;

public class NamingConnectionDetails {

   private String host;
   private String rootNamespace;
   private String userName;
   private String password;
   private int port;

   public NamingConnectionDetails (String host, int port, String rootNamespace, String userName, String password) {

      this.host = host;
      this.port = port;
      this.rootNamespace = rootNamespace;
      this.userName = userName;
      this.password = password;
   }

   public String getHost () {

      return host;
   }

   public int getPort () {

      return port;
   }

   public String getRootNamespace () {

      return rootNamespace;
   }

   public String getUserName () {

      return userName;
   }

   public String getPassword () {

      return password;
   }
}
