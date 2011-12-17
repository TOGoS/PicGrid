package togos.picgrid.store;

import togos.blob.ByteChunk;

public interface DataMap
{
	public void put( ByteChunk key, ByteChunk value );
	public ByteChunk get( ByteChunk key );
}
