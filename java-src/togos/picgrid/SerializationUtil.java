package togos.picgrid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import togos.mf.base.SimpleByteChunk;
import togos.mf.value.ByteBlob;
import togos.picgrid.io.ByteBlobInputStream;

public class SerializationUtil
{
	public static ByteBlob serialize( Object o ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			return new SimpleByteBlob( new SimpleByteChunk(baos.toByteArray()) );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static Object unserialize( ByteBlob b ) {
		ByteBlobInputStream bbis = new ByteBlobInputStream(b.chunkIterator());
		
		try {
			return new ObjectInputStream( bbis ).readObject();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		} catch( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		} finally {
			try { bbis.close(); } catch( IOException e ) { throw new RuntimeException(e); }
		}
	}
}
