package togos.picgrid

import scala.collection.mutable.HashMap

class MemoryFunctionCache extends FunctionCache
{
	val data = new HashMap[String,Array[Byte]]
	
	def apply( key:String ):Array[Byte] = {
		data.getOrElse(key, null)
	}
	
	def update( key:String, v:Array[Byte] ):Unit = {
		data(key) = v
	}
}
