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
package org.smallmind.javafx.extras.text;

import java.util.regex.Pattern;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextField;

/**
 * A {@link TextField} that restricts user input to text matching a configurable {@link Pattern}.
 * Both typed characters and clipboard pastes are validated by pre-computing the resulting field
 * value and testing it against the pattern before permitting the edit. If no pattern is configured
 * all input is accepted, making the field behave identically to a plain {@link TextField}.
 */
public class PatternTextField extends TextField {

  private Pattern pattern;

  /**
   * Creates an unrestricted text field with no validation pattern.
   */
  public PatternTextField () {

  }

  /**
   * Creates a text field that enforces the given pattern on all edits.
   *
   * @param pattern the pattern the full field value must match after any edit; may be {@code null}
   *                to allow unrestricted input
   */
  public PatternTextField (Pattern pattern) {

    this.pattern = pattern;
  }

  /**
   * Creates a text field with an initial value and a validation pattern.
   *
   * @param pattern the pattern the full field value must match after any edit; may be {@code null}
   *                to allow unrestricted input
   * @param text    the initial text content; must not be {@code null}
   */
  public PatternTextField (Pattern pattern, String text) {

    super(text);

    this.pattern = pattern;
  }

  /**
   * Replaces the current validation pattern. The new pattern takes effect on the next edit.
   *
   * @param pattern the new validation pattern; may be {@code null} to remove the restriction
   * @return this field for method chaining
   */
  public synchronized PatternTextField setPattern (Pattern pattern) {

    this.pattern = pattern;

    return this;
  }

  /**
   * Replaces the text in the range {@code [start, end)} with {@code text} only if the resulting
   * field value would match the configured pattern. If no pattern is set the replacement is always
   * applied.
   *
   * @param start the inclusive start index of the range to replace
   * @param end   the exclusive end index of the range to replace
   * @param text  the replacement text; must not be {@code null}
   */
  @Override
  public synchronized void replaceText (int start, int end, String text) {

    StringBuilder forecast = new StringBuilder(getText()).replace(start, end, text);

    if ((pattern == null) || pattern.matcher(forecast).matches()) {
      super.replaceText(start, end, text);
    }
  }

  /**
   * Replaces the current selection with {@code text} only if the resulting field value would match
   * the configured pattern. If no pattern is set the replacement is always applied.
   *
   * @param text the replacement text; must not be {@code null}
   */
  @Override
  public synchronized void replaceSelection (String text) {

    IndexRange range = getSelection();
    StringBuilder forecast = new StringBuilder(getText()).replace(range.getStart(), range.getEnd(), text);

    if ((pattern == null) || pattern.matcher(forecast).matches()) {
      super.replaceSelection(text);
    }
  }
}
