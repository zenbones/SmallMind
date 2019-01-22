/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

public class PatternTextField extends TextField {

  private Pattern pattern;

  public PatternTextField () {

  }

  public PatternTextField (Pattern pattern) {

    this.pattern = pattern;
  }

  public PatternTextField (Pattern pattern, String text) {

    super(text);

    this.pattern = pattern;
  }

  public synchronized PatternTextField setPattern (Pattern pattern) {

    this.pattern = pattern;

    return this;
  }

  @Override
  public synchronized void replaceText (int start, int end, String text) {

    StringBuilder forecast = new StringBuilder(getText()).replace(start, end, text);

    if ((pattern == null) || pattern.matcher(forecast).matches()) {
      super.replaceText(start, end, text);
    }
  }

  @Override
  public synchronized void replaceSelection (String text) {

    IndexRange range = getSelection();
    StringBuilder forecast = new StringBuilder(getText()).replace(range.getStart(), range.getEnd(), text);

    if ((pattern == null) || pattern.matcher(forecast).matches()) {
      super.replaceSelection(text);
    }
  }
}
