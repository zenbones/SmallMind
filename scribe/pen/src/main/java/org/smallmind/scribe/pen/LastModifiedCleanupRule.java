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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * Cleanup rule that removes files older than a configured duration.
 */
public class LastModifiedCleanupRule implements CleanupRule<LastModifiedCleanupRule> {

  private final long now = System.currentTimeMillis();
  private Stint stint;
  private long durationAsMilliseconds;

  /**
   * Creates an unconfigured rule. Set the {@link Stint} before use.
   */
  public LastModifiedCleanupRule () {

  }

  /**
   * Creates a rule that deletes files older than the given duration.
   *
   * @param stint maximum age to retain
   */
  public LastModifiedCleanupRule (Stint stint) {

    setStint(stint);
  }

  /**
   * Returns the configured age threshold.
   *
   * @return stint representing maximum file age
   */
  public Stint getStint () {

    return stint;
  }

  /**
   * Sets the age threshold for deletion.
   *
   * @param stint duration to retain files
   */
  public void setStint (Stint stint) {

    this.stint = stint;

    durationAsMilliseconds = stint.toMilliseconds();
  }

  /**
   * Creates a copy of this rule with the same stint.
   *
   * @return duplicated rule
   */
  @Override
  public LastModifiedCleanupRule copy () {

    return new LastModifiedCleanupRule(stint);
  }

  /**
   * Determines whether the file exceeds the configured age threshold.
   *
   * @param possiblePath path to evaluate
   * @return {@code true} if the file is older than the stint duration
   * @throws IOException if file metadata cannot be read
   */
  @Override
  public boolean willCleanup (Path possiblePath)
    throws IOException {

    return now - Files.getLastModifiedTime(possiblePath).toMillis() > durationAsMilliseconds;
  }

  /**
   * No-op finish implementation for compatibility.
   */
  @Override
  public void finish () {

  }
}
