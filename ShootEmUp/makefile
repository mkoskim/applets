PROJECT=ShootEmUp

# --------------------------------------------------------------------
# Windows
# --------------------------------------------------------------------
#JAVAC=c:/progra~1/java/jdk1.5.0_02/bin/javac -Xlint:all -source 1.4 
#JAR=c:/progra~1/java/jdk1.5.0_02/bin/jar
#DELETE=del classfiles\*.class

# --------------------------------------------------------------------
# Linux
# --------------------------------------------------------------------
JAVAC=javac -Xlint:all
JAR=jar
DELETE=rm -f classfiles/*.class

# --------------------------------------------------------------------
# Common
# --------------------------------------------------------------------

jar/$(PROJECT).jar: FORCE
	$(DELETE)
	$(JAVAC) -d classfiles sources/*.java
	$(JAR) cmf Manifest.txt jar/$(PROJECT).jar -C classfiles .

FORCE:
