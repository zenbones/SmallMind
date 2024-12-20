/*
 * Copyright (c) 2007 through 2024 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.lang;

import java.util.concurrent.ConcurrentHashMap;

public class PerApplicationContext {

  private static final InheritableThreadLocal<ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object>> PER_APPLICATION_MAP_LOCAL = new InheritableThreadLocal<>();

  private ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

  public PerApplicationContext () {

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      PER_APPLICATION_MAP_LOCAL.set(perApplicationMap = new ConcurrentHashMap<>());
    }
  }

  public static void setPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Object data) {

    ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      throw new MissingPerApplicationContextException("No per-application context has been set in this thread environment");
    } else {

      perApplicationMap.put(clazz, data);
    }
  }

  public static <K> K getPerApplicationData (Class<? extends PerApplicationDataManager> clazz, Class<K> type) {

    ConcurrentHashMap<Class<? extends PerApplicationDataManager>, Object> perApplicationMap;

    if ((perApplicationMap = PER_APPLICATION_MAP_LOCAL.get()) == null) {
      throw new MissingPerApplicationContextException("No per-application context has been set in this thread environment");
    } else {

      return type.cast(perApplicationMap.get(clazz));
    }
  }

  public void prepareThread () {

    PER_APPLICATION_MAP_LOCAL.set(perApplicationMap);
  }
}
