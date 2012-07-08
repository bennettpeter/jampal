package pgbennett.id3;

/*
 *  Copyright (C) 2004 Peter Bennett
 *
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
/**
 * This class holds the information found in an id3v2 APIC frame.  <br />
 *
 * <dl>
 *   <dt> <b>Version History</b> </dt>
 *   <dt> 1.3.1 - <small>2004/2/27 by pgbennett</small></dt>
 *   <dd> Created this class to hold APIC data</dd>
 * </dl>
 *
 *
 *@author    Peter Bennett
 *@version   1.3.1
 */

public class ID3v2Picture {
    public String mimeType;
    public byte pictureType;
    public String Description;
    public byte[] pictureData;
}