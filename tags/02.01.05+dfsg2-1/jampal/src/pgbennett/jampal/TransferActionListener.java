/*
    Copyright 2004 Peter Bennett

    This file is part of Jampal.

    This file is part of Jampal.

    Jampal is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Jampal is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Jampal.  If not, see <http://www.gnu.org/licenses/>.
*/


package pgbennett.jampal;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;


/*
 * A class that tracks the focused component.  This is necessary
 * to delegate the menu cut/copy/paste commands to the right
 * component.  An instance of this class is listening and
 * when the user fires one of these commands, it calls the
 * appropriate action on the currently focused component.
 */
public class TransferActionListener implements ActionListener,
                                              PropertyChangeListener {
    private JComponent focusOwner = null;
    JComponent widget;
    
    public TransferActionListener(JComponent widget) {
        KeyboardFocusManager manager = KeyboardFocusManager.
           getCurrentKeyboardFocusManager();
        manager.addPropertyChangeListener("permanentFocusOwner", this);
        this.widget = widget;
    }
    
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getNewValue();
        if (o instanceof JComponent) {
            focusOwner = (JComponent)o;
        } else {
            focusOwner = null;
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        JComponent actionComponent = focusOwner;
        if (actionComponent == null)
            actionComponent = widget;
        if (actionComponent == null)
            return;
        String action = (String)e.getActionCommand();
        Action a = actionComponent.getActionMap().get(action);
        if (a==null) {
            actionComponent = widget;
            a = actionComponent.getActionMap().get(action);
        }
        if (a != null) {
            a.actionPerformed(new ActionEvent(actionComponent,
                                              ActionEvent.ACTION_PERFORMED,
                                              null));
        }
    }
}
