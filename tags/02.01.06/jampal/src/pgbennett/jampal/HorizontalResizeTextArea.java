/*
    Copyright 2004 Peter Bennett

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

import java.awt.Dimension;
import javax.swing.JTextArea;

/**
 *
 * @author peter
 */
public class HorizontalResizeTextArea extends JTextArea {

    int minimumWidth;

    public HorizontalResizeTextArea(String text, int rows, int columns, int minimumWidth) {
        // Bug in Linux java - if text area is not editable
        // and text zero length - it makes the text area very tall.
        super( text==null||text.length()==0 ? " " :  text,  rows,  columns);
        this.minimumWidth = minimumWidth;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension size =  super.getMinimumSize();
        size.width = minimumWidth;
        return size;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        preferredSize.width = minimumWidth;
        return preferredSize;
    }
    

}
