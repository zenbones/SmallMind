/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExceptionSuppressingLogFilter implements Filter {

  private static final HashSet<Class<? extends Throwable>> SUPPRESSED_THROWABLE_SET = new HashSet<>();
  private static final ReentrantReadWriteLock UPDATE_LOCK = new ReentrantReadWriteLock();

  public static void addSuppressedThrowableClasses (List<Class<? extends Throwable>> suppressedThrowableClasses) {

    if (suppressedThrowableClasses != null) {
      UPDATE_LOCK.writeLock().lock();
      try {
        SUPPRESSED_THROWABLE_SET.addAll(suppressedThrowableClasses);
      } finally {
        UPDATE_LOCK.writeLock().unlock();
      }
    }
  }

  @Override
  public boolean willLog (Record record) {

    Throwable loggedThrowable;

    if ((loggedThrowable = record.getThrown()) != null) {
      UPDATE_LOCK.readLock().lock();
      try {

        return !SUPPRESSED_THROWABLE_SET.contains(loggedThrowable.getClass());
      } finally {
        UPDATE_LOCK.readLock().unlock();
      }
    }

    return true;
  }
}