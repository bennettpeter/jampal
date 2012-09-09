package pgbennett.id3;

/*
  Copyright (C) 2002 Jonathan Hilliker & Andreas Grunewald
  Copyright (C) 2001 Jonathan Hilliker<br/>
 
    This file is part of the jd3lib library.

    This copy of jd3lib has been incorporated into Jampal under the 
    GNU general Public License.

    Modifications to the file Copyright 2004 Peter Bennett.

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
/**
 * This <code>FileFilter</code> only accepts files with a ".mp3" extension.
 *<br/>
 * <dl>
 * <dt><b>Version History:</b></dt> 
 * <dt>2.0 - <small>by gruni</small></dt>
 * <dd>-Slight optimization of getDescription()</dd>
 * <dd>-Fixed Bug #562622</dd>
 * <dt>2.0beta1 <small>by gruni</small></dt>
 * <dd>-now developed by Andreas Grunewald
 * <dd>-Fully rewritten Code</dd>
 * <dd>-extends <code>javax.swing.filechooser.FileFilter</code></dd>
 * <dt>1.2 - <small>2001.1019 by helliker</small></dd>
 * <dd>-All set for release.</dd>
 * </dl> 
 * @author <a href="mailto:gruni@users.sourceforge.net">Andreas Grunewald</a>
 * @version 2.0
 */

public class MP3FileFilter extends javax.swing.filechooser.FileFilter
                           implements java.io.FileFilter {

  /**allows Directories
   */ 
  private boolean allowDirectories;

  /**
   * Create a default MP3FileFilter.  The allowDirectories field will 
   * default to false.
   *
   */
  public MP3FileFilter () {
    this(false);
  }

  /**
   * Create an MP3FileFilter.  If allowDirectories is true, then this filter
   * will accept directories as well as mp3 files.  If it is false then
   * only mp3 files will be accepted.
   *
   * @param allowDirectories whether or not to accept directories
   */
  public MP3FileFilter (boolean allowDirectories) {
    this.allowDirectories = allowDirectories;
  }

  /**
   * Determines whether or not the file is an mp3 file.  If the file is 
   * a directory, whether or not is accepted depends upon the 
   * allowDirectories flag passed to the constructor.
   *
   * @param file the file to test
   * @return true if this file or directory should be accepted
  */
  public boolean accept(java.io.File file) {
    return ((file.getName()).toLowerCase().endsWith(".mp3"))
            || (file.isDirectory() && (this.allowDirectories == true));
  }
  
  /**
   * Returns the Name of the Filter for use in the Chooser Dialog
   * 
   * @return The Description of the Filter
   */
  public String getDescription() {
    return new String(".mp3 Files");
  }
}//MP3FileFilter
