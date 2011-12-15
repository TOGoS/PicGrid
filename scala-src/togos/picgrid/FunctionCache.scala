package togos.picgrid
import togos.mf.value.ByteChunk

trait FunctionCache
{
	def apply( key:String ):ByteChunk
	def update( key:String, value:ByteChunk ):Unit
}
