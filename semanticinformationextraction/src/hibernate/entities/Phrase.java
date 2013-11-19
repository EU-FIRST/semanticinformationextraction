package hibernate.entities;

// Generated Aug 13, 2012 4:14:16 PM by Hibernate Tools 3.4.0.CR1

import java.util.HashSet;
import java.util.Set;

/**
 * Phrase generated by hbm2java
 */
public class Phrase implements java.io.Serializable {

  private long id;
  private PhraseType phraseType;
  private Integer startnode;
  private Integer endnode;
  private String text;
  private Long negativeDomNumber;
  private Long positiveDomNumber;
  private Integer sentimentScore;
  private Integer distanceToSentimentobjectInTokens;
  private Set sentimentPhraseRelations = new HashSet(0);

  public Phrase() {
  }

  public Phrase(long id, PhraseType phraseType) {
    this.id = id;
    this.phraseType = phraseType;
  }

  public Phrase(long id, PhraseType phraseType, Integer startnode, Integer endnode, String text,
                Long negativeDomNumber, Long positiveDomNumber, Integer sentimentScore,
                Integer distanceToSentimentobjectInTokens, Set sentimentPhraseRelations) {
    this.id = id;
    this.phraseType = phraseType;
    this.startnode = startnode;
    this.endnode = endnode;
    this.text = text;
    this.negativeDomNumber = negativeDomNumber;
    this.positiveDomNumber = positiveDomNumber;
    this.sentimentScore = sentimentScore;
    this.distanceToSentimentobjectInTokens = distanceToSentimentobjectInTokens;
    this.sentimentPhraseRelations = sentimentPhraseRelations;
  }

  public Phrase(PhraseType type,String textPhrase, Integer score,
		int start, int end) {
	  /*Sentiment per sentence*/

		this.phraseType = type;
		this.text = textPhrase;
		this.sentimentScore = score;
		this.startnode = start;
		this.endnode = end;

}

public long getId() {
    return this.id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public PhraseType getPhraseType() {
    return this.phraseType;
  }

  public void setPhraseType(PhraseType phraseType) {
    this.phraseType = phraseType;
  }

  public Integer getStartnode() {
    return this.startnode;
  }

  public void setStartnode(Integer startnode) {
    this.startnode = startnode;
  }

  public Integer getEndnode() {
    return this.endnode;
  }

  public void setEndnode(Integer endnode) {
    this.endnode = endnode;
  }

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public Long getNegativeDomNumber() {
    return this.negativeDomNumber;
  }

  public void setNegativeDomNumber(Long negativeDomNumber) {
    this.negativeDomNumber = negativeDomNumber;
  }

  public Long getPositiveDomNumber() {
    return this.positiveDomNumber;
  }

  public void setPositiveDomNumber(Long positiveDomNumber) {
    this.positiveDomNumber = positiveDomNumber;
  }

  public Integer getSentimentScore() {
    return this.sentimentScore;
  }

  public void setSentimentScore(Integer sentimentScore) {
    this.sentimentScore = sentimentScore;
  }

  public Integer getDistanceToSentimentobjectInTokens() {
    return this.distanceToSentimentobjectInTokens;
  }

  public void setDistanceToSentimentobjectInTokens(Integer distanceToSentimentobjectInTokens) {
    this.distanceToSentimentobjectInTokens = distanceToSentimentobjectInTokens;
  }

  public Set getSentimentPhraseRelations() {
    return this.sentimentPhraseRelations;
  }

  public void setSentimentPhraseRelations(Set sentimentPhraseRelations) {
    this.sentimentPhraseRelations = sentimentPhraseRelations;
  }

  @Override
  public String toString() {
    return "Phrase [id=" + id + ", phraseType=" + phraseType + ", startnode=" + startnode + ", endnode="
        + endnode + ", text=" + text + ", negativeDomNumber=" + negativeDomNumber
        + ", positiveDomNumber=" + positiveDomNumber + ", sentimentScore=" + sentimentScore
        + ", distanceToSentimentobjectInTokens=" + distanceToSentimentobjectInTokens
        + ", sentimentPhraseRelations=" + sentimentPhraseRelations + "]";
  }

  
}