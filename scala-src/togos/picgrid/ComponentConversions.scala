package togos.picgrid

import togos.picgrid.store.DataMap;

object ComponentConversions
{
	implicit def dataMapAsFunctionCache( dm:DataMap ) = {
		if( dm == null ) null else DataMapFunctionCache(dm) 
	}
}
