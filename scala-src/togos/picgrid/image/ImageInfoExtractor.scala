package togos.picgrid.image
import java.io.InputStream
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import togos.mf.value.ByteBlob
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import togos.picgrid.io.ByteBlobInputStream
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.MemoryCacheImageInputStream

class ImageInfoExtractor( var datastore:String=>ByteBlob )
{
	var cachedDimensions:Map[String,(Integer,Integer)] = new HashMap[String,(Integer,Integer)]()
	
	val GIF_MAGIC:Integer = 0x47494638
	val PNG_MAGIC:Integer = 0x89504E47
	val JPG_MAGIC:Short = 0xFFD8.toShort
	val BMP_MAGIC:Short = 0x424D
	
	def getImageType( magic:Array[Byte] ):ImageFormat = {
		val magicI:Integer =
			((magic(0) & 0xFF) << 24) |
			((magic(1) & 0xFF) << 16) |
			((magic(2) & 0xFF) <<  8) |
			((magic(3) & 0xFF) <<  0)
		
		val magicS:Short = (magicI >> 16).toShort
		
		magicI match {
		case GIF_MAGIC => return ImageFormat.GIF; 
		case PNG_MAGIC => return ImageFormat.PNG;
		case _ =>
		}
		
		magicS match {
		case JPG_MAGIC => return ImageFormat.JPEG;
		case BMP_MAGIC => return ImageFormat.BMP;
		case _ =>
		}
		
		return null
	}
	
	def getImageDimensions( inputStream:InputStream, formatName:String ):(Integer,Integer) = {
		val readerIter = ImageIO.getImageReadersByFormatName(formatName)
		if( readerIter.hasNext() ) {
			val reader = readerIter.next().asInstanceOf[ImageReader]
			reader.setInput(new MemoryCacheImageInputStream(inputStream))
			val w = reader.getWidth(reader.getMinIndex())
			val h = reader.getHeight(reader.getMinIndex())
			return (w,h)
		}
		return (0,0)
	}
	
	def getImageDimensions( imageUri:String ):(Integer,Integer) = {
		var dims = cachedDimensions.getOrElse(imageUri, null)
		if( dims == null ) {
			val image = datastore(imageUri)
			
			val inputStream1 = new ByteBlobInputStream(image.chunkIterator())
			val magic = new Array[Byte](4)
			inputStream1.read( magic )
			inputStream1.close()
			val format = getImageType(magic)
			if( format == null ) {
				System.err.println("Couldn't determine type of "+imageUri+": "+magic(0)+","+magic(1)+","+magic(2)+","+magic(3))
				return null
			}
			
			val inputStream = new ByteBlobInputStream(image.chunkIterator())
			dims = getImageDimensions(inputStream, format.name)
			inputStream.close()
			cachedDimensions(imageUri) = dims
		}
		return dims
	}
}
