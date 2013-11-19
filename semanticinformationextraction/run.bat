TITLE Completepipeline
COLOR 0C
java -XX:MaxPermSize=128m -XX:+HeapDumpOnOutOfMemoryError -Dlog4j.configuration=file:/'PATH-TO-log4j'/resources/config/log4j.xml -XX:-UseGCOverheadLimit -jar CompleteJSIKnowledgeBasedCrispClassificationReceiver.jar