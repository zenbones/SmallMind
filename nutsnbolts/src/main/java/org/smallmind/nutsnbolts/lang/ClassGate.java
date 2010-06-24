package org.smallmind.nutsnbolts.lang;

import java.io.InputStream;

public interface ClassGate {

   public static final long STATIC_CLASS = 0;

   public abstract ClassStreamTicket getClassAsTicket (String name)
      throws Exception;

   public abstract InputStream getResourceAsStream (String path)
      throws Exception;

   public abstract long getLastModDate (String path);
}
