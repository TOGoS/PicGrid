package togos.picgrid
import java.awt.image.RenderedImage

import javax.imageio.ImageIO
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.stream.ImageInputStream

import togos.picgrid.io.ByteBlobInputStream;

import java.io.ByteArrayOutputStream
import java.io.OutputStream

class Resizer
{
	var datastore:Datastore = null
	
	def load( orig:ImageHandle ):BufferedImage = {
		val data = datastore(orig.uri);
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
	
	def resize( img:BufferedImage, newWidth:Integer, newHeight:Integer ):BufferedImage = {
		null
	}
	
	def resize( orig:ImageHandle, boxWidth:Integer, boxHeight:Integer ):ImageHandle = {
		val oImg = load(orig)
		val (w,h) = fit( oImg.getWidth(null), oImg.getHeight(null), boxWidth, boxHeight )
		
		val thamb:BufferedImage = resize( oImg, w, h )
		val baos:BetterByteArrayOutputStream = new BetterByteArrayOutputStream()
		ImageIO.write( thamb, "jpeg", baos )
		val thambUri = datastore.store( new SimpleByteBlob(baos) )
		new ImageHandle( thambUri, ImageFormat.JPEG, baos.getSize(), thamb.getWidth(null), thamb.getHeight(null) )
	}
}
