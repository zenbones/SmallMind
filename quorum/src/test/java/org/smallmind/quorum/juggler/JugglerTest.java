/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.quorum.juggler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JugglerTest {

  // recoveryCheckSeconds is held at zero throughout so the background recovery executor is never
  // scheduled; every test stays single-threaded and deterministic.
  private static final int NO_RECOVERY = 0;

  private static Juggler<String, String> jugglerOf (List<FakePin> pins) {

    return new Juggler<>(String.class, String.class, NO_RECOVERY, new ListPinFactory(pins), new String[pins.size()]);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testPickResourceBeforeInitializeIsRejected ()
    throws NoAvailableJugglerResourceException {

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("only"));

    jugglerOf(pins).pickResource();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testPickResourceAfterShutdownIsRejected ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("only"));

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();
    juggler.shutdown();

    juggler.pickResource();
  }

  public void testRotationServesEveryPinExactlyOncePerCycle ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("red"));
    pins.add(new FakePin("green"));
    pins.add(new FakePin("blue"));

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    Set<String> servedInOneCycle = new HashSet<>();

    for (int index = 0; index < 3; index++) {
      servedInOneCycle.add(juggler.pickResource());
    }

    Assert.assertEquals(servedInOneCycle.size(), 3, "every pin should be drawn once before any repeats");
    Assert.assertTrue(servedInOneCycle.contains("red"));
    Assert.assertTrue(servedInOneCycle.contains("green"));
    Assert.assertTrue(servedInOneCycle.contains("blue"));
  }

  public void testFailedObtainBlacklistsPinAndRetriesHealthyOnes ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    FakePin failing = new FakePin("broken");
    failing.setFailOnObtain(true);

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("healthy"));
    pins.add(failing);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    // Regardless of which pin the random draw selects first, the failing pin is quarantined on the spot
    // and the call retries until the healthy resource is returned.
    for (int index = 0; index < 5; index++) {
      Assert.assertEquals(juggler.pickResource(), "healthy");
    }
  }

  public void testPinThatFailsToStartIsBlacklistedDuringStartup ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    FakePin failing = new FakePin("broken");
    failing.setFailOnStart(true);

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("healthy"));
    pins.add(failing);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    for (int index = 0; index < 5; index++) {
      Assert.assertEquals(juggler.pickResource(), "healthy", "a pin that failed to start should never be served");
    }
  }

  public void testExhaustionThrowsNoAvailableResourceCarryingACause ()
    throws JugglerResourceCreationException {

    FakePin firstFailure = new FakePin("first");
    FakePin secondFailure = new FakePin("second");

    firstFailure.setFailOnObtain(true);
    secondFailure.setFailOnObtain(true);

    List<FakePin> pins = new ArrayList<>();
    pins.add(firstFailure);
    pins.add(secondFailure);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    NoAvailableJugglerResourceException thrown = Assert.expectThrows(NoAvailableJugglerResourceException.class, juggler::pickResource);

    Assert.assertNotNull(thrown.getCause(), "the terminating exception should carry the most recent blacklist cause");
    Assert.assertTrue(thrown.getMessage().contains("black listed"), "the message should describe why no resource is available");
    // Both pins fail within the same loop (and almost certainly the same millisecond); the blacklist
    // map probes forward on key collision, so the second cause survives as a suppressed exception
    // rather than overwriting the first.
    Assert.assertEquals(thrown.getSuppressed().length, 1, "every distinct blacklist cause should be retained");
  }

  public void testAddToBlackListRemovesPinFromCirculation ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    FakePin quarantined = new FakePin("quarantined");

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("survivor"));
    pins.add(quarantined);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    juggler.addToBlackList(new BlacklistEntry<>(quarantined, new JugglerResourceException("pulled by its own code")));

    for (int index = 0; index < 4; index++) {
      Assert.assertEquals(juggler.pickResource(), "survivor", "a blacklisted pin must not return to service without recovery");
    }
  }

  public void testSingleProviderConstructorReplicatesTheProviderAcrossPins ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    // The convenience constructor fills a typed array of the requested size with one provider; the
    // factory then yields a distinct, uniquely named pin per slot.
    Juggler<String, String> juggler = new Juggler<>(String.class, String.class, NO_RECOVERY, new CountingPinFactory("node"), "ignored-provider", 3);

    juggler.initialize();
    juggler.startup();

    Set<String> served = new HashSet<>();

    for (int index = 0; index < 3; index++) {
      served.add(juggler.pickResource());
    }

    Assert.assertEquals(served.size(), 3, "the array should have been filled with three independently managed pins");
    for (String resource : served) {
      Assert.assertTrue(resource.startsWith("node-"), "every pin should have been built from the replicated provider");
    }
  }

  @Test(groups = "unit")
  public void testBlacklistedPinIsRecoveredByTheBackgroundScan ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException, InterruptedException {

    FakePin flaky = new FakePin("flaky");

    flaky.setFailOnObtain(true);
    flaky.setRecoverable(true);

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("healthy"));
    pins.add(flaky);

    // A one-second recovery age with the fixed three-second scan cadence means a five-second wait
    // comfortably brackets the single scan that promotes the recovered pin.
    Juggler<String, String> juggler = new Juggler<>(String.class, String.class, 1, new ListPinFactory(pins), new String[pins.size()]);

    juggler.initialize();
    juggler.startup();
    try {
      // The flaky pin fails on obtain and is quarantined; only the healthy pin is served meanwhile.
      for (int index = 0; index < 4; index++) {
        Assert.assertEquals(juggler.pickResource(), "healthy");
      }

      Thread.sleep(5000L);

      Set<String> servedAfterRecovery = new HashSet<>();

      for (int index = 0; index < 20; index++) {
        servedAfterRecovery.add(juggler.pickResource());
      }

      Assert.assertTrue(servedAfterRecovery.contains("flaky"), "a recovered pin should be returned to rotation by the background scan");
    } finally {
      juggler.shutdown();
    }
  }

  public void testCreationExceptionWrapsItsCause () {

    Throwable cause = new IllegalStateException("provider unavailable");
    JugglerResourceCreationException exception = new JugglerResourceCreationException(cause);

    Assert.assertSame(exception.getCause(), cause);
    Assert.assertTrue(exception instanceof JugglerResourceException, "a creation failure should remain a JugglerResourceException");
  }

  public void testPickResourceIsAllowedInTheInitializedStateBeforeStartup ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    // pickResource permits both INITIALIZED and STARTED; an initialized-but-not-started juggler still
    // serves, exercising the second arm of the state guard.
    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("solo"));

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();

    Assert.assertEquals(juggler.pickResource(), "solo", "an initialized juggler should serve before startup");
  }

  public void testBlacklistingAPinThatHasRotatedIntoTheTargetListRemovesIt ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("one"));
    pins.add(new FakePin("two"));

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    // A successful pick moves the chosen pin from the source list into the target list; blacklisting it
    // now must traverse the target-list arm of addToBlackList rather than the source-list arm.
    String firstServed = juggler.pickResource();
    FakePin rotatedPin = pins.get(0).resource.equals(firstServed) ? pins.get(0) : pins.get(1);

    juggler.addToBlackList(new BlacklistEntry<>(rotatedPin, new JugglerResourceException("pulled from the target list")));

    for (int index = 0; index < 4; index++) {
      Assert.assertNotEquals(juggler.pickResource(), firstServed, "a pin blacklisted out of the target list must not return to service");
    }
  }

  public void testBlacklistingAnUnknownPinIsASilentNoOp ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    List<FakePin> pins = new ArrayList<>();
    pins.add(new FakePin("kept"));

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    // A pin the juggler never knew about is in neither list, so both removal arms fall through and the
    // call leaves circulation untouched.
    juggler.addToBlackList(new BlacklistEntry<>(new FakePin("stranger"), new JugglerResourceException("never managed")));

    Assert.assertEquals(juggler.pickResource(), "kept", "blacklisting an unmanaged pin should not disturb the pool");
  }

  public void testShutdownStopsAndDeconstructClosesEveryPinExactlyOnce ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    FakePin first = new FakePin("first");
    FakePin second = new FakePin("second");

    List<FakePin> pins = new ArrayList<>();
    pins.add(first);
    pins.add(second);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();
    juggler.pickResource();
    juggler.pickResource();

    juggler.shutdown();
    Assert.assertEquals(first.getStopCount(), 1, "shutdown should stop every pin exactly once across both lists");
    Assert.assertEquals(second.getStopCount(), 1);

    juggler.deconstruct();
    Assert.assertEquals(first.getCloseCount(), 1, "deconstruct should close every pin exactly once");
    Assert.assertEquals(second.getCloseCount(), 1);
  }

  public void testDeconstructedJugglerCanBeReinitializedAndReused ()
    throws NoAvailableJugglerResourceException, JugglerResourceCreationException {

    // CountingPinFactory mints a fresh pin on every call, so the second initialize() — which builds a
    // new pin per provider — is satisfied without exhausting a fixed list.
    Juggler<String, String> juggler = new Juggler<>(String.class, String.class, NO_RECOVERY, new CountingPinFactory("node"), "ignored-provider", 2);

    juggler.initialize();
    juggler.startup();
    juggler.shutdown();
    juggler.deconstruct();

    // deconstruct returns the juggler to UNINITIALIZED, so it can be brought back up and used again.
    juggler.initialize();
    juggler.startup();
    Assert.assertNotNull(juggler.pickResource(), "a deconstructed juggler should be reusable after re-initialization");
    juggler.shutdown();
  }

  public void testDeconstructIsANoOpWhenNotStopped ()
    throws JugglerResourceCreationException {

    FakePin pin = new FakePin("live");

    List<FakePin> pins = new ArrayList<>();
    pins.add(pin);

    Juggler<String, String> juggler = jugglerOf(pins);

    juggler.initialize();
    juggler.startup();

    // deconstruct only acts in the STOPPED state; on a STARTED juggler it must do nothing.
    juggler.deconstruct();

    Assert.assertEquals(pin.getCloseCount(), 0, "deconstruct must not close pins while the juggler is still started");
  }

  // FakePin implements JugglingPin directly rather than extending AbstractJugglingPin, because the
  // abstract base narrows start/stop/close to not throw, and a subclass override cannot widen the
  // declaration back to the JugglerResourceException that Juggler.startup catches.
  private static class FakePin implements JugglingPin<String> {

    private final String resource;
    private boolean failOnStart = false;
    private boolean failOnObtain = false;
    private boolean recoverable = false;
    private int stopCount = 0;
    private int closeCount = 0;

    private FakePin (String resource) {

      this.resource = resource;
    }

    private int getStopCount () {

      return stopCount;
    }

    private int getCloseCount () {

      return closeCount;
    }

    private void setFailOnStart (boolean failOnStart) {

      this.failOnStart = failOnStart;
    }

    private void setFailOnObtain (boolean failOnObtain) {

      this.failOnObtain = failOnObtain;
    }

    // A recoverable pin clears its obtain failure the first time the background scan offers it
    // recovery, so that once promoted back into rotation it serves its resource normally.
    private void setRecoverable (boolean recoverable) {

      this.recoverable = recoverable;
    }

    @Override
    public void start (Method method, Object... args)
      throws JugglerResourceException {

      if (failOnStart) {
        throw new JugglerResourceException("forced start failure for (%s)", resource);
      }
    }

    @Override
    public void stop (Method method, Object... args) {

      stopCount++;
    }

    @Override
    public void close (Method method, Object... args) {

      closeCount++;
    }

    @Override
    public String obtain ()
      throws JugglerResourceException {

      if (failOnObtain) {
        throw new JugglerResourceException("forced obtain failure for (%s)", resource);
      }

      return resource;
    }

    @Override
    public boolean recover () {

      if (recoverable) {
        failOnObtain = false;

        return true;
      }

      return false;
    }

    @Override
    public String describe () {

      return "fake-pin(" + resource + ")";
    }
  }

  private static class ListPinFactory implements JugglingPinFactory<String, String> {

    private final List<FakePin> pins;
    private int index = 0;

    private ListPinFactory (List<FakePin> pins) {

      this.pins = pins;
    }

    @Override
    public JugglingPin<String> createJugglingPin (String provider, Class<String> resourceClass)
      throws JugglerResourceCreationException {

      return pins.get(index++);
    }
  }

  // Builds a fresh, uniquely named pin per call so the single-provider constructor can be shown to
  // produce one independent pin per array slot.
  private static class CountingPinFactory implements JugglingPinFactory<String, String> {

    private final String prefix;
    private int count = 0;

    private CountingPinFactory (String prefix) {

      this.prefix = prefix;
    }

    @Override
    public JugglingPin<String> createJugglingPin (String provider, Class<String> resourceClass) {

      return new FakePin(prefix + "-" + (count++));
    }
  }
}
