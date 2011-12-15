package togos.picgrid

import togos.blob.ByteChunk
import togos.picgrid.store.DataMap

case class DataMapFunctionCache( val dataMap:DataMap ) extends FunctionCache
{
	def apply( key:ByteChunk ) = dataMap.get(key)
	def update( key:ByteChunk, value:ByteChunk ) = dataMap.put(key, value)
}
