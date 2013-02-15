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
-injars ext-lib/scala-library.jar(!META-INF/MANIFEST.MF)
#-injars /home/stevens/ext-apps/eclipse-3.7-scala-2.1/configuration/org.eclipse.osgi/bundles/208/1/.cp/lib/scala-library.jar(!META-INF/MANIFEST.MF)
#-libraryjars /home/stevens/ext-apps/eclipse-3.7-scala-2.1/plugins/org.junit_3.8.2.v3_8_2_v20100427-1100/junit.jar(!META-INF/MANIFEST.MF)
-libraryjars ext-lib/rt.jar(!META-INF/MANIFEST.MF)

-outjar PicGrid.jar

-keep class togos.**
-keep class togos.** { *; }

# ProGuard complains about these.
# Maybe we could ignore them?
#-keep class scala.Function1
#-keep class scala.Tuple2
#-keep class scala.runtime.IntRef
#-keep class scala.runtime.FloatRef
#-keep class scala.runtime.DoubleRef
#-keep class scala.runtime.ObjectRef
#-keep class scala.collection.Seq
#-keep class scala.collection.immutable.List
#-keep class scala.collection.mutable.ListBuffer
#-keep class scala.collection.mutable.ArrayBuffer
# Since we're not obfuscating anything, the notes are useless
-dontnote

# Someone on the internet said this about something:
#-dontskipnonpubliclibraryclassmembers

-keep public class togos.picgrid.app.PicGridCommand {
    public static void main(java.lang.String[]);
}
