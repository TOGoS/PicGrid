package togos.picgrid.file

import java.io.File
import java.io.FileOutputStream
import org.bitpedia.util.Base32
import togos.blob.ByteBlob
import togos.blob.ByteChunk
import togos.picgrid.file.FileUtil.makeParentDirs
import scala.collection.mutable.ListBuffer
import togos.picgrid.BitprintDigest

class FSSHA1Datastore( val dir:File, val extraReadDirs:List[String]=List() ) extends FSDatastore
{
	val readDirs = dir.getPath() :: extraReadDirs
	
	val BITPRINT_REGEX = """^urn:bitprint:([^\.]+)\.([^\.]+)$""".r 
	
	protected def subSectorPath( sha1:String ) = sha1.substring(0,2) + "/" + sha1 
	
	protected def fullPathTo( sha1:String ):File = {
		new File(dir + "/" + subSectorPath(sha1))
	}
	
	/**
	 * @return a temporary path inside the repository
	 */
	def tempFile( ext:String="" ):File = {
		new File(dir + "/.temp-" + (Math.random*Integer.MAX_VALUE).toInt + "-" + (Math.random*Integer.MAX_VALUE).toInt + "-" + (Math.random*Integer.MAX_VALUE).toInt + ext )
	}
	
	protected def tempFileFor( sha1:String ):File = {
		new File(dir + "/" + sha1.substring(0,2) + "/." + sha1 + ".temp-" + (Math.random*Integer.MAX_VALUE).toInt )
	}
	
	def apply( uri:String ):ByteBlob = {
		uri match {
		case BITPRINT_REGEX(sha1,tt) =>
			val sp = subSectorPath(sha1)
			for( d <- readDirs ) {
				val f = new File(d + "/" + sp)
				if( f.exists ) return new FileBlob(f)
			}
		case _ =>
		}
		null
	}
	
	/**
	 * @param data the data to be identified
	 * @param chunkCallback will be called for each chunk of data as it is processed
	 * @return (full bitprint URN, base32-encoded SHA1)
	 */
	def identify( data:ByteBlob, chunkCallback:ByteChunk=>Unit=null ):(String,String) = {
		val digestor = new BitprintDigest()
		val chunkIter = data.chunkIterator()
		while( chunkIter.hasNext() ) {
			val chunk:ByteChunk = chunkIter.next().asInstanceOf[ByteChunk]
			if( chunkCallback != null ) chunkCallback( chunk )
			digestor.update( chunk.getBuffer(), chunk.getOffset(), chunk.getSize() )
		}
		val hash = digestor.digest
		val sha1Hash = new Array[Byte](20)
		System.arraycopy( hash, 0, sha1Hash, 0, 20);
		val tigerTreeHash = new Array[Byte](24);
		System.arraycopy( hash, 20, tigerTreeHash, 0, 24);
		val sha1String = Base32.encode(sha1Hash)
		return ("urn:bitprint:" + sha1String + "." + Base32.encode(tigerTreeHash), sha1String)
	}
	
	protected def storeAndDelete( tempFile:File, destFile:File ) = {
		if( destFile.exists() ) {
			if( !tempFile.delete() ) {
				// Then try again later, I guess.
				tempFile.deleteOnExit()
			}
		} else {
			makeParentDirs( destFile )
			tempFile.renameTo( destFile )
		}
	}
	
	/**
	 *  
	 */
	def store( data:ByteBlob ):String = {
		val tempFile = this.tempFile()
		makeParentDirs( tempFile )
		val fos = new FileOutputStream(tempFile)
		val (urn, sha1String) = identify(data, (chunk:ByteChunk) => {fos.write( chunk.getBuffer(), chunk.getOffset(), chunk.getSize() )} )
		fos.close()
		
		storeAndDelete( tempFile, fullPathTo(sha1String) )
		
		urn
	}
	
	def +=( data:ByteBlob ):String = store(data)
	
	/**
	 * Import an existing temporary file, moving it directly into the repository, returning its URN
	 */
	def storeAndRemove( tempFile:File ):String = {
		val fileBlob = new FileBlob(tempFile)
		val (urn, sha1String) = identify(fileBlob)
		
		storeAndDelete( tempFile, fullPathTo(sha1String) )
		
		urn
	}
}
object FSSHA1Datastore
{
	def main( args:Array[String] ) {
		var datastoreDir:File = null
		var command:String = null
		var bareArgs:ListBuffer[String] = new ListBuffer[String]()
		var i = 0
		while( i < args.length ) {
			args(i) match {
				case "-datastore" =>
					i += 1
					datastoreDir = new File(args(i)) 
				case arg if !arg.startsWith("-") =>
					if( command == null ) {
						command = arg
					} else {
						bareArgs += arg
					}
				case arg => throw new RuntimeException("Unrecognised argument: "+arg)
			}
			i += 1
		}
		
		command match {
			case "store" =>
				if( datastoreDir == null ) {
					throw new RuntimeException("No -datastore directory specified")
				}
				val datastore = new FSSHA1Datastore(datastoreDir) 
				for( filename <- bareArgs ) {
					val uri = datastore += new FileBlob(new File(filename))
					System.out.println(filename + " -> " + uri)
				}
			case null => throw new RuntimeException("No sub-command given.")
			case _ => throw new RuntimeException("Unrecognised sub-command: "+command)
		}
	}
}
