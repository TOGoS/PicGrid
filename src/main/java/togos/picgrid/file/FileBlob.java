package togos.picgrid.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;

import togos.blob.ByteBlob;
import togos.blob.InputStreamable;
import togos.blob.util.BlobUtil;
import togos.blob.util.InputStreamChunkIterator;

public class FileBlob implements ByteBlob, InputStreamable
{
	File file;
	
	public FileBlob( File file ) {
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	public Iterator chunkIterator() {
		try {
			return new InputStreamChunkIterator( inputStream() );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public InputStream inputStream() throws IOException {
		return new FileInputStream(file);
	}
	
	public long getSize() {
		return file.length();
	}
	
	public boolean equals( Object o ) {
		return o instanceof ByteBlob ? BlobUtil.equals( this, (ByteBlob)o ) : false;
	}
}
