package togos.picgrid.file;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;
import togos.blob.ByteChunk;
import togos.blob.SimpleByteChunk;

public class SimpleListFile2Test extends TestCase
{
	Random r = new Random();
	
	protected ByteChunk rand( int maxLen ) {
		int length = r.nextInt(maxLen);
		byte[] data = new byte[length];
		r.nextBytes(data);
		return new SimpleByteChunk(data);
	}
	
	File f = new File("junk/slf-test.slf2");
	
	public void setUp() throws IOException {
		if( f.exists() ) f.delete();
		else FileUtil.makeParentDirs(f);
	}
	
	public void tearDown() throws IOException {
		f.deleteOnExit();
	}
	
	public void testLongCodec() {
		byte[] buf = new byte[16];
		for( int i=0; i<100; ++i ) {
			long v = r.nextLong();
			int off = r.nextInt(8);
			SimpleListFile2.encodeLong(v, buf, off);
			assertEquals( v, SimpleListFile2.decodeLong(buf, off));
		}
	}
	
	protected void _testReadWrite( boolean lock ) throws IOException {
		if( f.exists() ) f.delete();
		
		RandomAccessFileBlob blob = new RandomAccessFileBlob(f, "rw");
		SimpleListFile2 slf = new SimpleListFile2(blob, 16, lock);
		
		HashMap kv = new HashMap();
		
		for( int i=0; i<1024; ++i ) {
			ByteChunk k = rand( 128 );
			ByteChunk v = rand( 512 );
			kv.put( k, v );
			slf.put( k, v );
			assertEquals( v, slf.get(k) );
		}
		
		for( Iterator i=kv.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			assertEquals( e.getValue(), slf.get((ByteChunk)e.getKey()) );
		}
		
		blob.close();
		blob = new RandomAccessFileBlob(f, "rw");
		slf = new SimpleListFile2(blob, 8, lock);
		
		for( Iterator i=kv.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry e = (Map.Entry)i.next();
			assertEquals( e.getValue(), slf.get((ByteChunk)e.getKey()) );
		}
	}
	
	public void testReadWrite() throws IOException {
		_testReadWrite( false );
	}
	
	public void testReadWriteWithLocks() throws IOException {
		_testReadWrite( true );
	}
}
