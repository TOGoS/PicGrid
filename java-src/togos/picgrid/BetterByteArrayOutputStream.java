package togos.picgrid;

import java.io.ByteArrayOutputStream;

import togos.mf.base.Util;
import togos.mf.value.ByteChunk;

/**
 * Extends ByteArrayOutputStream to provide direct access to the backing
 * buffer and act as a ByteChunk.
 * This can save us from having to do a copy in cases where the backing buffer will
 * be quickly thrown out, anyway. 
 */
public class BetterByteArrayOutputStream extends ByteArrayOutputStream
	implements ByteChunk
{
	public BetterByteArrayOutputStream( byte[] buf ) {
		this.buf = buf;
	}
	
	public BetterByteArrayOutputStream( int size ) {
		super(size);
	}
	
	public BetterByteArrayOutputStream() {
		super();
	}
	
	public byte[] getBuffer() {  return buf;  }
	public int getOffset() {  return 0;  }
	public int getSize() {  return count;  }
	
	public boolean equals( Object other ) {
		if( !(other instanceof ByteChunk) ) return false;
		return Util.equals(this, (ByteChunk)other);
	}
	
	public int hashCode() {
		return Util.hashCode(buf, 0, count);
	}
}
