package togos.picgrid.io

import java.io.File
import scala.collection.mutable.HashMap
import togos.picgrid.FunctionCache
import togos.picgrid.file.SimpleListFile;

class SLFFunctionCache( val cacheDir:File )
{
	val slfCache = new HashMap[String,SimpleListFile]()
	
	protected def getSlf( name:String, allowCreate:Boolean ):SimpleListFile = synchronized {
		var slf = slfCache.getOrElse( name, null )
		if( slf == null && allowCreate ) {
			val slfFile = new File( cacheDir + "/" + name + ".slf" )
			slf = new SimpleListFile( slfFile, "rw" )
			slfCache( name ) = slf
		}
		slf
	}
	
	def apply( cacheName:String, key:String ):String = {
		val slf = getSlf( cacheName, false )
		if( slf == null ) return null
		new String( slf.get( key ), "UTF-8" )
	}
	
	def update( cacheName:String, key:String, v:String ) {
		val slf = getSlf( cacheName, true )
		slf.put( key, v.getBytes("UTF-8") )
	}
}
