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
package org.smallmind.persistence.orm.database.mysql;

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.persistence.orm.database.Sequence;

public class SequenceManager {

  private static final AtomicReference<Sequence> SEQUENCE_REFERENCE = new AtomicReference<Sequence>();

  public static void register (Sequence sequence) {

    SEQUENCE_REFERENCE.set(sequence);
  }

  public static long nextLong (String name) {

    Sequence sequence;

    if ((sequence = SEQUENCE_REFERENCE.get()) == null) {
      throw new IllegalStateException("The SequenceManager has not been properly initialized");
    }

    return sequence.nextLong(name);
  }
}
