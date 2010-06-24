package org.smallmind.nutsnbolts.xml.sax;

import org.xml.sax.SAXException;

public interface SAXExtender {

   public abstract void completedChildElement (ElementExtender elementExtender)
      throws SAXException;
}
