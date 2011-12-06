package togos.picgrid.file

import java.io.File
import scala.collection.mutable.HashMap
import java.io.IOException
import togos.picgrid.FunctionCache

class SLFFunctionCache( val cacheFile:File ) extends FunctionCache
{
	val slfCache = new HashMap[String,SimpleListFile]()
	var slf:SimpleListFile = null
	
	protected def getSlf( allowCreate:Boolean ):SimpleListFile = synchronized {
		if( slf == null ) {
			if( cacheFile.exists() || allowCreate ) {
				FileUtil.makeParentDirs( cacheFile )
				slf = new SimpleListFile( cacheFile, "rw" )
				slf.initIfEmpty( 65536, 1024*1024 )
			}
		}
		slf
	}
	
	def apply( key:String ):Array[Byte] = {
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
	
	def update( key:String, v:Array[Byte] ) {
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
