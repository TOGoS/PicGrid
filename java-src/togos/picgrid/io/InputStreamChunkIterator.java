package togos.picgrid.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import togos.mf.base.SimpleByteChunk;

class InputStreamChunkIterator implements Iterator, Closeable
{
	InputStream is;
	boolean eofReached;
	static int MAX_CHUNK_SIZE = 1024*1024;
	
	public InputStreamChunkIterator( InputStream is ) {
		this.is = is;
	}
	
	public Object next() {
		try {
			int avail = is.available();
			if( avail == 0 ) avail = 256;
			if( avail > MAX_CHUNK_SIZE ) avail = MAX_CHUNK_SIZE;
			byte[] buf = new byte[MAX_CHUNK_SIZE];
			int read = is.read( buf );
			if( read == -1 ) {
				eofReached = true;
				is.close();
				read = 0;
			}
			return new SimpleByteChunk( buf, 0, read );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean hasNext() {
		return !eofReached;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	public void close() throws IOException {
		is.close();
	}
}
