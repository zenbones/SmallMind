package org.smallmind.nutsnbolts.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class GatingClassLoader extends ClassLoader {

   private final HashMap<String, ClassGateTicket> ticketMap;

   private ClassGate[] classGates;
   private int reloadInterval;

   public GatingClassLoader (int reloadInterval, ClassGate... classGates) {

      this(null, reloadInterval, classGates);
   }

   public GatingClassLoader (ClassLoader parent, int reloadInterval, ClassGate... classGates) {

      super(parent);

      this.classGates = classGates;
      this.reloadInterval = reloadInterval;

      ticketMap = new HashMap<String, ClassGateTicket>();
   }

   public ClassGate[] getClassGates () {

      return classGates;
   }

   public Class findClass (String name)
      throws ClassNotFoundException {

      Class definedClass;
      ClassStreamTicket classStreamTicket;
      InputStream classInputStream;
      byte[] classData;

      for (ClassGate classGate : classGates) {
         try {
            if ((classStreamTicket = classGate.getClassAsTicket(name)) != null) {
               classInputStream = classStreamTicket.getInputStream();
               classData = getClassData(classInputStream);
               classInputStream.close();

               definedClass = defineClass(name, classData, 0, classData.length);

               if (reloadInterval >= 0) {
                  synchronized (ticketMap) {
                     ticketMap.put(name, new ClassGateTicket(classGate, classStreamTicket.getTimeStamp()));
                  }
               }

               return definedClass;
            }
         }
         catch (Exception exception) {
            throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ")", exception);
         }
      }

      throw new ClassNotFoundException(name);
   }

   private byte[] getClassData (InputStream classInputStream)
      throws IOException {

      byte[] classData;
      int dataLength;
      int totalBytesRead = 0;
      int bytesRead;

      dataLength = classInputStream.available();
      classData = new byte[dataLength];
      while (totalBytesRead < dataLength) {
         bytesRead = classInputStream.read(classData, totalBytesRead, dataLength - totalBytesRead);
         totalBytesRead += bytesRead;
      }
      return classData;
   }

   public Class loadClass (String name)
      throws ClassNotFoundException {

      Class gatedClass = null;

      if (getParent() != null) {
         gatedClass = getParent().loadClass(name);
      }

      if (gatedClass == null) {
         gatedClass = loadClass(name, false);
      }

      return gatedClass;
   }

   public synchronized Class loadClass (String name, boolean resolve)
      throws ClassNotFoundException {

      Class gatedClass;
      ClassGateTicket classGateTicket;

      if ((gatedClass = findLoadedClass(name)) != null) {
         if (reloadInterval >= 0) {
            synchronized (ticketMap) {
               classGateTicket = ticketMap.get(name);
            }

            if (classGateTicket.getTimeStamp() != ClassGate.STATIC_CLASS) {
               if (System.currentTimeMillis() >= (classGateTicket.getTimeStamp() + (reloadInterval * 1000))) {
                  if (classGateTicket.getClassGate().getLastModDate(name) > classGateTicket.getTimeStamp()) {
                     throw new StaleClassLoaderException(name);
                  }
               }
            }
         }
      }

      if (gatedClass == null) {
         try {
            gatedClass = findClass(name);
         }
         catch (ClassNotFoundException c) {
         }
      }

      if (gatedClass == null) {
         gatedClass = findSystemClass(name);
      }

      if (resolve) {
         resolveClass(gatedClass);
      }

      return gatedClass;
   }

   public InputStream getResourceAsStream (String name) {

      InputStream resourceStream;

      for (ClassGate classGate : classGates) {
         try {
            if ((resourceStream = classGate.getResourceAsStream(name)) != null) {
               return resourceStream;
            }
         }
         catch (Exception exception) {
         }
      }

      return super.getResourceAsStream(name);
   }
}
