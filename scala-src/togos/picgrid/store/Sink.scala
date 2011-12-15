package togos.picgrid.store

trait Sink[A,B]
{
	def store( value:A ):B
}
