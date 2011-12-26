package togos.picgrid.file;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;
import togos.picgrid.store.AsyncDataMap;
import junit.framework.TestCase;

public class AsyncSimpleListFile2Test extends TestCase
{
	Random r = new Random();
	
	protected ByteChunk fixedRand( int length ) {
		byte[] data = new byte[length];
		r.nextBytes(data);
		return new SimpleByteChunk(data);
	}
	
	protected ByteChunk rand( int maxLen ) {
		return fixedRand(r.nextInt(maxLen));
	}
	
	File f = new File("junk/async-slf-test.slf2");
	
	public void setUp() throws IOException {
		if( f.exists() ) f.delete();
		else FileUtil.makeParentDirs(f);
	}
	
	public void tearDown() throws IOException {
		f.deleteOnExit();
	}
	
	public void testReadMultithreadedWrite() throws IOException, InterruptedException {
		final int numThreads = 16;
		final int insertsPerThread = 1024;
		final int numEntries = numThreads*insertsPerThread;
		
		final ByteChunk[] keys   = new ByteChunk[numEntries];
		final ByteChunk[] values = new ByteChunk[numEntries];
		
		for( int j=0; j<numEntries; ++j ) {
			keys[j] = fixedRand(32);
			values[j] = rand(2048);
		}
		
		if( f.exists() ) f.delete();
		
		final RandomAccessFileBlob blob = new RandomAccessFileBlob(f, "rw");
		final SimpleListFile2 slf = new SimpleListFile2(blob, 7, true);
		final AsyncDataMap adm = new AsyncDataMap(slf);
		
		try {
			adm.start();
			Thread[] threads = new Thread[numThreads];
			for( int i=0; i<numThreads; ++i ) {
				final int offset = i*insertsPerThread;
				threads[i] = new Thread() {
					public void run() {
						for( int j=0; j<insertsPerThread; ++j ) {
							adm.put( keys[j+offset], values[j+offset] );
						}
					};
				};
				threads[i].start();
			}
			
			for( int i=0; i<numThreads; ++i ) {
				threads[i].join();
			}
			
			for( int j=0; j<numEntries; ++j ) {
				assertEquals( values[j], adm.get(keys[j]) );
			}
			
			adm.haltAndJoin();
			
			for( int j=0; j<numEntries; ++j ) {
				assertEquals( values[j], slf.get(keys[j]) );
			}
		} finally {
			adm.halt();
		}
	}
}
