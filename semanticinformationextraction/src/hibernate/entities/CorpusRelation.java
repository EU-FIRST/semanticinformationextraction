package hibernate.entities;

// Generated Aug 13, 2012 4:14:16 PM by Hibernate Tools 3.4.0.CR1

/**
 * CorpusRelation generated by hbm2java
 */
public class CorpusRelation implements java.io.Serializable {

  private long id;
  private DocumentMetaData documentMetaData;
  private CorpusMetaData corpusMetaData;

  public CorpusRelation() {
  }

  public CorpusRelation(long id) {
    this.id = id;
  }

  public CorpusRelation(long id, DocumentMetaData documentMetaData, CorpusMetaData corpusMetaData) {
    this.id = id;
    this.documentMetaData = documentMetaData;
    this.corpusMetaData = corpusMetaData;
  }

  public long getId() {
    return this.id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public DocumentMetaData getDocumentMetaData() {
    return this.documentMetaData;
  }

  public void setDocumentMetaData(DocumentMetaData documentMetaData) {
    this.documentMetaData = documentMetaData;
  }

  public CorpusMetaData getCorpusMetaData() {
    return this.corpusMetaData;
  }

  public void setCorpusMetaData(CorpusMetaData corpusMetaData) {
    this.corpusMetaData = corpusMetaData;
  }

}