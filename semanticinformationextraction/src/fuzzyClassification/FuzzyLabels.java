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
package fuzzyClassification;

import hibernate.DatabaseFacade;
import hibernate.entities.DegreeOfMembership;

/**
 * Enum Type, which specifies 5 Fuzzylabels,
 * and initialize them once per loading on database.
 * Map the databaseentries to the GATE-label 
 * @author lgredel
 *
 */
public enum FuzzyLabels {
  
  //fieldName ("gate-label","db-label")
  NOAMOUNT ("NoAmount","no amount"),
  SMALLAMOUNT ("ASmallAmount","small amount"),
  MODERATEAMOUNT ("AModerateAmount","moderate amount"),
  LARGEAMOUNT ("ALargeAmount","large amount"),
  MAXIMUMAMOUNT ("AMaximumAmount","maximum amount"),
  NOTDEFINED ("","n/a");

  private final String gateLabelName;
  private final String databaseLabel;
  private DegreeOfMembership doM = null;
  
  /**
   * Constructor wich initializes each entry from database.
   *   
   * @param gateLabelName
   * @param databaseLabel
   */
  FuzzyLabels(String gateLabelName,String databaseLabel){ 
    this.databaseLabel = databaseLabel;
    this.gateLabelName = gateLabelName;
    doM = DatabaseFacade.getSingletonFacade().loadDegreeOfMemberShip(databaseLabel);
  }

  public String getGateLabelName() {
    return gateLabelName;
  }

  public String getDatabaseLabel() {
    return databaseLabel;
  }

  public DegreeOfMembership getDoM() {
    return doM;
  }
}
