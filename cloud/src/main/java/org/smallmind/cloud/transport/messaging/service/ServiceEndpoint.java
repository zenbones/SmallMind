package org.smallmind.cloud.transport.messaging.service;

public class ServiceEndpoint {

   private Object service;
   private Class serviceInterface;
   private String serviceSelector;

   public ServiceEndpoint (Object service, Class serviceInterface, String serviceSelector) {

      this.service = service;
      this.serviceInterface = serviceInterface;
      this.serviceSelector = serviceSelector;
   }

   public Object getService () {

      return service;
   }

   public Class getServiceInterface () {

      return serviceInterface;
   }

   public String getServiceSelector () {

      return serviceSelector;
   }
}
