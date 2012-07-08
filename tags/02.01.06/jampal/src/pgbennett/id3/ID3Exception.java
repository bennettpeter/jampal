package pgbennett.id3;

/*
 * Copyright (C) 2002 Jonathan Hilliker
 *
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
 *  A top-level exception for the library.
 * <br/ >
 * <dl>
 * <dt><b>Version History:</b></dt>
 * <dt>1.1.1 - <small>2002.1015 by gruni</small></dt>
 * <dd>cleaned Sourcecode and Javadoc</dd> 
 * <dt>1.1 - <small>2002.0318 by helliker</small></dt>
 * <dd>Initial version</dd>
 * </dl>
 *
 * @author  Jonathan Hilliker
 * @version 1.1.1
 */

public class ID3Exception extends Exception {
  
  /**
   * Creates an ID3Exception with a default message
   *
   */
  public ID3Exception() {
    super("ID3Exception");
  }
  
  /**
   * Creates an ID3Exception with a specified message
   *
   * @param msg the message for this exception
   */
  public ID3Exception(String msg) {
    super(msg);
  }

}// ID3Exception
