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

import gate.CorpusController;
import gate.Document;
import gate.FeatureMap;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import performance.PerformanceMeasurement;
import classification.SentimentClassification;

/**
 * Superclass concerning the commonness of the
 * different Applications used from GATE
 * 
 * @author lgredel
 *
 */
public abstract class GateApplication {

  protected static Logger log = Logger.getLogger(SentimentClassification.class);
  protected static Properties configProperties = GlobalParameters.loadConfigFile();

  protected static GateInitSingleton gateInit;
  protected CorpusController application = null;
  protected static CorpusController resetApplication = null;

  protected static ResourceBundle bundle = ResourceBundle.getBundle("messages");
  protected DatabaseFacade databaseConn = DatabaseFacade.getSingletonFacade();
  protected static SimpleDateFormat sdf = GlobalParameters.getFormat();
  protected static SimpleDateFormat pubDateFormat = GlobalParameters.getPubDateFormat();

  protected PerformanceMeasurement timeMeasurement = null;

  public GateApplication() {
    super();

    long gateStart = new Date().getTime();
    log.debug("Starting Gate as Singleton on: " + gateStart);
    // Initialize Gate as Singleton and log4j
    gateInit = GateInitSingleton.getInstance();

    long gateEnd = new Date().getTime();
    log.debug("End Gate as Singleton on: " + gateEnd + " initialised");

  }

  /**
   * Initialize the Gateapplication from parameter file and load all 
   * corresponding resources.
   * 
   * @param gateApp File from which 
   */
  protected void initGateApplications(File gateApp) {
    try {
      application = gateInit.loadApplication(gateApp);
      log.debug("Load GateApplication: " + gateApp.getAbsolutePath());
      resetApplication = gateInit.loadApplication(GlobalParameters.getDocResetApp());
    } catch (PersistenceException e) {
      log.error("PersistenceException loading Gatepapplication" + e.getMessage());
      e.printStackTrace();
      javax.swing.JOptionPane.showMessageDialog(null, bundle.getString("error.LoadApplicationError")
          + "\n" + bundle.getString("error.SystemExit"), bundle.getString("error.LoadApplicationError"),
          JOptionPane.ERROR_MESSAGE);
      System.exit(-1);

    } catch (ResourceInstantiationException e) {
      log.error("ResourceInstantiationException loading Gatepapplication" + e.getMessage());
      e.printStackTrace();
      javax.swing.JOptionPane.showMessageDialog(null, bundle.getString("error.LoadApplicationError")
          + "\n" + bundle.getString("error.SystemExit"), bundle.getString("error.LoadApplicationError"),
          JOptionPane.ERROR_MESSAGE);
      System.exit(-1);

    } catch (IOException e) {
      log.error("IOException loading Gatepapplication" + e.getMessage());
      e.printStackTrace();
      javax.swing.JOptionPane.showMessageDialog(null, bundle.getString("error.LoadApplicationError")
          + "\n" + bundle.getString("error.SystemExit"), bundle.getString("error.LoadApplicationError"),
          JOptionPane.ERROR_MESSAGE);
      System.exit(-1);
    }
  }

  /**
   * Writes the Gate-Document in XML format to file.
   * The filename will be generated with the id containing as Gate-Feature in the Document
   * 
   * @param doc - Gatedocument which should be written to xml
   * @param fileSuffix - File suffix for the result xml-file
   * @param writeDirectory - Directory where to write the output xml-file
   * @return Input Gate-Document as XML-File
   * @throws IOException
   */
  public File writeGateDocumentXML(Document doc, String fileSuffix, File writeDirectory)
      throws IOException {

    String fileName = extractIDFromGateFeature(doc);

    String docContent = doc.toXml();

    long startTime = 0;
    long endTime = 0;
    File outputFile = new File(writeDirectory, fileName + fileSuffix);

    FileWriter writer;
    try {
      writer = new FileWriter(outputFile);

      startTime = System.currentTimeMillis();
      writer.write(docContent);
      endTime = System.currentTimeMillis();

      writer.close();
    } catch (IOException e) {
      log.error("IOException on writing docContent to file: " + outputFile.getAbsolutePath());
      log.error(e.getMessage());
      throw e;
    }

    long runTime = endTime - startTime;
    timeMeasurement.addWriteFileTime(outputFile.getName(), runTime);

    return outputFile;
  }

  /**
   * Extract JSI_WP3_ID Gate-Feature, and use this as filename.
   * 
   * @param doc Input Gate-Document
   *          
   * @return Filename for the document
   */
  public String extractIDFromGateFeature(Document doc) {
    String fileName = null;
    FeatureMap docFeatures = doc.getFeatures();

    if (docFeatures.containsKey("JSI_WP3_ID")) {
      fileName = docFeatures.get("JSI_WP3_ID").toString();
    }
    return fileName;
  }

  /**
   * Extract the documentMetaData information from input document annotations
   * and save these information in database (publicationDate, Author, title,
   * ...)
   * 
   * @throws Exception
   * */
  public DocumentMetaData extractDocumentMetaData(Document gateDoc) throws Exception {
    log.debug("Extracting document meta data on gateDocument startet");

    DocumentMetaData dbDocument = null;
    FeatureMap docFeatures = gateDoc.getFeatures();

    try {

      if (docFeatures.containsKey("JSI_WP3_ID")) {

        dbDocument = this.extractJSIID(gateDoc, dbDocument);

        /*
         * Extrac the responseURL, title and the publicationDate, and save them
         * into DB
         */

        if (docFeatures.containsKey("responseUrl")) {
          String responseURL = docFeatures.get("responseUrl").toString();
          log.info("Documentfeature 'responseURL' extracted: " + "'" + responseURL + "'");
          dbDocument.setUrl(responseURL);
        } else {
          log.info("No 'responseUrl' feature in Document with id: " + dbDocument.getId() + " extracted");
        }

        if (docFeatures.containsKey("title")) {
          String documentTitle = docFeatures.get("title").toString();
          log.info("Documentfeature 'title' extracted: " + "'" + documentTitle + "'");
          dbDocument.setTitle(documentTitle);
        } else {
          log.info("No 'title' feature in Document with id: " + dbDocument.getId() + " extracted");
        }

        if (docFeatures.containsKey("pubDate")) {
          String publicationDateFeatureStr = docFeatures.get("pubDate").toString();

          try {
            Date publicationDate = (Date) pubDateFormat.parse(publicationDateFeatureStr);
            dbDocument.setPublicationDate(publicationDate);
            log.info("Documentfeature 'pubDate' extracted: " + "'" + publicationDate + "'");
          } catch (ParseException e) {
            Date currentDate = new Date();
            String dateFormattet = pubDateFormat.format(currentDate);
            log.info("Publicationdate '" + publicationDateFeatureStr + "' in document with ID: '"
                + dbDocument.getId() + "' cannot be parsed. Set to current date as default "
                + dateFormattet);
            log.error(e.getMessage());
            dbDocument.setPublicationDate(currentDate);
          }
        } else {
          log.info("No publication in Document with id: " + dbDocument.getId() + " extracted");
          Date currentDate = new Date();
          String dateFormattet = pubDateFormat.format(currentDate);
          log.info("Publicationdate in document with ID: '" + dbDocument.getId()
              + "' cannot be parsed. Set to current date as default " + dateFormattet);
          dbDocument.setPublicationDate(currentDate);
        }

        //databaseConn.updateObjectToDatabase(dbDocument);
      } else {
        throw new MyNoIDException("Document has no 'JSI_WP3_ID Feature-> continue with next document");
      }

    } catch (Exception e) {
      log.error(e.getClass().getName() + " occured on extracting DocumentMetaData");
      log.error(e.getMessage());
      throw e;
    }

    return dbDocument;
  }

  /**
   * Extract the GATE-FEATURE JSI_WP3_ID from document, check if this document exists 
   * in db if not create new db-object
   * 
   * @param gateDoc the GATE-DOCUMENT containing JSI_WP3_ID FEATURE
   * @param dbDocument current databaseObject
   * @return modified databaseObject
   * @throws Exception
   */
  private DocumentMetaData extractJSIID(Document gateDoc, DocumentMetaData dbDocument) throws Exception {

    String jsiwp3ID = gateDoc.getFeatures().get("JSI_WP3_ID").toString();

    log.info("jsiWp3ID: " + jsiwp3ID + " extracted on current gateDocument");
    try {
      dbDocument = databaseConn.checkJSI_IDExists(jsiwp3ID);
    } catch (Exception e1) {
      log.error(e1.getClass().getName() + " occured ");
      log.error(e1.getMessage());
      log.error("Error occured checkJSI_IDExists with id: '" + jsiwp3ID
          + "' -> throwing new MyNoJSIIDException()");
      throw e1;
    }

    if (dbDocument == null) {
      Date retrievalDate = new Date();
      dbDocument = new DocumentMetaData();
      dbDocument.setRetrievalDate(retrievalDate);
      dbDocument.setJsiWp3Id(jsiwp3ID);
      log.info("Created new DocumentMetaData with id: " + dbDocument.getId() + " on Retrieval-Date: "
          + retrievalDate);
      databaseConn.saveOrUpdateObjectToDatabase(dbDocument);
    } else {
      log.debug("DocumentMetaData Object with jsiWp3ID: " + jsiwp3ID + " exists in Database with id: "
          + dbDocument.getId());
    }
    return dbDocument;
  }

  public void setTimeMeasurement(PerformanceMeasurement timeMeasurement) {
    this.timeMeasurement = timeMeasurement;
  }
}
