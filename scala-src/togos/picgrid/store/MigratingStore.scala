package togos.picgrid.store
import togos.ccouch2.store.Store

class MigratingStore[K,V]( val old:Function[K,V], val neu:Store[K,V]) extends Store[K,V]
{
	def apply( key:K ):V = {
		var v = neu(key)
		if( v != null ) return v
		v = old(key)
		if( v != null ) neu(key) = v
		return v
	}
	
	def update( key:K, value:V ) {
		neu(key) = value
	}
}
