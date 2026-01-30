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
 * Coordinates discovery and execution of Sleuth test suites and methods.
 * <p>
 * The runner manages event listeners, cancellation, and orchestrates suite/test execution across a thread pool.
 */
public class SleuthRunner {

  private final LinkedList<SleuthEventListener> eventListenerList = new LinkedList<>();
  private final AtomicBoolean cancelled = new AtomicBoolean(false);

  /**
   * Registers a listener to receive Sleuth events.
   *
   * @param listener listener to add
   */
  public void addListener (SleuthEventListener listener) {

    eventListenerList.add(listener);
  }

  /**
   * Removes a previously registered listener.
   *
   * @param listener listener to remove
   */
  public void removeListener (SleuthEventListener listener) {

    eventListenerList.remove(listener);
  }

  /**
   * Fires an event to all registered listeners.
   *
   * @param sleuthEvent event to dispatch
   */
  public void fire (SleuthEvent sleuthEvent) {

    for (SleuthEventListener listener : eventListenerList) {
      listener.handle(sleuthEvent);
    }
  }

  /**
   * Requests cancellation of the currently running test run.
   */
  public void cancel () {

    cancelled.set(true);
  }

  /**
   * @return {@code true} while execution has not been cancelled
   */
  public boolean isRunning () {

    return !cancelled.get();
  }

  /**
   * Executes the supplied classes as test suites.
   *
   * @param groups        optional list of groups to include; {@code null} runs suites honoring annotation defaults, empty array means all
   * @param threadCount   number of threads to permit; values {@literal <}= 0 map to unbounded
   * @param stopOnError   whether to halt on unexpected errors
   * @param stopOnFailure whether to halt on assertion failures
   * @param classes       classes to execute
   */
  public void execute (String[] groups, int threadCount, boolean stopOnError, boolean stopOnFailure, Class<?>... classes) {

    execute(groups, threadCount, stopOnError, stopOnFailure, Arrays.asList(classes));
  }

  /**
   * Executes the supplied iterable of suite classes.
   *
   * @param groups        optional list of groups to include; {@code null} uses defaults, empty array means all
   * @param threadCount   number of threads to permit; values {@literal <}= 0 map to unbounded
   * @param stopOnError   whether to halt on unexpected errors
   * @param stopOnFailure whether to halt on assertion failures
   * @param classIterable classes to execute
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
              suiteAnalysis.add(new Dependency<>(clazz.getName(), annotationDictionary.getSuite(), clazz, annotationDictionary.getSuite().priority(), annotationDictionary.getSuite().executeAfter(), annotationDictionary.getSuite().dependsOn()));
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
   * Checks whether any of our configured groups intersect with the requested set.
   *
   * @param ours   groups defined on the suite or test
   * @param theirs requested groups from the command line/configuration
   * @return {@code true} if execution should proceed
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
