/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.swing.menu;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.smallmind.nutsnbolts.xml.XMLEntityResolver;
import org.smallmind.nutsnbolts.xml.XMLErrorHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class MenuXmlParser implements ContentHandler {

  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int[] VK_CODES = {KeyEvent.VK_A, KeyEvent.VK_B, KeyEvent.VK_C, KeyEvent.VK_D, KeyEvent.VK_E, KeyEvent.VK_F, KeyEvent.VK_G, KeyEvent.VK_H, KeyEvent.VK_I, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_L, KeyEvent.VK_M, KeyEvent.VK_N, KeyEvent.VK_O, KeyEvent.VK_P, KeyEvent.VK_Q, KeyEvent.VK_R, KeyEvent.VK_S, KeyEvent.VK_T, KeyEvent.VK_U, KeyEvent.VK_V, KeyEvent.VK_W, KeyEvent.VK_X, KeyEvent.VK_Y, KeyEvent.VK_Z};

  private final MenuModel menuModel;
  private final MenuActionProvider actionProvider;
  private final LinkedList<JComponent> menuStack;

  private MenuXmlParser (MenuModel menuModel, MenuActionProvider actionProvider) {

    this.menuModel = menuModel;
    this.actionProvider = actionProvider;

    menuStack = new LinkedList<JComponent>();
  }

  public static MenuModel parse (InputSource inputSource, MenuActionProvider actionProvider)
    throws IOException, SAXException, ParserConfigurationException {

    SAXParserFactory parserFactory;
    SAXParser parser;
    MenuXmlParser contentHandler;
    MenuModel menuModel;

    parserFactory = SAXParserFactory.newInstance();
    parserFactory.setValidating(true);
    parserFactory.setNamespaceAware(true);
    parser = parserFactory.newSAXParser();

    menuModel = new MenuModel();
    contentHandler = new MenuXmlParser(menuModel, actionProvider);

    parser.getXMLReader().setProperty("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
    parser.getXMLReader().setContentHandler(contentHandler);
    parser.getXMLReader().setEntityResolver(XMLEntityResolver.getInstance());
    parser.getXMLReader().setErrorHandler(XMLErrorHandler.getInstance());
    parser.getXMLReader().parse(inputSource);

    return menuModel;
  }

  public void setDocumentLocator (Locator locator) {

  }

  public void startDocument ()
    throws SAXException {

  }

  public void endDocument ()
    throws SAXException {

  }

  private void pushMenuItem (JMenuItem menuItem) {

    if (menuStack.getLast() instanceof JMenuBar) {
      ((JMenuBar)menuStack.getLast()).add((JMenu)menuItem);
    } else {
      ((JMenu)menuStack.getLast()).add(menuItem);
    }

    menuStack.add(menuItem);
  }

  private String getActionPath () {

    StringBuilder pathBuilder;
    Iterator menuStackIter;
    Object menuObject;

    pathBuilder = new StringBuilder();
    menuStackIter = menuStack.iterator();
    while (menuStackIter.hasNext()) {
      menuObject = menuStackIter.next();
      if (!(menuObject instanceof JMenuBar)) {
        if (pathBuilder.length() != 0) {
          pathBuilder.append("/");
        }
        pathBuilder.append(((AbstractButton)menuObject).getText());
      }
    }

    return pathBuilder.toString();
  }

  public void startElement (String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException {

    JMenu menu;
    JMenuItem menuItem;
    Action menuAction;
    String actionPath;
    String mnemonic;
    String icon;

    if (qName.equals("menu-bar")) {
      menuStack.add(menuModel.addMenuBar());
    } else if (qName.equals("menu")) {
      menu = new JMenu(atts.getValue("text"));
      if ((mnemonic = atts.getValue("mnemonic")) != null) {
        menu.setMnemonic(VK_CODES[ALPHABET.indexOf(mnemonic.charAt(0))]);
      }
      if ((icon = atts.getValue("icon")) != null) {
        menu.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icon)));
      }

      pushMenuItem(menu);
    } else if (qName.equals("menu-item")) {
      pushMenuItem(new JMenuItem());
    } else if (qName.equals("separator")) {
      ((JMenu)menuStack.getLast()).addSeparator();
    } else if (qName.equals("definition")) {
      actionPath = getActionPath() + atts.getValue("text");

      menuItem = (JMenuItem)menuStack.getLast();
      menuItem.setText(atts.getValue("text"));
      menuItem.setEnabled(Boolean.parseBoolean(atts.getValue("enabled")));
      menuItem.setActionCommand(actionPath);
      if ((mnemonic = atts.getValue("mnemonic")) != null) {
        menuItem.setMnemonic(VK_CODES[ALPHABET.indexOf(mnemonic.charAt(0))]);
      }
      if ((icon = atts.getValue("icon")) != null) {
        menuItem.setIcon(new ImageIcon(Thread.currentThread().getContextClassLoader().getResource(icon)));
      }

      menuItem.addActionListener(actionProvider.getDefaultActionListener());

      menuModel.addMenuReference(actionPath, menuItem);
    } else if (qName.equals("action")) {
      if ((menuAction = actionProvider.getAction(atts.getValue("class"))) == null) {
        throw new SAXException("No such Action (" + atts.getValue("class") + ")");
      }

      actionPath = getActionPath() + menuAction.getValue(Action.NAME);

      menuItem = (JMenuItem)menuStack.getLast();
      menuItem.setAction(menuAction);

      menuItem.setActionCommand(getActionPath());

      menuModel.addMenuReference(actionPath, menuItem);
    }
  }

  public void endElement (String namespaceURI, String localName, String qName)
    throws SAXException {

    if (qName.equals("menu-bar") || qName.equals("menu") || qName.equals("menu-item")) {
      menuStack.removeLast();
    }
  }

  public void characters (char[] ch, int start, int length)
    throws SAXException {

  }

  public void ignorableWhitespace (char[] ch, int start, int length)
    throws SAXException {

  }

  public void processingInstruction (String target, String data)
    throws SAXException {

  }

  public void skippedEntity (String name)
    throws SAXException {

  }

  public void startPrefixMapping (String prefix, String uri)
    throws SAXException {

  }

  public void endPrefixMapping (String prefix)
    throws SAXException {

  }
}
