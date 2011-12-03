package togos.picgrid.file

import java.io.File
import scala.collection.mutable.HashMap

class SLFFunctionCache( val cacheDir:File )
{
	val slfCache = new HashMap[String,SimpleListFile]()
	
	protected def getSlf( name:String, allowCreate:Boolean ):SimpleListFile = synchronized {
		var slf = slfCache.getOrElse( name, null )
		if( slf == null ) {
			val slfFile = new File( cacheDir + "/" + name + ".slf" )
			if( slfFile.exists() || allowCreate ) {
				FileUtil.makeParentDirs( slfFile )
				slf = new SimpleListFile( slfFile, "rw" )
				slf.initIfEmpty( 65536, 1024*1024 )
				slfCache( name ) = slf
			}
		}
		slf
	}
	
	def apply( cacheName:String, key:String ):String = {
		val slf = getSlf( cacheName, false )
		if( slf == null ) return null
		val bytes = slf.get( key )
		if( bytes == null ) return null
		new String( bytes, "UTF-8" )
	}
	
	def update( cacheName:String, key:String, v:String ) {
		val slf = getSlf( cacheName, true )
		slf.put( key, v.getBytes("UTF-8") )
	}
	
	def flush() = synchronized {
		for( f <- slfCache.values ) {
			f.flush()
		}
	}
}
