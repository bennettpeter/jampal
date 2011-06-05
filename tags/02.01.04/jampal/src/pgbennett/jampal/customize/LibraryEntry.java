/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pgbennett.jampal.customize;

/**
 *
 * @author peter
 */
class LibraryEntry {

    String type;
    String frame;
    String language;
    String description;
    String title;
    DisplayEntry displayEntry;
    // Note that the displaySequence is only filled
    // in during save.
    int displaySequence;
}
