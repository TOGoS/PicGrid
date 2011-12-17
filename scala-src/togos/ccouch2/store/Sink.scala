package togos.ccouch2.store

trait Sink[A,B]
{
	def store( value:A ):B
}
