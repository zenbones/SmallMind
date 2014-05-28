/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen.probe;

import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class ProbeFactory {

  private static final ProbeStackThreadLocal PROBE_STACK_LOCAL = new ProbeStackThreadLocal();
  private static final ThreadLocal<Throwable> THROWABLE_LOCAL = new ThreadLocal<Throwable>();
  private static final ThreadLocal<byte[]> IDENTIFIER_LOCAL = new ThreadLocal<byte[]>();

  public static void setIdentifier (byte[] identifier) {

    IDENTIFIER_LOCAL.set(identifier);
  }

  public static byte[] getIdentifier () {

    return IDENTIFIER_LOCAL.get();
  }

  public static void setThrowable (Throwable throwable) {

    THROWABLE_LOCAL.set(throwable);
  }

  public static Throwable getThrowable () {

    return THROWABLE_LOCAL.get();
  }

  public static Probe getProbe () {

    return PROBE_STACK_LOCAL.get().peek();
  }

  public static Probe createProbe (Logger logger, Discriminator discriminator, Level level, String title) {

    return PROBE_STACK_LOCAL.get().push(logger, discriminator, level, title);
  }

  protected static void closeProbe (Probe probe)
    throws ProbeException {

    PROBE_STACK_LOCAL.get().pop(probe);
  }

  public static void executeInstrumentation (Logger logger, Discriminator discriminator, Level level, String title, Instrument instrument)
    throws ProbeException {

    Probe probe = createProbe(logger, discriminator, level, title);

    probe.start();
    try {
      instrument.withProbe(probe);
    }
    catch (ProbeException probeException) {
      probe.abort();

      throw probeException;
    }
    catch (Exception exception) {

      probe.abort(exception);

      throw new ProbeException(exception);
    }
    finally {
      if (!probe.isAborted()) {
        probe.stop();
      }
    }
  }

  public static <T> T executeInstrumentationAndReturn (Logger logger, Discriminator discriminator, Level level, String title, InstrumentAndReturn<T> instrumentAndReturn)
    throws ProbeException {

    Probe probe = createProbe(logger, discriminator, level, title);

    probe.start();
    try {
      return instrumentAndReturn.withProbe(probe);
    }
    catch (ProbeException probeException) {
      probe.abort();

      throw probeException;
    }
    catch (Exception exception) {

      probe.abort(exception);

      throw new ProbeException(exception);
    }
    finally {
      if (!probe.isAborted()) {
        probe.stop();
      }
    }
  }

  private static class ProbeStackThreadLocal extends InheritableThreadLocal<ProbeStack> {

    protected ProbeStack initialValue () {

      return new ProbeStack(getIdentifier());
    }

    protected ProbeStack childValue (ProbeStack parentValue) {

      return new ProbeStack(parentValue.getCurrentIdentifier());
    }
  }
}