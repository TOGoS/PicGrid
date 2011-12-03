package togos.picgrid;

import junit.framework.TestCase;
import togos.mf.base.SimpleByteChunk;
import togos.mf.value.ByteBlob;

public class BlobUtilTest extends TestCase
{
	protected ByteBlob mkBlob( String content ) {
		return new SimpleByteBlob( new SimpleByteChunk( content.getBytes() ) );
	}
	
	public void testEquals() {
		ByteBlob testBlob1 = mkBlob("Hello, world!");
		ByteBlob testBlob2 = mkBlob("Hello, world!");
		ByteBlob testBlob3 = mkBlob("Hello, world!\n");
		ByteBlob testBlob4 = mkBlob("Hello, world.");
		ByteBlob testBlob5 = mkBlob("Hello!");
		
		assertTrue( testBlob1.equals(testBlob2) );
		assertTrue( testBlob2.equals(testBlob1) );
		assertFalse( testBlob1.equals(testBlob3) );
		assertFalse( testBlob1.equals(testBlob4) );
		assertFalse( testBlob1.equals(testBlob5) );
		assertFalse( testBlob3.equals(testBlob1) );
	}
}
