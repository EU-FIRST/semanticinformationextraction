INSERT INTO [FIRST].[DOCUMENT_TYPE]
           ([TYPE]
           ,[DESCRIPTION])
     VALUES
			('preprocessed','Gate-xml preprocessed with UHOH-Document'),
			('txt',NULL),
			('classified','Gate-xml classified with UHOH-Document'),
			('result',NULL),
			('html',NULL),
			('ontology',NULL),
			('JSI-preprocessed','Gate-xml preprocessed with JSI-Document'),
			('JSI-classified','Gate-xml classified with JSI-Document'),
			('gate-xml',NULL)
GO

INSERT INTO [FIRST].[PHRASE_TYPE]
           ([NAME]
           ,[DESCRIPTION])
     VALUES
			('orientationPhrase',NULL),
			('uncertaintyPhrase',NULL),
			('intensifierPhrase',NULL),
			('diminisherPhrase',NULL),
			('sentimentphrase',NULL),
			('sentimentsentencePhrase',NULL),
			('sentimentObjectPhrase',NULL),
			('featurePhrase',NULL),
			('subfeaturePhrase',NULL),
			('indicatorPhrase',NULL)
GO

INSERT INTO [FIRST].[SENTIMENT_FEATURE_TYPE]
           ([NAME]
           ,[Ontology_Concept_URI])
     VALUES
			('expectedFutureReputationChange','http://project-first.eu/FIRSTOntology_ObjectFeature#Reputation'),
			('expectedFuturePriceChange','http://project-first.eu/FIRSTOntology_ObjectFeature#ExpectedFuturePriceChange'),
			('expectedFutureVolatilityChange','http://project-first.eu/FIRSTOntology_ObjectFeature#ExpectedFutureVolatilityChange')
GO

INSERT INTO [FIRST].[SENTIMENT_LEVEL_DEFINITION]
           ([NAME])
     VALUES
           ('sentence'),
			('document')
GO

INSERT INTO [FIRST].[SENTIMENT_CLASSIFIER_TYPE]
	(	CLASSIFIER_METHOD,
		SENTIMENT_TYPE)
VALUES
	('Knowledge-Based','CRISP'),
	('MachineLearning SVM linear','CRISP'),
	('PosNegWordRatio','CRISP'),
	('Hybrid KnowledgeBased MachineLearning','CRISP'),
	('Hybrid KnowledgeBased MachineLearning','FUZZY'),
	('MachineLearning SVM linear','FUZZY')
GO

INSERT INTO [FIRST].[DEGREE_OF_MEMBERSHIP]
	(	LABEL,
		LOWER_BOUND,
		UPPER_BOUND,
		SCORE_AVERAGE)
VALUES
	('no amount','0.0','0.2','0.1'),
	('small amount','0.2','0.4','0.3'),
	('moderate amount','0.4','0.6','0.5'),
	('large amount','0.6','0.8','0.7'),
	('maximum amount','0.8','1.0','0.9'),
	('n/a',NULL,NULL,NULL)
GO