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
package classification;

import gate.util.InvalidOffsetException;
import hibernate.DatabaseFacade;
import hibernate.HibernateConstantEntities;
import hibernate.entities.DegreeOfMembership;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentLevelDefinition;
import hibernate.entities.SentimentObject;

import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import aggregation.NewArticleAssessmentClassificator;

/**
 * Class used for serialization and storage of the Sentiment on Documentlevel 
 * including some helper methods.
 * 
 * @author lgredel, oaltuntas
 *
 */
@XmlType(propOrder = { "sentimentObjectName", "sentimentObjectType", "sentimentObjectFeatureName",
    "sentimentPolarity", "sentimentScoreDocument", "sentimentSentences" })
@XmlRootElement
public class SentimentDocument {
  public static Logger log = Logger.getLogger(SentimentDocument.class);

  private String sentimentObjectType;
  private String sentimentPolarity;
  private String sentimentObjectName;
  private String sentimentObjectFeatureName;

  private float sentimentScoreDocument;

  @XmlTransient
  private Sentiment dbDocumentLevelSentiment = null;
  @XmlTransient
  private SentimentFeatureType sentimentFeature = null;
  @XmlTransient
  private SentimentObject sentimentObject = null;
  @XmlTransient
  private DegreeOfMembership posDom = null;
  @XmlTransient
  private DegreeOfMembership negDom = null;
  @XmlTransient
  private HibernateConstantEntities hibernateConstants = null;
  /**
   * Hashmap that contains all Sentences as SentimentSentence the key is the
   * GateID
   */
  private Map<Integer, SentimentSentence> sentimentSentences;

  public SentimentDocument() {
      hibernateConstants = DatabaseFacade.getSingletonFacade().getConstantEntites();
      this.dbDocumentLevelSentiment = new Sentiment();
  }

public SentimentDocument(DocumentMetaData document,SentimentClassifierType classifierType) {
    this();
    this.dbDocumentLevelSentiment.setDocumentMetaData(document);

    Set<Sentiment> sentimentsSet = document.getSentiments();
    sentimentsSet.add(dbDocumentLevelSentiment);
        
    SentimentLevelDefinition level = hibernateConstants.getSentimentLevelDocument();
    this.dbDocumentLevelSentiment.setSentimentLevelDefinition(level);

    dbDocumentLevelSentiment.setSentimentClassifierType(classifierType);

    DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(document);
    log.info("Created new Sentiment on documentlevel for document with ID: " + document.getId() + " and sentimen on documentLevel: " + dbDocumentLevelSentiment.getId());
  }

  public SentimentDocument(SentimentObject so, SentimentFeatureType sft, SentimentLevelDefinition level,
                           SentimentClassifierType classifierType, DocumentMetaData documentMeta) {
    this();
    this.dbDocumentLevelSentiment.setDocumentMetaData(documentMeta);
    this.dbDocumentLevelSentiment.setSentimentLevelDefinition(level);

    /* Feature of the document */
    this.sentimentFeature = sft;
    this.dbDocumentLevelSentiment.setSentimentFeatureType(sentimentFeature);

    /* Object of the document */
    this.sentimentObject = so;
    this.dbDocumentLevelSentiment.setSentimentObject(sentimentObject);

    this.dbDocumentLevelSentiment.setSentimentClassifierType(classifierType);

    log.info("Created new Sentiment on documentlevel for document with ID: " + documentMeta.getId()
        + " and Sentiment on documentLevel");
  }

  @XmlElement
  private NewMapEntry[] getSentimentSentences() {
    NewMapEntry[] me = null;

    if (sentimentSentences != null) {
      me = new NewMapEntry[sentimentSentences.size()];
      int i = 0;
      for (Integer key : sentimentSentences.keySet())
        me[i++] = new NewMapEntry(key, sentimentSentences.get(key));
    }

    return me;
  }

  public String getSentimentPolarity() {
    return sentimentPolarity;
  }

  public void setSentimentPolarity(String sentimentPolarity) {
    this.sentimentPolarity = sentimentPolarity;
  }

  public Map<Integer, SentimentSentence> getSentimentSentenceMap() {
    return this.sentimentSentences;
  }

  public void setSentimentSentencesMap(Map<Integer, SentimentSentence> sentimentSentenceMap) {
    this.sentimentSentences = sentimentSentenceMap;
  }

  public float calculateSentimentScoreDocument() {
    NewArticleAssessmentClassificator aac = new NewArticleAssessmentClassificator();
    try {
      this.sentimentScoreDocument = aac.extractOpinion(sentimentSentences);
    } catch (InvalidOffsetException e) {
      log.error("Error extraction Sentiment on document " + e.getMessage());
      e.printStackTrace();
    }

    return sentimentScoreDocument;
  }

  public float getSentimentScoreDocument() {
    return sentimentScoreDocument;
  }

  public void setSentimentScoreDocument(float sentimentScoreDocument) {

    this.sentimentScoreDocument = sentimentScoreDocument;
  }

  @XmlTransient
  public Sentiment getDbsentiment() {
    return dbDocumentLevelSentiment;
  }

  /**
   * sets the sentiment and saves it in database
   * 
   * @param dbsentiment
   */
  public void setDbsentiment(Sentiment dbsentiment) {
    DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(dbsentiment);
    this.dbDocumentLevelSentiment = dbsentiment;
  }

  @XmlTransient
  public SentimentFeatureType getSentimentFeature() {
    return sentimentFeature;
  }

  public void setSentimentFeature(SentimentFeatureType sentimentFeature) {
    this.sentimentFeature = sentimentFeature;
  }

  @XmlTransient
  public SentimentObject getSentimentObject() {
    SentimentObject object = dbDocumentLevelSentiment.getSentimentObject();
    return object;
  }

  public void setSentimentObject(SentimentObject sentimentObject) {
    this.sentimentObject = sentimentObject;
  }

  public String getSentimentObjectName() {
    SentimentObject obj = dbDocumentLevelSentiment.getSentimentObject();
    String senObjName = null;
    if (obj != null) {
      senObjName = obj.getName();
    }
    return senObjName;
  }

  public void setSentimentObjectName(String sentimentObjectName) {
    dbDocumentLevelSentiment.getSentimentObject().setName(sentimentObjectName);
    this.sentimentObjectName = sentimentObjectName;
  }

  public String getSentimentObjectFeatureName() {

    SentimentFeatureType obj = dbDocumentLevelSentiment.getSentimentFeatureType();
    String sentFeatureName = null;
    if (obj != null) {
      sentFeatureName = obj.getName();
    }
    return sentFeatureName;
  }

  public void setSentimentObjectFeatureName(String sentimentObjectFeatureName) {
    this.sentimentObjectFeatureName = sentimentObjectFeatureName;
  }

  public String getSentimentObjectType() {
    return sentimentObjectType;
  }

  public void setSentimentObjectType(String sentimentObjectType) {
    this.sentimentObjectType = sentimentObjectType;
  }
  
  public DegreeOfMembership getPosDom() {
    return posDom;
  }

  public void setPosDom(DegreeOfMembership posDom) {
    this.posDom = posDom;
  }

  public DegreeOfMembership getNegDom() {
    return negDom;
  }

  public void setNegDom(DegreeOfMembership negDom) {
    this.negDom = negDom;
  }
  
  private void setSentimentSentences(NewMapEntry[] me) {
    // Convert to our internal map
    for (NewMapEntry entry : me)
      sentimentSentences.put(entry.key, (classification.SentimentSentence) entry.sentence);
  }
}

/**
 * Inner Helper class for serialization of the sentimentSentences-Map
 * 
 * @author lgredel
 *
 * @param <SentimentSentence>
 */
class NewMapEntry<SentimentSentence> {
  @XmlTransient
  public Integer key;
  @XmlElement
  public SentimentSentence sentence;

  @SuppressWarnings("unused")
  private NewMapEntry() {
  }

  public NewMapEntry(Integer key, SentimentSentence sentence) {
    this.key = key;
    this.sentence = sentence;
  }
}
