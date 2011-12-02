package togos.picgrid.image

case class ImageFormat( val name:String, val suffix:String, val isRaster:Boolean )
{
	override def toString() = name
}
object ImageFormat
{
	val COMPOSITE = new ImageFormat( "COMPOSITE", ".composite-image", false )
	val JPEG = new ImageFormat( "JPEG", ".jpg", true )
	val PNG  = new ImageFormat( "PNG", ".png", true )
	val GIF  = new ImageFormat( "GIF", ".gif", true )
	val BMP  = new ImageFormat( "BMP", ".bmp", true )
	// yadda yadda
}