package togos.picgrid;

import togos.mf.value.ByteChunk;

public interface DataMap
{
	public void put( ByteChunk key, ByteChunk value );
	public ByteChunk get( ByteChunk key );
}
