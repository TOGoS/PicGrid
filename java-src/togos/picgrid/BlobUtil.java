package togos.picgrid;

import togos.mf.value.ByteBlob;
import togos.picgrid.io.ByteBlobInputStream;

public class BlobUtil
{
	/**
	 * Don't compare blobs this way.
	 */
	public static boolean equals( ByteBlob b1, ByteBlob b2 ) {
		if( b1.getSize() != -1 && b2.getSize() != -1 && b1.getSize() != b2.getSize() ) return false;
		
		int r1;
		ByteBlobInputStream s1 = new ByteBlobInputStream(b1.chunkIterator());
		ByteBlobInputStream s2 = new ByteBlobInputStream(b2.chunkIterator());
		do {
			r1 = s1.read();
			if( s2.read() != r1 ) return false;
		} while( r1 != -1 );
		return true;
	}
}
