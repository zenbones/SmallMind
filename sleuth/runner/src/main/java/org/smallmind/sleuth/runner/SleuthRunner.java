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
package org.smallmind.sleuth.runner;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.sleuth.runner.annotation.AnnotationDictionary;
import org.smallmind.sleuth.runner.annotation.AnnotationProcessor;
import org.smallmind.sleuth.runner.annotation.NativeAnnotationTranslator;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.smallmind.sleuth.runner.annotation.TestNGAnnotationTranslator;
import org.smallmind.sleuth.runner.event.CancelledSleuthEvent;
import org.smallmind.sleuth.runner.event.FatalSleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEventListener;

/**
 * Top-level coordinator for Sleuth test discovery and execution.
 * <p>
 * A single {@code SleuthRunner} instance manages the full lifecycle of a test run: listener
 * registration, cancellation, and execution. On each call to {@link #execute execute(...)}, the runner
 * discovers annotated test classes using a chain of both native and TestNG translators, builds a
 * priority-ordered dependency graph of suites, and dispatches them concurrently via a
 * {@link SleuthThreadPool}. Execution blocks until all suites complete or the run is cancelled via
 * {@link #cancel()}.
 * <p>
 * This class is thread-safe with respect to listener management and cancellation.
 *
 * @see SuiteRunner
 * @see SleuthEventListener
 */
public class SleuthRunner {

  private final LinkedList<SleuthEventListener> eventListenerList = new LinkedList<>();
  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  /**
   * Registers a listener to receive all Sleuth events emitted during a run.
   * <p>
   * Listeners are notified synchronously on the thread that fires the event. Add listeners before
   * calling {@link #execute execute(...)}.
   *
   * @param listener listener to register; must not be {@code null}
   */
  public void addListener (SleuthEventListener listener) {

    eventListenerList.add(listener);
  }

  /**
   * Removes a previously registered listener.
   * <p>
   * Removing a listener that was never registered has no effect.
   *
   * @param listener listener to remove; must not be {@code null}
   */
  public void removeListener (SleuthEventListener listener) {

    eventListenerList.remove(listener);
  }

  /**
   * Dispatches an event to every registered listener in registration order.
   * <p>
   * Called internally by suite and test runners at each lifecycle transition.
   *
   * @param sleuthEvent event to broadcast; must not be {@code null}
   */
  public void fire (SleuthEvent sleuthEvent) {

    for (SleuthEventListener listener : eventListenerList) {
      listener.handle(sleuthEvent);
    }
  }

  /**
   * Signals that the current test run should stop as soon as possible.
   * <p>
   * After cancellation, {@link #isRunning()} returns {@code false} and no further suites or tests
   * are dispatched. In-flight runners complete their current method and then call {@link TestController#complete()}
   * without starting new work. A {@link org.smallmind.sleuth.runner.event.CancelledSleuthEvent} is
   * fired when the execute call detects the cancellation before the latch is awaited.
   */
  public void cancel () {

    cancelled.set(true);
  }

  /**
   * @return {@code true} while the run has not been cancelled; {@code false} once {@link #cancel()} is called
   */
  public boolean isRunning () {

    return !cancelled.get();
  }

  /**
   * Executes the supplied classes as test suites, blocking until all suites finish or the run is cancelled.
   * <p>
   * Delegates to {@link #execute(String[], int, boolean, boolean, Iterable)} after wrapping the array
   * in a list.
   *
   * @param groups        group names to include; {@code null} uses annotation defaults; empty array runs all suites
   * @param threadCount   maximum parallel suite/test threads per tier; values {@literal <=} 0 are treated as
   *                      {@link Integer#MAX_VALUE}
   * @param stopOnError   when {@code true}, unexpected errors cancel the remaining run
   * @param stopOnFailure when {@code true}, assertion failures cancel the remaining run
   * @param classes       test classes to execute; classes with no recognised annotations are skipped
   */
  public void execute (String[] groups, int threadCount, boolean stopOnError, boolean stopOnFailure, Class<?>... classes) {

    execute(groups, threadCount, stopOnError, stopOnFailure, Arrays.asList(classes));
  }

  /**
   * Executes the supplied iterable of test classes as suites, blocking until all finish or the run is cancelled.
   * <p>
   * Each class is processed by an {@link AnnotationProcessor} configured with both the native and
   * TestNG translators. Enabled classes whose groups intersect the requested set are added to a
   * {@link DependencyAnalysis}; the resulting {@link DependencyQueue} is consumed in a loop that
   * dispatches each suite to the {@link SleuthThreadPool}. The method blocks on a
   * {@link CountDownLatch} until all suite runners have decremented it.
   *
   * @param groups        group names to include; {@code null} uses annotation defaults; empty array runs all
   * @param threadCount   maximum parallel threads per tier; values {@literal <=} 0 become {@link Integer#MAX_VALUE}
   * @param stopOnError   when {@code true}, unexpected errors cancel the remaining run
   * @param stopOnFailure when {@code true}, assertion failures cancel the remaining run
   * @param classIterable classes to execute; {@code null} is treated as an empty set
   */
  public void execute (String[] groups, int threadCount, boolean stopOnError, boolean stopOnFailure, Iterable<Class<?>> classIterable) {

    if (classIterable != null) {

      long startMilliseconds = System.currentTimeMillis();

      try {

        SleuthThreadPool threadPool = new SleuthThreadPool(this, threadCount);
        AnnotationProcessor annotationProcessor = new AnnotationProcessor(new NativeAnnotationTranslator(), new TestNGAnnotationTranslator());
        DependencyAnalysis<Suite, Class<?>> suiteAnalysis = new DependencyAnalysis<>(Suite.class);
        DependencyQueue<Suite, Class<?>> suiteDependencyQueue;
        Dependency<Suite, Class<?>> suiteDependency;
        CountDownLatch suiteCompletedLatch;

        for (Class<?> clazz : classIterable) {

          AnnotationDictionary annotationDictionary;

          if ((annotationDictionary = annotationProcessor.process(clazz)) != null) {
            if (annotationDictionary.getSuite().enabled() && ((groups == null) || inGroups(annotationDictionary.getSuite().groups(), groups))) {
              suiteAnalysis.add(new Dependency<>(clazz.getName(), annotationDictionary.getSuite(), clazz, annotationDictionary.getSuite().priority(), annotationDictionary.getSuite().executeAfter(), annotationDictionary.getSuite().dependsOn(), null));
            }
          }
        }

        suiteDependencyQueue = suiteAnalysis.calculate();
        suiteCompletedLatch = new CountDownLatch(suiteDependencyQueue.size());
        while (isRunning() && ((suiteDependency = suiteDependencyQueue.poll()) != null)) {
          TestIdentifier.updateIdentifier(suiteDependency.getValue().getName(), null);

          threadPool.execute(TestTier.SUITE, new SuiteRunner(this, suiteCompletedLatch, suiteDependency, suiteDependencyQueue, annotationProcessor, threadPool, stopOnError, stopOnFailure));
        }

        if (isRunning()) {
          suiteCompletedLatch.await();
        } else {
          fire(new CancelledSleuthEvent(SleuthRunner.class.getName(), "cancelled"));
        }
      } catch (Exception exception) {
        fire(new FatalSleuthEvent(SleuthRunner.class.getName(), "execute", System.currentTimeMillis() - startMilliseconds, exception));
      }
    }
  }

  /**
   * Determines whether a suite's groups intersect the requested group filter.
   * <p>
   * Returns {@code true} when the requested set is empty (all suites run), or when at least one
   * element of the suite's own groups array matches an element of the requested set.
   * Returns {@code false} when a non-empty filter is active but the suite belongs to no groups.
   *
   * @param ours   group names declared on the suite annotation; may be {@code null} or empty
   * @param theirs requested group names from the run configuration; may be {@code null} or empty
   * @return {@code true} if the suite should be included in this run
   */
  private boolean inGroups (String[] ours, String[] theirs) {

    if ((theirs == null) || (theirs.length == 0)) {

      return true;
    } else if ((ours == null) || (ours.length == 0)) {

      return false;
    } else {
      for (String oneOfTheirs : theirs) {
        for (String oneOfOurs : ours) {
          if ((oneOfOurs != null) && oneOfOurs.equals(oneOfTheirs)) {

            return true;
          }
        }
      }
    }

    return false;
  }
}
