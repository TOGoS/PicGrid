package togos.picgrid

import scala.collection.mutable.HashMap
import togos.blob.ByteChunk

class MemoryFunctionCache extends FunctionCache
{
	val data = new HashMap[ByteChunk,ByteChunk]
	
	def apply( key:ByteChunk ):ByteChunk = {
		data.getOrElse(key, null)
	}
	
	def update( key:ByteChunk, v:ByteChunk ):Unit = {
		data(key) = v
	}
}
