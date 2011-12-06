package togos.picgrid
import java.io.ByteArrayOutputStream
import togos.mf.value.ByteChunk
import togos.mf.value.ByteBlob
import scala.collection.JavaConversions
import togos.mf.base.SimpleByteChunk

object BlobConversions
{
	implicit def byteBlobAsChunkIterator(b : ByteBlob):Iterator[ByteChunk] = { 
		JavaConversions.JIteratorWrapper(b.chunkIterator()).asInstanceOf[Iterator[ByteChunk]]
	}
	
	implicit def byteBlobAsByteArray( b:ByteBlob ):Array[Byte] = {
		val baos = new ByteArrayOutputStream()
		for( c <- b ) {
			baos.write( c.getBuffer(), c.getOffset(), c.getSize() )
		}
		baos.toByteArray()
	}
	
	implicit def byteBlobAsString( b:ByteBlob ):String = {
		new String( b, "UTF-8" )
	}
	
	implicit def stringAsByteBlob( s:String ):ByteBlob = {
		new SimpleByteBlob( new SimpleByteChunk(s.getBytes("UTF-8")) )
	}
}
