/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.constellation.ephemeral;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Map;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import org.smallmind.constellation.component.SmallMindComponent;
import org.smallmind.scribe.pen.Logger;

public class EphemeralHandle {

   private static final int ALL_ATTRIBUTES = 0;
   private static final int CLASS_NAME = 2;
   private static final int CREATION_TIME = 4;
   private static final int LAST_ACCESS_TIME = 8;
   private static final int TIMEOUT = 16;
   private static final int VALIDITY = 32;

   private SmallMindComponent smallMindComponent;
   private Logger logger;
   private EphemeralKey ephemeralKey;
   private WeakReference<Ephemeral> weakReference;
   private Class ephemeralClass;
   private String ephemeralClassName;
   private boolean validity;
   private boolean isNew;
   private long creationTime;
   private long lastAccessTime;
   private int timeout;

   public EphemeralHandle (SmallMindComponent smallMindComponent, Logger logger, EphemeralKey ephemeralKey, String ephemeralClassName, int creationTimeout)
      throws EphemeralCreationException {

      this.smallMindComponent = smallMindComponent;
      this.logger = logger;
      this.ephemeralKey = ephemeralKey;
      this.ephemeralClassName = ephemeralClassName;

      creationTime = System.currentTimeMillis();
      lastAccessTime = creationTime;
      timeout = creationTimeout;
      validity = true;
      isNew = true;

      try {
         ephemeralClass = Class.forName(ephemeralClassName);
         cache(ALL_ATTRIBUTES);
      }
      catch (Exception e) {
         throw new EphemeralCreationException(e);
      }
   }

   public EphemeralHandle (SmallMindComponent smallMindComponent, Logger logger, EphemeralKey ephemeralKey, String ephemeralClassName, Long resultCreationTime, Long resultLastAccessTime, Integer resultTimeout, Boolean resultValidity)
      throws EphemeralCreationException {

      this.smallMindComponent = smallMindComponent;
      this.logger = logger;
      this.ephemeralKey = ephemeralKey;
      this.ephemeralClassName = ephemeralClassName;

      creationTime = resultCreationTime;
      lastAccessTime = resultLastAccessTime;
      timeout = resultTimeout;
      validity = resultValidity;
      isNew = false;

      try {
         ephemeralClass = Class.forName(ephemeralClassName);
      }
      catch (Exception e) {
         throw new EphemeralCreationException(e);
      }
   }

   private Ephemeral constructEphemeral (Class ephemeralClass, Class<EphemeralHandle> parameterClass, Object parameterValue)
      throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

      Constructor constructor;
      Class[] paramClasses = new Class[1];
      Object[] paramValues = new Object[1];

      paramClasses[0] = parameterClass;
      paramValues[0] = parameterValue;
      constructor = ephemeralClass.getConstructor(paramClasses);
      return (Ephemeral)constructor.newInstance(paramValues);
   }

   public synchronized Ephemeral getEphemeral ()
      throws EphemeralRecoveryException {

      Ephemeral ephemeral;

      if ((weakReference != null) && ((ephemeral = weakReference.get()) != null)) {
         return ephemeral;
      }
      else {
         try {
            if ((ephemeral = constructEphemeral(ephemeralClass, EphemeralHandle.class, this).postRevival()) != null) {
               weakReference = new WeakReference<Ephemeral>(ephemeral);
               return ephemeral;
            }

            return null;
         }
         catch (Exception e) {
            throw new EphemeralRecoveryException(e);
         }
      }
   }

   public synchronized void clear () {

      weakReference.clear();
   }

   public synchronized void remove () {

      NamingEnumeration<NameClassPair> dataKeyEnum;
      NameClassPair nameClassPair;

      weakReference.clear();

      try {
         dataKeyEnum = smallMindComponent.getGlobalList("ephemeral/" + ephemeralKey.getEphemeralId());
         while (dataKeyEnum.hasMore()) {
            nameClassPair = dataKeyEnum.next();
            smallMindComponent.removeGlobalNamespaceContext("ephemeral/" + ephemeralKey.getEphemeralId() + "/" + nameClassPair.getName());
         }
         smallMindComponent.removeGlobalNamespaceContext("ephemeral/" + ephemeralKey.getEphemeralId());
      }
      catch (Exception e) {
         logError(e);
      }
   }

   public synchronized boolean isValid () {

      return validity;
   }

   public synchronized void invalidate (boolean cache) {

      Ephemeral ephemeral;

      if (validity) {
         try {
            if ((ephemeral = getEphemeral()) != null) {
               ephemeral.postInvalidation();
            }
         }
         catch (Exception e) {
            logError(e);
         }

         if (cache) {
            try {
               cache(VALIDITY);
            }
            catch (EphemeralPersistenceException p) {
               logError(p);
            }
         }

         validity = false;
      }
   }

   public synchronized void touch () {

      lastAccessTime = System.currentTimeMillis();

      try {
         cache(LAST_ACCESS_TIME);
      }
      catch (EphemeralPersistenceException p) {
         logError(p);
      }
   }

   public synchronized long getLastAccessTime () {

      return lastAccessTime;
   }

   public synchronized void setTimeout (int seconds) {

      timeout = seconds;

      try {
         cache(TIMEOUT);
      }
      catch (EphemeralPersistenceException p) {
         logError(p);
      }
   }

   public synchronized int getTimeout () {

      return timeout;
   }

   public synchronized boolean isNew () {

      return isNew;
   }

   public EphemeralKey getEphemeralKey () {

      return ephemeralKey;
   }

   public long getCreationTime () {

      return creationTime;
   }

   public synchronized void bind (String key, Object data) {

      try {
         smallMindComponent.setGlobalData("ephemeral/" + ephemeralKey.getEphemeralId() + "/" + key, data);
      }
      catch (Exception e) {
         logError(e);
      }
   }

   public synchronized void unbind (String key) {

      try {
         smallMindComponent.removeGlobalData("ephemeral/" + ephemeralKey.getEphemeralId() + "/" + key);
      }
      catch (Exception e) {
         logError(e);
      }
   }

   public synchronized Map<String, Object> getBindings ()
      throws EphemeralPersistenceException {

      try {
         return smallMindComponent.getGlobalDataMap("ephemeral/" + ephemeralKey.getEphemeralId());
      }
      catch (Exception e) {
         throw new EphemeralPersistenceException(e);
      }
   }

   private void cache (int filter)
      throws EphemeralPersistenceException {

      LinkedList<ModificationItem> modList;
      ModificationItem[] modItems;
      BasicAttribute objectClass;

      if (validity) {
         modList = new LinkedList<ModificationItem>();

         if (filter == ALL_ATTRIBUTES) {
            objectClass = new BasicAttribute("objectclass");
            objectClass.add("top");
            objectClass.add("extensibleObject");
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, objectClass));
         }
         if ((filter == ALL_ATTRIBUTES) || ((filter & CLASS_NAME) != 0)) {
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("classname", ephemeralClassName)));
         }
         if ((filter == ALL_ATTRIBUTES) || ((filter & CREATION_TIME) != 0)) {
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("creationtime", String.valueOf(creationTime))));
         }
         if ((filter == ALL_ATTRIBUTES) || ((filter & LAST_ACCESS_TIME) != 0)) {
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("lastaccesstime", String.valueOf(lastAccessTime))));
         }
         if ((filter == ALL_ATTRIBUTES) || ((filter & TIMEOUT) != 0)) {
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("timeout", String.valueOf(timeout))));
         }
         if ((filter == ALL_ATTRIBUTES) || ((filter & VALIDITY) != 0)) {
            modList.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("validity", String.valueOf(validity))));
         }

         modItems = new ModificationItem[modList.size()];
         modList.toArray(modItems);

         try {
            smallMindComponent.modifyGlobalAttributes("ephemeral/" + ephemeralKey.getEphemeralId(), modItems);
         }
         catch (Exception e) {
            throw new EphemeralPersistenceException(e);
         }
      }
   }

   public void logError (Exception exception) {

      logger.error(exception);
   }
}
