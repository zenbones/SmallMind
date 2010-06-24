package org.smallmind.nutsnbolts.swing.menu;

import java.awt.event.ActionListener;
import javax.swing.Action;

public interface MenuActionProvider {

   public abstract ActionListener getDefaultActionListener ();

   public abstract Action getAction (String key);

}
