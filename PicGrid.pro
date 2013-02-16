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

-outjar PicGrid.jar

-keep class togos.**
-keep class togos.** { *; }

# Notes are useless because we are not obfuscating anything:
-dontnote

# Someone on the internet said this about something:
#-dontskipnonpubliclibraryclassmembers

-keep public class togos.picgrid.app.PicGridCommand {
    public static void main(java.lang.String[]);
}
