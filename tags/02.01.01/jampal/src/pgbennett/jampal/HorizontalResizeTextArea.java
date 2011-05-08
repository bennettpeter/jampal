/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
