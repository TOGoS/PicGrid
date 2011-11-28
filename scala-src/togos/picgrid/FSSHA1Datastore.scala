package togos.picgrid

import java.io.File
import java.io.FileOutputStream

import org.bitpedia.util.Base32

import togos.mf.value.ByteBlob
import togos.mf.value.ByteChunk
import togos.picgrid.io.FileBlob

class FSSHA1Datastore( val dir:File ) extends Datastore
{
	val BITPRINT_REGEX = """^urn:bitprint:([^\.]+)\.([^\.]+)$""".r 
	
	protected def fullPathTo( sha1:String ):File = {
		new File(dir + "/" + sha1.substring(0,2) + "/" + sha1)
	}
	
	protected def tempPath():File = {
		new File(dir + "/.temp-" + (Math.random*Integer.MAX_VALUE).toInt + "-" + (Math.random*Integer.MAX_VALUE).toInt + "-" + (Math.random*Integer.MAX_VALUE).toInt )
	}
	
	protected def tempPathFor( sha1:String ):File = {
		new File(dir + "/" + sha1.substring(0,2) + "/." + sha1 + ".temp-" + (Math.random*Integer.MAX_VALUE).toInt )
	}
	
	protected def makeParentDirs( f:File ) = {
		val parentFile = f.getParentFile() 
		if( parentFile != null && !parentFile.exists() ) parentFile.mkdirs()
	}
	
	def apply(fn: String):ByteBlob = {
		fn match {
		case BITPRINT_REGEX(sha1,tt) =>
			val f = fullPathTo(sha1)
			if( f.exists ) new FileBlob(f) else null 
		case _ => null
		}
	}
	
	def store(data: ByteBlob): String = {
		val digestor = new BitprintDigest()
		val chunkIter = data.chunkIterator()
		val tempFile = tempPath()
		makeParentDirs( tempFile )
		val fos = new FileOutputStream(tempFile)
		while( chunkIter.hasNext() ) {
			val chunk:ByteChunk = chunkIter.next().asInstanceOf[ByteChunk]
			fos.write( chunk.getBuffer(), chunk.getOffset(), chunk.getSize() )
			digestor.update( chunk.getBuffer(), chunk.getOffset(), chunk.getSize() )
		}
		val hash = digestor.digest
		val sha1Hash = new Array[Byte](20)
		System.arraycopy( hash, 0, sha1Hash, 0, 20);
		val tigerTreeHash = new Array[Byte](24);
		System.arraycopy( hash, 20, tigerTreeHash, 0, 24);
		val sha1String = Base32.encode(sha1Hash)
		fos.close()
		
		val destFile = fullPathTo(sha1String)
		makeParentDirs( destFile )
		tempFile.renameTo( destFile )
		
		return "urn:bitprint:" + sha1String + "." + Base32.encode(tigerTreeHash);
	}
}
