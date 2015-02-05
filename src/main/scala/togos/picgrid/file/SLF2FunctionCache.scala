package togos.picgrid.file

import java.io.Closeable
import java.io.File
import java.io.Flushable
import togos.blob.ByteChunk
import togos.picgrid.ComponentConversions.dataMapAsFunctionCache
import togos.picgrid.FunctionCache

class SLF2FunctionCache( val cacheFile:File ) extends FunctionCache with Flushable with Closeable
{
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
	
	def apply( key:ByteChunk ):ByteChunk = {
		val slf:FunctionCache = getSlf( false )
		if( slf == null ) return null
		slf( key )
	}
	
	def update( key:ByteChunk, v:ByteChunk ) {
		getSlf( true )( key ) = v
	}
	
	def flush() { slf.flush() }
	
	def close() {
		slf.close()
		slf = null
	}
}
