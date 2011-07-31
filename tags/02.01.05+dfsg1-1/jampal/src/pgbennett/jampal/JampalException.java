package pgbennett.jampal;
import java.io.*;
import java.lang.*;
import java.util.*;

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


public class JampalException extends Exception
{

	public JampalException(String m, Throwable e) {
	    super(m,e);
	}

	public JampalException(String m) {
	    super(m);
	}

	public JampalException(Throwable e) {
	    super(e);
	}

}
