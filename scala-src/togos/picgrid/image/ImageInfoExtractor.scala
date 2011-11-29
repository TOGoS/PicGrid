package togos.picgrid.image
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import togos.mf.value.ByteBlob
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import togos.picgrid.io.ByteBlobInputStream

class ImageInfoExtractor( var datastore:String=>ByteBlob )
{
	var cachedDimensions:Map[String,(Integer,Integer)] = new HashMap[String,(Integer,Integer)]()
	
	def getImageDimensions( inputStream:InputStream, formatName:String ):(Integer,Integer) = {
		val readerIter = ImageIO.getImageReadersByFormatName(formatName)
		if( readerIter.hasNext() ) {
			val reader = readerIter.next().asInstanceOf[ImageReader]
			reader.setInput(inputStream)
			val w = reader.getWidth(reader.getMinIndex())
			val h = reader.getHeight(reader.getMinIndex())
			return (w,h)
		}
		return (0,0)
	}
	
	def getImageDimensions( imageUri:String ):(Integer,Integer) = {
		var dims = cachedDimensions(imageUri)
		if( dims == null ) {
			val image = datastore(imageUri)
			val imageInputStream = new ByteBlobInputStream(image.chunkIterator())
			dims = getImageDimensions(imageInputStream, "JPEG")
			imageInputStream.close()
			cachedDimensions(imageUri) = dims
		}
		return dims
	}
}
