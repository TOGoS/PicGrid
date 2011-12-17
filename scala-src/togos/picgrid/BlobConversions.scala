package togos.picgrid

import java.io.ByteArrayOutputStream

import togos.blob.ByteChunk
import togos.blob.ByteBlob
import scala.collection.JavaConversions
import togos.blob.SimpleByteChunk
import togos.blob.SingleChunkByteBlob

object BlobConversions
{
	implicit def byteBlobAsChunkIterator(b : ByteBlob):Iterator[ByteChunk] = { 
		JavaConversions.JIteratorWrapper(b.chunkIterator()).asInstanceOf[Iterator[ByteChunk]]
	}
	
	//// To byte array ////
	
	implicit def stringAsByteArray( s:String ):Array[Byte] = {
		if( s == null ) null else s.getBytes("UTF-8")
	}
	
	implicit def byteChunkAsByteArray( b:ByteChunk ):Array[Byte] = {
		if( b == null ) return null
		if( b.getBuffer().length == b.getSize() ) return b.getBuffer()
		
		throw new RuntimeException("Can't convert chunk to array when chunk is not blahl;asd")
	}
	
	implicit def byteBlobAsByteArray( b:ByteBlob ):Array[Byte] = {
		if( b == null ) return null
		
		if( b.isInstanceOf[SingleChunkByteBlob] ) return b.asInstanceOf[SingleChunkByteBlob].chunk
		
		val baos = new ByteArrayOutputStream()
		for( c <- b ) {
			baos.write( c.getBuffer(), c.getOffset(), c.getSize() )
		}
		baos.toByteArray()
	}
	
	//// To ByteChunk ////
	
	implicit def stringAsByteChunk( s:String ):ByteChunk = {
		if( s == null ) null else s.getBytes("UTF-8")
	}
	
	implicit def byteArrayAsByteChunk( b:Array[Byte] ):ByteChunk = {
		if( b == null ) null else new SimpleByteChunk(b)
	}
	
	implicit def byteBlobAsByteChunk( b:ByteBlob ):ByteChunk = {
		if( b == null ) return null
		if( b.isInstanceOf[SingleChunkByteBlob] ) return b.asInstanceOf[SingleChunkByteBlob].chunk
		
		return byteBlobAsByteArray( b )
	}
	
	//// To ByteBlob ////
	
	implicit def stringAsByteBlob( s:String ):ByteBlob = {
		if( s == null ) null else new SingleChunkByteBlob( s )
	}
	
	implicit def byteArrayAsByteBlob( b:Array[Byte] ):ByteBlob = {
		if( b == null ) null else new SingleChunkByteBlob(new SimpleByteChunk(b))
	}
	
	implicit def byteChunkAsByteBlob( b:ByteChunk ):ByteBlob = {
		if( b == null ) null else new SingleChunkByteBlob( b )
	}
	
	//// To string ////
	
	implicit def byteChunkAsString( b:ByteChunk ):String = {
		if( b == null ) null else new String( b.getBuffer(), b.getOffset(), b.getSize(), "UTF-8" )
	}
	
	implicit def byteBlobAsString( b:ByteBlob ):String = {
		if( b == null ) null else new String( b, "UTF-8" )
	}
	
	implicit def byteArrayAsString( a:Array[Byte] ):String = {
		if( a == null ) null else new String(a,"UTF-8")
	}
}
