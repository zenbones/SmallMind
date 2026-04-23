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
package org.smallmind.sleuth.maven.surefire;

import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.smallmind.nutsnbolts.lang.StackTraceUtility;
import org.smallmind.sleuth.runner.Culprit;

/**
 * Surefire {@link StackTraceWriter} implementation that delegates stack trace rendering to
 * Sleuth's internal utilities.
 * <p>
 * Created by {@link SurefireSleuthEventListener} when translating
 * {@link org.smallmind.sleuth.runner.event.FailureSleuthEvent},
 * {@link org.smallmind.sleuth.runner.event.ErrorSleuthEvent}, and
 * {@link org.smallmind.sleuth.runner.event.MootSleuthEvent} into Surefire report entries.
 * It provides three representations of the failure:
 * <ul>
 *   <li>{@link #writeTraceToString()} — full stack trace via {@link StackTraceUtility}</li>
 *   <li>{@link #writeTrimmedTraceToString()} — delegates to the full trace (no trimming)</li>
 *   <li>{@link #smartTrimmedStackTrace()} — a concise single-line summary from {@link Culprit}</li>
 * </ul>
 * When no throwable is present all string methods return an empty string and
 * {@link #getThrowable()} returns {@code null}.
 *
 * @see SurefireSleuthEventListener
 * @see Culprit
 */
public class SleuthStackTraceWriter
  implements StackTraceWriter {

  private final Throwable throwable;
  private final String testClass;
  private final String testMethod;

  /**
   * Constructs a writer for the given failure context.
   *
   * @param testClass  fully qualified name of the class where the failure occurred; must not be {@code null}
   * @param testMethod name of the method where the failure occurred; must not be {@code null}
   * @param throwable  the exception or error to render; {@code null} indicates no throwable (all methods return empty strings)
   */
  public SleuthStackTraceWriter (String testClass, String testMethod, Throwable throwable) {

    this.testClass = testClass;
    this.testMethod = testMethod;
    this.throwable = throwable;
  }

  /**
   * Returns a {@link SafeThrowable} wrapping the underlying throwable, or {@code null} when none is present.
   *
   * @return wrapped throwable, or {@code null}
   */
  @Override
  public SafeThrowable getThrowable () {

    return (throwable == null) ? null : new SafeThrowable(throwable);
  }

  /**
   * Returns the full stack trace as a string, or an empty string when no throwable is present.
   *
   * @return full stack trace text; never {@code null}
   */
  @Override
  public String writeTraceToString () {

    return (throwable == null) ? "" : StackTraceUtility.obtainStackTraceAsString(throwable);
  }

  /**
   * Returns the trimmed stack trace; delegates to {@link #writeTraceToString()} (no trimming is applied).
   *
   * @return stack trace text; never {@code null}
   */
  @Override
  public String writeTrimmedTraceToString () {

    return writeTraceToString();
  }

  /**
   * Returns a concise single-line culprit summary including the originating class, method, line number,
   * and exception message. Returns an empty string when no throwable is present.
   *
   * @return condensed culprit string, or an empty string; never {@code null}
   */
  @Override
  public String smartTrimmedStackTrace () {

    if (throwable == null) {

      return "";
    } else {

      return new Culprit(testClass, testMethod, throwable).toString();
    }
  }
}
