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
package org.smallmind.nutsnbolts.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLErrorHandler implements ErrorHandler {

  private static final XMLErrorHandler ERROR_HANDLER = new XMLErrorHandler();

  public static XMLErrorHandler getInstance () {

    return ERROR_HANDLER;
  }

  public void warning (SAXParseException saxParseException)
    throws SAXException {

    throw handleException(saxParseException);
  }

  public void error (SAXParseException saxParseException)
    throws SAXException {

    throw handleException(saxParseException);
  }

  public void fatalError (SAXParseException saxParseException)
    throws SAXException {

    throw handleException(saxParseException);
  }

  private SAXException handleException (SAXParseException saxParseException) {

    SAXException saxException;
    String locatedMessage;

    locatedMessage = saxParseException.getMessage() + " (public id[" + saxParseException.getPublicId() + "] system id[" + saxParseException.getSystemId() + "] line[" + saxParseException.getLineNumber() + "] column[" + saxParseException.getColumnNumber() + "])";
    saxException = new SAXException(locatedMessage, saxParseException.getException());
    return saxException;
  }
}
