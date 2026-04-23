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
package org.smallmind.forge.deploy;

/**
 * Console progress indicator that tracks completion of a fixed-size unit of work. Supports two
 * rendering modes controlled by {@code emulateGraphics}: a single-line rewriting ASCII bar, or a
 * multi-line percentage report. Output is emitted only when the completion segment advances.
 * Both {@link #close()} and {@link #update(long)} are {@code synchronized}.
 */
public class TextProgressBar {

  private final String measure;
  private final boolean emulateGraphics;
  private final double total;
  private final int segmentPercent;
  private final int numberOfSegments;
  private boolean done = false;
  private int previousSegment = -1;

  /**
   * Create a progress bar for a bounded amount of work.
   *
   * @param total           total number of units to complete; serves as the 100% baseline
   * @param measure         label appended to each progress line describing the unit (e.g. {@code bytes})
   * @param segmentPercent  percentage increment between successive console updates
   * @param emulateGraphics {@code true} to overwrite a single console line with an ASCII bar;
   *                        {@code false} to emit one percentage-complete line per segment
   */
  public TextProgressBar (long total, String measure, int segmentPercent, boolean emulateGraphics) {

    this.emulateGraphics = emulateGraphics;

    this.total = total;
    this.measure = measure;
    this.segmentPercent = segmentPercent;

    numberOfSegments = (100 / segmentPercent) + ((100 % segmentPercent == 0) ? 0 : 1);
  }

  /**
   * Finalize the progress bar display.
   *
   * <p>In graphics-emulation mode, emits a trailing newline so subsequent console output begins on
   * a fresh line. Subsequent calls after the first are no-ops.
   */
  public synchronized void close () {

    if (!done) {
      done = true;
      if (emulateGraphics) {
        System.out.println();
      }
    }
  }

  /**
   * Advance the displayed progress to {@code current} units completed.
   *
   * <p>Output is written only when the completion segment changes, avoiding redundant console
   * writes. When {@code current} equals {@code total} the bar is automatically marked as done.
   *
   * @param current units of work completed so far; must not exceed {@code total}
   * @throws IllegalArgumentException if {@code current} is greater than {@code total}
   */
  public synchronized void update (long current) {

    if (current > total) {
      throw new IllegalArgumentException("Current values must be <= " + total);
    }

    double currentPercent = (current / total) * 100;
    int currentSegment = (int)(currentPercent / segmentPercent);

    if ((!done) && (currentSegment != previousSegment)) {
      if (!emulateGraphics) {
        System.out.print((int)currentPercent);
        System.out.print("% (");

        System.out.print(current);
        System.out.print(" of ");
        System.out.print((long)total);
        System.out.print(" ");
        System.out.print(measure);
        System.out.println(")");

        if (current == total) {
          done = true;
        }
      } else {
        System.out.print("\r[");
        for (int tail = 0; tail < currentSegment; tail++) {
          System.out.print("=");
        }
        if (current < total) {
          System.out.print(">");
        }
        for (int blank = currentSegment; blank < (numberOfSegments - 1); blank++) {
          System.out.print(" ");
        }
        System.out.print("] ");

        System.out.print((int)currentPercent);
        System.out.print("% (");

        System.out.print(current);
        System.out.print(" of ");
        System.out.print((long)total);
        System.out.print(" ");
        System.out.print(measure);

        if (current == total) {
          done = true;
          System.out.println(")");
        } else {
          System.out.print(")");
        }
      }
    }

    previousSegment = currentSegment;
  }
}
