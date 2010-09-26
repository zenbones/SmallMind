/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.cache;

import java.util.concurrent.locks.ReentrantLock;

public class StripeLockFactory {

   public static ReentrantLock[] createStripeLockArray (int concurrencyLevel) {

      ReentrantLock[] stripeLocks;

      if ((concurrencyLevel <= 0) || (concurrencyLevel % 2 != 0)) {
         throw new CacheException("Concurrency level(%d) must be > 0 and an even power of 2", concurrencyLevel);
      }

      stripeLocks = new ReentrantLock[concurrencyLevel];

      for (int count = 0; count < stripeLocks.length; count++) {
         stripeLocks[count] = new ReentrantLock();
      }

      return stripeLocks;
   }
}
