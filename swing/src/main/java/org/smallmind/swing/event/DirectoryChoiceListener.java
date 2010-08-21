package org.smallmind.swing.event;

import java.util.EventListener;

public interface DirectoryChoiceListener extends EventListener {

   public abstract void rootChosen (DirectoryChoiceEvent directoryChoiceEvent);

   public abstract void directoryChosen (DirectoryChoiceEvent directoryChoiceEvent);

}



