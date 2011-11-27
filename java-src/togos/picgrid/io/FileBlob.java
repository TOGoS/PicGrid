package togos.picgrid.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import togos.mf.value.ByteBlob;

public class FileBlob implements ByteBlob
{
	File file;
	
	public FileBlob( File file ) {
		this.file = file;
	}
	
	public Iterator chunkIterator() {
		try {
			FileInputStream fis = new FileInputStream(file);
			return new InputStreamChunkIterator( fis );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public long getSize() {
		return file.length();
	}
}