/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.swing.panel;

import java.awt.LayoutManager;
import javax.swing.JPanel;
import org.smallmind.swing.dialog.DialogState;
import org.smallmind.swing.dialog.OptionDialog;

public abstract class OptionPanel extends JPanel {

  private OptionDialog optionDialog;
  private boolean initialized = false;

  public OptionPanel (LayoutManager layoutManager) {

    super(layoutManager);
  }

  public OptionDialog getOptionDialog () {

    return optionDialog;
  }

  public DialogState getDialogState () {

    return optionDialog.getDialogState();
  }

  public void setDialogSate (DialogState dialogState) {

    optionDialog.setDialogState(dialogState);
  }

  public void initialize (OptionDialog optionDialog) {

    this.optionDialog = optionDialog;

    initialized = true;
  }

  public void closeParent () {

    if (!initialized) {
      throw new IllegalStateException("Parent dialog was never initialized");
    }

    optionDialog.windowClosing(null);
  }

  public abstract String validateOption (DialogState dialogState);
}
