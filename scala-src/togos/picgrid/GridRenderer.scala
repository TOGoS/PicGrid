package togos.picgrid

import togos.picgrid.image.ImageFormat
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.CompoundImage
import scala.collection.mutable.ArrayBuffer
import togos.picgrid.io.FileBlob
import java.io.FileNotFoundException

class GridRenderer(
	val functionCache:FunctionCache,
	val datastore:FSDatastore,
	val imageInfoExtractor:ImageInfoExtractor,
	val convertExe:String
) {
	def rasterize( imageUri:String ):String = {
		val imageType = imageInfoExtractor.getImageType(imageUri)
		if( imageType == null ) {
			throw new Exception("Couldn't determine type of "+imageUri+" to rasterize")
		}
		if( imageType.isRaster ) return imageUri
		if( imageType == ImageFormat.COMPOSITE ) {
			var rasterizedUri = functionCache( ("rasterize", imageUri) )
			if( rasterizedUri == null ) {
				rasterizedUri = rasterize( CompoundImage.unserialize( datastore( imageUri ) ) )
				functionCache( ("rasterize", imageUri) ) = rasterizedUri
			}
			return rasterizedUri
		}
		throw new Exception("Don't know how to rasterize image type: "+imageType)
	}
	
	def rasterize( ci:CompoundImage ):String = {
		if( ci.components.length == 1 ) {
			return rasterize( ci.components.head.uri )
		}
		
		val destFile = datastore.tempFile(".jpg")
		
		// http://www.imagemagick.org/Usage/layers/
		
		val args = ArrayBuffer[String]( convertExe, "-size", ci.width + "x" + ci.height, "xc:black" )
		for( comp <- ci.components ) {
			val compRasterUri = rasterize(comp.uri)
			val compRasterBlob = datastore(compRasterUri)
			if( compRasterBlob != null && compRasterBlob.isInstanceOf[FileBlob] ) {
				val compRasterFile = compRasterBlob.asInstanceOf[FileBlob].getFile()
				args += "-draw"
				args += "image over "+comp.x+","+comp.y+" "+comp.w+","+comp.h+" '"+compRasterFile+"'"
			} else {
				throw new FileNotFoundException("Couln't find file for component image "+compRasterUri) 
			}
		}
		args += destFile.getPath()
		
		System.err.print("$")
		for( arg <- args ) {
			System.err.print(" \""+arg+"\"")
		}
		System.err.println()
		
		val res = Runtime.getRuntime().exec(args.toArray[String]).waitFor()
		if( res != 0 ) {
			throw new Exception("convert returned non-zero status!")
		}
		
		datastore.storeAndRemove(destFile)
	}
}
