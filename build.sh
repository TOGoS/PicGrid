#!/bin/sh
# Based on script from http://www.eishay.com/2010/06/scala-on-eclipse-without-plugin.html
# Create a classpath from the eclipse .classpath file
lib=`grep classpathentry .classpath | grep "kind=\"lib\"" | tr -d '\t' | sed 's/.*path=\"\(.*[^src]\.jar\)\".*/\1/' | grep -v classpathentry`
CLASSPATH=`echo ${lib} | sed 's/ /:/g' `  
CLASSPATH=$CLASSPATH:/home/stevens/ext-apps/eclipse-3.7/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar
 
# point SCALA_HOME to Scala home (might want to add it to your project as well)  
#export SCALA_HOME=lib-tools/scala-2.8.0
#export SCALA_HOME=/home/stevens/ext-apps/scala-2.9.1.final
export SCALA_HOME=/home/stevens/ext-apps/scala-2.10.1-RC1
 
# java opts for your compilation server
export JAVA_OPTS="-client -Xmx1024M -Xms256M -XX:PermSize=128m -Xss2M -XX:MaxPermSize=256m -Xverify:none -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled"
 
DEST=bin
mkdir -p $DEST

JAVAC_CMD=javac.cmd
echo "-classpath $CLASSPATH:bin" > $JAVAC_CMD
echo "-d $DEST -deprecation" >> $JAVAC_CMD
find java-src -name *.java >> $JAVAC_CMD

javac @$JAVAC_CMD

SCALAC_CMD=scalac.cmd
echo "-classpath $CLASSPATH:bin" > $SCALAC_CMD
echo "-d $DEST -deprecation" >> $SCALAC_CMD
find java-src java-test scala-src -name *.java -o -name *.scala >> $SCALAC_CMD
 
time $SCALA_HOME/bin/scalac @$SCALAC_CMD

# proguard @PicGrid.pro
