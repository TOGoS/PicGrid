package togos.blob;

import java.util.Iterator;

import togos.blob.util.SingleItemIterator;
import togos.blob.util.BlobUtil;

public final class SingleChunkByteBlob implements ByteBlob
{
	public final ByteChunk chunk;
	
	public SingleChunkByteBlob( ByteChunk c ) { this.chunk = c; }
	public Iterator chunkIterator() { return new SingleItemIterator(chunk); }
	public long getSize() { return chunk.getSize(); }
	
	public boolean equals( Object o ) {
		return o instanceof ByteBlob && BlobUtil.equals( this, (ByteBlob)o );
	}
}
