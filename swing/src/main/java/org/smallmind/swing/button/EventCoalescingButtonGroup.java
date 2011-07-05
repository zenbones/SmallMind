package org.smallmind.swing.button;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class EventCoalescingButtonGroup extends ButtonGroup implements ActionListener {

  private WeakEventListenerList<ActionListener> listenerList = new WeakEventListenerList<ActionListener>();

  @Override
  public void add (AbstractButton button) {

    button.addActionListener(this);
    super.add(button);
  }

  @Override
  public void remove (AbstractButton button) {

    button.removeActionListener(this);
    super.remove(button);
  }

  @Override
  public void actionPerformed (ActionEvent actionEvent) {

    GroupedActionEvent groupedActionEvent = new GroupedActionEvent(this, actionEvent);

    for (ActionListener actionListener : listenerList) {
      actionListener.actionPerformed(groupedActionEvent);
    }
  }

  public void addActionListener (ActionListener actionListener) {

    listenerList.addListener(actionListener);
  }

  public void removeActionListener (ActionListener actionListener) {

    listenerList.removeListener(actionListener);
  }
}
