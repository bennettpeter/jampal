package pgbennett.id3;

import java.io.IOException;

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
import java.io.UnsupportedEncodingException;
/**
 *  A common interface for ID3Tag objects so they can easily communicate with
 *  each other.<br/><dl>
 * <dt><b>Version History:</b></dt>
 * <dt>1.2.1 - <small>2002.1023 by gruni</small></dt>
 * <dd>-Made sourcecode compliant to the Sun Coding Conventions</dd>
 * <dt>1.2 - <small>2002.0127 by  helliker</small></dt>
 * <dd>-Added getBytes method.</dd>
 *
 * <dt>1.1 - <small>2002/01/13 by helliker</small></dt>
 * <dd>-Initial version</dd>
 * </dl>
 * @author  Jonathan Hilliker
 * @version 1.2.1
 */

public interface ID3Tag {
    
    /**
     * Copies information from the ID3Tag parameter and inserts it into
     * this tag.  Previous data will be overwritten.
     *
     * @param tag the tag to copy from
     */
    public void copyFrom(ID3Tag tag);

    /**
     * Saves all data in this tag to the file it is bound to.
     *
     * @exception IOException if an error occurs
     */
    public void writeTag() throws IOException;

    /**
     * Removes this tag from the file it is bound to.
     *
     * @exception IOException if an error occurs
     */
    public void removeTag() throws IOException;
    

    /**
     * Returns a binary representation of the tag as it would appear in
     * a file.
     *
     * @return a binary representation of the tag
     */
    public byte[] getBytes ()throws UnsupportedEncodingException;

} // ID3Tag
