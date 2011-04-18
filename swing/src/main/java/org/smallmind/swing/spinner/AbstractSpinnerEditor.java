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
package org.smallmind.swing.spinner;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.EditorEvent;
import org.smallmind.swing.EditorListener;

public abstract class AbstractSpinnerEditor implements SpinnerEditor {

  private WeakEventListenerList<EditorListener> listenerList;

  private StopEditingAction stopEditingAction;
  private CancelEditingAction cancelEditingAction;

  public AbstractSpinnerEditor () {

    stopEditingAction = new StopEditingAction(this);
    cancelEditingAction = new CancelEditingAction(this);

    listenerList = new WeakEventListenerList<EditorListener>();
  }

  public StopEditingAction getStopEditingAction () {

    return stopEditingAction;
  }

  public CancelEditingAction getCancelEditingAction () {

    return cancelEditingAction;
  }

  public synchronized void addEditorListener (EditorListener editorListener) {

    listenerList.addListener(editorListener);
  }

  public synchronized void removeEditorListener (EditorListener editorListener) {

    listenerList.removeListener(editorListener);
  }

  public synchronized void fireEditorStatus (EditorEvent editorEvent) {

    for (EditorListener editorListener : listenerList) {
      editorListener.editorStatus(editorEvent);
    }
  }

  public class StopEditingAction extends AbstractAction {

    AbstractSpinnerEditor editor;

    public StopEditingAction (AbstractSpinnerEditor editor) {

      this.editor = editor;
    }

    public void actionPerformed (ActionEvent actionEvent) {

      fireEditorStatus(new EditorEvent(editor, EditorEvent.State.STOPPED));
    }
  }

  public class CancelEditingAction extends AbstractAction {

    AbstractSpinnerEditor editor;

    public CancelEditingAction (AbstractSpinnerEditor editor) {

      this.editor = editor;
    }

    public void actionPerformed (ActionEvent actionEvent) {

      fireEditorStatus(new EditorEvent(editor, EditorEvent.State.CANCELLED));
    }
  }
}
