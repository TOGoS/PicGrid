package togos.picgrid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.bitpedia.util.Base32;

import togos.mf.value.ByteBlob;
import togos.mf.value.ByteChunk;

public class DigestUtil
{
	public static MessageDigest newSha1Digest() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Apparently SHA-1 isn't available", e);
		}
	}
	
	public static void update( ByteChunk c, MessageDigest dig ) {
		dig.update( c.getBuffer(), c.getOffset(), c.getSize() );
	}
	
	public static void update( ByteBlob blob, MessageDigest dig ) {
		for( Iterator i=blob.chunkIterator(); i.hasNext(); ) {
			update( (ByteChunk)i.next(), dig );
		}
	}
	
	public static byte[] digest( ByteBlob blob, MessageDigest dig ) {
		update( blob, dig);
		return dig.digest();
	}
	
	public static String sha1Base32( ByteBlob blob ) {
		return Base32.encode(digest(blob, newSha1Digest()));
	}
}