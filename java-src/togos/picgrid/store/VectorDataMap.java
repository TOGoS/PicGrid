package togos.picgrid.store;

import togos.blob.ByteChunk;

public interface VectorDataMap extends DataMap
{
	public void multiPut( ByteChunk[] keys, ByteChunk[] values, int offset, int count );
	public void multiGet( ByteChunk[] keys, ByteChunk[] values, int offset, int count );
}
