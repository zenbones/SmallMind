/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.file;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.filechooser.FileFilter;
import org.smallmind.swing.event.FileChoiceEvent;
import org.smallmind.swing.event.FileChoiceListener;

public class FileChooserDialog extends JDialog implements FileChoiceListener {

  private FileChooserPanel fileChooserPanel;
  private File chosenFile;

  public FileChooserDialog (Window parentWindow, FileChooserState state) {

    this(parentWindow, state, new File(System.getProperty("user.home")));
  }

  public FileChooserDialog (Window parentWindow, FileChooserState state, File directory) {

    this(parentWindow, state, directory, null);
  }

  public FileChooserDialog (Window parentWindow, FileChooserState state, File directory, FileFilter filter) {

    super(parentWindow, state.getTitle(), ModalityType.APPLICATION_MODAL);

    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

    setLayout(new BorderLayout());
    add(fileChooserPanel = new FileChooserPanel(this, state, directory, filter));
    fileChooserPanel.addFileChoiceListener(this);

    setMinimumSize(new Dimension(550, 350));
    setLocationRelativeTo(parentWindow);
  }

  public File getChosenFile () {

    return chosenFile;
  }

  public FileFilter getFilter () {

    return fileChooserPanel.getFilter();
  }

  public void setFilter (FileFilter filter) {

    this.fileChooserPanel.setFilter(filter);
  }

  @Override
  public void fileChosen (FileChoiceEvent fileChoiceEvent) {

    chosenFile = fileChoiceEvent.getChosenFile();
    setVisible(false);
    dispose();
  }
}