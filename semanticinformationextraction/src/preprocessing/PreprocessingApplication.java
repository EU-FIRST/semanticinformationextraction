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
package preprocessing;

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import java.util.Date;

import utils.GateApplication;
import utils.GateInitSingleton;

/**
 * Utility subclass relating to preprocessing execution
 * 
 * @author lgredel
 *
 */
public class PreprocessingApplication extends GateApplication {

  public PreprocessingApplication(CorpusController gateApp) {
    super();
    application = gateApp;
  }


  /**
   * Executes the Gate-Preprocessing Application
   * 
   * @param doc - the Document to process
   * @return the preprocessed Gate-Document
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public Document executeDocument(Document doc) throws Exception {
    log.info("--> Starting preprocessing on Document");

    Corpus corpus = null;

    try {
  
      corpus = Factory.newCorpus("Preprocessing_Corpus");
      corpus.add(doc);

      application.setCorpus(corpus);

      long appRunStart = 0;
      long appRunEnd = 0;

      appRunStart = new Date().getTime();

      application.execute();
      
      log.info("Application :'" + application.getName() + "' executed on Corpus: '" + corpus.getName()
          + "'");
      appRunEnd = new Date().getTime();
      long preprocessingRunTime = appRunEnd - appRunStart;
      log.info("--------------------------------------------------------");
      log.info("Application run time: " + preprocessingRunTime);
      log.info("on Gatedocument: " + doc.getName());
      log.info("--------------------------------------------------------");
      
      timeMeasurement.setPreprocessingRunTime(preprocessingRunTime);

      /*Put a feature to the document, which contains the name of the current PreprocessingApplication*/
      String gateApplicationName = application.getName();
      
      doc.getFeatures().put("PreprocessedGapName", gateApplicationName);

    }catch (ResourceInstantiationException e) {
      GateInitSingleton.executeResetApplication(doc);
      GateInitSingleton.unloadGateResources(doc,corpus);
      log.error("Cannot create Gate-Resource");
      throw e;
    } catch (ExecutionException e) {
      log.error(e.getClass().getName() + " occured on executing preprocessing");
      log.error(e.getMessage());
      GateInitSingleton.executeResetApplication(doc);
      GateInitSingleton.unloadGateResources(doc,corpus);     
      throw e;
    } catch (Exception e) {
      log.error("Exception with class: " + e.getClass() + " on executing document: " + doc.getName()
          + " on Application: " + application.getName());
      GateInitSingleton.executeResetApplication(doc);
      GateInitSingleton.unloadGateResources(doc,corpus);
      
      if (e.getMessage() != null) {
        log.error(e.getMessage());
      } else {
        log.error("Printing stacktrace of exception without message");
        e.printStackTrace();
      }
      GateInitSingleton.executeResetApplication(doc);
      GateInitSingleton.unloadGateResources(doc,corpus);
      log.error(e.getMessage());
      throw e;
    }
    
    GateInitSingleton.unloadGateResources(null, corpus);
    
    return doc;
  }
}
