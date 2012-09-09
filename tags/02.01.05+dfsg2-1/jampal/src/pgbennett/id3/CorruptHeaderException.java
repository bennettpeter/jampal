package pgbennett.id3;

/*
 * Copyright (C) 2001,2002 Jonathan Hilliker
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
 * This exception is thrown when a corrupt mpeg or Xing header is encountered.
 * <br/>
 * <dl>
 * <dt><b>Version History:</b></dt>
 * <dt>1.2.1 - <small>2002.1015 by gruni</small></dt>
 * <dd>cleaned Sourcecode and Javadoc</dd>
 *
 * <dt>1.2 - <small>2002/03/18 by helliker</small></dt>
 * <dd>Inherits from ID3Exception now</dd>
 *
 * <dt>1.1 - <small>2002/01/21 helliker</small></dt>
 * <dd>Initial version.</dd>
 * </dl>
 *
 * @author Jonathan Hilliker
 * @version 1.2.1
 */
    
public class CorruptHeaderException extends ID3Exception {
  
  /**
   * Create a CorruptHeaderException with a default message
   *
   */
  public CorruptHeaderException() {
    super("Header is corrupt");
  }
  
  /**
   * Create a CorruptHeaderException with a specified message
   *
   * @param msg the message for this exception
   */
  public CorruptHeaderException(String msg) {
    super(msg);
  }
  
}// CorruptHeaderException
