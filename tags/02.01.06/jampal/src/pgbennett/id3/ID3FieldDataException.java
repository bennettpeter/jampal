package pgbennett.id3;

/*
 * Copyright (C) 2001 Jonathan Hilliker
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
 *  An exception that is thrown when invalid data is set in an ID3 tag.<br/>
 * <dl>
 * <dt><b>Version History:</b></dt>
 * <dt>1.3.1 - <small>2002.1023 by gruni</small></dt>
 * <dd>-Made sourcecode compliant to the Sun Coding Conventions</dd>
 * <dt>1.3 - <small>2002.0318 by helliker</small></dt>
 * <dd>-Inherits from ID3Exception now.</dd>
 *
 * <dt>1.2 - <small>2001.1019 by helliker</small></dt>
 * <dd>-All set for release.</dd>
 * </dl>
 * @author  Jonathan Hilliker
 * @version 1.3.1
 */

public class ID3FieldDataException extends ID3Exception {
  
  /**
   * Create an ID3FieldDataException with a default message
   *
   */
  public ID3FieldDataException() {
    super("Invalid data supplied to ID3 tag.");
  }
  
  /**
   * Create an ID3FieldDataException with the specified message
   *
   * @param msg a String specifying the specific problem encountered
   */
  public ID3FieldDataException(String msg) {
    super(msg);
  }
  
} // ID3FieldDataException
