package org.smallmind.scribe.pen.probe;

import org.smallmind.scribe.pen.Discriminator;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.Logger;

public class ProbeFactory {

   private static final ThreadLocal<Throwable> THROWABLE_LOCAL = new ThreadLocal<Throwable>();
   private static final ProbeStackThreadLocal PROBE_STACK_LOCAL = new ProbeStackThreadLocal();

   public static void storeThrowable (Throwable throwable) {

      THROWABLE_LOCAL.set(throwable);
   }

   public static Throwable retrieveThrowable () {

      return THROWABLE_LOCAL.get();
   }

   public static Probe retrieveProbe () {

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

         return new ProbeStack();
      }

      protected ProbeStack childValue (ProbeStack parentValue) {

         return new ProbeStack(parentValue.getCurrentIdentifier());
      }
   }
}