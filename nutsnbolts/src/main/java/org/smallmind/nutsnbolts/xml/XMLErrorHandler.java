package org.smallmind.nutsnbolts.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLErrorHandler implements ErrorHandler {

   private static XMLErrorHandler ERROR_HANDLER = new XMLErrorHandler();

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
