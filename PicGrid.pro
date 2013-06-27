-dontwarn scala.**

-keep class * implements org.xml.sax.EntityResolver

-dontobfuscate

-keepclassmembers class * {
    ** MODULE$;
}

-injars bin
-injars resources
-injars ext-lib/scala-library.jar(!META-INF/MANIFEST.MF)
-libraryjars ext-lib/rt.jar(!META-INF/MANIFEST.MF)
-libraryjars ext-lib/junit-3.8.1.jar(!META-INF/MANIFEST.MF)

-outjar PicGrid.jar

# -keep class togos.**
# -keep class togos.** { *; }

# Notes are useless because we are not obfuscating anything:
-dontnote

-keep public class togos.picgrid.app.PicGridCommand {
    public static void main(java.lang.String[]);
}
