package togos.picgrid.image

import java.io.InputStream
import java.lang.Integer
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import togos.blob.ByteBlob
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import togos.blob.util.ByteBlobInputStream
import togos.picgrid.BlobConversions._
import togos.picgrid.BlobSource
import togos.picgrid.FunctionCache
import javax.imageio.stream.ImageInputStream
import javax.imageio.stream.MemoryCacheImageInputStream
import java.io.FileNotFoundException

object ImageInfoExtractor
{
	val GIF_MAGIC:Int = 0x47494638
	val PNG_MAGIC:Int = 0x89504E47
	val JPG_MAGIC:Short = 0xFFD8.toShort
	val BMP_MAGIC:Short = 0x424D
	val COMP_MAGIC:Int = 0x434F4D50 // "COMP" (as in "COMPOSITE-IMAGE")
	
	def extractImageType( magic:Array[Byte] ):ImageFormat = {
		val magicI:Int =
			((magic(0) & 0xFF) << 24) |
			((magic(1) & 0xFF) << 16) |
			((magic(2) & 0xFF) <<  8) |
			((magic(3) & 0xFF) <<  0)
		
		val magicS:Short = (magicI >> 16).toShort
		
		magicI match {
		case COMP_MAGIC => return ImageFormat.COMPOSITE 
		case GIF_MAGIC => return ImageFormat.GIF
		case PNG_MAGIC => return ImageFormat.PNG
		case _ =>
		}
		
		magicS match {
		case JPG_MAGIC => return ImageFormat.JPEG
		case BMP_MAGIC => return ImageFormat.BMP
		case _ =>
		}
		
		val s = new String( magic )
		if( s.startsWith("<htm") ) return ImageFormat.HTML
		
		return null
	}
	
	def extractImageDimensions( inputStream:InputStream, formatName:String ):(Int,Int) = {
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
	
	def extractMagicNumber( blob:ByteBlob ):Array[Byte] = {
		val inputStream1 = new ByteBlobInputStream(blob.chunkIterator())
		val magic = new Array[Byte](8)
		inputStream1.read( magic )
		inputStream1.close()
		magic
	}
	
	def extractImageType( imageBlob:ByteBlob ):ImageFormat = {
		extractImageType( extractMagicNumber(imageBlob) )
	}
}

import togos.picgrid.image.ImageInfoExtractor._

class ImageInfoExtractor( val imageDimensionCache:FunctionCache, val datastore:BlobSource )
{
	var dimensionCache = new Function[String,(Int,Int)] {
		def apply( uri:String ):(Int,Int) = {
			val str = imageDimensionCache( uri ):String
			if( str == null ) return null
			val parts = str.split(',')
			(parts(0).toInt, parts(1).toInt)
		}
		
		def update( uri:String, dims:(Int,Int) ) {
			imageDimensionCache( uri ) = dims._1 + "," + dims._2
		}
	}
	
	def getImageType( imageUri:String ):ImageFormat = {
		val imageBlob = datastore(imageUri)
		if( imageBlob == null ) {
			throw new FileNotFoundException("Couldn't find image to get format from: "+imageUri)
		}
		extractImageType( imageBlob )
	}
	
	def getImageDimensions( imageUri:String ):(Int,Int) = {
		var dims = dimensionCache(imageUri)
		if( dims == null ) {
			val imageBlob = datastore(imageUri)
			if( imageBlob == null ) {
				throw new FileNotFoundException("Couldn't find image to get dimensions from: "+imageUri)
			}
			
			val magic = extractMagicNumber(imageBlob)
			val format = extractImageType( magic )
			if( format == null ) {
				System.err.println("Couldn't determine type of "+imageUri+": "+magic(0)+","+magic(1)+","+magic(2)+","+magic(3))
				return null
			}
			
			val inputStream = new ByteBlobInputStream(imageBlob.chunkIterator())
			dims = extractImageDimensions(inputStream, format.name)
			inputStream.close()
			dimensionCache(imageUri) = dims
		}
		dims
	}
}
