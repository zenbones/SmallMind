package org.smallmind.wicket.component.panel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.smallmind.wicket.event.OnChangeListener;

public class EnumRadioPanel<E extends Enum> extends Panel {

   private Class<E> enumClass;
   private E selectedEnum;

   public EnumRadioPanel (String id, Class<E> enumClass) {

      this(id, enumClass, null, null);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, E selectedEnum) {

      this(id, enumClass, selectedEnum, null);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, OnChangeListener onChangeListener) {

      this(id, enumClass, null, onChangeListener);
   }

   public EnumRadioPanel (String id, Class<E> enumClass, E selectedEnum, OnChangeListener onChangeListener) {

      super(id);

      final E[] enumerations = enumClass.getEnumConstants();

      RadioGroup<E> radioGroup;

      this.enumClass = enumClass;
      this.selectedEnum = selectedEnum;

      add(radioGroup = new EnumRadioGroup<E>("enumRadioGroup", new Model<E>(selectedEnum), onChangeListener));
      radioGroup.add(new Loop("enumRadioLoop", new Model<Integer>(enumerations.length)) {

         @Override
         protected void populateItem (LoopItem item) {
            item.add(new Radio<E>("enumRadioButton", new Model<E>(enumerations[item.getIteration()])));
            item.add(new Label("enumRadioLabel", new Model<String>(enumerations[item.getIteration()].toString())));
         }
      });
   }

   public E getSelectedEnum () {

      return selectedEnum;
   }

   private class EnumRadioGroup<E extends Enum> extends RadioGroup<E> {

      private OnChangeListener onChangeListener;

      public EnumRadioGroup (String id, IModel<E> model, OnChangeListener onChangeListener) {

         super(id, model);

         this.onChangeListener = onChangeListener;
      }

      @Override
      protected boolean wantOnSelectionChangedNotifications () {

         return true;
      }

      @Override
      protected synchronized void onSelectionChanged (Object selection) {

         if (selection != null) {
            selectedEnum = enumClass.cast(selection);

            if (onChangeListener != null) {
               onChangeListener.onSelectionChanged(selection);
            }
         }
      }

      public synchronized OnChangeListener getOnChangeListener () {

         return onChangeListener;
      }

      public synchronized void setOnChangeListener (OnChangeListener onChangeListener) {

         this.onChangeListener = onChangeListener;
      }
   }
}