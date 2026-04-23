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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.smallmind.nutsnbolts.io.PathUtility;

/**
 * Scans the parent directory of an active log file for rolled files that share its base name,
 * then applies a set of {@link CleanupRule} instances to identify and delete files that exceed
 * configured retention limits.
 */
public class Cleanup {

  private CleanupRule<?>[] rules;
  private char separator = '-';

  /**
   * Constructs a cleanup manager with no rules and a default {@code '-'} separator.
   */
  public Cleanup () {

  }

  /**
   * Constructs a cleanup manager with the given rules and a default {@code '-'} separator.
   *
   * @param rules the cleanup rules to apply during each vacuum pass
   */
  public Cleanup (CleanupRule<?>... rules) {

    this.rules = rules;
  }

  /**
   * Constructs a cleanup manager with an explicit separator and cleanup rules.
   *
   * @param separator the character that separates the base file name from the rollover suffix
   * @param rules     the cleanup rules to apply during each vacuum pass
   */
  public Cleanup (char separator, CleanupRule<?>... rules) {

    this.separator = separator;
    this.rules = rules;
  }

  /**
   * Replaces the separator used to distinguish base file names from rollover suffixes.
   *
   * @param separator the new separator character
   */
  public void setSeparator (char separator) {

    this.separator = separator;
  }

  /**
   * Replaces the set of cleanup rules applied during each vacuum pass.
   *
   * @param rules the new cleanup rules to enforce
   */
  public void setRules (CleanupRule<?>[] rules) {

    this.rules = rules;
  }

  /**
   * Iterates over rolled files in the same directory as {@code logPath}, passes each candidate to
   * every rule's {@code willCleanup} method (deleting immediately on the first {@code true} result),
   * and then calls {@code finish()} on each rule copy to perform any deferred deletions.
   * Each rule is copied before the pass so that accumulated state from a previous call does not interfere.
   *
   * @param logPath the path of the active (non-rolled) log file used to derive the base-name filter
   * @throws IOException     if directory listing, attribute reading, or file deletion fails
   * @throws LoggerException if a cleanup rule signals a configuration error
   */
  public void vacuum (Path logPath)
    throws IOException, LoggerException {

    if ((rules != null) && (rules.length > 0)) {

      CleanupRule<?>[] copyOfRules;
      Path parentPath;
      String logFileName;
      String prologue;
      String epilogue = null;
      int dotPos;

      copyOfRules = new CleanupRule[rules.length];
      for (int index = 0; index < copyOfRules.length; index++) {
        copyOfRules[index] = rules[index].copy();
      }

      logFileName = PathUtility.fileNameAsString(logPath);

      if ((dotPos = logFileName.lastIndexOf('.')) >= 0) {
        prologue = logFileName.substring(0, dotPos) + separator;
        epilogue = logFileName.substring(dotPos);
      } else {
        prologue = logFileName + separator;
      }

      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(((parentPath = logPath.getParent()) == null) ? Paths.get("/") : parentPath)) {
        for (Path possiblePath : directoryStream) {
          if (Files.isRegularFile(possiblePath) && (!possiblePath.equals(logPath))) {

            String possibleFileName = PathUtility.fileNameAsString(possiblePath);

            if (possibleFileName.startsWith(prologue) && ((epilogue == null) || possibleFileName.endsWith(epilogue))) {
              for (CleanupRule<?> rule : copyOfRules) {
                if (rule.willCleanup(possiblePath)) {
                  Files.deleteIfExists(possiblePath);
                  break;
                }
              }
            }
          }
        }

        for (CleanupRule<?> rule : copyOfRules) {
          rule.finish();
        }
      }
    }
  }
}
