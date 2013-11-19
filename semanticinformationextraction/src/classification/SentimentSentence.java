
package classification;

import hibernate.DatabaseFacade;
import hibernate.entities.DocumentMetaData;
import hibernate.entities.Phrase;
import hibernate.entities.PhraseType;
import hibernate.entities.Sentiment;
import hibernate.entities.SentimentClassifierType;
import hibernate.entities.SentimentFeatureType;
import hibernate.entities.SentimentIndicator;
import hibernate.entities.SentimentLevelDefinition;
import hibernate.entities.SentimentObject;
import hibernate.entities.SentimentPhraseRelation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

@XmlType(propOrder = { "sentimentSentenceType", "sentimentSentenceText", "sentimentPhraseText",
    "sentimentPolarity", "sentimentScore", "sentimentObjectPhraseText", "indicatorConcept",
    "indicatorPolarity", "indicatorCorrelation", "orientationPhraseText", "orientationTermScore",
    "indicatorPhrase", "sentimentSubFeatureType" })
/**
 * Class used for serialization and storage of the Sentiment on Sentencelevel 
 * including some helper methods.
 * 
 * @author lgredel, oaltuntas
 *
 */
public class SentimentSentence {
  private long startNode;
  private long endNode;
  private String sentimentPolarity;
  private String orientationTermScore;

  private String sentimentSentenceType; //direct | indirect
  private String sentimentSentenceText;
  private String sentimentObjectPhraseText;
  private String indicatorConcept;
  /*
   * If the sentence has an indirect sentiment (indicator), the polarity of the
   * Orientationterm complies with the score of the indicator
   */
  private String indicatorPolarity;
  private String indicatorCorrelation;
  private String indicatorPhrase;

  private Integer sentimentScore;

  private String sentimentPhraseText;

  /*********** Database-Properties **************/
  private Sentiment dbSentenceLevelSentiment;
  private SentimentObject sentimentObject;
  private SentimentFeatureType sentimentFeatureType;
  private SentimentFeatureType sentimentSubFeatureType;
  private Phrase sentimentObjectPhrase;
  private Phrase sentencePhrase;
  private Phrase sentimentSentencePhrase;
  private Phrase sentimentFeaturePhrase;
  private Phrase sentimentIndicatorPhrase;

  private List<Phrase> orientationTermPhraseList = new ArrayList<Phrase>();;
  private SentimentIndicator indicator;

  @XmlTransient
  private static Logger log = Logger.getLogger(SentimentSentence.class);

  public SentimentSentence() {
    super();
  }

  public SentimentSentence(Long startSentenceNode, Long endSentenceNode, SentimentObject so,
                           SentimentFeatureType sft, SentimentLevelDefinition level,
                           SentimentClassifierType type, DocumentMetaData docMeta, int score,
                           String polarity, SentimentFeatureType sentimentSubFeature,
                           SentimentIndicator sentimentIndicator) {
    super();

    this.dbSentenceLevelSentiment = new Sentiment();
    this.dbSentenceLevelSentiment.setDocumentMetaData(docMeta);

    this.startNode = startNode;
    this.endNode = endNode;

    this.dbSentenceLevelSentiment.setSentimentLevelDefinition(level);

    this.dbSentenceLevelSentiment.setSentimentClassifierType(type);

    this.dbSentenceLevelSentiment.setSentimentObject(so);
    this.setSentimentScore(score);
    this.sentimentPolarity = polarity;

    this.dbSentenceLevelSentiment.setSentimentFeatureType(sft);
    this.dbSentenceLevelSentiment.setScore(new BigDecimal(score));
    this.dbSentenceLevelSentiment.setSentimentSubFeatureType(sentimentSubFeature);
    this.dbSentenceLevelSentiment.setIndicator(sentimentIndicator);
  }

  @XmlTransient
  public long getStartNode() {
    return startNode;
  }

  public void setStartNode(long startNode) {
    this.sentencePhrase.setStartnode(new Long(startNode).intValue());
    this.startNode = startNode;
  }

  @XmlTransient
  public long getEndNode() {
    return endNode;
  }

  public void setEndNode(long endNode) {
    this.sentencePhrase.setEndnode(new Long(endNode).intValue());
    this.endNode = endNode;
  }

  public List<Phrase> getOrientationTermPhraseList() {
    return orientationTermPhraseList;
  }

  public void setOrientationTermPhraseList(List<Phrase> orientationTermPhraseList) {
    this.orientationTermPhraseList = orientationTermPhraseList;
  }

  public Phrase getSentimentIndicatorPhrase() {
    return sentimentIndicatorPhrase;
  }

  public void setSentimentIndicatorPhrase(Phrase sentimentIndicatorPhrase) {
    this.sentimentIndicatorPhrase = sentimentIndicatorPhrase;
  }

  public String getSentimentSentenceText() {
    String sentenceText = sentencePhrase.getText();

    return sentenceText;
  }

  public void setSentimentSentenceText(String sentimentSentenceText) {
    sentencePhrase.setText(sentimentPhraseText);
    this.sentimentSentenceText = sentimentSentenceText;
  }

  public String getIndicatorConcept() {
    return indicatorConcept;
  }

  public void setIndicatorConcept(String indicatorConcept) {
    this.indicatorConcept = indicatorConcept;
  }

  public String getIndicatorPolarity() {
    return indicatorPolarity;
  }

  public void setIndicatorPolarity(String indicatorPolarity) {

    this.indicatorPolarity = indicatorPolarity;
  }

  public String getIndicatorCorrelation() {
    return indicatorCorrelation;
  }

  public void setIndicatorCorrelation(String indicatorCorrelation) {
    this.indicatorCorrelation = indicatorCorrelation;
  }

  public String getIndicatorPhrase() {
    return indicatorPhrase;
  }

  public void setIndicatorPhrase(String indicatorPhrase) {
    this.indicatorPhrase = indicatorPhrase;
  }

  public Integer getSentimentScore() {
    Integer score = dbSentenceLevelSentiment.getScore().intValue();
    return score;
  }

  public void setSentimentScore(Integer sentimentScore) {

    this.dbSentenceLevelSentiment.setScore(new BigDecimal(sentimentScore));
    this.sentimentScore = sentimentScore;
  }

  public String getSentimentPolarity() {
    return sentimentPolarity;
  }

  public void setSentimentPolarity(String sentimentPolarity) {
    this.sentimentPolarity = sentimentPolarity;
  }

  public String getSentimentSentenceType() {
    return sentimentSentenceType;
  }

  public void setSentimentSentenceType(String sentimentSentenceType) {
    this.sentimentSentenceType = sentimentSentenceType;
  }

  public void setSentimentPhrase(String sentimentPhrase) {
    this.sentimentPhraseText = sentimentPhrase;
  }

  @XmlTransient
  public Sentiment getSentimentSentence() {
    return dbSentenceLevelSentiment;
  }

  public void setSentimentSentence(Sentiment sentimentSentence) {
    this.dbSentenceLevelSentiment = sentimentSentence;
  }

  @XmlTransient
  public Phrase getSentencePhrase() {
    return sentencePhrase;
  }

  public void setSentencePhrase(Phrase sentencePhrase) {
    this.sentencePhrase = sentencePhrase;
  }

  @XmlTransient
  public SentimentObject getSentimentObject() {
    return sentimentObject;
  }

  public void setSentimentObject(SentimentObject sentimentObject) {
    this.sentimentObject = sentimentObject;
  }

  @XmlTransient
  public SentimentFeatureType getSentimentFeatureType() {
    return sentimentFeatureType;
  }

  public void setSentimentFeatureType(SentimentFeatureType sentimentFeatureType) {
    this.sentimentFeatureType = sentimentFeatureType;
  }

  public String getSentimentPhraseText() {
    return sentimentPhraseText;
  }

  public void setSentimentPhraseText(String sentimentPhraseText) {
    this.sentimentPhraseText = sentimentPhraseText;
  }

  public String getSentimentObjectPhraseText() {
    if (sentimentObjectPhrase != null) {
      String text = sentimentObjectPhrase.getText();
      if (text != null) {
        sentimentObjectPhraseText = text;
      }
    }

    return sentimentObjectPhraseText;
  }

  public Phrase getSentimentFeaturePhrase() {
    return sentimentFeaturePhrase;
  }

  public void setSentimentFeaturePhrase(Phrase sentimentFeaturePhrase) {
    this.sentimentFeaturePhrase = sentimentFeaturePhrase;
  }

  public void setSentimentObjectPhraseText(String sentimentObjectPhraseText) {

    sentimentObjectPhrase.setText(sentimentObjectPhraseText);
    this.sentimentObjectPhraseText = sentimentObjectPhraseText;
  }

  @XmlTransient
  public Phrase getSentimentObjectPhrase() {

    Set<Phrase> sentimentSOPhraseRelation = dbSentenceLevelSentiment.getSentimentPhraseRelations();
    Iterator<Phrase> it = sentimentSOPhraseRelation.iterator();
    while (it.hasNext()) {
      Phrase current = (Phrase) it.next();
      PhraseType soPhraseType = DatabaseFacade.getSingletonFacade().getPhraseType(
          "sentimentObjectPhrase");
      if (current.getPhraseType() == soPhraseType) {
        sentimentObjectPhrase = current;
        break;
      }
    }

    return sentimentObjectPhrase;
  }

  public void setSentimentObjectPhrase(Phrase sentimentObjectPhrase) {
    this.sentimentObjectPhrase = sentimentObjectPhrase;
  }

  @XmlTransient
  public Phrase getSentimentSentencePhrase() {

    Set<Phrase> sentimentSOPhraseRelation = dbSentenceLevelSentiment.getSentimentPhraseRelations();
    Iterator<Phrase> it = sentimentSOPhraseRelation.iterator();
    while (it.hasNext()) {
      Phrase current = (Phrase) it.next();
      PhraseType soPhraseType = DatabaseFacade.getSingletonFacade().getPhraseType(
          "sentimentsentencePhrase");
      if (current.getPhraseType() == soPhraseType) {
        sentimentSentencePhrase = current;
        break;
      }
    }
    return sentimentSentencePhrase;
  }

  public void setSentimentSentencePhrase(Phrase sentimentSentencePhrase) {

    this.sentimentSentencePhrase = sentimentSentencePhrase;
  }

  @XmlElement(name = "orientationTermPolarity")
  public String getOrientationTermScore() {
    return orientationTermScore;
  }

  public void setOrientationTermScore(String orientationTermScore) {
    this.orientationTermScore = orientationTermScore;
  }

  @XmlTransient
  public SentimentIndicator getIndicator() {
    indicator = this.dbSentenceLevelSentiment.getIndicator();
    return indicator;
  }

  public void setSentimentSentenceIndicator(SentimentIndicator indicator) {

    this.indicator = indicator;

    this.indicatorConcept = indicator.getName();

    Integer correlation = indicator.getCorrelationDefinition();

    if (correlation == 1) {
      this.indicatorCorrelation = "positive";
    } else {
      if (correlation == -1) {
        this.indicatorCorrelation = "negative";
      }
    }
  }

  public SentimentFeatureType getSentimentSubFeatureType() {
    return sentimentSubFeatureType;
  }

  public Sentiment getDbSentenceLevelSentiment() {
    return dbSentenceLevelSentiment;
  }

  private SentimentPhraseRelation createPhraseRelation(Phrase phrase) {

    /* n-m relation between phrase and sentiment */
    SentimentPhraseRelation sentPhraseRelation = new SentimentPhraseRelation();
    sentPhraseRelation.setPhrase(phrase);
    sentPhraseRelation.setSentiment(dbSentenceLevelSentiment);

    return sentPhraseRelation;
  }

  public void setAllSentimentPhraseRelations() {

    SentimentPhraseRelation sentPhraseRelation = null;
    sentPhraseRelation = this.createPhraseRelation(sentencePhrase);
    DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(sentPhraseRelation);

    Iterator<Phrase> iterator = orientationTermPhraseList.iterator();

    while (iterator.hasNext()) {
      Phrase phrase = (Phrase) iterator.next();
      sentPhraseRelation = this.createPhraseRelation(phrase);
      DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(sentPhraseRelation);
    }

    //save SentimentFeaturePhrase
    if (sentimentFeaturePhrase != null) {
      sentPhraseRelation = this.createPhraseRelation(sentimentFeaturePhrase);
      DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(sentPhraseRelation);
    }

    if (this.sentimentSentenceType.equalsIgnoreCase("direct") && (sentimentObjectPhrase != null)) {
      //save SentimentObjectPhrase
      sentPhraseRelation = this.createPhraseRelation(sentimentObjectPhrase);
      DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(sentPhraseRelation);
    } else {
      if (this.sentimentSentenceType.equalsIgnoreCase("indirect")) {
        // save SentimentIndicatorPhrase
        sentPhraseRelation = this.createPhraseRelation(sentimentIndicatorPhrase);
        DatabaseFacade.getSingletonFacade().saveOrUpdateObjectToDatabase(sentPhraseRelation);
      }
    }
  }
}
