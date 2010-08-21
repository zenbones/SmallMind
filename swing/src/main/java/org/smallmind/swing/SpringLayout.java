/*
 * @(#)SpringLayout.java	1.16 97/11/11
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

package org.smallmind.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;
import javax.swing.JComponent;

/**
 * Simple Springs and Struts LayoutManager.
 * The _CAN_CHANGE values can be or-ed together to specify all the
 * springs for a.nutsnbolts. For example:
 * <PRE>
 * layout.setSprings(widget, SpringLayout.RIGHT_MARGIN_CAN_CHANGE
 * | SpringLayout.TOP_MARGIN_CAN_CHANGE));
 * or
 * parent.add(child, SpringLayout.RIGHT_TOP_SPRINGS);
 * </PRE>
 * Space will be evenly distributed among all springs along a single
 * axis.  i.e. If you have set the springs such that
 * RIGHT_MARGIN_CAN_CHANGE | LEFT_MARGIN_CAN_CHANGE and the parent
 * grows by 10 pixels in the x dimension, the left and right margins
 * of the child will each grow by 5 pixels.
 * <P> This layout manager does not determine default sizes for
 * components. It simply allocates changes in space to the components
 * as appropriate. To properly utilize this LayoutManager, you should
 * specify the bounds of the Components prior to placing them in the
 * view hierarchy. When the parent's bounds change, the children will
 * have their bounds changed accoordingly.
 * <P> The current SmallMind manages the springs as a set of
 * integer values. You may pass an Integer object as a constraint to
 * the add.nutsnbolts. constraint) method of Container. For example:
 * <PRE>
 * parent.add(child, new Integer(SpringLayout.RIGHT_MARGIN_CAN_CHANGE
 * | SpringLayout.TOP_MARGIN_CAN_CHANGE));
 * </PRE>
 * Using the createSpring() is the preferred manner of getting
 * a properly configured constraint object.
 *
 * @author David Kloba
 * @version 1.16 11/11/97
 */
public class SpringLayout implements LayoutManager2, Serializable {

   /**
    * Indicates that the space to the right of widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int RIGHT_MARGIN_CAN_CHANGE = 1;
   /**
    * Indicates that the space to the left of widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int LEFT_MARGIN_CAN_CHANGE = 2;
   /**
    * Indicates that the space to the top of widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int TOP_MARGIN_CAN_CHANGE = 4;
   /**
    * Indicates that the space to the bottom of widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int BOTTOM_MARGIN_CAN_CHANGE = 8;
   /**
    * Indicates that the width of the widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int WIDTH_CAN_CHANGE = 16;
   /**
    * Indicates that the height of the widget will
    * grow and shrink as the parent.nutsnbolts.changes size.
    */
   public final static int HEIGHT_CAN_CHANGE = 32;

   protected final static int HW_SPRINGS_MASK = HEIGHT_CAN_CHANGE | WIDTH_CAN_CHANGE;

   protected final static int VALIDITY_MASK = ~(RIGHT_MARGIN_CAN_CHANGE | LEFT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE | WIDTH_CAN_CHANGE | HEIGHT_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to RIGHT_MARGIN_CAN_CHANGE.
    */
   public final static Integer RIGHT_SPRING = new Integer(RIGHT_MARGIN_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to LEFT_MARGIN_CAN_CHANGE.
    */
   public final static Integer LEFT_SPRING = new Integer(LEFT_MARGIN_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to TOP_MARGIN_CAN_CHANGE.
    */
   public final static Integer TOP_SPRING = new Integer(TOP_MARGIN_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to BOTTOM_MARGIN_CAN_CHANGE.
    */
   public final static Integer BOTTOM_SPRING = new Integer(BOTTOM_MARGIN_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to WIDTH_CAN_CHANGE.
    */
   public final static Integer WIDTH_SPRING = new Integer(WIDTH_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to HEIGHT_CAN_CHANGE.
    */
   public final static Integer HEIGHT_SPRING = new Integer(HEIGHT_CAN_CHANGE);
   /**
    * Convience constraint object. Equivalent to HEIGHT_CAN_CHANGE  | WIDTH_CAN_CHANGE.
    */
   public final static Integer HEIGHT_WIDTH_SPRING = new Integer(HW_SPRINGS_MASK);

   /**
    * Convience constraint object. Equivalent to LEFT_MARGIN_CAN_CHANGE | RIGHT_MARGIN_CAN_CHANGE.
    */
   public final static Integer CENTER_HORIZ_SPRINGS = new Integer(LEFT_MARGIN_CAN_CHANGE | RIGHT_MARGIN_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to TOP_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE.
    */
   public final static Integer CENTER_VERT_SPRINGS = new Integer(TOP_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to LEFT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE.
    */
   public final static Integer BOTTOM_LEFT_SPRINGS = new Integer(LEFT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to RIGHT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE.
    */
   public final static Integer BOTTOM_RIGHT_SPRINGS = new Integer(RIGHT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to RIGHT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE.
    */
   public final static Integer TOP_RIGHT_SPRINGS = new Integer(RIGHT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE);

   /**
    * Convience constraint object. Equivalent to LEFT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE.
    */
   public final static Integer TOP_LEFT_SPRINGS = new Integer(LEFT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE);

   /**
    * This is the key used in the object properties list to store the
    * springs value.
    */
   protected final static String SPRINGS_KEY = "springLayoutSpringsKey";

   /**
    * This is the key used in the object properties list to store the
    * orginal bounds of the.nutsnbolts.
    */
   protected final static String ORIGINAL_BOUNDS_KEY = "springLayoutOriginalBoundsKey";

   protected boolean noticeBoundsChanged;
   protected Hashtable<Component, SpringValues> componentAttributes;

   // PENDING(klobad) Need to determine process to detect bounds changes by
   //                 processes other than layout machinery.
   //    protected BoundsChecker checker;

   public SpringLayout () {
      noticeBoundsChanged = true;
      //	checker = new BoundsChecker();
      componentAttributes = new Hashtable<Component, SpringValues>(15);
   }

   ///////////////////////////////////////////////////////////////////////////////////
   /// Custom Methods
   ///////////////////////////////////////////////////////////////////////////////////

   /**
    * Sets the <b>springs</b> values for this <b>container</b>.
    * <b>springs</b> must be a valid combination of the constants
    * defined, or this method will throw an IllegalArgumentException.
    */
   public void setSprings (Component container, int springs) {
      if (!isValidSpring(springs)) {
         throw new IllegalArgumentException("Invalid Spring: " + springs);
      }
      if (container instanceof JComponent) {
         ((JComponent)container).putClientProperty(SPRINGS_KEY, createSpring(springs));
      }
      else {
         SpringValues sv = componentAttributes.get(container);
         if (sv == null) {
            sv = new SpringValues();
            sv.springConstraint = createSpring(springs);
            componentAttributes.put(container, sv);
         }
         else {
            sv.springConstraint = createSpring(springs);
         }
      }
   }

   /**
    * Returns the current spring settings for this <b>container</b>.
    * If no values have been set returns BOTTOM_RIGHT_SPRINGS.intValue().
    */
   public int getSprings (Component container) {
      if (container instanceof JComponent) {
         Integer springs = (Integer)((JComponent)container).getClientProperty(SPRINGS_KEY);
         if (springs == null) {
            return BOTTOM_RIGHT_SPRINGS.intValue();
         }
         return springs.intValue();
      }
      else {
         SpringValues sv = componentAttributes.get(container);
         if (sv != null && sv.springConstraint != null) {
            return sv.springConstraint.intValue();
         }
         return BOTTOM_RIGHT_SPRINGS.intValue();
      }
   }

   /**
    * Returns an Integer used for storing springs in data structures
    * needing objects. Current SmallMind returns a shared instance
    * for common values. This object can be passed into the
    * add.nutsnbolts. constraint) method as a constraint.
    */
   public static Integer createSpring (int spring) {
      switch (spring) {

         case RIGHT_MARGIN_CAN_CHANGE:
            return RIGHT_SPRING;
         case LEFT_MARGIN_CAN_CHANGE:
            return LEFT_SPRING;
         case TOP_MARGIN_CAN_CHANGE:
            return TOP_SPRING;
         case BOTTOM_MARGIN_CAN_CHANGE:
            return BOTTOM_SPRING;
         case WIDTH_CAN_CHANGE:
            return WIDTH_SPRING;
         case HEIGHT_CAN_CHANGE:
            return HEIGHT_SPRING;

         case HW_SPRINGS_MASK:
            return HEIGHT_WIDTH_SPRING;
         case RIGHT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE:
            return BOTTOM_RIGHT_SPRINGS;
         case LEFT_MARGIN_CAN_CHANGE | RIGHT_MARGIN_CAN_CHANGE:
            return CENTER_HORIZ_SPRINGS;
         case TOP_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE:
            return CENTER_VERT_SPRINGS;
         case LEFT_MARGIN_CAN_CHANGE | BOTTOM_MARGIN_CAN_CHANGE:
            return BOTTOM_LEFT_SPRINGS;
         case RIGHT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE:
            return TOP_RIGHT_SPRINGS;
         case LEFT_MARGIN_CAN_CHANGE | TOP_MARGIN_CAN_CHANGE:
            return TOP_LEFT_SPRINGS;

         default:
            return new Integer(spring);
      }
   }

   /**
    * Returns false if <b>spring</b> is not a valid combination of springs.
    */
   public boolean isValidSpring (int spring) {
      if ((spring & VALIDITY_MASK) != 0) {
         return false;
      }
      return true;
   }

   /**
    * The first time the <b>parent</b> is asked to layout, it's bounds
    * and the bounds of it's immediate children are cached. Changes in
    * size are calculated from this original bounds. Calling this method
    * causes the cache to be updated to the current bounds of the <b>parent</b>
    * and it's immediate children. This can be useful, if you have manually
    * adjusted the bounds of a view after it's already had layout called on it,
    * and you want all further calculations to use the updated values.
    * You normally won't need to call this method.
    */
   public void resetOriginalBounds (Container parent) {
      Rectangle parentOriginalBounds;
      Component[] children;
      int i, count;
      Component childView;

      storeOriginalBoundsFor(parent, parent.getBounds());

      children = parent.getComponents();
      count = parent.getComponentCount();
      for (i = 0; i < count; i++) {
         childView = children[i];
         storeOriginalBoundsFor(childView, childView.getBounds());
      }
   }

   ///////////////////////////////////////////////////////////////////////////////////
   /// LayoutManager Interface
   ///////////////////////////////////////////////////////////////////////////////////

   public void addLayoutComponent (String name, Component comp) {
      //	comp.addComponentListener(checker);
   }

   public void removeLayoutComponent (Component comp) {
      //	comp.removeComponentListener(checker);
      storeOriginalBoundsFor(comp, null);
   }

   /**
    * Returns the parent's size.
    */
   public Dimension preferredLayoutSize (Container parent) {
      if (parent.getLayout() instanceof SpringLayout) {
         return parent.getSize();
      }
      else {
         return parent.getPreferredSize();
      }
   }

   /**
    * Returns the parent's minimum size.
    */
   public Dimension minimumLayoutSize (Container parent) {
      if (parent.getLayout() instanceof SpringLayout) {
         return new Dimension(0, 0);
      }
      else {
         return parent.getMinimumSize();
      }
   }

   /**
    * Performs the actual layout of the.nutsnbolts.based on their spring settings.
    */
   public void layoutContainer (Container parent) {
      Rectangle parentOriginalBounds;
      Component[] children;
      int i, count;
      Rectangle childOrigBounds;
      int xDelta, yDelta, adjustedDelta;
      Component childView;
      int springs, springCount;
      int _x, _y, _w, _h;

      count = parent.getComponentCount();
      if (count < 1) {
         return;
      }

      noticeBoundsChanged = false;

      parentOriginalBounds = originalBoundsFor(parent);
      xDelta = parent.getSize().width - parentOriginalBounds.width;
      yDelta = parent.getSize().height - parentOriginalBounds.height;

      children = parent.getComponents();
      for (i = 0; i < count; i++) {
         childView = children[i];
         springs = getSprings(childView);
         childOrigBounds = originalBoundsFor(childView);

         _x = childOrigBounds.x;
         _y = childOrigBounds.y;
         _w = childOrigBounds.width;
         _h = childOrigBounds.height;

         // Control the Horizontal
         springCount = horizontalSpringCount(springs);
         if (springCount > 1) {
            adjustedDelta = xDelta / springCount;
         }
         else {
            adjustedDelta = xDelta;
         }

         if ((springs & LEFT_MARGIN_CAN_CHANGE) != 0) {
            _x += adjustedDelta;
         }
         if ((springs & WIDTH_CAN_CHANGE) != 0) {
            _w += adjustedDelta;
         }
         if ((springs & RIGHT_MARGIN_CAN_CHANGE) != 0) {
         }

         // Control the Vertical
         springCount = verticalSpringCount(springs);
         if (springCount > 1) {
            adjustedDelta = yDelta / springCount;
         }
         else {
            adjustedDelta = yDelta;
         }
         if ((springs & TOP_MARGIN_CAN_CHANGE) != 0) {
            _y += adjustedDelta;
         }
         if ((springs & HEIGHT_CAN_CHANGE) != 0) {
            _h += adjustedDelta;
         }
         if ((springs & BOTTOM_MARGIN_CAN_CHANGE) != 0) {
         }

         // If there was a delta change, reset the bounds
         Rectangle r = childView.getBounds();
         if (_x != r.x || _y != r.y || _w != r.width || _h != r.height) {
            childView.setBounds(_x, _y, _w, _h);
         }
      }
      noticeBoundsChanged = true;
   }
   ///////////////////////////////////////////////////////////////////////////////////
   /// LayoutManager2 Interface
   ///////////////////////////////////////////////////////////////////////////////////

   /**
    * If <b>constraints</b> is a valid spring object (as returned from
    * createSpring()) then these constraints will be set on the
    * <b>comp</b>. Normally you will use setSprings() to accomplish this.
    * If <b>constraints</b> is invalid, this method throw an IllegalArgumentException.
    */
   public void addLayoutComponent (Component comp, Object constraints) {
      if (constraints != null) {
         if (constraints instanceof Integer && isValidSpring(((Integer)constraints).intValue())) {
            // User might be optimizing here, might be better to use
            // their constraint object
            setSprings(comp, ((Integer)constraints).intValue());
         }
         else {
            throw new IllegalArgumentException("Invalid Spring: " + constraints);
         }
      }
      //	comp.addComponentListener(checker);
   }

   /**
    * Returns the target's maximum size.
    */
   public Dimension maximumLayoutSize (Container target) {
      if (target.getLayout() instanceof SpringLayout) {
         return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
      }
      else {
         return target.getMaximumSize();
      }
   }

   /**
    * Returns the target's alignment x.
    */
   public float getLayoutAlignmentX (Container target) {
      if (target.getLayout() instanceof SpringLayout) {
         return 0.0f;
      }
      else {
         return target.getAlignmentX();
      }
   }

   /**
    * Returns the target's alignment y.
    */
   public float getLayoutAlignmentY (Container target) {
      if (target.getLayout() instanceof SpringLayout) {
         return 0.0f;
      }
      else {
         return target.getAlignmentY();
      }
   }

   public void invalidateLayout (Container target) {
   }

   ///////////////////////////////////////////////////////////////////////////////////
   /// Protected methods
   ///////////////////////////////////////////////////////////////////////////////////

   protected int verticalSpringCount (int springs) {
      int result = 0;

      if ((springs & TOP_MARGIN_CAN_CHANGE) != 0) {
         result++;
      }
      if ((springs & BOTTOM_MARGIN_CAN_CHANGE) != 0) {
         result++;
      }
      if ((springs & HEIGHT_CAN_CHANGE) != 0) {
         result++;
      }
      return result;
   }

   protected int horizontalSpringCount (int springs) {
      int result = 0;

      if ((springs & LEFT_MARGIN_CAN_CHANGE) != 0) {
         result++;
      }
      if ((springs & RIGHT_MARGIN_CAN_CHANGE) != 0) {
         result++;
      }
      if ((springs & WIDTH_CAN_CHANGE) != 0) {
         result++;
      }
      return result;
   }

   // Note the possible side effect of this call
   // We are lazily storing the original bounds of the.nutsnbolts.

   protected Rectangle originalBoundsFor (Component view) {
      Rectangle origBounds = null;
      if (view instanceof JComponent) {
         origBounds = (Rectangle)((JComponent)view).getClientProperty(ORIGINAL_BOUNDS_KEY);
         if (origBounds == null) {
            origBounds = view.getBounds();
            storeOriginalBoundsFor(view, origBounds);
         }
         return origBounds;
      }
      else {
         SpringValues sv = componentAttributes.get(view);
         if (sv != null && sv.origBounds != null) {
            return sv.origBounds;
         }
         origBounds = view.getBounds();
         storeOriginalBoundsFor(view, origBounds);
         return origBounds;
      }
   }

   protected void storeOriginalBoundsFor (Component view, Rectangle bounds) {
      if ((view instanceof JComponent) && (bounds != null)) {
         ((JComponent)view).putClientProperty(ORIGINAL_BOUNDS_KEY, bounds);
      }
      else {
         SpringValues sv = componentAttributes.get(view);
         if (sv == null) {
            sv = new SpringValues();
            sv.origBounds = bounds;
            componentAttributes.put(view, sv);
         }
         else {
            sv.origBounds = bounds;
         }
      }
   }

   //    protected class BoundsChecker extends ComponentAdapter {
   //	public void componentResized(ComponentEvent e)    {
   //	    if(noticeBoundsChanged) {
   //		Component c = (Component)e.getSource();
   ////		storeOriginalBoundsFor(c, c.getBounds());
   //	    }
   //	}
   //	public void componentMoved(ComponentEvent e) {
   //	    if(noticeBoundsChanged) {
   //		Component c = (Component)e.getSource();
   ////		storeOriginalBoundsFor(c, c.getBounds());
   //	    }
   //	}
   //    };

   protected class SpringValues implements Serializable {

      public Rectangle origBounds;
      public Integer springConstraint;
   }

   ;
}
