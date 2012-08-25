-dontwarn scala.**

#-keepclasseswithmembers public class * {
#    public static void main(java.lang.String[]);
#}

-keep class * implements org.xml.sax.EntityResolver

-dontobfuscate

-keepclassmembers class * {
    ** MODULE$;
}

#-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinPool {
#    long eventCount;
#    int  workerCounts;
#    int  runControl;
#    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode syncStack;
#    scala.concurrent.forkjoin.ForkJoinPool$WaitQueueNode spareStack;
#}
#
#-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinWorkerThread {
#    int base;
#    int sp;
#    int runState;
#}
#
#-keepclassmembernames class scala.concurrent.forkjoin.ForkJoinTask {
#    int status;
#}
#
#-keepclassmembernames class scala.concurrent.forkjoin.LinkedTransferQueue {
#    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference head;
#    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference tail;
#    scala.concurrent.forkjoin.LinkedTransferQueue$PaddedAtomicReference cleanMe;
#}

-injars bin
-injars resources
-injars /home/stevens/ext-apps/eclipse-3.7-scala-2.1/configuration/org.eclipse.osgi/bundles/208/1/.cp/lib/scala-library.jar
-libraryjars /home/stevens/ext-apps/eclipse-3.7-scala-2.1/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar
-libraryjars /usr/lib/jvm/java-6-openjdk-i386/jre/lib/rt.jar

-outjar PicGrid.jar

-keep public class togos.picgrid.app.PicGridCommand {
    public static void main(java.lang.String[]);
}
