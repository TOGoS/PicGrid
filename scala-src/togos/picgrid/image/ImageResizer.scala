package togos.picgrid.image

trait ImageResizer
{
	def resize( origUri:String, boxWidth:Int, boxHeight:Int ):String
}
