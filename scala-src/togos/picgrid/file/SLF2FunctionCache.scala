package togos.picgrid.file

import java.io.File
import java.io.IOException
import scala.collection.mutable.HashMap
import togos.picgrid.BlobConversions.stringAsByteChunk
import togos.picgrid.FunctionCache
import togos.mf.value.ByteChunk

class SLF2FunctionCache( val cacheFile:File ) extends FunctionCache
{
	val slfCache = new HashMap[String,SimpleListFile]()
	var slf:SimpleListFile2 = null
	
	protected def getSlf( allowCreate:Boolean ):SimpleListFile2 = synchronized {
		if( slf == null ) {
			if( cacheFile.exists() || allowCreate ) {
				FileUtil.makeParentDirs( cacheFile )
				slf = new SimpleListFile2( new RandomAccessFileBlob(cacheFile, "rw"), 15, false )
			}
		}
		slf
	}
	
	def apply( key:String ):ByteChunk = {
		val slf = getSlf( false )
		if( slf == null ) return null
		try {
			slf.get( key )
		} catch {
			case e : IOException =>
				System.err.println("Warning: Exception when fetching '"+key+"' in "+cacheFile+": "+e.getMessage())
				e.printStackTrace()
				null
		}
	}
	
	def update( key:String, v:ByteChunk ) {
		val slf = getSlf( true )
		try {
			slf.put( key, v )
		} catch {
			case e : IOException =>
				System.err.println("Warning: Exception when adding '"+key+"' in "+cacheFile+": "+e.getMessage())
				e.printStackTrace()
		}
	}
	
	def flush() = synchronized {
		for( f <- slfCache.values ) {
			f.flush()
		}
	}
}
