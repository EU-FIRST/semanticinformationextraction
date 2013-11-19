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
package zeroMQ.messageQueue;

import gate.AnnotationSet;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.creole.ExecutionException;
import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import javax.xml.bind.JAXB;

import org.first.messaging.Messenger;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import performance.PerformanceMeasurement;
import preprocessing.PreprocessingApplication;
import utils.GateInitSingleton;
import utils.GlobalParameters;
import zeroMQ.receiver.CompletePipelineReceiver;
import classification.ClassifiedDocument;
import classification.SentimentClassification;

/**
 * Thread Subclass, which read a Document from queue, and start complete gate-processing 
 * (all steps successively -> Preprocessing - Classification - FUZZY-Classificaton - MachineLearning)
 * If configured save the generated output and input files per network/SMB protocol
 * 
 * Save data using Databasefacade to database
 * 
 * @author lgredel
 *
 */

public class CompletePipelineWorker extends WorkerThread {

  private PreprocessingApplication preprocessing = null;
  private SentimentClassification classification = null;

  public Session hibernateSession;
  private Transaction tx = null;
  
  public CompletePipelineWorker(CorpusController preprocessingGateApp, CorpusController classificationGateApp, String workerThreadName) throws FileNotFoundException {

    super(workerThreadName);
    this.preprocessing = new PreprocessingApplication(preprocessingGateApp);
    this.classification = new SentimentClassification(classificationGateApp);
  }


  /**
   * Thread run method
   * read Gate-Document from Queue and process each document
   * 
   */
  @Override
  public void run() {
    DocumentMetaData dbDocument = null;

    boolean loop = true;

    PerformanceMeasurement timeMeasurement = null;
    try {
      timeMeasurement = new PerformanceMeasurement(this.getName());
      this.preprocessing.setTimeMeasurement(timeMeasurement);
      this.classification.setTimeMeasurement(timeMeasurement);
    } catch (NullPointerException e1) {
      log.error(e1.getClass().getName() + " occured on initializing PerformanceMeasurement");
      log.error(e1.getMessage());
      log.error("System exit - Nullpointer on initializing PerformanceMeasurement");
      System.exit(MIN_PRIORITY);
    } catch (IOException e1) {
      log.error(e1.getClass().getName() + " occured on initializing PerformanceMeasurement");
      log.error(e1.getMessage());
      log.error("System exit - IOException on initializing PerformanceMeasurement");
      System.exit(MIN_PRIORITY);
    }

    while (loop) {
      String currentMessageString = null;
      Document gateDoc = null;
      Long docID = null;
      
      try {
        currentMessageString = queue.popMessage();
       
        log.info("*******************************************************************************************");
        log.info("Thread " + this.getName() + " with ID: " + this.getId() + " startet executing preprocessing");
        timeMeasurement.startTimeMeasurementLoop();
       
        hibernateSession = databaseConn.openSession();
        hibernateSession.setFlushMode(FlushMode.COMMIT);
        int level = hibernateSession.connection().getTransactionIsolation();
        log.trace("TransactionIsolationLevel: " + level);
        hibernateSession.connection().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        databaseConn.setHibernateSession(hibernateSession);
                
        long startLoadDocument = System.currentTimeMillis();

        log.debug("Starting load Gate-Document for preprocessing on: " + startLoadDocument);
        
        gateDoc = Factory.newDocument(currentMessageString);        
        
        long endLoadDocument = System.currentTimeMillis();
        log.debug("End load Gate-Document for preprocessing on: " + endLoadDocument);
        long loadGateDocumentDuration = endLoadDocument - startLoadDocument;
        log.debug("Loading Gate-Document needs : " + loadGateDocumentDuration + " ms ");

        dbDocument = classification.extractDocumentMetaData(gateDoc);
        
        docID =  dbDocument.getId();
        
        Date publicationDate = dbDocument.getPublicationDate();
        
        boolean writeInputFile = Boolean.parseBoolean(configProperties.get("writeInputFile").toString());
        if (writeInputFile) {
          boolean zipFile = Boolean.parseBoolean(configProperties.get("zipFile").toString());

          String suffix = ".gate-xml.xml";
          
          if (zipFile) {
            String fileName = preprocessing.extractIDFromGateFeature(gateDoc);

            File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
            File outputZipFile = new File(outputDir, fileName
                + suffix);
            outputZipFile = GlobalParameters.zipStringToFile(gateDoc.toXml(), outputZipFile);
            networkConfig.copyFile(outputZipFile,true);
            
            DatabaseFacade.getSingletonFacade().createDocumentVersion("gate-xml", outputZipFile, dbDocument);
          } else {
            
            File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
            File outputFile = preprocessing.writeGateDocumentXML(gateDoc, suffix, outputDir);
            networkConfig.copyFile(outputFile,true);
            
            DatabaseFacade.getSingletonFacade().createDocumentVersion("gate-xml", outputFile, dbDocument);
          }
        }
            
        gateDoc = preprocessing.executeDocument(gateDoc);
        
        String preprocessedDocumentStr = gateDoc.toXml();

        boolean writepreprocessingOutputFile = Boolean.parseBoolean(configProperties.get("writeOutputFile")
            .toString());
        if (writepreprocessingOutputFile) {
          boolean zipFile = Boolean.parseBoolean(configProperties.get("zipFile").toString());

          if (zipFile) {
            String fileName = preprocessing.extractIDFromGateFeature(gateDoc);

            File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
            File outputZipFile = new File(outputDir, fileName
                + ".preprocessed.xml");
            outputZipFile = GlobalParameters.zipStringToFile(preprocessedDocumentStr, outputZipFile);
            networkConfig.copyFile(outputZipFile,true);
            DatabaseFacade.getSingletonFacade().createDocumentVersion("preprocessed", outputZipFile, dbDocument);
          } else {
            String suffix = ".preprocessed.xml";
            File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
            File outputFile = preprocessing.writeGateDocumentXML(gateDoc, suffix, outputDir);
            networkConfig.copyFile(outputFile,true);
            DatabaseFacade.getSingletonFacade().createDocumentVersion("preprocessed", outputFile, dbDocument);
          }
        }
        
        AnnotationSet docAnnotSet = gateDoc.getAnnotations();
        Set<String> annotationTypes = docAnnotSet.getAllTypes();
        
        if (!annotationTypes.contains("SO")){
          /**
           * No Sentimentobject 
           * -> POS/NEG ORIENTATION TERM WORD COUNT AGGREGATION
           */   
          log.info("Input Document contains no SO -> starting with PosNegWordRatio Sentiment on database DocumentMetaData-object: " + docID);
          DatabaseFacade.getSingletonFacade().deleteSentimentsWithClassifierType(dbDocument,"PosNegWordRatio","CRISP");
          
          try{   
            this.calculatePosNegWordRatioSentiment(gateDoc, dbDocument,classification);
                        
          }catch (HibernateException hibex) {
            log.error("Hibernateexception on calculating Pos/Neg Word-Ratio Sentiment on document: " + gateDoc.getName() + " in databaseObject with ID: " + docID);
            log.error(hibex.getMessage());
            GateInitSingleton.executeResetApplication(gateDoc);
          }
        }else{
          /*
           * Sentimentobject extracted
           * KnowledgebasedCrisp Sentimentextraction
           */
          log.info("Input Document contains SO -> starting with Knowledgebased CRISP Sentimentclassification on database DocumentMetaData-object: " + docID);
                   
          gateDoc = classification.executeDocument(gateDoc,dbDocument);
                    
          boolean writeOutputFile = Boolean.parseBoolean(configProperties.get("writeOutputFile").toString());
          if(writeOutputFile){
            boolean zipFile = Boolean.parseBoolean(configProperties.get("zipFile").toString());
            
            if(zipFile){
              String currentDocString = gateDoc.toXml();
              String fileName = classification.extractIDFromGateFeature(gateDoc);
              
              File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
              File outputZipFile = new File(outputDir, fileName
                  + ".classified.xml");
              outputZipFile = GlobalParameters.zipStringToFile(currentDocString, outputZipFile);
              networkConfig.copyFile(outputZipFile,true);
              DatabaseFacade.getSingletonFacade().createDocumentVersion("classified", outputZipFile, dbDocument);
            } else {
              String suffix = ".classified.xml";
              File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
              File outputFile = classification.writeGateDocumentXML(gateDoc, suffix, outputDir);
              networkConfig.copyFile(outputFile,true);
              DatabaseFacade.getSingletonFacade().createDocumentVersion("classified", outputFile, dbDocument);
            }    
                      
          boolean writeResultXML = Boolean.parseBoolean(configProperties.get("writeResultXML").toString());       
          if(writeResultXML){          
            boolean zipResultFile = Boolean.parseBoolean(configProperties.get("zipFile").toString());

            String fileName = classification.extractIDFromGateFeature(gateDoc);
            File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
            File classificationResultXML = new File(outputDir, fileName + ".result.xml");
            log.info("Starting marshalling Result of Classification to: " + classificationResultXML.getAbsolutePath());
            ClassifiedDocument result = classification.getClassifiedDocument();
            JAXB.marshal(result, classificationResultXML);
            
            if(zipResultFile){                             
              GlobalParameters.zipFile(classificationResultXML);
              
              classificationResultXML = new File(classificationResultXML.getAbsolutePath() + ".zip");
              networkConfig.copyFile(classificationResultXML,true);
              DatabaseFacade.getSingletonFacade().createDocumentVersion("result", classificationResultXML, dbDocument);
            }
          }
        }
        }
                
        //Write txt-File with DocumentContent
        String txtFileName = classification.extractIDFromGateFeature(gateDoc);
        File outputDir = GlobalParameters.createTmpDirectoriesFromDate(publicationDate);
        File txtContentFile = new File(outputDir, txtFileName + ".txt");
        
        String gateDocContent = gateDoc.getContent().toString();
        GlobalParameters.zipStringToFile(gateDocContent, txtContentFile);
        txtContentFile = new File(txtContentFile.getAbsolutePath() + ".zip");
        networkConfig.copyFile(txtContentFile,true);
        
        DatabaseFacade.getSingletonFacade().createDocumentVersion("txt", txtContentFile,dbDocument);
        
        databaseConn.startTransaction();
        tx = databaseConn.getTx();
        databaseConn.saveOrUpdateObjectToDatabase(dbDocument);
        log.trace("Starting commit");
        long start = System.currentTimeMillis();
        tx.commit(); 
        long end = System.currentTimeMillis();
        long commitTime = end-start;
        log.trace("Commit time for one doucment: " + commitTime);
        hibernateSession.connection().setTransactionIsolation(level);
        log.info("Added and committet new Document in Database with ID: " + dbDocument.getId() + " sucessfully");
        dbDocument = null;
      } catch (NullPointerException npe) {
        log.error("NullPointerException: " + npe.getClass().getName() + " occured during processing Document");
        if (npe.getMessage() != null) {
          log.error(npe.getMessage());
        } else {
          log.error("NullPointerException without message occured");
          npe.printStackTrace();
        }
        continue;
      } catch (HibernateException hibex){
        log.error("HibernateException: " + hibex.getClass().getName() + " occured during processing Document");
        log.error(hibex.getMessage());
        log.error("Continue with next message");
        
        if (tx != null) {
          tx.rollback();
        }    
        continue;
      }catch (RuntimeException runtEx){
        log.error("RuntimeException: " + runtEx.getClass().getName() + " occured during processing Document");
        log.error(runtEx.getMessage());
        log.error("Continue with next message");
        
        if (tx != null) {
          tx.rollback();
        }
        continue;
      }catch (InterruptedException iex) {
        log.error("take message from Messageque interrupted");
        log.error(iex.getMessage());
        log.error("Continue with next message");
        continue;
      } catch (ExecutionException executionEx) {
        log.error("ExecutionException on preprocessing occured");
        log.error(executionEx.getMessage());
        continue;
      } catch (Exception e) {
        log.error("Exception " + e.getClass().getName() + " occured on preprocessing document: ");
        log.error(e.getMessage());
        log.error("Continue with next message");
        continue;
      } finally {   
        dbDocument = null;    
        clean(timeMeasurement, gateDoc);         
        log.info("Finished with current Document withID: " + docID);
      }
    }

    log.trace("End of run-Method in PreprocessingWorkerThread: " + this.getName());
  }


  /**
   * Clean and unload all gate-Ressources (from Heap -> memory leak),
   * and reset TimeMeasurement. 
   * 
   * @param timeMeasurement
   * @param gateDoc
   */
  private void clean(PerformanceMeasurement timeMeasurement, Document gateDoc) {
    DatabaseFacade.closeDBSession(hibernateSession);
    hibernateSession = null;
    tx = null;
    GateInitSingleton.executeResetApplication(gateDoc);
    GateInitSingleton.unloadGateResources(gateDoc);

    timeMeasurement.endTimeMeasurementLoop();
    timeMeasurement.printRunTimeResults();
    timeMeasurement.printThroughputResults();
    timeMeasurement.printMemoryResults();      
    timeMeasurement.printCurrentGateRunTimes();
    timeMeasurement.printFileWritingTimes();
    PerformanceMeasurement.setWriteFileTimes(new HashMap<String, Long>());
    
    System.gc();
  }

  /**
   * Initialize Workerthreads, which are polling on the queue and process each document
   * 
   * @param messenger
   * @throws Exception
   */
  public static void inizializeWorker(Messenger messenger) throws Exception {
    
    String executionPipeline = configProperties.getProperty("pipelineExecution");
    
    WorkerThread currentPipeLineWorker = null;
    CorpusController preprocessingGateApp = null;
    CorpusController preprocessingGateAppCopy = null;

    CorpusController classificationGateApp = null;
    CorpusController classificationGateAppCopy = null;
    
    Integer MAX_THREAD = null;
    String threadNo = null;
    try {
      threadNo = configProperties.get("currentThreadNo").toString();
      MAX_THREAD = new Integer(threadNo);
    } catch (NumberFormatException nfex) {
      log.error("Cannot extract Threadnumber in configurationFile to Integer" + threadNo);
      log.info("Setting default value to 1");
      MAX_THREAD = new Integer(1);
    }
    
    
    for (int i = 0; i < MAX_THREAD; i++) {
      CompletePipelineReceiver.log.info("Creating new CompletePipelineWorker for executionPipeline: "
          + executionPipeline);
      
      File preprocessingApplicationFile = null;
      File classificationApplicationFile = null;
      if (executionPipeline.equalsIgnoreCase("JSI")) {
        preprocessingApplicationFile = GlobalParameters.getJsiPreprocessingGateApp();
        classificationApplicationFile = GlobalParameters.getJsiClassificatonGateApp();  
      }else {
        if (executionPipeline.equalsIgnoreCase("UHOH")) {
          classificationApplicationFile = GlobalParameters.getUhohClassificationGateApp();
          preprocessingApplicationFile = GlobalParameters.getUhohPreprocessingGateApp();
        }
      }
      
      CorpusController currentPreprocessingGateApp = null;
      CorpusController currentClassificationGateApp = null;
      if ((preprocessingGateApp == null) && (classificationGateApp == null)) {
        preprocessingGateApp = GateInitSingleton.getInstance().loadApplication(preprocessingApplicationFile);       
        classificationGateApp = GateInitSingleton.getInstance().loadApplication(classificationApplicationFile);
        currentPreprocessingGateApp = preprocessingGateApp;
        currentClassificationGateApp = classificationGateApp;
      } else {
        preprocessingGateAppCopy = (CorpusController) Factory.duplicate(preprocessingGateApp);
        classificationGateAppCopy = (CorpusController) Factory.duplicate(classificationGateApp);
        currentPreprocessingGateApp = preprocessingGateAppCopy;
        currentClassificationGateApp = classificationGateAppCopy;
      }
      
      
      String workerThreadName = "CompletepipelineWorker_" + i;
      log.info("Creating new CompletepipelineWorker_-Thread Object with name: " + workerThreadName);

      currentPipeLineWorker = new CompletePipelineWorker(currentPreprocessingGateApp, currentClassificationGateApp, workerThreadName);
      currentPipeLineWorker.start();
    
      log.info("Startet new Thread with CompletepipelineWorkerThread");
      currenThreadList.add(currentPipeLineWorker);
    }
  }
}
