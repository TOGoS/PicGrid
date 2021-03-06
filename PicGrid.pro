-dontwarn scala.**

-keep class * implements org.xml.sax.EntityResolver

-dontobfuscate

-keepclassmembers class * {
    ** MODULE$;
}

-injars target/scala-2.10/classes
-injars resources
-injars ext-lib/scala-library.jar(!META-INF/MANIFEST.MF)
-libraryjars ext-lib/rt.jar(!META-INF/MANIFEST.MF)
-libraryjars ext-lib/junit-3.8.1.jar(!META-INF/MANIFEST.MF)

-outjar PicGrid.jar

# It seems these are necessary for some update(...) methods
# to be included.
-keep class togos.**
-keep class togos.** { *; }

# Notes are useless because we are not obfuscating anything:
-dontnote

-keep public class togos.picgrid.app.PicGridCommand {
    public static void main(java.lang.String[]);
}
