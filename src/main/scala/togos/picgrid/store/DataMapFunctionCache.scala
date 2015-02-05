package togos.picgrid.store

import togos.blob.ByteChunk
import togos.picgrid.FunctionCache

case class DataMapFunctionCache( val dataMap:DataMap ) extends FunctionCache
{
	def apply( key:ByteChunk ) = dataMap.get(key)
	def update( key:ByteChunk, value:ByteChunk ) = dataMap.put(key, value)
}
