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
package org.smallmind.nutsnbolts.spring;

import java.util.LinkedList;
import org.smallmind.nutsnbolts.util.DotNotation;
import org.smallmind.nutsnbolts.util.DotNotationException;

public class KeyDebugger {

  private LinkedList<DebugMatcher> matcherList;
  boolean debug = false;

  public KeyDebugger (String[] patterns)
    throws DotNotationException {

    DebugMatcher debugMatcher;

    matcherList = new LinkedList<DebugMatcher>();
    for (String pattern : patterns) {
      matcherList.add(debugMatcher = new DebugMatcher(pattern));
      if (!debugMatcher.isExclusion()) {
        debug = true;
      }
    }
  }

  public boolean willDebug () {

    return debug;
  }

  public boolean matches (String key) {

    boolean match = false;

    for (DebugMatcher debugMatcher : matcherList) {
      if (debugMatcher.matches(key)) {
        if (debugMatcher.isExclusion()) {

          return false;
        }
        else {
          match = true;
        }
      }
    }

    return match;
  }

  private class DebugMatcher {

    private DotNotation dotNotation;
    private boolean exclusion = false;

    public DebugMatcher (String pattern)
      throws DotNotationException {

      if (pattern.charAt(0) == '-') {
        exclusion = true;
        dotNotation = new DotNotation(pattern.substring(1));
      }
      else {
        dotNotation = new DotNotation(pattern);
      }
    }

    public boolean isExclusion () {

      return exclusion;
    }

    public boolean matches (String key) {

      return dotNotation.getPattern().matcher(key).matches();
    }
  }
}
