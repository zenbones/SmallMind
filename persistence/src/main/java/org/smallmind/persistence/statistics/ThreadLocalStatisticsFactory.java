/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.persistence.statistics;

public class ThreadLocalStatisticsFactory implements StatisticsFactory {

   private static final InheritableThreadLocal<Statistics> STATISTICS_THREAD_LOCAL = new InheritableThreadLocal<Statistics>() {

      protected Statistics initialValue () {

         return new Statistics();
      }
   };

   private final InheritableThreadLocal<Boolean> ENABLED_THREAD_LOCAL = new InheritableThreadLocal<Boolean>() {

      protected Boolean initialValue () {

         return enabled;
      }
   };

   private boolean enabled = false;

   public ThreadLocalStatisticsFactory (boolean enabled) {

      this.enabled = enabled;
   }

   public boolean isEnabled () {

      return ENABLED_THREAD_LOCAL.get();
   }

   public void setEnabled (boolean enabled) {

      ENABLED_THREAD_LOCAL.set(enabled);
   }

   public Statistics getStatistics () {

      if (!isEnabled()) {

         return null;
      }

      return STATISTICS_THREAD_LOCAL.get();
   }

   public Statistics removeStatistics () {

      try {
         if (!isEnabled()) {

            return null;
         }

         return STATISTICS_THREAD_LOCAL.get();
      }
      finally {
         STATISTICS_THREAD_LOCAL.set(new Statistics());
      }
   }
}