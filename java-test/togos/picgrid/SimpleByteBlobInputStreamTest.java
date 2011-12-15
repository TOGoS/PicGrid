package togos.picgrid;

import java.util.Random;

import junit.framework.TestCase;
import togos.mf.base.SimpleByteChunk;
import togos.mf.value.ByteBlob;
import togos.picgrid.io.ByteBlobInputStream;

public class SimpleByteBlobInputStreamTest extends TestCase
{
	Random r = new Random();
	
	public void _testReadBytewise(int length) {
		int offset = r.nextInt(4);
		byte[] data = new byte[length+offset];
		r.nextBytes(data);
		ByteBlob blob = new SimpleByteBlob( new SimpleByteChunk( data, offset, length ));
		ByteBlobInputStream bbis = new ByteBlobInputStream(blob.chunkIterator());
		int read = 0;
		for( int i=offset; i<data.length; ++i ) {
			assertEquals( data[i]&0xFF, bbis.read() );
			++read;
		}
		assertEquals( -1, bbis.read() );
		assertEquals( read, blob.getSize() );
	}
	
	public void testRead() {
		for( int i=0; i<64; ++i ) {
			_testReadBytewise(r.nextInt(1024));
		}
	}
}
