package togos.picgrid.image

import java.awt.image.BufferedImage
import java.lang.Integer
import javax.imageio.ImageIO
import togos.blob.util.ByteBlobInputStream
import togos.picgrid.BetterByteArrayOutputStream
import togos.picgrid.BlobAutoStore
import togos.blob.SingleChunkByteBlob

class CrapResizer extends ImageResizer
{
	var datastore:BlobAutoStore = null
	
	def load( origUri:String ):BufferedImage = {
		val data = datastore(origUri);
		if( data == null ) return null
		val is = new ByteBlobInputStream(data.chunkIterator())
		ImageIO.read(is)
	}
	
	def fit( oWidth:Int, oHeight:Int, boxWidth:Int, boxHeight:Int ) = {
		var newWidth = oWidth
		var newHeight = oHeight
		if( newWidth > boxWidth ) {
			newHeight = (newHeight * boxWidth) / newWidth
			newWidth = boxWidth
		}
		if( newHeight > boxHeight ) {
			newWidth = (newWidth * boxHeight) / newHeight
			newHeight = boxHeight
		}
		
		(newWidth, newHeight)
	}
	
	def resize( origUri:String, boxWidth:Int, boxHeight:Int ):String = {
		val oImg = load(origUri)
		if( oImg == null ) throw new Exception("Couldn't find "+origUri)
		val (w,h) = fit( oImg.getWidth(null), oImg.getHeight(null), boxWidth, boxHeight )
		
		val thamb:BufferedImage = ResizeUtil.chrisResize( oImg, w, h )
		val baos:BetterByteArrayOutputStream = new BetterByteArrayOutputStream()
		ImageIO.write( thamb, "jpeg", baos )
		datastore.store( new SingleChunkByteBlob(baos) )
	}
}
