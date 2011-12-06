package togos.picgrid.image
import java.awt.image.BufferedImage

import javax.imageio.ImageIO
import togos.picgrid.io.ByteBlobInputStream
import togos.picgrid.BetterByteArrayOutputStream
import togos.picgrid.Datasink
import togos.picgrid.SimpleByteBlob

class CrapResizer
{
	var datastore:Datasink = null
	
	def load( orig:ImageHandle ):BufferedImage = {
		val data = datastore(orig.uri);
		if( data == null ) return null
		val is = new ByteBlobInputStream(data.chunkIterator())
		ImageIO.read(is)
	}
	
	def fit( oWidth:Integer, oHeight:Integer, boxWidth:Integer, boxHeight:Integer ) = {
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
	
	def resize( orig:ImageHandle, boxWidth:Integer, boxHeight:Integer ):ImageHandle = {
		val oImg = load(orig)
		if( oImg == null ) throw new Exception("Couldn't find "+orig.uri)
		val (w,h) = fit( oImg.getWidth(null), oImg.getHeight(null), boxWidth, boxHeight )
		
		val thamb:BufferedImage = ResizeUtil.chrisResize( oImg, w, h )
		val baos:BetterByteArrayOutputStream = new BetterByteArrayOutputStream()
		ImageIO.write( thamb, "jpeg", baos )
		val thambUri = datastore.store( new SimpleByteBlob(baos) )
		new ImageHandle( thambUri, ImageFormat.JPEG, baos.getSize(), thamb.getWidth(null), thamb.getHeight(null) )
	}
}
