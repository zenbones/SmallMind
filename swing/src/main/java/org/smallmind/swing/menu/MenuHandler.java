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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.swing.Action;
import javax.swing.JFrame;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MenuHandler implements ActionListener, MenuActionProvider {

  private final JFrame parentFrame;
  private final MenuModel menuModel;
  private final MenuDelegateFactory menuDelegateFactory;

  public MenuHandler (JFrame parentFrame, MenuDelegateFactory menuDelegateFactory, InputSource configSource)
    throws IOException, SAXException, ParserConfigurationException {

    this.parentFrame = parentFrame;
    this.menuDelegateFactory = menuDelegateFactory;

    menuModel = MenuXmlParser.parse(configSource, this);
    parentFrame.setJMenuBar(menuModel.getMenuBar(0));
  }

  public JFrame getParentFrame () {

    return parentFrame;
  }

  public MenuDelegate getDelegate (String menuPath) {

    return menuDelegateFactory.getDelegate(menuPath);
  }

  public void setEnabled (String menuPath, boolean enabled) {

    menuModel.getMenuItem(menuPath).setEnabled(enabled);
  }

  @Override
  public void actionPerformed (ActionEvent actionEvent) {

    getDelegate(actionEvent.getActionCommand()).execute(this);
  }

  @Override
  public ActionListener getDefaultActionListener () {

    return this;
  }

  @Override
  public Action getAction (String className) {

    throw new UnsupportedOperationException();
  }
}
