package togos.picgrid

trait Store[A,B] extends Function[A,B]
{
	def update( key:A, value:B ):Unit
}
