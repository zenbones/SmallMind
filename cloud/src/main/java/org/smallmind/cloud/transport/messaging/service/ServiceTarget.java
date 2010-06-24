package org.smallmind.cloud.transport.messaging.service;

import org.smallmind.cloud.transport.messaging.InvocationMessageTarget;
import org.smallmind.scribe.pen.LoggerManager;

public class ServiceTarget extends InvocationMessageTarget {

   Class serviceInterface;

   public ServiceTarget (ServiceEndpoint serviceEndpoint)
      throws NoSuchMethodException {

      super(serviceEndpoint.getService(), serviceEndpoint.getServiceInterface());

      serviceInterface = serviceEndpoint.getServiceInterface();
   }

   public void logError (Throwable throwable) {

      LoggerManager.getLogger(serviceInterface).error(throwable);
   }
}