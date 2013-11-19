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

import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.LanguageResource;
import gate.Resource;
import gate.creole.ExecutionException;
import gate.creole.RealtimeCorpusController;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.util.persistence.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

/**
 * @author lgredel, oaltuntas
 * 
 * Singleton Class to control the GATE-INSTANCE access
 * Init GATE and load the initial configuration
 * Clear not used Annotations and Resources from Gate, otherwise occurs Memory Heap Space Problem
 * 
 */

public class GateInitSingleton {
	
	private static Logger log = Logger.getLogger(GateInitSingleton.class);
	
	// The folder where gate plugins are stored
	private File GATE_PLUGINS_HOME = new File(System.getProperty("user.dir") + File.separator + "lib" + File.separator + "plugins");
	
	private File GATE_HOME = new File(System.getProperty("user.dir"));
	private File GATE_SITE_CONFIG_FILE = new File(System.getProperty("user.dir")+ File.separator + "resources" + File.separator + "config" + File.separator + "common" + File.separator + "gate.xml");
	private File USER_SITE_CONFIG_FILE = new File(System.getProperty("user.dir")+ File.separator + "resources" + File.separator + "config" + File.separator + "user" + File.separator + "gate.xml");

	// Gate application that deletes all annotation in the document in order to free Heap Space
	private File docResetApp = GlobalParameters.getDocResetApp();
	private static CorpusController resetApplication = null;
	
	private static GateInitSingleton INSTANCE;
		
	private GateInitSingleton() {
		initGate();
	}

	/**
	 * Initialize Gate and load Configuration Files
	 * if an error orrcurs, the stop the application with exitcode -1;
	 * 
	 */
	private void initGate() 
	{
		if(GATE_HOME != null)
		{
			if(GATE_HOME.exists())
			{
				Gate.setGateHome(GATE_HOME);
			}else
			{
				log.error("Error initializing GATE_HOME");
				System.exit(-1);
			}
		}else{
			log.error("Error GateHome is NULL");			
			System.exit(-1);
		}
		
		Gate.setPluginsHome(GATE_PLUGINS_HOME);
		Gate.setSiteConfigFile(GATE_SITE_CONFIG_FILE);
		Gate.setUserConfigFile(USER_SITE_CONFIG_FILE);
		
		try {
			Gate.init();			
			log.debug("Init Gate sucessfully");

			//Benchmark.setBenchmarkingEnabled(true);
		} catch (GateException e) {
			log.error("Error init Gate: " + e.getMessage());
			log.error(e.getMessage());
		}
		
		// init learning plugin
		File learningPluginDir = null;
		try {
		  learningPluginDir = new File(GATE_PLUGINS_HOME.getAbsolutePath() + File.separator + "Learning").getCanonicalFile();
          System.out.println(learningPluginDir);
          
          try {
              Gate.getCreoleRegister().registerDirectories(learningPluginDir.toURI().toURL());
              
          } catch (MalformedURLException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          } catch (GateException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
          }
         System.out.println("...Learning Plugin loaded");
      } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
      }
		
		//init ResetApplication
    	try {
          resetApplication = loadApplication(docResetApp);
        } catch (PersistenceException e) {
          log.error("PersistenceException loading resetApplication");
          log.error(e.getMessage());
        } catch (ResourceInstantiationException e) {
          log.error("ResourceInstantiationException loading resetApplication");
          log.error(e.getMessage());
        } catch (IOException e) {
          log.error("IOException loading resetApplication");
          log.error(e.getMessage());
        }
	}

	/**
	 * Instanciate the SINGLETON-INSTANCE and initilize, otherwise return the existing one
	 * @return GateInitSingleton Instance
	 */
	public static synchronized GateInitSingleton getInstance() {

		if (INSTANCE == null) {
			INSTANCE = new GateInitSingleton();
		}

		return INSTANCE;
	}
	
	/**
	 * Load GATE processing pipeline.
	 * 
	 * @param gateApp - the file where the pipeline is stored
	 * @return
	 * @throws PersistenceException
	 * @throws ResourceInstantiationException
	 * @throws IOException
	 */
	public CorpusController loadApplication(File gateApp) throws PersistenceException, ResourceInstantiationException, IOException 
	{
	  CorpusController application = null;
		
		long appInitStart = new Date().getTime();
		log.debug("Starting load Application on: " + appInitStart);

		application = (CorpusController)PersistenceManager.loadObjectFromFile(gateApp);
				
		long appInitEnd = new Date().getTime();
		log.info("--------------------------------------------------------");
		log.info("Application init time: " + (appInitEnd - appInitStart));
		log.info("--------------------------------------------------------");

		return application;
	}

	public File getGATE_PLUGINS_HOME() {
		return GATE_PLUGINS_HOME;
	}

	public void setGATE_PLUGINS_HOME(File gATE_PLUGINS_HOME) {
		GATE_PLUGINS_HOME = gATE_PLUGINS_HOME;
	}
	
	 /**
	 * Executes the reset application on document in order to delete 
	 * all annotation in the document and to free the Heap Space
	 * @param doc: document to be processed
	 */ 
	public static void executeResetApplication(Document doc) {
	  
	  log.trace("Starting resetApplication");
	    Corpus corpus = null;
      try {
        corpus = Factory.newCorpus("Corpus");
        if(doc != null){
          corpus.add(doc);
        
        resetApplication.setCorpus(corpus);
        resetApplication.execute();
        resetApplication.cleanup();
        resetApplication.setCorpus(null);
        }
      } catch (ResourceInstantiationException e) {
        log.error("Cannot create Corpus for executing ResetApplication");
        log.error(e.getMessage());
      } catch (ExecutionException e) {
        log.error("Error executing ResetApplication");
        log.error(e.getMessage());
      } 
	  
      unloadGateResources(doc,corpus);
	}

	 /**
	 * Unload the corpus instance
	 * @param corpus: the corpus to be deleted
	 */
  public static void unloadGateResources(Corpus corpus) {
    if(corpus != null)
    {
        log.trace("Corpus ressources " +corpus.getName() + " delelted ");
        Factory.deleteResource(corpus);
        corpus=null;
    }
  }
  
  /**
   * Unload the document instance
    * @param doc: the document to be deleted
   */
  public static void unloadGateResources(Document doc) {
    if(doc != null){
      log.trace("ClassifiedDocument ressources " +doc.getName()+" delelted ");
      Factory.deleteResource(doc);
      doc=null;
    }
  }

  /**
   * Unload the corpus and document instances
   * @param doc: document to be deleted
   * @param corpus: corpus to be deleted
   */
  public static void unloadGateResources(Document doc, Corpus corpus) {
    
    if(corpus != null && doc != null)
    {
      log.trace("Unload GateDocument " +doc.getName());
      corpus.unloadDocument(doc); 
    }
    
    if(doc != null)
    {
       log.trace("ClassifiedDocument ressources " +doc.getName()+" delelted ");
        Factory.deleteResource(doc);
        doc=null;
    }
    
    if(corpus != null)
    {
        log.trace("Corpus ressources " +corpus.getName() + " delelted ");
        Factory.deleteResource(corpus);
        corpus=null;
    }
  }
  
  /**
   * Unload the gate processing pipeline
    * @param application: pipeline to be deleted
   */
  public void deleteGateApplications(CorpusController application) {
      Factory.deleteResource(application);
      Factory.deleteResource(resetApplication);
  }

  /**
   * Unload all active gate reources
   */
  public static void unloadGateResources() {
    log.trace("Start to unload all GateResources");
    
    List<LanguageResource> appList;
    appList = Gate.getCreoleRegister().getLrInstances();
    Iterator<LanguageResource> appIter = appList.iterator();
    
    while(appIter.hasNext()) {
      Resource currentResource = (Resource) appIter.next();
      log.trace("Delete resource: " + currentResource.getName());
      Factory.deleteResource(currentResource);
    }
  }

  public CorpusController getResetApplication() {
    return resetApplication;
  }

  public void setResetApplication(CorpusController resetApplication) {
    this.resetApplication = resetApplication;
  }
}
