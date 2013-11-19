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

import gate.Annotation;
import gate.AnnotationSet;
import gate.CorpusController;
import gate.Document;
import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassifierType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.apache.log4j.Logger;

import utils.CifsNetworkConfig;
import utils.GateInitSingleton;
import utils.GlobalParameters;
import classification.ClassifiedDocument;
import classification.SentimentClassification;
import classification.SentimentDocument;


/**
 * Super class, which contains the common elements of all different
 * document processe.
 * 
 * @author lgredel
 *
 */
public abstract class WorkerThread extends Thread{
  
  protected static Logger log = Logger.getLogger(WorkerThread.class);
  protected static Properties configProperties = GlobalParameters.loadConfigFile();
  protected CorpusController resetApplication = null;
  protected DatabaseFacade databaseConn = DatabaseFacade.getSingletonFacade();
  private GateInitSingleton gate = GateInitSingleton.getInstance();
  protected static SimpleDateFormat sdf = GlobalParameters.getFormat();
  protected static ArrayList<CorpusController> preprocessingApplications = new ArrayList<CorpusController>();
  protected static ArrayList<CorpusController> classificationApplications = new ArrayList<CorpusController>();
  protected static ArrayList<Thread> currenThreadList = new ArrayList<Thread>();
  
  protected MessageFileQueue queue = null;
  protected CifsNetworkConfig networkConfig = null;
  
  public WorkerThread() throws FileNotFoundException {
    super();
    
    networkConfig = CifsNetworkConfig.getINSTANCE();
    try {
      queue = MessageFileQueue.getInstance();
    } catch (FileNotFoundException e1) {
      log.error("Cannot initialize queue on Workerthreadconstructor");
      throw e1;
    }
    
    try {
      resetApplication = gate.getResetApplication();
    } catch (Exception e) {
      log.error(e.getMessage());
    }  
  }

  /**
   * Set the name of the Thread
   * @param workerThreadName
   * @throws FileNotFoundException 
   */
  public WorkerThread(String workerThreadName) throws FileNotFoundException {    
    this();
    log.trace("WorkerThread Constructor setting name of thread to: " + workerThreadName);
    this.setName(workerThreadName);
  }
  
  public static ArrayList<Thread> getCurrenThreadList() {
    return currenThreadList;
  }

  public static void setCurrenThreadList(ArrayList<Thread> currenThreadList) {
    WorkerThread.currenThreadList = currenThreadList;
  }
  
  /**
   * Calculate the Pos/neg Word ratio Sentiment on the current Document
   * 
   * @param gateDoc - Input Gate-Document where to calculate the Sentiment
   * @param dbDocument - DocumentMetaData Object where to save in database
   * @param classification 
   * @return
   * @throws Exception
   */
  protected void calculatePosNegWordRatioSentiment(Document gateDoc, DocumentMetaData dbDocument, SentimentClassification classification) throws Exception{
    log.debug("Starting to calculate Pos/Neg WordRatio Sentiment on gateDoc: " + gateDoc.getName() + " and documentMetadata: " + dbDocument.getId());
    int positive = 0;
    int negative = 0;
    
    SentimentClassifierType classifierType = DatabaseFacade.getSingletonFacade().loadSentimentClassifierType("PosNegWordRatio", "CRISP");
    SentimentDocument currentDocLevel = new SentimentDocument(dbDocument,classifierType);   
    
    AnnotationSet orientationTermAnnotations = gateDoc.getAnnotations().get("OrientationTerm");
    
    Iterator<Annotation> annotIter = orientationTermAnnotations.iterator();
    
    while(annotIter.hasNext()){
      Annotation ann = (Annotation) annotIter.next();
      String label = ann.getFeatures().get("class").toString().toLowerCase();
      if(label.contains("positive")){
          positive++;
          
      }else if (label.contains("negative")){
          negative++;
      }
    }
        
    log.info("Extracted " + positive + " and " + negative + "  Orientationterms on document with databaseID: " + dbDocument.getId());
    
    double score = (((double) positive - (double) negative) / ((double) positive + (double) negative));
    log.debug("Calculated score: " + score);
    
    /*double value could be NAN after Division by Zero, or it contains the Value of INFINITY*/
    if(!Double.isNaN(score)){
      // Setting for Result-XML
      currentDocLevel.setSentimentScoreDocument((float) score);
  
      String sentimentPolarity = null;
      if (score > (double) 0.0) {
        sentimentPolarity = "positive";
      } else {
        sentimentPolarity = "negative";
      }
      currentDocLevel.setSentimentPolarity(sentimentPolarity);
   
      Sentiment currentDocSentiment = currentDocLevel.getDbsentiment();
      BigDecimal bdScore = new BigDecimal(score);
      bdScore = bdScore.setScale(16,BigDecimal.ROUND_HALF_EVEN);
      currentDocSentiment.setScore(bdScore);
           
      //currentDocSentiment.setSentimentClassifierType(classifierType);
      
      currentDocLevel.setDbsentiment(currentDocSentiment);
      
      String fileName = classification.extractIDFromGateFeature(gateDoc);
      
      File serializedResult = new File(GlobalParameters.getShareDirectory(), fileName + ".result.xml");      
      log.info("Starting marshalling Result of PosNegWordRatio  to: " + serializedResult.getAbsolutePath());
      
      ClassifiedDocument posNegWordAggregation = new ClassifiedDocument(dbDocument);   
      
      ArrayList<SentimentDocument> sentDocList = new ArrayList<SentimentDocument>();
      sentDocList.add(currentDocLevel);
      posNegWordAggregation.setSentimentDocuments(sentDocList);
      posNegWordAggregation.setAuthor(dbDocument.getAuthor());
      
      Date pubDate = dbDocument.getPublicationDate();
      if(pubDate != null){
        posNegWordAggregation.setPublicationDate(sdf.format(pubDate));
        log.trace("Set Publicationdate on ClassifiedDocument for JAXB-Marshalling to: " + sdf.format(pubDate));
      }else{
        log.trace("Publicationdate on docMetaObject for ClassifiedDocument with id: " + dbDocument.getId() + " is null");
      }
      
      String title = dbDocument.getTitle();
      if(title != null){
        posNegWordAggregation.setTitle(title);
        log.trace("Set Title on ClassifiedDocument for JAXB-Marshalling to: " + title);
      }else{
        log.trace("Title on docMetaObject for ClassifiedDocument with id: " + dbDocument.getId() + " is null");
      }
      
      String url = dbDocument.getUrl();
      
      if(url != null){
        posNegWordAggregation.setUrl(url);
        log.trace("Set URL on ClassifiedDocument for JAXB-Marshalling to: " + url);
      }else{
        log.trace("URL on docMetaObject for ClassifiedDocument with id: " + dbDocument.getId() + " is null");
      }
         
      boolean writeResultXML = Boolean.parseBoolean(configProperties.get("writeResultXML").toString());       
      if(writeResultXML){
        
        fileName = classification.extractIDFromGateFeature(gateDoc);
        
        File classificationResultXML = new File(GlobalParameters.getShareDirectory(), fileName + ".result.xml");
        log.info("Starting marshalling Result of Classification to: " + classificationResultXML.getAbsolutePath());
        ClassifiedDocument result = classification.getClassifiedDocument();
        JAXB.marshal(result, classificationResultXML);
        
        boolean zipResultFile = Boolean.parseBoolean(configProperties.get("zipFile").toString());
        if(zipResultFile){        
          GlobalParameters.zipFile(classificationResultXML);
          
          GlobalParameters.deleteFile(classificationResultXML);
        }
      }
      
      try {
        DatabaseFacade.getSingletonFacade().createDocumentVersion("result",serializedResult, dbDocument);
      } catch (MalformedURLException e) {
        log.error("Cannot createDocumentVersion on Resultfile: " + serializedResult.getAbsolutePath() + " and databaseObject: " + dbDocument.getId());
        log.error(e.getMessage());
        throw e;
      } catch (IOException e) {
        log.error("IOError on creating DocumentVersion on file: " + serializedResult.getAbsolutePath() + " and databaseObject: " + dbDocument.getId());
        log.error(e.getMessage());
        throw e;
      }
      
      databaseConn.updateObjectToDatabase(currentDocSentiment);
      log.info("Update sentiment on docLevel: " + score);
    }else{
      log.error("Calculated score is NaN");
    }
  }
}
