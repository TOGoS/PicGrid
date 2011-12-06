package togos.picgrid

trait FunctionCache
{
	def apply( key:String ):Array[Byte]
	def update( key:String, value:Array[Byte] ):Unit
}
