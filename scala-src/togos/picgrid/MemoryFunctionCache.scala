package togos.picgrid

import scala.collection.mutable.HashMap
import togos.mf.value.ByteChunk

class MemoryFunctionCache extends FunctionCache
{
	val data = new HashMap[String,ByteChunk]
	
	def apply( key:String ):ByteChunk = {
		data.getOrElse(key, null)
	}
	
	def update( key:String, v:ByteChunk ):Unit = {
		data(key) = v
	}
}
