package togos.picgrid.build;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

class PicGridBuilder
{
	public static long latestModificationTime( File f ) {
		if( !f.exists() ) return 0;
		if( f.isDirectory() ) {
			long maxTime = 0;
			for( File sf : f.listFiles() ) {
				long t = latestModificationTime(sf);
				if( t > maxTime ) maxTime = t;
			}
			return maxTime;
		}
		return f.lastModified();
	}
	
	protected static boolean isNewerThan( File target, File...sources ) {
		if( !target.exists() ) return false;
		long targetMod = latestModificationTime(target);
		for( File s : sources ) {
			if( !s.exists() || latestModificationTime(s) > targetMod ) return false;
		}
		return true;
	}
	
	protected static void mkParentDirs( File f ) {
		File p = f.getParentFile();
		if( p != null && !p.exists() ) p.mkdirs();
	}
	
	protected static void rmdir( File f ) {
		if( f.isDirectory() ) {
			for( File g : f.listFiles() ) rmdir(g);
		}
		f.delete();
	}
	
	protected static File tempFile( File dest ) {
		File parent = dest.getParentFile();
		String tempName = "."+System.currentTimeMillis()+"-"+dest.getName();
		return new File( parent, tempName );
	}
	
	protected static void copy( File src, File dest ) throws IOException {
		mkParentDirs(dest);
		File temp = tempFile(dest);
		FileInputStream fis = new FileInputStream(src);
		try {
			FileOutputStream fos = new FileOutputStream(dest);
			try {
				byte[] buffer = new byte[65536];
				int r;
				while( (r = fis.read(buffer)) > 0 ) {
					fos.write(buffer, 0, r);
				}
			} finally {
				fos.close();
			}
		} finally {
			fis.close();
		}
		if( dest.exists() ) dest.delete();
		temp.renameTo(dest);
	}
	
	protected static Process exec( String cmd, Object...args ) throws IOException {
		String[] argstrings = new String[args.length];
		for( int i=0; i<args.length; ++i ) argstrings[i] = args[i].toString();
		return Runtime.getRuntime().exec( cmd, argstrings );
	}
	
	protected static List<File> find( File root, String extension, List<File> addHere ) {
		if( root.isDirectory() ) {
			for( File r : root.listFiles() ) find( r, extension, addHere );
		} else if( root.getName().endsWith(extension) ) {
			addHere.add(root);
		}
		return addHere;
	}
	
	protected static List<File> find( File root, String extension ) {
		return find( root, extension, new ArrayList<File>() );
	}
	
	protected static void writeList( List<?> fileList, File writeTo ) throws IOException {
		OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(writeTo));
		for( Object item : fileList ) w.write( item.toString() + "\n" );
		w.close();
	}
	
	File localRepo = new File(System.getProperty("user.home")+"/.ccouch");
	
	Pattern SHA1_PATTERN     = Pattern.compile("urn:sha1:([A-Z0-9]{32})");
	Pattern BITPRINT_PATTERN = Pattern.compile("urn:bitprint:([A-Z0-9]{32})\\.([A-Z0-9]{39})");
	
	protected File findBlob( String urn ) {
		String sha1Base32 = null;
		Matcher m;
		if( (m = SHA1_PATTERN.matcher(urn)).matches() || (m = BITPRINT_PATTERN.matcher(urn)).matches() ) {
			sha1Base32 = m.group(1);
		}
		if( sha1Base32 == null ) {
			System.err.println("Can't locate blob: Unrecongised URN: "+urn);
			return null;
		}
		String sectorPostfix = sha1Base32.substring(0,2)+"/"+sha1Base32;
		File sectorParentDir = new File( localRepo + "/data" );
		File[] sectors = sectorParentDir.listFiles();
		for( File sector : sectors ) {
			File blob = new File(sector, sectorPostfix);
			if( blob.exists() ) return blob;
		}
		return null;
	}
	
	protected File checkout( String urn, File dest ) throws IOException {
		if( dest.exists() ) return dest;
		File blob = findBlob(urn);
		if( blob == null ) {
			throw new RuntimeException("Could not locate blob: "+urn);
		}
		copy( blob, dest );
		return dest;
	}
	
	////

	File bin        = new File("bin");
	File javaSrc    = new File("java-src");
	File javaTest   = new File("java-test");
	File scalaSrc   = new File("scala-src");
	File scalaTest  = new File("scala-test");
	
	File proguardJar        = new File("ext-lib/proguard.jar");
	File scalaLibraryJar    = new File("ext-lib/scala-library.jar");
	File scalaEverythingJar = new File("ext-lib/scala-everything.jar");
	File rtJar              = new File("ext-lib/rt.jar");
	File picGridJar         = new File("PicGrid.jar");
	
	String proguardJarUrn        = "urn:bitprint:MYGRCO7NRSUP6ZKWDXXJRAS2YDV753FF.LEQHGVMGMQFZDJDB3XHI7UFS2QWKWDFHBHSNMDA";
	String scalaLibraryJarUrn    = "urn:bitprint:YJEYYZSC2K2MYKICPTMBY42MDY46FEQD.YLSD3HUOKLUQPOUJ5ESYLW7KTPXKT2G7QMBSM7A";
	String scalaEverythingJarUrn = "urn:bitprint:EO3DIVRTIU5WYR2CFWS72X6GU6CNRC2K.FXSDPXBZQZIYBJ4YC2M6PVUD5O2PNTDSOHHKOSA";
	String rtJarUrn              = "urn:bitprint:QTSCSZTEFHMDN4HJVC55TBRGSP4IT44L.ODXTBENAOEIJ4RIR4YIDVTGB2JLCGFBP4TVGQUQ";
	
	public File buildProguardJar() throws Exception {
		return checkout( proguardJarUrn, proguardJar );
	}
	public File buildScalaLibraryJar() throws Exception {
		return checkout( scalaLibraryJarUrn, scalaLibraryJar );
	}
	public File buildScalaEverythingJar() throws Exception {
		return checkout( scalaEverythingJarUrn, scalaEverythingJar );
	}
	
	public File buildBin() throws Exception {
		if( isNewerThan(bin, javaSrc, javaTest, scalaSrc, scalaTest) ) return bin;
		
		rmdir( bin );
		bin.mkdirs();
		
		writeList( find(javaSrc ,".java" ,find(javaTest, ".java" )), new File(".java-src.lst") );
		Process javac  = exec( "javac", "-classpath", "bin", "-d", "bin", "-deprecation", "@.java-src.lst" );
		writeList( find(scalaSrc,".scala",find(scalaTest,".scala")), new File(".scala-src.lst") );
		javac.waitFor();
		Process scalac = exec( "java", "-cp", "ext-lib/scala-everything.jar", "scala.tools.nsc.Main",
							   "-classpath", "bin", "-d", "bin", "-deprecation", "@.java-src.lst" );
		scalac.waitFor();
		return bin;
	}
	
	public File buildPicGridJar() throws Exception {
		if( isNewerThan(picGridJar, buildBin()) ) return picGridJar;
		
		Process pro = exec( "java", "-jar", buildProguardJar(), "@PicGrid.pro" );
		pro.waitFor();
		
		return picGridJar;
	}
	
	public int run( String[] args ) throws Exception {
		buildPicGridJar();
		return 0;
	}
	
	public static void main( String[] args ) throws Exception {
		System.exit(new PicGridBuilder().run( args ));
	}
}
