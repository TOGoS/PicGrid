-injars       resources
-injars	      bin
-injars	      C:\apps\scala-2.9.1.final/lib/scala-library.jar(!META-INF/MANIFEST.MF)
-libraryjars  <java.home>/lib/rt.jar(!META-INF/MANIFEST.MF)
-outjars      PicGrid-compressed.jar

-keep public class togos.picgrid.app.PicGridCommand {
	public void main( java.lang.String[] );
}

-dontobfuscate
-dontwarn
-ignorewarnings
