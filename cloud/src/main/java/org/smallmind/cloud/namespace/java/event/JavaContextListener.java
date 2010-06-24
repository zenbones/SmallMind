package org.smallmind.cloud.namespace.java.event;

import java.util.EventListener;

public interface JavaContextListener extends EventListener {

   public abstract void contextClosed (JavaContextEvent javaContextEvent);

   public abstract void contextAborted (JavaContextEvent javaContextEvent);

}
