package togos.picgrid.image

case class ImageFormat( val name:String, val suffix:String )
object ImageFormat {
	val JPEG = new ImageFormat( "JPEG", ".jpg" )
	val PNG = new ImageFormat( "PNG", ".png" )
	// yadda yadda
}