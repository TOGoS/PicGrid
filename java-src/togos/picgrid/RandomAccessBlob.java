package togos.picgrid;

import java.io.Flushable;

import togos.mf.value.ByteChunk;

public interface RandomAccessBlob extends Flushable
{
	public long getSize();
	public ByteChunk get( long offset, int length );
	public void put( long offset, ByteChunk data );
}