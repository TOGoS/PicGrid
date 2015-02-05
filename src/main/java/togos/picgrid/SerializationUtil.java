package togos.picgrid;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import togos.blob.ByteBlob;
import togos.blob.SimpleByteChunk;
import togos.blob.SingleChunkByteBlob;
import togos.blob.util.BlobUtil;

public class SerializationUtil
{
	public static ByteBlob serialize( Object o ) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			return new SingleChunkByteBlob( new SimpleByteChunk(baos.toByteArray()) );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public static Object unserialize( ByteBlob b ) {
		InputStream bbis = null;
		try {
			bbis = BlobUtil.inputStream(b);
			return new ObjectInputStream( bbis ).readObject();
		} catch( IOException e ) {
			throw new RuntimeException(e);
		} catch( ClassNotFoundException e ) {
			throw new RuntimeException(e);
		} finally {
			if( bbis != null ) {
				try { bbis.close(); } catch( IOException e ) { throw new RuntimeException(e); }
			}
		}
	}
}
