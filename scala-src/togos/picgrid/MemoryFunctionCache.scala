package togos.picgrid

import scala.collection.mutable.HashMap

class MemoryFunctionCache {
	val maps = new HashMap[String,HashMap[String,String]]
	
	protected def map( name:String ):HashMap[String,String] = {
		var m = maps.getOrElse(name,null)
		if( m == null ) {
			m = new HashMap()
			maps(name) = m
		} 
		m
	}
	
	def apply( cacheName:String, key:String ):String = {
		map(cacheName).getOrElse(key, null)
	}
	
	def update( cacheName:String, key:String, v:String ):Unit = {
		map(cacheName)(key) = v
	}
}
