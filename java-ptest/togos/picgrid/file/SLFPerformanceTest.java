package togos.picgrid.file;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import togos.mf.base.SimpleByteChunk;
import togos.mf.value.ByteChunk;

public class SLFPerformanceTest
{
	static Random r = new Random();
	
	protected static ByteChunk rand( int maxLen ) {
		int length = r.nextInt(maxLen);
		byte[] data = new byte[length];
		r.nextBytes(data);
		return new SimpleByteChunk(data);
	}
	
	public static void main( String[] args ) {
		try {
			File slfFile  = new File("junk/ptest.slf");
			File slf2File = new File("junk/ptest.slf2");
			File slf2lFile = new File("junk/ptest+locks.slf2");
			FileUtil.makeParentDirs(slfFile);
			
			if( slfFile.exists() ) slfFile.delete();
			if( slf2File.exists() ) slf2File.delete();
			if( slf2lFile.exists() ) slf2lFile.delete();
			
			SimpleListFile  slf  = new SimpleListFile( slfFile, "rw" );
			slf.initIfEmpty( 256, 65536 );
			SimpleListFile2 slf2 = new SimpleListFile2( new RandomAccessFileBlob(slf2File, "rw"), 8, false );
			SimpleListFile2 slf2l = new SimpleListFile2( new RandomAccessFileBlob(slf2lFile, "rw"), 8, true );
			
			long st;
			long totalSlfTime = 0;
			long totalSlf2Time = 0;
			long totalSlf2lTime = 0;
			
			for( int i=0; i<10; ++i ) { // Any higher than 10 and SLF tends to lock up :P
				HashMap values = new HashMap();
				for( int j=0; j<256; ++j ) {
					values.put( rand(128), rand(512) );
				}
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					slf2.put( (ByteChunk)e.getKey(), (ByteChunk)e.getValue() );
				}
				totalSlf2Time += System.currentTimeMillis() - st;
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					// These byte chunks' buffers are the entire chunk, so we can do this:
					slf.put( ((ByteChunk)e.getKey()).getBuffer(), ((ByteChunk)e.getValue()).getBuffer() );
				}
				totalSlfTime += System.currentTimeMillis() - st;
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					slf2l.put( (ByteChunk)e.getKey(), (ByteChunk)e.getValue() );
				}
				totalSlf2lTime += System.currentTimeMillis() - st;
				
				// Read them back!
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					slf2.get( (ByteChunk)e.getKey() );
				}
				totalSlf2Time += System.currentTimeMillis() - st;
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					// These byte chunks' buffers are the entire chunk, so we can do this:
					slf.get( ((ByteChunk)e.getKey()).getBuffer() );
				}
				totalSlfTime += System.currentTimeMillis() - st;
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					slf2l.get( (ByteChunk)e.getKey() );
				}
				totalSlf2lTime += System.currentTimeMillis() - st;
			}
			
			System.out.println("Total SLF time   = "+totalSlfTime+"ms");
			System.out.println("Total SLF2 time  = "+totalSlf2Time+"ms");
			System.out.println("Total SLF2L time = "+totalSlf2lTime+"ms");
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
