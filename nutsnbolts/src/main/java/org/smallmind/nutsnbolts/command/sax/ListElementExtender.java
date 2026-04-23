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
package org.smallmind.nutsnbolts.command.sax;

import org.smallmind.nutsnbolts.command.template.Argument;
import org.smallmind.nutsnbolts.command.template.ListArgument;
import org.smallmind.nutsnbolts.xml.sax.AbstractElementExtender;

/**
 * SAX element extender for the {@code <list>} element that creates a {@link ListArgument} whose description is taken from the element's text content.
 */
public class ListElementExtender extends AbstractElementExtender implements ArgumentCompiler {

  private ListArgument listArgument;

  /**
   * Returns the compiled argument as an {@link Argument} reference.
   *
   * @return compiled {@link ListArgument}
   */
  @Override
  public Argument getArgument () {

    return getListArgument();
  }

  /**
   * Returns the strongly typed list argument assembled from the element content.
   *
   * @return {@link ListArgument} with the description string from the element body
   */
  public ListArgument getListArgument () {

    return listArgument;
  }

  /**
   * Constructs the {@link ListArgument} using the element's text content as the description.
   *
   * @param namespaceURI   namespace URI of the closing element
   * @param localName      local name of the closing element
   * @param qName          qualified name of the closing element
   * @param contentBuilder text content accumulated within the element
   */
  @Override
  public void endElement (String namespaceURI, String localName, String qName, StringBuilder contentBuilder) {

    listArgument = new ListArgument(contentBuilder.toString());
  }
}
