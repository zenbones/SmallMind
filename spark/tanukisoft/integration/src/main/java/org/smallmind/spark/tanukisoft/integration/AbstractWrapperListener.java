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
package org.smallmind.spark.tanukisoft.integration;

import org.smallmind.nutsnbolts.util.PropertyExpander;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.tanukisoftware.wrapper.WrapperListener;
import org.tanukisoftware.wrapper.WrapperManager;

public abstract class AbstractWrapperListener implements WrapperListener {

  private static final int NO_ERROR_CODE = 0;
  private static final int STACK_TRACE_ERROR_CODE = 2;

  public abstract void startup (String[] args)
    throws Exception;

  public abstract void shutdown ()
    throws Exception;

  public void controlEvent (int event) {

    if (!WrapperManager.isControlledByNativeWrapper()) {
      if ((event == WrapperManager.WRAPPER_CTRL_C_EVENT) || (event == WrapperManager.WRAPPER_CTRL_CLOSE_EVENT) || (event == WrapperManager.WRAPPER_CTRL_SHUTDOWN_EVENT)) {
        WrapperManager.stop(0);
      }
    }
  }

  public Integer start (String[] args) {

    try {
      startup(args);
    }
    catch (Exception exception) {
      exception.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return null;
  }

  public int stop (int event) {

    try {
      shutdown();
    }
    catch (Exception exception) {
      exception.printStackTrace();
      return STACK_TRACE_ERROR_CODE;
    }

    return NO_ERROR_CODE;
  }

  public static void main (String... args)
    throws Exception {

    if (args.length < 1) {
      throw new IllegalArgumentException(String.format("First application parameter must be the class of the %s in use", WrapperListener.class.getSimpleName()));
    }
    else {

      String[] trimmedArgs;
      PropertyExpander propertyExpander;

      trimmedArgs = new String[args.length - 1];
      System.arraycopy(args, 1, trimmedArgs, 0, args.length - 1);

      propertyExpander = new PropertyExpander(false, SystemPropertyMode.FALLBACK, true);
      for (int count = 0; count < trimmedArgs.length; count++) {
        trimmedArgs[count] = propertyExpander.expand(trimmedArgs[count]);
      }

      WrapperManager.start((WrapperListener)Class.forName(args[0]).newInstance(), trimmedArgs);
    }
  }
}