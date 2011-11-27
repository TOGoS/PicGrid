package togos.picgrid;

import java.util.Iterator;

import togos.mf.value.ByteBlob;
import togos.mf.value.ByteChunk;

public class SimpleByteBlob implements ByteBlob
{
	ByteChunk c;
	
	public SimpleByteBlob( ByteChunk c ) { this.c = c; }
	public Iterator chunkIterator() { return new SingleItemIterator(c); }
	public long getSize() { return c.getSize(); }
}
