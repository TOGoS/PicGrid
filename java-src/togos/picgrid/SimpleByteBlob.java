package togos.picgrid;

import java.util.Iterator;

import togos.mf.value.ByteBlob;
import togos.mf.value.ByteChunk;

public final class SimpleByteBlob implements ByteBlob
{
	public final ByteChunk chunk;
	
	public SimpleByteBlob( ByteChunk c ) { this.chunk = c; }
	public Iterator chunkIterator() { return new SingleItemIterator(chunk); }
	public long getSize() { return chunk.getSize(); }
	
	public boolean equals( Object o ) {
		return o instanceof ByteBlob ? BlobUtil.equals( this, (ByteBlob)o ) : false;
	}
}
