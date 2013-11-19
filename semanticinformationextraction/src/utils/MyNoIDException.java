/*
 * Copyright (c) 2013, University of Hohenheim Department of Informations Systems 2
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>
 */
package utils;


/**
 * Exception if there`s no ID in the Gate-XML as Gate-Feature
 * 
 * @author lgredel
 *
 */
public class MyNoIDException extends Exception {

  public MyNoIDException() {
    super("Document contains no ID");
  }
  
  public MyNoIDException(String message) {
    super(message);
  }
}
