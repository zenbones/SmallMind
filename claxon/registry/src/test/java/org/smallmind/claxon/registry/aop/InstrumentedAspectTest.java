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
package org.smallmind.claxon.registry.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import org.smallmind.claxon.registry.ClaxonConfiguration;
import org.smallmind.claxon.registry.ClaxonRegistry;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.PushEmitter;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.json.TallyParser;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.time.Stint;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the full {@link Instrumented} → {@link InstrumentedAspect} → {@link Instrument}
 * → meter → emitter path. Calls a method annotated with {@code @Instrumented} and asserts
 * that the woven advice routed timing and tags through to a bound {@link PushEmitter}.
 *
 * <p>This test relies on the AspectJ compile-time weaver configured by
 * {@code dev.aspectj:aspectj-maven-plugin} in the parent build; without weaving the
 * annotation is inert and the test fails because no recording arrives.
 *
 * <p>The {@link PerApplicationContext} is reset in {@link #setUp()} so each test installs
 * its own registry through {@link ClaxonRegistry#initializeInstrumentation()} without
 * leaking state from other test classes in the same suite.
 */
@Test(groups = "unit")
public class InstrumentedAspectTest {

  private static final long AWAIT_TIMEOUT_MILLIS = 10000L;

  private PerApplicationContext.ContextCarrier priorContext;
  private ClaxonRegistry registry;
  private RecordingPushEmitter emitter;

  @BeforeMethod
  public void setUp () {

    priorContext = PerApplicationContext.generateCarrier();
    new PerApplicationContext();

    ClaxonConfiguration configuration = new ClaxonConfiguration();

    configuration.setCollectionStint(new Stint(50, TimeUnit.MILLISECONDS));
    configuration.setNamingStrategy(caller -> caller.getName());

    registry = new ClaxonRegistry(configuration);
    emitter = new RecordingPushEmitter();

    registry.bind("recorder", emitter);
    registry.initializeInstrumentation();
  }

  @AfterMethod
  public void tearDown ()
    throws InterruptedException, TimeoutException {

    try {
      registry.stop();
    } finally {
      priorContext.prepareThread();
    }
  }

  public void testAnnotatedMethodRoutesThroughAspectToEmitter ()
    throws InterruptedException {

    Subject subject = new Subject();

    Assert.assertEquals(subject.measuredCall("p1"), "ok:p1");

    Recording recording = emitter.await(Subject.class.getName(), candidate -> candidate.hasTag("service", "checkout") && candidate.hasTag("variant", "primary"), AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Aspect should produce a tagged recording for the annotated method; check that the AspectJ weaver ran during test-compile");
    Assert.assertNotNull(recording.findQuantity("count"), "Recording should carry a Tally count quantity");
  }

  public void testInactiveAnnotationProducesNoRecording ()
    throws InterruptedException {

    Subject subject = new Subject();
    int invocations = 0;

    while (invocations++ < 3) {
      Assert.assertEquals(subject.inertCall(), "inert");
    }

    Thread.sleep(200L);

    Assert.assertEquals(emitter.recordingCountFor(Subject.class.getName()), 0, "Inactive instrumentation should not register a meter for the join point");
  }

  public void testParameterTagIsExtractedFromArgument ()
    throws InterruptedException {

    Subject subject = new Subject();

    Assert.assertEquals(subject.parameterTaggedCall("admin"), "role:admin");

    Recording recording = emitter.await(Subject.class.getName(), candidate -> candidate.hasTag("role", "admin"), AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Aspect should extract parameter value as a tag");
  }

  public void testExplicitCallerIsUsedAsMeterId ()
    throws InterruptedException {

    Subject subject = new Subject();

    Assert.assertEquals(subject.callerOverriddenCall(), "caller-override");

    Recording recording = emitter.await(ExplicitCaller.class.getName(), candidate -> true, AWAIT_TIMEOUT_MILLIS);

    Assert.assertNotNull(recording, "Aspect should use the explicit caller class as the meter identity");
  }

  public static class ExplicitCaller {

  }

  public static class Subject {

    @Instrumented(
      parser = TallyParser.class,
      constants = {
        @ConstantTag(key = "service", constant = "checkout"),
        @ConstantTag(key = "variant", constant = "primary")
      },
      timeUnit = TimeUnit.MICROSECONDS
    )
    public String measuredCall (String userId) {

      return "ok:" + userId;
    }

    @Instrumented(parser = TallyParser.class, active = false)
    public String inertCall () {

      return "inert";
    }

    @Instrumented(
      parser = TallyParser.class,
      parameters = {@ParameterTag(key = "role", parameter = "role")},
      timeUnit = TimeUnit.MICROSECONDS
    )
    public String parameterTaggedCall (String role) {

      return "role:" + role;
    }

    @Instrumented(
      parser = TallyParser.class,
      caller = ExplicitCaller.class,
      timeUnit = TimeUnit.MICROSECONDS
    )
    public String callerOverriddenCall () {

      return "caller-override";
    }
  }

  private static final class RecordingPushEmitter extends PushEmitter {

    private final List<Recording> recordings = new ArrayList<>();
    private final Object monitor = new Object();

    @Override
    public void record (String meterName, Tag[] tags, Quantity[] quantities) {

      synchronized (monitor) {
        recordings.add(new Recording(meterName, tags, quantities));
        monitor.notifyAll();
      }
    }

    int recordingCountFor (String marker) {

      synchronized (monitor) {

        int count = 0;

        for (Recording recording : recordings) {
          if (marker.equals(recording.meterName())) {
            count++;
          }
        }

        return count;
      }
    }

    Recording await (String meterName, Predicate<Recording> condition, long timeoutMillis)
      throws InterruptedException {

      long deadline = System.currentTimeMillis() + timeoutMillis;
      int scanned = 0;

      synchronized (monitor) {
        while (true) {

          while (scanned < recordings.size()) {

            Recording candidate = recordings.get(scanned++);

            if (meterName.equals(candidate.meterName()) && condition.test(candidate)) {

              return candidate;
            }
          }

          long remaining = deadline - System.currentTimeMillis();

          if (remaining <= 0L) {

            return null;
          }

          monitor.wait(remaining);
        }
      }
    }
  }

  private record Recording(String meterName, Tag[] tags, Quantity[] quantities) {

    Quantity findQuantity (String name) {

      for (Quantity quantity : quantities) {
        if (name.equals(quantity.getName())) {

          return quantity;
        }
      }

      throw new AssertionError("No quantity named '" + name + "' in this recording");
    }

    boolean hasTag (String key, String value) {

      if (tags == null) {

        return false;
      }
      for (Tag tag : tags) {
        if (key.equals(tag.getKey()) && value.equals(tag.getValue())) {

          return true;
        }
      }

      return false;
    }
  }
}
