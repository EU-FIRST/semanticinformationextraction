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
package hibernate;

import hibernate.entities.CorpusMetaData;
import hibernate.entities.DegreeOfMembership;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.DocumentType;
import hibernate.entities.DocumentVersion;
import hibernate.entities.Indicator;
import hibernate.entities.IndicatorType;
import hibernate.entities.PhraseType;
import hibernate.entities.QueryParameter;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassiferTypeEntity;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentIndicator;
import hibernate.entities.SentimentObject;
import hibernate.entities.SentimentObjectType;
import hibernate.entities.Url;
import hibernate.entities.Website;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JOptionPane;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import ontology.WeblogTag;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.LockAcquisitionException;

import utils.CifsNetworkConfig;
import utils.GlobalParameters;

/**
 * @author Lilli Gredel Class which contains the configuration and the
 *         Initialization of hibernate
 */

/**
 * This class is ment to surve as a Facade, 
 * which encapsulates the access and all methods to database.
 * To fully comply to the pattern some modifications would have to be made.
 *  
 * @author lgredel
 *
 */
public class DatabaseFacade {
  private static Logger log = Logger.getLogger(DatabaseFacade.class.getName());
  private SessionFactory sessionFactory = null;
  private static Configuration configuration = null;
  private static DatabaseFacade singletonFacade = null;
  private static Properties configProperties = GlobalParameters.loadConfigFile();
  private CifsNetworkConfig networkConfig = null;
  private static final int TRANSACTION_TIMEOUT = GlobalParameters.getTRANSACTION_TIMEOUT();

  private Session hibernateSession = null;
  private Transaction tx = null;

  private static HibernateConstantEntities constantEntites = null;

  /**
   * Constructor initializing Hibernate
   */
  private DatabaseFacade() {

    String currentDirectoryPath = System.getProperty("user.dir");
    File currentDirectory = new File(currentDirectoryPath);
    currentDirectory.setWritable(true);
    File hibernateConfigurationFile = new File(currentDirectory, "hibernate.cfg.xml");
    log.info("Hibernate Config File: " + hibernateConfigurationFile.getAbsolutePath());
    if (hibernateConfigurationFile.isFile()) {
      DatabaseFacade.configuration = new Configuration().configure(hibernateConfigurationFile);
      this.sessionFactory = configuration.buildSessionFactory();
    } else {
      log.error("Cannot create Databaseconfiguration on file: "
          + hibernateConfigurationFile.getAbsolutePath());
      log.error("System will be exitet");

      // TODO: errorMessage auslagern nach properties Datei (Sprachunabhängig)
      String errorMessage = "Error reading hibernate.cfg.xml";
      JOptionPane.showMessageDialog(null, errorMessage, errorMessage, JOptionPane.ERROR_MESSAGE);
      System.exit(0);
    }

    networkConfig = CifsNetworkConfig.getINSTANCE();
  }

  /**
   * @return the singleton Instance of the Databasefacade 
   */
  public static synchronized DatabaseFacade getSingletonFacade() {

    if (singletonFacade == null) {
      singletonFacade = new DatabaseFacade();
    }

    return singletonFacade;
  }

  /**
   * close the hibernateSession to database, and relase the connection.
   * Clear the content before closing the session.
   * 
   * @param the hibernateSession to close
   */
  public static void closeDBSession(Session hibernateSession) {
    log.trace("Starting to close hibernateSession " + hibernateSession.hashCode());
    if (hibernateSession != null) {
      if (hibernateSession.isConnected() && hibernateSession.isOpen()) {
        hibernateSession.clear();
        hibernateSession.close();
      } else {
        log.error("Cannot close hibernateSession");
      }
    }
  }

  /**
   * Update the object-instance to database using hibernate.
   * If object is instance of DocumentMetaData initialize the Sentimentscollection.
   * 
   * @param object - the instance 
   * @throws HibernateException
   */
  public void updateObjectToDatabase(Object object) throws HibernateException {
    log.trace("Starting to update Object: " + object.getClass() + " in database");

    try {
      hibernateSession.update(object);

      /*
       * Initialize and Load the Sentiment-Collection which is in next session
       * used
       */
      if (object instanceof DocumentMetaData) {
        Hibernate.initialize(((DocumentMetaData) object).getSentiments());
      }

    } catch (LockAcquisitionException lockEx) {
      log.error("Lock on databse occured with Exception: " + lockEx.getClass().getName());
      throw lockEx;
    } catch (HibernateException ex) {
      if (tx != null) {
        log.error("Starting rollback after updating Object: " + object);
        log.error(ex.getMessage());
        throw ex;
      }
    }
  }

  /**
   * Either save or update the object-instance to database using hibernate.
   * If the persistent instance is not in database insert a new one. Otherwise 
   * update the existing one.
   * 
   * @param object
   * @throws HibernateException
   */
  public void saveOrUpdateObjectToDatabase(Object object) throws HibernateException {
   
    int counter = 0;
    
      do{
      try {
        hibernateSession.saveOrUpdate(object);
  
        log.debug("saved or updated object: " + object.toString() + " with class " + object.getClass()
            + " in database with id: " + hibernateSession.getIdentifier(object));
        break;
      } catch (LockAcquisitionException lockEx) {
        log.error("Lock on databse occured with Exception: " + lockEx.getClass().getName());
        log.error(lockEx.getMessage());
        
        counter++;
        
        throw lockEx;
      } catch (HibernateException ex) {
        log.error(ex.getClass().getName() + " exception occured saving object: '" + object
            + " with class: " + object.getClass() + "' to database." + "--> Rollback");
        log.error(ex.getMessage());
  
        throw ex;
      }
      }while ( (counter != 0) && (counter < 5));
  }

  
  /**
   * Create a Documenversion and the corresponding database entries,
   * save or update the documentversion.
   * 
   * @param type - the type of the current Document, which represents the process step like 'classified', 'txt' or 'ontology' 
   * @param file - the file Object containing the path information which will be saved in database 
   * @param docMeta - the DocumentMetaData which the DocumentVersion belongs to.
   * @throws Exception
   */
  public void createDocumentVersion(String type, File file, DocumentMetaData docMeta) throws Exception {
    log.debug("Start createDocumentVersion with type: " + type + " for DocumentMetaData with ID: "
        + docMeta.getId());

    /*
     * reload the doument-data from db
     */
    DocumentVersion docVersion = createDocumentVersionFKRelations(type, file);

    try {

      docVersion.setDocumentMetaData(docMeta);

      setOntologyURL(docVersion);

      saveOrUpdateObjectToDatabase(docVersion);
      log.info("UpdateObjectToDatabase finished on docMeta: " + docMeta.getId());

    } catch (HibernateException hibEx) {
      log.error("throw HibernateException on updating documentMetadata with ID: " + docMeta.getId()
          + " added new documentVersion with type: " + type);
      log.error(hibEx.getMessage());
      throw hibEx;
    }
  }
  
  /**
   * Set the Url information of the used Ontology for GATE processing steps
   * 'preprocessing' and 'classification'. 
   * 
   * @param docVersion - the DocumentVersion to set the used Ontology information
   * @throws Exception
   */
  private void setOntologyURL(DocumentVersion docVersion) throws Exception {

    String docVersionDescription = docVersion.getDocumentType().getType();

    if (docVersionDescription.equalsIgnoreCase("JSI-preprocessed")
        || docVersionDescription.equalsIgnoreCase("preprocessed")
        || docVersionDescription.equalsIgnoreCase("classified")) {

      Url ontologyURL = DatabaseFacade.getSingletonFacade().checkOntology();

      docVersion.setUrlByOntologyUrlFk(ontologyURL);
    }
  }

  /**
   * Create a new Documentversion and set the correspong URL,
   * which contains the path information from the file. 
   * 
   * @param type - the type to set of the DocumentVersion
   * @param file - create a DocumentVersion and corresponding relations with these file information 
   * @return
   * @throws MalformedURLException
   * @throws SmbException
   */
  private DocumentVersion createDocumentVersionFKRelations(String type, File file)
      throws MalformedURLException, SmbException {

    log.debug("*******Setting docVersion properties************");
    DocumentVersion docVersion = new DocumentVersion();

    if (file != null) {
      hibernate.entities.Url dbURL = setURL_FKRelation(file);
      log.debug("Setting URL with ID: " + dbURL.getId());
      docVersion.setUrl(dbURL);
      log.debug("Setting documentName: " + file.getName());
      docVersion.setDocumentName(file.getName());
    }

    DocumentType docType = constantEntites.selectDocumentType(type);
    log.debug("Selected DocumentType with ID: " + docType.getId() + " and type: " + docType.getType());
    docVersion.setDocumentType(docType);
    Date creationDate = new Date();
    GlobalParameters.getFormat().format(creationDate);
    log.debug("Setting Creation Date to: " + creationDate);
    docVersion.setCreationDate(creationDate);

    saveOrUpdateObjectToDatabase(docVersion);

    return docVersion;
  }

  /**
   * Create a new Url for this Ontology-File and save it via hibernate in database.
   * Save the ontology physical per JCIFS on Remote host.   
   * 
   * @param newOntologyVersionSmbPath - the network path to save the file
   * @param ontology - the ontology to save
   * @return
   * @throws Exception
   */
  private hibernate.entities.Url setOntologyURL_FKRelation(String newOntologyVersionSmbPath,
      WeblogTag ontology) throws Exception {
    log.debug("*******Setting URL properties************");

    hibernate.entities.Url dbUrl = new Url();

    String ontologyName = ontology.getOnto().getName();
    String owlSuffix = ".owl";
    int owlSuffixLength = owlSuffix.length();
    ontologyName = ontologyName.substring(0, ontologyName.lastIndexOf(owlSuffix) + owlSuffixLength);

    //If this file is set in networkConfig, the file has been copied via smb
    SmbFile smbFile = networkConfig.getCurrentSmbFile();

    if (smbFile != null) {

      if (smbFile.getName().contains(ontologyName)) {
        dbUrl.setProtocol("smb://");
        dbUrl.setHost(smbFile.getServer());
        String completePath = smbFile.getPath();

        int shareIndex = completePath.indexOf(smbFile.getShare());
        shareIndex = shareIndex - 1;
        String path = completePath.substring(shareIndex, completePath.length());

        dbUrl.setPath(path);

        try {
          hibernateSession.save(dbUrl);
          log.info("Saved OntologyURL with ID: " + dbUrl.getId());

        } catch (HibernateException hibEx) {
          log.error("Error adding new URL in database for ontology " + newOntologyVersionSmbPath);
          log.error(hibEx.getMessage());
          throw hibEx;
        }
      } else {
        log.error("Cannot set foreign key to OntologyURL because SmbFile " + smbFile.getName()
            + " contains no " + owlSuffix);
      }
    } else {
      String errorMessage = "Current SMB-File is null -> tried to set URL with wrong newOntologyVersionSmbPath"
          + newOntologyVersionSmbPath;
      log.error(errorMessage);
      throw new Exception(errorMessage);
    }

    return dbUrl;
  }

  /**
   * Create a new Url for this file and save it to database via hibernate.
   * 
   * @param file - the file which contains the Url information to save
   * @return the new Url database object
   * @throws MalformedURLException
   * @throws SmbException
   */
  private hibernate.entities.Url setURL_FKRelation(File file) throws MalformedURLException, SmbException {
    log.debug("*******Setting URL properties************");

    hibernate.entities.Url dbUrl = new Url();

    //If this file is set in networkConfig, the file has been copied via smb
    SmbFile smbFile = networkConfig.getCurrentSmbFile();

    if (smbFile != null) {
      if (smbFile.getName().equals(file.getName())) {
        dbUrl.setProtocol("smb://");
        dbUrl.setHost(smbFile.getServer());
        String completePath = smbFile.getPath();

        int shareIndex = completePath.indexOf(smbFile.getShare());
        shareIndex = shareIndex - 1;
        String path = completePath.substring(shareIndex, completePath.length());

        dbUrl.setPath(path);
      }
    } else {

      String IPAdress = configProperties.getProperty("serverIP");
      log.debug("Selected IPAdress for dbURL: " + IPAdress);

      String ip = File.separator + File.separator + File.separator + File.separator + IPAdress;
      String replacedFilePath = file.getAbsolutePath().replaceFirst(ip, "");
      log.debug("Path for dbURL: " + replacedFilePath);

      dbUrl.setProtocol("file");
      dbUrl.setHost(IPAdress);
      dbUrl.setPath(replacedFilePath);
    }

    try {
      saveOrUpdateObjectToDatabase(dbUrl);

    } catch (HibernateException hibEx) {
      log.error("Error adding new URL in database for file: " + file.getAbsolutePath());
      log.error(hibEx.getMessage());
    }
    return dbUrl;
  }

  /**
   * Select from database the SentimentFeatureType,
   * or create a new one.
   * 
   * @param currentFeatureName - use this featurename for database search
   * @param sentimentObjectOntologyURI - use this sentimentObjectOntologyURI for database search
   * @return
   */
  public SentimentFeatureType createOrgetSF(String currentFeatureName, String sentimentObjectOntologyURI) {

    SentimentFeatureType type = null;

    Criteria crit = hibernateSession.createCriteria(SentimentFeatureType.class);
    crit.add(Restrictions.eq("name", currentFeatureName));
    if (sentimentObjectOntologyURI != null) {
      crit.add(Restrictions.eq("ontologyConceptUri", sentimentObjectOntologyURI));
    }
    crit.addOrder(Order.asc("id"));
    List<SentimentFeatureType> typeList = crit.list();

    if (typeList.isEmpty()) {
      type = new SentimentFeatureType();
      type.setName(currentFeatureName);
      type.setOntologyConceptUri(sentimentObjectOntologyURI);
      saveOrUpdateObjectToDatabase(type);
    } else {
      if (typeList.size() == 1) {
        type = typeList.get(0);
        log.info("SentimentFeatureType " + type + " exists in database. Extract first with ID: "
            + type.getId());
      } else {
        int count = typeList.size() - 1;
        log.debug("There are " + count + " more than one feature in database with name: "
            + currentFeatureName);
        type = typeList.get(0);
        log.debug("Setting to first id: " + type.getId());
      }
    }
    return type;
  }

  /**
   * Select the SentimentClassifierType from this parameter
   * 
   * @param classifierMethod - search for this classifierMethod in database
   * @param sentimentType - search for this sentimentType in database
   * @return the SentimentClassifierType from database search
   */
  public SentimentClassifierType loadSentimentClassifierType(String classifierMethod,
      String sentimentType) {

    SentimentClassifierType classifierType = SentimentClassiferTypeEntity.loadSentimentClassifierType(
        classifierMethod, sentimentType);

    return classifierType;
  }

  /**
   * Select the PhraseType for the name of the PhraseType 
   * 
   * @param phraseTypeName - 
   * @return the selected PhraseType
   */
  public PhraseType getPhraseType(String phraseTypeName) {
    PhraseType phraseType = null;

    Criteria crit = hibernateSession.createCriteria(PhraseType.class);
    crit.add(Restrictions.eq("name", phraseTypeName).ignoreCase());

    List<PhraseType> phraseTypeList = crit.list();
    if (phraseTypeList.size() == 1) {
      phraseType = phraseTypeList.get(0);
    } else {
      log.error("Error extracting Phrasetype with name: " + phraseTypeName + " from Databse");
      log.error("Setting phrasetype to null");
    }
    return phraseType;
  }

  /**
   * @param jsiwp3ID
   * @return if the DocumentMetaData with the jsiwp3id exists in Database return
   *         DocumentMetaData object from database else null
   */
  
  
  public DocumentMetaData checkJSI_IDExists(String jsiwp3ID) throws Exception {
    DocumentMetaData document = null;

    int lockAquCounter = 0;

    do {
      try {

        Criteria crit = hibernateSession.createCriteria(DocumentMetaData.class);
        crit.add(Restrictions.eq("jsiWp3Id", jsiwp3ID));
        crit.setMaxResults(1);

        int level = hibernateSession.connection().getTransactionIsolation();
        log.trace("TransactionIsolationLevel: " + level);
        hibernateSession.connection().setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        
        log.trace("Startet criteria list with jsiID");
        long start = System.currentTimeMillis();
        List<DocumentMetaData> documentList = crit.list();
        long end = System.currentTimeMillis();
        log.trace("Loading JSI-ID needs: " + (end - start));
        
        hibernateSession.connection().setTransactionIsolation(level);

        if (!documentList.isEmpty()) {
          log.debug("Resultlist of DocumentMetaData-Objects mit jsiWp3Id " + jsiwp3ID + " is not empty");
          if (documentList.size() > 0) {
            document = documentList.get(0);
            log.info("Selected documentMetaDataObject in database with ID: " + document.getId());
            //document.getDocumentVersions().clear();

            Set<DocumentVersion> allDocVersions = document.getDocumentVersions();
            Iterator<DocumentVersion> it = allDocVersions.iterator();

            while (it.hasNext()) {
              DocumentVersion dv = it.next();
              it.remove();
            }
          }
        } else {
          log.debug("Resultlist of DocumentMetaData-Objects mit jsiWp3Id " + jsiwp3ID + " is empty");
        }
      } catch (LockAcquisitionException lockAquEx) {
        log.error("LockAcquisitionException: occured on checkJSI_IDExists() with jsiWP3ID String: "
            + jsiwp3ID);
        log.info("Wait and try again");
        Thread.sleep(10000);
        lockAquCounter++;
        continue;
      } catch (Exception e) {
        log.error("Exception: " + e.getClass()
            + " occured on checkJSI_IDExists() with jsiWP3ID String: " + jsiwp3ID);
        throw e;
      }
    } while (lockAquCounter > 0 && lockAquCounter < 3);
    return document;
  }

  /**
   * @param uhohID
   * @return if the DocumentMetaData with the id exists in Database return
   *         DocumentMetaData object from database else null
   */
  public DocumentMetaData checkUHOH_IDExists(String uhohID, Session hibernateSession) throws Exception {
    DocumentMetaData document = null;
    try {
      Criteria crit = hibernateSession.createCriteria(DocumentMetaData.class);
      crit.add(Restrictions.eq("id", uhohID));
      List<DocumentMetaData> documentList = crit.list();

      if (!documentList.isEmpty()) {
        log.debug("Resultlist of DocumentMetaData-Objects with uhohID " + uhohID + " is not empty");
        if (documentList.size() > 0) {
          document = documentList.get(0);
          log.info("Selected documentMetaDataObject in database with ID: " + document.getId());
        }
      } else {
        log.debug("Resultlist of DocumentMetaData-Objects mit jsiWp3Id " + uhohID + " is not empty");
      }

    } catch (Exception e) {
      log.error("Exception: " + e.getClass() + " occured on checkUHOH_IDExists() with uhohID String: "
          + uhohID);
      throw e;
    }

    return document;
  }

  /**
   * Checks the current Version of the used ontology for this run, and if the
   * versionInfo field changed, copy the current ontology to the shared
   * directory and create a new URL-Ontology Object in database
   * 
   * @return
   * @throws Exception
   */
  public Url checkOntology() throws Exception {
    log.info("Starting to check Ontology Version");

    WeblogTag ontology = WeblogTag.getInstance();

    String currentOntologyVersionInfo = null;
    File ontologyFile = null;

    synchronized (ontology) {
      currentOntologyVersionInfo = ontology.loadVersionInfo();
      ontologyFile = ontology.getOntologyFile();
    }

    String newOntologyVersionSmbPath = null;

    Url ontologyURL = null;

    HashMap<String, Long> ontologyVersions = constantEntites.getOntologyVersionsMap();
    Long ontologyURLID = ontologyVersions.get(currentOntologyVersionInfo);

    if (ontologyURLID != null) {
      ontologyURL = (Url) hibernateSession.load(Url.class, ontologyURLID);

    }

    if ((ontologyURLID == null) && (ontology != null)) {
      try {

        CifsNetworkConfig networkConfig = CifsNetworkConfig.getINSTANCE();
        newOntologyVersionSmbPath = networkConfig.copyOntologyFile(ontologyFile, false);

        ontologyURL = setOntologyURL_FKRelation(newOntologyVersionSmbPath, ontology);
        ontologyURL.setVersionInfo(currentOntologyVersionInfo);

        constantEntites.getOntologyVersionsMap().put(ontologyURL.getVersionInfo(), ontologyURL.getId());

      } catch (MalformedURLException e) {
        log.error("Error saving Ontology as DocumentVersion: " + ontologyFile.getAbsolutePath());
        log.error(e.getMessage());
        throw e;
      } catch (HibernateException e) {
        log.error("DB-Error saving Ontology as DocumentVersion in Database: "
            + ontologyFile.getAbsolutePath());
        log.error(e.getMessage());
        throw e;
      } catch (IOException e) {
        log.error("I/O-Error on saving Ontologyfile as DocumentVersion: "
            + ontologyFile.getAbsolutePath() + " to new path: " + newOntologyVersionSmbPath);
        log.error(e.getMessage());
        throw e;
      }
    }

    return ontologyURL;
  }

  /**
   * Remove all sentiments with the classifier type  from current document.
   * 
   * @param dbDocument
   * @param classifierType
   * @param sentimentType
   * @throws SQLException
   */
  public void deleteSentimentsWithClassifierType(DocumentMetaData dbDocument, String classifierType,
      String sentimentType) throws SQLException {
    log.debug("Starting to delete sentiments with classifiertype: " + classifierType
        + " and sentimentType: " + sentimentType + " on Document: " + dbDocument);

    if (dbDocument != null) {

      Set<Sentiment> sentiments = dbDocument.getSentiments();
      Iterator<Sentiment> it = sentiments.iterator();

      while (it.hasNext()) {
        Sentiment currSent = (Sentiment) it.next();

        SentimentClassifierType currentType = currSent.getSentimentClassifierType();
        if (currentType != null) {
          String classifierMethod = currentType.getClassifierMethod();
          String currentSentimentType = currentType.getSentimentType();

          boolean classifierMethodEq = classifierMethod.equalsIgnoreCase(classifierType);
          boolean sentimentTypeEq = currentSentimentType.equalsIgnoreCase(sentimentType);

          if (classifierMethodEq && sentimentTypeEq) {
            log.info("delete extracted sentiment with " + classifierType
                + " Method on document with ID: " + dbDocument.getId() + " and sentimentID: "
                + currSent.getId());
            it.remove();
            currSent = null;
          }
        } else {
          log.info("delete extracted sentiment without SentimentClassifierType on document with ID: "
              + dbDocument.getId() + " and sentimentID: " + currSent.getId());
          it.remove();
          currSent = null;
        }
      }      
    }
  }

  /**
   *  Removes all DocumentVersions with the specified Type from the
   *  docMeta-Object, and creates a new DocumentVersion with the
   *  inputXML filedata
   * 
   * @param docMeta - the current document
   * @param inputXML    - The file containing the fileinformation to save in database
   * @param documentType - the corresponding type
   * @throws Exception
   */
  public void updateDocumentVersionWithDocumentVersionType(DocumentMetaData docMeta, File inputXML,
      String documentType) throws Exception {
    try {

      Set<DocumentVersion> docVersions = docMeta.getDocumentVersions();

      DocumentType searchDocType = constantEntites.selectDocumentType(documentType);

      if (docVersions != null) {
        Iterator<DocumentVersion> iter = docVersions.iterator();

        while (iter.hasNext()) {
          DocumentVersion docV = (DocumentVersion) iter.next();

          DocumentType type = docV.getDocumentType();
          String currDocType = type.getType();

          if (currDocType.equals(searchDocType.getType())) {
            iter.remove();
          }
        }
      }

      DatabaseFacade.getSingletonFacade().createDocumentVersion(documentType, inputXML, docMeta);

    } catch (MalformedURLException e1) {
      log.error("Cannot crate documentVersion on docMeta-Object: " + docMeta.getId());
      log.error(e1.getMessage());
      throw e1;
    } catch (IOException e) {
      log.error("IOException happend on createDocumentVersion on docMeta-Object: " + docMeta.getId());
      log.error(e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(e.getClass() + " on creating documentVersion with DocumentMetaData-Object: "
          + docMeta.getId());
      log.error(e.getMessage());
      throw e;
    }
  }

  /**
   * Check if DocumentVersion with fileName exist.
   * If there are old DocumentVersions with these fileName and the corresponding fileType
   * delete the old ones, and create a new one.
   * Otherwise create a new DocumentVersion for this file.
   * 
   * @param file
   * @param fileType
   * @param document
   * @return
   * @throws Exception
   */
  public DocumentMetaData updateDocumentVersionFromFileName(File file, String fileType,
      DocumentMetaData document) throws Exception {
    @SuppressWarnings("unchecked")
    DocumentVersion docVersion = null;

    if (file != null) {
      docVersion = getDocumentVersionFromFileName(file);
    }
    Session hibernateSession = null;

    if (docVersion == null) {
      if (document == null) {
        document = new DocumentMetaData();
        Date retrievalDate = new Date();
        document.setRetrievalDate(retrievalDate);
        saveOrUpdateObjectToDatabase(document);
        Long id = document.getId();
        log.debug("Saved new DocumentMetaDataObject with id: '" + id + "'");
      }
    } else {
      hibernateSession = getHibernateSession();
      if (document == null) {

        if (docVersion.getDocumentMetaData() != null) {
          document = (DocumentMetaData) hibernateSession.get(DocumentMetaData.class, docVersion
              .getDocumentMetaData().getId());
        }

        if (document == null) {
          document = new DocumentMetaData();
          saveOrUpdateObjectToDatabase(document);
          Long id = document.getId();
          log.debug("Saved new DocumentMetaDataObject with id: '" + id + "' and docVersion: '"
              + docVersion.getId() + "'");
        }
      }
    }

    if (hibernateSession == null || !hibernateSession.isOpen()) {
      hibernateSession = getHibernateSession();
    }
    /*
     * delete current DocumentVersion with the corresponding Type
     */
    Set<DocumentVersion> docVersionsSet = document.getDocumentVersions();

    Iterator<DocumentVersion> it = docVersionsSet.iterator();
    while (it.hasNext()) {
      DocumentVersion currentDocVersion = (DocumentVersion) it.next();

      DocumentType currentType = (DocumentType) hibernateSession.load(DocumentType.class,
          currentDocVersion.getDocumentType().getId());

      DocumentType searchfileType = constantEntites.selectDocumentType(fileType);

      if (currentType.getType().equalsIgnoreCase(searchfileType.getType())) {
        log.debug("Remove DocumentVersion with Type: " + searchfileType + "and ID: "
            + currentDocVersion.getId() + " from DocumentMetaDataobject: " + document.getId());
        it.remove();
      }
    }

    document.setDocumentVersions(docVersionsSet);

    saveOrUpdateObjectToDatabase(document);

    try {
      this.createDocumentVersion(fileType, file, document);
    } catch (MalformedURLException e1) {
      log.error("Cannot crate documentVersion on documentMetaData-Object: " + document.getId());
      log.error(e1.getMessage());
      throw e1;
    } catch (IOException e) {
      log.error("IOException happend on createDocumentVersion on documentMetaData-Object: "
          + document.getId());
      log.error(e.getMessage());
      throw e;
    } catch (HibernateException hibex) {
      log.error("Hibernateexception on creating DocumentVersion documentMetaData-Object: "
          + document.getId() + " and fileType: " + fileType);
      log.error(hibex.getMessage());
    }

    return document;
  }

  /**
   * Extract the DocumentVersion from an existing Filename
   * 
   * @param file
   * @return
   */
  private DocumentVersion getDocumentVersionFromFileName(File file) {
    Class searchClass = DocumentVersion.class;
    DocumentVersion docVersion = null;

    Session hibernateSession = getHibernateSession();
    Criteria crit = hibernateSession.createCriteria(searchClass);
    crit.add(Restrictions.eq("documentName", file.getName()));
    @SuppressWarnings("unchecked")
    List<DocumentVersion> results = crit.list();

    if (results.size() >= 1) {
      docVersion = results.get(0);
      log.info("DocumentVersion with documentName: " + file.getName() + " exists with ID: "
          + docVersion.getId());
    }

    return docVersion;
  }

  /**
   * Extract the ontologyConceptUri from String, and
   * select a SentimentObjectType from database.
   * Update changes to database.
   *  
   * @param objectTypeDescription
   * @return - the selected SentimentObjectType or a new Instance
   * @throws MalformedURLException
   * @throws URISyntaxException
   */
  public SentimentObjectType getSentimentObjectType(String objectTypeDescription)
      throws MalformedURLException, URISyntaxException {

    SentimentObjectType type = null;
    URL ontologyUrl = new URL(objectTypeDescription);

    String soName = ontologyUrl.toURI().getFragment();

    Criteria crit = hibernateSession.createCriteria(SentimentObjectType.class);
    crit.add(Restrictions.eq("ontologyConceptUri", objectTypeDescription));
    crit.addOrder(Order.asc("id"));

    List<SentimentObjectType> currentObjectTypeList = crit.list();

    if (currentObjectTypeList.size() >= 1) {
      type = currentObjectTypeList.get(0);
    } else {
      log.info("There are no SentimentObjectType in Database with name: " + objectTypeDescription);

      type = new SentimentObjectType();
      type.setName(soName);
      type.setOntologyConceptUri(objectTypeDescription);

      try {
        DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(type);
        log.info("Added new ObjectType with name: " + objectTypeDescription + " sucessfully");
      } catch (HibernateException ex) {
        log.error("Cannot save new ObjectType with name: " + objectTypeDescription + "in Database.");
        log.error(ex.getMessage());
      }

    }
    return type;
  }

  /**
   * Select a SentimentObjectType from database with existing objectTypeName.
   * Update changes to database. 
   * 
   * @param objectTypeName
   * @return the selected SentimentObjectType or a new Instance
   * @throws MalformedURLException
   * @throws URISyntaxException
   */
  public SentimentObjectType getSentimentObjectTypeFromName(String objectTypeName)
      throws MalformedURLException, URISyntaxException {

    SentimentObjectType type = null;

    String soName = objectTypeName;

    Criteria crit = hibernateSession.createCriteria(SentimentObjectType.class);
    crit.add(Restrictions.eq("name", objectTypeName));
    crit.addOrder(Order.asc("id"));

    List<SentimentObjectType> currentObjectTypeList = crit.list();

    if (currentObjectTypeList.size() >= 1) {
      type = currentObjectTypeList.get(0);
    } else {
      log.info("There are no SentimentObjectType in Database with name: " + objectTypeName);

      type = new SentimentObjectType();
      type.setName(soName);
      type.setOntologyConceptUri(objectTypeName);

      try {
        DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(type);
        log.info("Added new ObjectType with name: " + objectTypeName + " sucessfully");
      } catch (HibernateException ex) {
        log.error("Cannot save new ObjectType with name: " + objectTypeName + "in Database.");
        log.error(ex.getMessage());
      }

    }
    return type;
  }

  /**
   * Select a SentimentObject from database with current objectTypeName and ontologyURI.
   * Update changes to database.  
   * 
   * @param sot - the SentimentObjectType to identify in database
   * @param sentimentObjectName - the name of the SentimentObject to identify
   * @param sentimentObjectOntologyURI - the ontologyURI to identify
   * @return If exists, the selected SentimentObject otherwise a new instance
   */
  public SentimentObject createOrgetSO(SentimentObjectType sot, String sentimentObjectName,
      String sentimentObjectOntologyURI) {
    SentimentObject sentimentObject = null;

    Criteria crit = hibernateSession.createCriteria(SentimentObject.class);
    crit.add(Restrictions.eq("name", sentimentObjectName));
    crit.add(Restrictions.eq("ontologyConceptUri", sentimentObjectOntologyURI));
    crit.addOrder(Order.asc("id"));
    List<SentimentObject> typeList = crit.list();

    if (typeList.isEmpty()) {
      sentimentObject = new SentimentObject();
      sentimentObject.setSentimentObjectType(sot);
      sentimentObject.setName(sentimentObjectName);
      sentimentObject.setOntologyConceptUri(sentimentObjectOntologyURI);
      saveOrUpdateObjectToDatabase(sentimentObject);
    } else {
      if (typeList.size() == 1) {
        sentimentObject = typeList.get(0);
        log.info("SentimentObject " + sentimentObject + " exists in database. Extract with ID: "
            + sentimentObject.getId());
      } else {
        int count = typeList.size() - 1;
        log.debug("There are " + count + " more than one sentiment Object in database with name: "
            + sentimentObjectName + " and ontologyConceptURI: " + sentimentObjectOntologyURI);
        sentimentObject = typeList.get(0);
        log.debug("Setting to first id: " + sentimentObject.getId());
      }
    }
    return (sentimentObject);

  }

  /**
   * Creates a new IndicatorType from string-representation in database
   * 
   * @param indicatortype
   * @return the new instance of the IndicatorType-object
   */
  public IndicatorType createIndicatorType(String indicatortype) {

    IndicatorType type = null;
    Criteria crit = hibernateSession.createCriteria(IndicatorType.class);

    crit.add(Restrictions.eq("name", indicatortype));
    crit.addOrder(Order.asc("id"));
    List<IndicatorType> typeList = crit.list();

    if (typeList.isEmpty()) {
      type = new IndicatorType();
      type.setName(indicatortype);
      DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(type);
    } else {
      if (typeList.size() == 1) {
        type = typeList.get(0);
        log.info("IndicatorType " + type + " exists in database. Extract first with ID: " + type.getId());
      } else {
        int count = typeList.size() - 1;
        log.debug("There are " + count + " more than one Indicatortypes in database with name: "
            + indicatortype);
        type = typeList.get(0);
        log.debug("Setting to first indicatorId: " + type.getId());
      }
    }
    return type;
  }
  
  /**
   * Select a Indicator from database with indicatorname and indicatorURI.
   * Update changes to database. 
   * 
   * @param indicatorname
   * @param indicatorURI
   * @return If exists, the selected Indicator otherwise a new instance
   */
  public Indicator createIndicatorConcept(String indicatorname, String indicatorURI) {

    Indicator indicator = null;
    Criteria crit = hibernateSession.createCriteria(Indicator.class);
    crit.add(Restrictions.eq("indicatorName", indicatorname));
    crit.add(Restrictions.eq("ontologyConceptUri", indicatorURI));
    crit.addOrder(Order.asc("id"));
    List<Indicator> typeList = crit.list();

    if (typeList.isEmpty()) {
      indicator = new Indicator();
      indicator.setIndicatorName(indicatorname);
      indicator.setOntologyConceptUri(indicatorURI);
      DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(indicator);
    } else {
      if (typeList.size() == 1) {
        indicator = typeList.get(0);
        log.info("Indicator " + indicator.getIndicatorName() + " exists in database. Extract with ID: "
            + indicator.getId());
      } else {
        int count = typeList.size() - 1;
        log.debug("There are " + count + " more than one Indicators in database with name: "
            + indicatorname + " and ontologyConceptURI: " + indicatorURI);
        indicator = typeList.get(0);
        log.debug("Setting to first id: " + indicator.getId());
      }
    }
    return indicator;
  }

  /**
   * Select a SentimentIndicator from database with the parameter.
   * Update changes to database. 
   *  
   * @param indicatorname - the name of theSentiment indicator
   * @param indicatorCorrelation - the correlation of the Sentimentindicator
   * @param indicatorScore - the score of the Sentimentindicator
   * @param inditype - the type of the Sentimentindicator
   * @param indicatorConcept - the ontolog-url of the Sentimentindicator
   * 
   * @return If exists, the selected SentimentIndicator otherwise a new instance
   */
  public SentimentIndicator createOrgetSentimentIndicator(String indicatorname,
      int indicatorCorrelation, Long indicatorScore, IndicatorType inditype, Indicator indicatorConcept) {

    SentimentIndicator sentimentIndicator = null;

    Criteria crit = hibernateSession.createCriteria(SentimentIndicator.class);
    crit.add(Restrictions.eq("name", indicatorname));
    crit.add(Restrictions.eq("indicatorType", inditype));
    crit.add(Restrictions.eq("indicator", indicatorConcept));
    crit.add(Restrictions.eq("indicatorScore", indicatorScore));
    crit.add(Restrictions.eq("correlationDefinition", indicatorCorrelation));
    List<SentimentIndicator> typeList = crit.list();

    if (typeList.isEmpty()) {
      sentimentIndicator = new SentimentIndicator();
      sentimentIndicator.setName(indicatorname);
      sentimentIndicator.setCorrelationDefinition(indicatorCorrelation);
      sentimentIndicator.setIndicatorScore(indicatorScore);
      sentimentIndicator.setIndicatorType(inditype);
      sentimentIndicator.setIndicator(indicatorConcept);

      saveOrUpdateObjectToDatabase(sentimentIndicator);
    } else {
      if (typeList.size() == 1) {
        sentimentIndicator = typeList.get(0);
        log.info("Indicator " + sentimentIndicator.getName() + " exists in database. Extract with ID: "
            + sentimentIndicator.getId());
      } else {
        int count = typeList.size() - 1;
        log.debug("There are " + count + " more than one SentimentIndicators in database with "
            + "name: " + indicatorname + " and indicatorType with ID: " + inditype.getId()
            + " and indicator with ID: " + indicatorConcept.getId() + " and indicatorScore: "
            + indicatorScore + " and correlationDefinition " + indicatorCorrelation);

        sentimentIndicator = typeList.get(0);
        log.debug("Setting to first id: " + sentimentIndicator.getId());
      }
    }
    return sentimentIndicator;
  }

  
  /**
   * Select a DegreeOfMembership from database with the parameter. 
   * 
   * @param databaseLabelName
   * @return
   */
  public DegreeOfMembership loadDegreeOfMemberShip(String databaseLabelName) {

    DegreeOfMembership doM = null;
    Criteria crit = hibernateSession.createCriteria(DegreeOfMembership.class);
    crit.add(Restrictions.eq("label", databaseLabelName).ignoreCase());

    List<DegreeOfMembership> domList = crit.list();
    if (domList.size() == 1) {
      doM = domList.get(0);
    } else {

      if (domList.isEmpty()) {
        log.debug("Degree of Membership List is empty for initializing");
        log.debug("Insert DOM-Table Values manually. System will exit now");
        System.exit(111);
      } else {
        int count = domList.size() - 1;
        log.debug("There are " + count + " more than one DegreeOfMemberships in database with "
            + "label: " + databaseLabelName);
        doM = domList.get(0);
        log.debug("Setting to first id: " + doM.getId());
      }
    }

    return doM;
  }

  /**
   * Check if the corpus already exists in Database 
   * 
   * @param corpusName - the name of the corpus
   * 
   * @return Corpus or null if not exists
   */
  public CorpusMetaData checkCorpusExists(String corpusName) {
    CorpusMetaData corpus = null;
    Session hibernateSession = getHibernateSession();

    Criteria crit = hibernateSession.createCriteria(CorpusMetaData.class);
    crit.add(Restrictions.eq("corpusName", corpusName));
    List<CorpusMetaData> results = crit.list();

    Iterator<CorpusMetaData> it = results.iterator();
    while (it.hasNext()) {
      corpus = (CorpusMetaData) it.next();
    }

    return corpus;
  }
  
  
  /**
   * Select a QueryParameter from database with the parameter.
   * Update changes to database.
   *  
   * @param queryDate
   * @param querySentimentObject
   * @param querySite
   * @return If exists, the selected QueryParameter otherwise a new instance
   */
  public QueryParameter checkQueryParameterExists(Date queryDate, String querySentimentObject,
      String querySite) {
    QueryParameter queryPar = null;
    Session hibernateSession = getHibernateSession();
    Criteria crit = hibernateSession.createCriteria(QueryParameter.class);
    crit.add(Restrictions.eq("queryDate", queryDate));
    crit.add(Restrictions.eq("querySentimentObject", querySentimentObject));
    crit.add(Restrictions.eq("querySite", querySite));
    List<QueryParameter> results = crit.list();

    if (results.isEmpty()) {
      queryPar = new QueryParameter();
      queryPar.setQueryDate(queryDate);
      queryPar.setQuerySentimentObject(querySentimentObject);
      queryPar.setQuerySite(querySite);
    } else {
      queryPar = results.get(0);
      log.info("QueryParameter exists - loaded from database: " + queryPar.getId());
    }
    return queryPar;
  }

  /**
   * Select if the {@link DocumentMetaData} url and the queryDate in database exists and
   * 
   * @param htmlURL - the url-homepage to select
   * @param currentQueryDate - the queryDate to select
   * @return if the html-site exists in Database return DocumentMetaData object
   *         else null
   */
  public DocumentMetaData checkHtmlUrlExists(String htmlURL, Date currentQueryDate) {
    DocumentMetaData document = null;
    Session hibernateSession = getHibernateSession();
    Criteria crit = hibernateSession.createCriteria(DocumentMetaData.class);
    crit.add(Restrictions.eq("url", htmlURL));
    crit.addOrder(Order.asc("id"));
    List<DocumentMetaData> documentList = crit.list();

    if (!documentList.isEmpty()) {

      if (documentList.size() == 1) {

        document = documentList.get(0);
        log.info("Current html-site exists in database on document with id: " + document.getId());
        Date existingQueryDate = document.getQueryParameter().getQueryDate();

        /*
         * If the publication date of the article from db equals querydate from
         * db than do nothing
         */
        if (!document.getPublicationDate().equals(existingQueryDate)) {
          /*
           * If publication date from db is equals or less than the
           * currentQuerydate set the db querydate to currentquerydate
           */
          if (document.getPublicationDate().compareTo(currentQueryDate) <= 0) {
            document.getQueryParameter().setQueryDate(currentQueryDate);
          }
        }
      }
    }
    return document;
  }

  /**
   * Delete object from database
   * 
   * @param object
   */
  public void deleteObjectOnDatabase(Object object) {

    int counter = 0;

    while (counter < 3) {
      Session hibernateSession = null;
      Transaction tx = null;
      try {
        hibernateSession = getHibernateSession();
        tx = hibernateSession.beginTransaction();
        hibernateSession.delete(object);
        tx.commit();
        counter = 3;
        log.debug("Delete object on database: " + object + " sucessfully");
      } catch (LockAcquisitionException lockEx) {
        log.error("Lock on databse occured with Exception: " + lockEx.getClass().getName());

        try {
          Thread.sleep(100);
          counter++;
          log.error("Try again " + counter);
          continue;
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } catch (HibernateException ex) {
        log.error("Cannot delete object: " + object + " on database");
        log.error(ex.getMessage());
        if (tx != null) {
          tx.rollback();
          throw ex;
        }
      }
    }
  }

  /**
   * Select the website with url-parameter from database
   * @param url
   * @return The found website
   */
  public Website checkWebSiteExists(String url) {
    Website site = null;
    Session hibernateSession = getHibernateSession();

    Criteria crit = hibernateSession.createCriteria(Website.class);
    crit.add(Restrictions.eq("url", url));
    List<Website> resultList = crit.list();

    if (resultList.size() > 0) {
      site = resultList.get(0);
    }

    return site;
  }

  public HibernateConstantEntities getConstantEntites() {
    return constantEntites;
  }

  public void setConstantEntites(HibernateConstantEntities constantEntites) {
    this.constantEntites = constantEntites;
  }

  /**
   * Start database transaction
   * @throws SQLException
   */
  public void startTransaction() throws SQLException {
    tx = hibernateSession.getTransaction();
    tx.setTimeout(TRANSACTION_TIMEOUT);
    tx.begin();
    log.trace("Transaction startet with hashcode: " + tx.hashCode());
    
  }

  /**
   * 
   * @return a new Database Session
   */
  public Session openSession() {
    Session hibSession = sessionFactory.openSession();

    return hibSession;
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public Session getHibernateSession() {
    return hibernateSession;
  }

  public void setHibernateSession(Session hibernateSession) {
    this.hibernateSession = hibernateSession;

    if (hibernateSession != null) {
      constantEntites = HibernateConstantEntities.getINSTANCE();
    }
  }

  public Transaction getTx() {
    return tx;
  }

  public void setTx(Transaction tx) {
    this.tx = tx;
  }
}
