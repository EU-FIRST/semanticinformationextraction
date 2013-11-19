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

import hibernate.entities.DocumentType;
import hibernate.entities.PhraseType;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentLevelDefinition;
import hibernate.entities.Url;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

public class HibernateConstantEntities {

  private static Logger log = Logger.getLogger(HibernateConstantEntities.class);
  public SentimentLevelDefinition sentimentLevelDocument = null;

  public SentimentLevelDefinition sentimentLevelSentence = null;

  public  SentimentClassifierType classifierTypeCrisp = null;
  private SentimentClassifierType classifierTypeFuzzy = null;

  private PhraseType sentencePhraseType = null;
  
  private HashMap<String, Long> ontologyVersionsMap = new HashMap<String, Long>();

  public static Logger getLog() {
    return log;
  }

  public static void setLog(Logger log) {
    HibernateConstantEntities.log = log;
  }

  public SentimentLevelDefinition getSentimentLevelDocument() {
    return sentimentLevelDocument;
  }

  public void setSentimentLevelDocument(SentimentLevelDefinition sentimentLevelDocument) {
    this.sentimentLevelDocument = sentimentLevelDocument;
  }

  public SentimentLevelDefinition getSentimentLevelSentence() {
    return sentimentLevelSentence;
  }

  public void setSentimentLevelSentence(SentimentLevelDefinition sentimentLevelSentence) {
    this.sentimentLevelSentence = sentimentLevelSentence;
  }

  public SentimentClassifierType getClassifierTypeCrisp() {
    return classifierTypeCrisp;
  }

  public void setClassifierTypeCrisp(SentimentClassifierType classifierTypeCrisp) {
    this.classifierTypeCrisp = classifierTypeCrisp;
  }

  public SentimentClassifierType getClassifierTypeFuzzy() {
    return classifierTypeFuzzy;
  }

  public void setClassifierTypeFuzzy(SentimentClassifierType classifierTypeFuzzy) {
    this.classifierTypeFuzzy = classifierTypeFuzzy;
  }

  public PhraseType getSentencePhraseType() {
    return sentencePhraseType;
  }

  public void setSentencePhraseType(PhraseType sentencePhraseType) {
    this.sentencePhraseType = sentencePhraseType;
  }

  public PhraseType getSentimentPhraseType() {
    return sentimentPhraseType;
  }

  public void setSentimentPhraseType(PhraseType sentimentPhraseType) {
    this.sentimentPhraseType = sentimentPhraseType;
  }

  public PhraseType getOrientationPhraseType() {
    return orientationPhraseType;
  }

  public void setOrientationPhraseType(PhraseType orientationPhraseType) {
    this.orientationPhraseType = orientationPhraseType;
  }

  public PhraseType getFeaturePhraseType() {
    return featurePhraseType;
  }

  public void setFeaturePhraseType(PhraseType featurePhraseType) {
    this.featurePhraseType = featurePhraseType;
  }

  public PhraseType getObjectPhraseType() {
    return objectPhraseType;
  }

  public void setObjectPhraseType(PhraseType objectPhraseType) {
    this.objectPhraseType = objectPhraseType;
  }

  public PhraseType getIndicatorPhraseType() {
    return indicatorPhraseType;
  }

  public void setIndicatorPhraseType(PhraseType indicatorPhraseType) {
    this.indicatorPhraseType = indicatorPhraseType;
  }

  private PhraseType sentimentPhraseType = null;

  private PhraseType orientationPhraseType = null;

  private PhraseType featurePhraseType = null;

  private PhraseType objectPhraseType = null;

  private PhraseType indicatorPhraseType = null;

  private static HibernateConstantEntities INSTANCE = null;
  
  public synchronized static HibernateConstantEntities getINSTANCE() {
    if(INSTANCE == null){
      INSTANCE = new HibernateConstantEntities();
    }  
    return INSTANCE;
  }

  private HibernateConstantEntities() {
    super();
    this.loadEntities();
  }

  private void loadEntities() {

    DatabaseFacade dbconn = DatabaseFacade.getSingletonFacade();
    Session sentimentSession = dbconn.getHibernateSession();
    
    sentimentLevelDocument = loadSentimentLevelDefinition("document", sentimentSession);
    sentimentLevelSentence = loadSentimentLevelDefinition("sentence", sentimentSession);

    classifierTypeCrisp = loadSentimentClassifierType("Knowledge-Based", "CRISP", sentimentSession);
    classifierTypeFuzzy = loadSentimentClassifierType("Hybrid KnowledgeBased MachineLearning", "FUZZY",
        sentimentSession);
    sentencePhraseType = loadPhraseType("sentimentsentencePhrase", sentimentSession);
    sentimentPhraseType = loadPhraseType("sentimentphrase", sentimentSession);
    orientationPhraseType = loadPhraseType("orientationPhrase", sentimentSession);
    featurePhraseType = loadPhraseType("featurePhrase", sentimentSession);
    objectPhraseType = loadPhraseType("sentimentObjectPhrase", sentimentSession);
    indicatorPhraseType = loadPhraseType("indicatorPhrase", sentimentSession);
    
    checkOntologyVersionField();
  }

  public static SentimentLevelDefinition loadSentimentLevelDefinition(String currentLevel,
      Session sentimentSession) {

    SentimentLevelDefinition level = null;

    Criteria crit = sentimentSession.createCriteria(SentimentLevelDefinition.class);
    crit.add(Restrictions.eq("name", currentLevel).ignoreCase());
    crit.addOrder(Order.asc("id"));

    List<SentimentLevelDefinition> currentSentLevelList = crit.list();
    if (currentSentLevelList.size() >= 1) {
      level = currentSentLevelList.get(0);
    } else {
      log.error("There are no SentimentLevelDefinition in database with level: " + currentLevel);
    }

    return level;
  }

  public SentimentClassifierType loadSentimentClassifierType(String classifierMethod,
      String sentimentType, Session sentimentSession) {
    SentimentClassifierType type = null;

    Criteria crit = sentimentSession.createCriteria(SentimentClassifierType.class);
    crit.add(Restrictions.eq("classifierMethod", classifierMethod).ignoreCase());
    crit.add(Restrictions.eq("sentimentType", sentimentType));
    crit.addOrder(Order.asc("id"));

    List<SentimentClassifierType> currentClassifierTypeList = crit.list();
    if (currentClassifierTypeList.size() >= 1) {
      type = currentClassifierTypeList.get(0);
    } else {
      log.error("There are no SentimentClassifierType in database with classifierMethod: "
          + classifierMethod + " and sentimentType: " + sentimentType);

      type = new SentimentClassifierType();
      type.setClassifierMethod(classifierMethod);
      type.setSentimentType(sentimentType);

      try {
        DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(type);
        log.info("Added new SentimentClassifierType with classifierMethod: " + classifierMethod
            + " and sentimentType: " + sentimentType + " sucessfully");
      } catch (HibernateException ex) {
        log.error("Cannot save new SentimentClassifierType with classifierMethod: " + classifierMethod
            + " and sentimentType: " + sentimentType + "in Database.");
        log.error(ex.getMessage());
      }

    }
    return type;
  }

  public PhraseType loadPhraseType(String phraseTypeName, Session sentimentSession) {
    PhraseType phraseType = null;

    Criteria crit = sentimentSession.createCriteria(PhraseType.class);
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
  
  public DocumentType selectDocumentType(String typeStr) {

    DocumentType type = null;

    Session hibernateSession = DatabaseFacade.getSingletonFacade().getHibernateSession();

    Criteria crit = hibernateSession.createCriteria(DocumentType.class);
    crit.add(Restrictions.like("type", typeStr));
    crit.addOrder(Order.asc("id"));
    List<DocumentType> types = crit.list();

    if (types.size() == 1) {
      type = types.get(0);
      log.info("Documenttype " + type + " exists in database. Extract first with ID: " + type.getId());
    } else {
      if (types.size() > 1) {
        int count = types.size();
        log.debug("There are " + count + " more than one Documenttypes in database with name: "
            + typeStr);
        type = types.get(0);
        log.debug("Setting Documenttype to first id: " + type.getId());
      } else {
        log.debug("Documenttype: " + typeStr + " not exists in Database");
      }
    }

    return type;
  }
  
  /**
   * Check all URL`s containing a versionInfo, which represents an OntologyURL
   * Put into ontologyVersionsMap the versionInfo and the databaseID 
   */
  public void checkOntologyVersionField() {
    Url ontologyURL = null;

    Session hibernateSession = DatabaseFacade.getSingletonFacade().getHibernateSession();
    
    Criteria crit = hibernateSession.createCriteria(Url.class);
    crit.add(Restrictions.isNotNull("versionInfo"));
    List<Url> urlList = crit.list();

    if (!urlList.isEmpty()) {

        ontologyURL = urlList.get(0);
             
        String versionInfo = ontologyURL.getVersionInfo();
        long urlId = ontologyURL.getId();
        ontologyVersionsMap.put(versionInfo, urlId);
        log.debug("Selected ontology with Versioninfo " + versionInfo
            + " in database with ID: " + ontologyURL.getId());

    } else {
      log.debug("Ontology Documentversion with Type \"Ontology\" not exists. Resullist is empty.");
    }
  }

  public HashMap<String, Long> getOntologyVersionsMap() {
    return ontologyVersionsMap;
  }
}
