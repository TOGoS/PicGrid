package togos.picgrid.image

case class ImageFormat( val name:String, val suffix:String, val mimeType:String, val isRaster:Boolean )
{
	override def toString() = name
}
object ImageFormat
{
	val HTML = new ImageFormat( "HTML", ".html", "text/html", false )
	val COMPOSITE = new ImageFormat( "COMPOSITE", "image/x-picgrid-composite", ".composite-image", false )
	val JPEG = new ImageFormat( "JPEG", ".jpg", "image/jpeg", true )
	val PNG  = new ImageFormat( "PNG", ".png", "image/png", true )
	val GIF  = new ImageFormat( "GIF", ".gif", "image/gif", true )
	val BMP  = new ImageFormat( "BMP", ".bmp", "image/bmp", true )
	// yadda yadda
}