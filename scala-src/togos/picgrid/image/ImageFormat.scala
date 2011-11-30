package togos.picgrid.image

case class ImageFormat( val name:String, val suffix:String )
object ImageFormat {
	val JPEG = new ImageFormat( "JPEG", ".jpg" )
	val PNG  = new ImageFormat( "PNG", ".png" )
	val GIF  = new ImageFormat( "GIF", ".gif" )
	val BMP  = new ImageFormat( "BMP", ".bmp" )
	// yadda yadda
}