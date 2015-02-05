package togos.picgrid.file;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;

public class SLFPerformanceTest
{
	static Random r = new Random();
	
	protected static ByteChunk rand( int maxLen ) {
		int length = r.nextInt(maxLen);
		byte[] data = new byte[length];
		r.nextBytes(data);
		return new SimpleByteChunk(data);
	}
	
	protected static String pad( String s, int places ) {
		while( s.length() < places ) {
			s = " "+s;
		}
		return s;
	}
	
	protected static String format( long num, int places ) {
		return pad(Long.toString(num), places);
	}
	
	public static void main( String[] args ) {
		try {
			File slfFile  = new File("junk/ptest.slf");
			File slf2File = new File("junk/ptest.slf2");
			File slf2lFile = new File("junk/ptest+locks.slf2");
			File SmartSlf2File = new File("junk/ptest-Smart.slf2");
			FileUtil.makeParentDirs(slfFile);
			
			if( slfFile.exists() ) slfFile.delete();
			if( slf2File.exists() ) slf2File.delete();
			if( slf2lFile.exists() ) slf2lFile.delete();
			if( SmartSlf2File.exists() ) SmartSlf2File.delete();
			
			int indexLengthPower = 14;
			int indexLength = 1<<indexLengthPower;
			
			SimpleListFile  slf  = new SimpleListFile( slfFile, "rw" );
			slf.initIfEmpty( indexLength, indexLength*8 );
			SimpleListFile2 slf2 = new SimpleListFile2( new RandomAccessFileBlob(slf2File, "rw"), indexLengthPower, false );
			SimpleListFile2 slf2l = new SimpleListFile2( new RandomAccessFileBlob(slf2lFile, "rw"), indexLengthPower, true );
			SimpleListFile2 SmartSlf2 = new SimpleListFile2( new RandomAccessFileBlob(SmartSlf2File, "rw"), indexLengthPower, true );
			
			long st;
			long totalSlfTime = 0;
			long totalSlf2Time = 0;
			long totalSlf2lTime = 0;
			long totalSmartSlf2Time = 0;
			
			for( int i=0; i<200; ++i ) { // Any higher than 10 and SLF tends to lock up :P
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
				
				/*
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					// These byte chunks' buffers are the entire chunk, so we can do this:
					slf.put( ((ByteChunk)e.getKey()).getBuffer(), ((ByteChunk)e.getValue()).getBuffer() );
				}
				totalSlfTime += System.currentTimeMillis() - st;
				*/
				
				st = System.currentTimeMillis();
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ) {
					Map.Entry e = (Map.Entry)j.next();
					slf2l.put( (ByteChunk)e.getKey(), (ByteChunk)e.getValue() );
				}
				totalSlf2lTime += System.currentTimeMillis() - st;
				
				st = System.currentTimeMillis();
				ByteChunk[] SmartKeys = new ByteChunk[values.size()];
				ByteChunk[] SmartValues = new ByteChunk[values.size()];
				int k=0;
				for( Iterator j=values.entrySet().iterator(); j.hasNext(); ++k ) {
					Map.Entry e = (Map.Entry)j.next();
					SmartKeys[k]  = (ByteChunk)e.getKey();
					SmartValues[k] = (ByteChunk)e.getValue();
				}
				SmartSlf2.smartPut( SmartKeys, SmartValues, 0, SmartKeys.length );
				totalSmartSlf2Time += System.currentTimeMillis() - st;
				
				// Read them back!
				
				/*
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
				*/
			}
			
			System.out.println("Total SLF time       = "+format(totalSlfTime     ,8)+"ms");
			System.out.println("Total SLF2 time      = "+format(totalSlf2Time    ,8)+"ms");
			System.out.println("Total SLF2L time     = "+format(totalSlf2lTime   ,8)+"ms");
			System.out.println("Total Smart SLF2 time = "+format(totalSmartSlf2Time,8)+"ms");
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
}
