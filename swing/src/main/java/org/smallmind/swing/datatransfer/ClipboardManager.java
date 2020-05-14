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
package org.smallmind.swing.datatransfer;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import javax.swing.AbstractAction;
import javax.swing.Action;

public class ClipboardManager implements ClipboardOwner, FocusListener {

  private static final Clipboard SYSTEM_CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();

  private final Action cutAction;
  private final Action copyAction;
  private final Action pasteAction;
  private final HashMap<Component, ClipboardListener> listenerMap;
  private Component selectedComponent;

  public ClipboardManager () {

    cutAction = new CutAction(this, "Cut");
    copyAction = new CopyAction(this, "Copy");
    pasteAction = new PasteAction(this, "Paste");

    listenerMap = new HashMap<Component, ClipboardListener>();
  }

  public Action getCutAction () {

    return cutAction;
  }

  public Action getCopyAction () {

    return copyAction;
  }

  public Action getPasteAction () {

    return pasteAction;
  }

  public synchronized void addClipboardListener (ClipboardListener clipboardListener, Component clipboardUser) {

    clipboardUser.addFocusListener(this);
    listenerMap.put(clipboardUser, clipboardListener);
  }

  public synchronized void removeClipboardListener (ClipboardListener clipboardListener, Component clipboardUser) {

    clipboardUser.removeFocusListener(this);
    listenerMap.remove(clipboardUser);
  }

  public void focusGained (FocusEvent focusEvent) {

    selectedComponent = (Component)focusEvent.getSource();
  }

  public void focusLost (FocusEvent focusEvent) {

    selectedComponent = null;
  }

  public void setContents (Transferable transferable) {

    SYSTEM_CLIPBOARD.setContents(transferable, this);
  }

  public void lostOwnership (Clipboard clipboard, Transferable contents) {

  }

  public class CutAction extends AbstractAction {

    private final ClipboardManager clipboardManager;

    public CutAction (ClipboardManager clipboardManager, String name) {

      super(name);

      this.clipboardManager = clipboardManager;
    }

    public void actionPerformed (ActionEvent actionEvent) {

      ClipboardEvent clipboardEvent;

      if (selectedComponent != null) {
        (listenerMap.get(selectedComponent)).cutAction(new ClipboardEvent(clipboardManager, selectedComponent));
      }
    }
  }

  public class CopyAction extends AbstractAction {

    private final ClipboardManager clipboardManager;

    public CopyAction (ClipboardManager clipboardManager, String name) {

      super(name);

      this.clipboardManager = clipboardManager;
    }

    public void actionPerformed (ActionEvent actionEvent) {

      if (selectedComponent != null) {
        (listenerMap.get(selectedComponent)).copyAction(new ClipboardEvent(clipboardManager, selectedComponent));
      }
    }
  }

  public class PasteAction extends AbstractAction {

    private final ClipboardManager clipboardManager;

    public PasteAction (ClipboardManager clipboardManager, String name) {

      super(name);

      this.clipboardManager = clipboardManager;
    }

    public void actionPerformed (ActionEvent actionEvent) {

      Transferable transferable;

      transferable = SYSTEM_CLIPBOARD.getContents(clipboardManager);
      if (selectedComponent != null) {
        (listenerMap.get(selectedComponent)).pasteAction(new ClipboardEvent(clipboardManager, selectedComponent, transferable));
      }
    }
  }
}
