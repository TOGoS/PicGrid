package togos.picgrid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import togos.mf.base.SimpleByteChunk;
import togos.picgrid.io.ByteBlobInputStream;

public class ByteBlobInputStreamTest extends TestCase
{
	List chunks;
	ByteBlobInputStream bbis;
	
	public void setUp() {
		chunks = new ArrayList();
		chunks.add( new SimpleByteChunk( new byte[]{0,1,2,3} ) );
		chunks.add( new SimpleByteChunk( new byte[]{4,5,6,7,8} ) );
		chunks.add( new SimpleByteChunk( new byte[]{} ) );
		chunks.add( new SimpleByteChunk( new byte[]{9} ) );
		chunks.add( new SimpleByteChunk( new byte[]{10,11,12,13,14,15,-1} ) );
		chunks.add( new SimpleByteChunk( new byte[]{} ) );
		bbis = new ByteBlobInputStream( chunks.iterator() );
	}
	
	public void testRead() {
		assertEquals(  0, bbis.read() );
		assertEquals(  1, bbis.read() );
		assertEquals(  2, bbis.read() );
		assertEquals(  3, bbis.read() );
		assertEquals(  4, bbis.read() );
		assertEquals(  5, bbis.read() );
		assertEquals(  6, bbis.read() );
		assertEquals(  7, bbis.read() );
		assertEquals(  8, bbis.read() );
		assertEquals(  9, bbis.read() );
		assertEquals( 10, bbis.read() );
		assertEquals( 11, bbis.read() );
		assertEquals( 12, bbis.read() );
		assertEquals( 13, bbis.read() );
		assertEquals( 14, bbis.read() );
		assertEquals( 15, bbis.read() );
		assertEquals( 255, bbis.read() );
		assertEquals( -1, bbis.read() );
	}
	
	protected void assertExactBytesRead( InputStream is, int bufsize, byte[] dat ) throws IOException {
		byte[] buf = new byte[bufsize];
		int z = is.read( buf );
		if( dat == null ) {
			assertEquals( -1, z );
		} else {
			assertEquals( dat.length, z );
			for( int i=0; i<z; ++i ) {
				assertEquals( dat[i], buf[i] );
			}
		}
	}
	
	public void testMultiread() throws IOException {
		assertExactBytesRead( bbis, 3, new byte[]{0,1,2} );
		assertExactBytesRead( bbis, 1, new byte[]{3} );
		assertExactBytesRead( bbis, 6, new byte[]{4,5,6,7,8} );
		assertExactBytesRead( bbis, 6, new byte[]{9} );
		assertExactBytesRead( bbis, 10, new byte[]{10,11,12,13,14,15,-1} );
		assertExactBytesRead( bbis, 10, null );
	}
}
