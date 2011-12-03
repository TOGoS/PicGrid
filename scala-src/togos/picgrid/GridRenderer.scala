package togos.picgrid

import java.io.FileNotFoundException

import scala.collection.mutable.ArrayBuffer

import togos.picgrid.file.FSDatastore
import togos.picgrid.file.FileBlob
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.ImageFormat
import togos.picgrid.image.ImageInfoExtractor

class GridRenderer(
	val functionCache:FunctionCache,
	val datastore:FSDatastore,
	val imageInfoExtractor:ImageInfoExtractor,
	val resizer:ImageResizer,
	val imConvert:CommandLine
) {
	def rasterize( imageUri:String ):String = {
		val imageType = imageInfoExtractor.getImageType(imageUri)
		if( imageType == null ) {
			throw new Exception("Couldn't determine type of "+imageUri+" to rasterize")
		}
		if( imageType.isRaster ) return imageUri
		if( imageType == ImageFormat.COMPOSITE ) {
			var rasterizedUri = functionCache( "rasterize", imageUri )
			if( rasterizedUri == null ) {
				rasterizedUri = rasterize( CompoundImage.unserialize( datastore( imageUri ) ) )
				functionCache( "rasterize", imageUri ) = rasterizedUri
			}
			return rasterizedUri
		}
		throw new Exception("Don't know how to rasterize image type: "+imageType)
	}
	
	def aspectRatio( w:Integer, h:Integer ) = w.toFloat / h
	
	def rasterize( ci:CompoundImage ):String = {
		if( ci.components.length == 1 ) {
			return rasterize( ci.components.head.uri )
		}
		
		val destFile = datastore.tempFile(".jpg")
		
		// http://www.imagemagick.org/Usage/layers/
		
		val args = ArrayBuffer[String]( "-size", ci.width + "x" + ci.height, "xc:black" )
		for( comp <- ci.components ) {
			val compRasterUri = rasterize(comp.uri)
			val (origWidth,origHeight) = imageInfoExtractor.getImageDimensions(compRasterUri)
			val arRat = aspectRatio(origWidth,origHeight) / aspectRatio(comp.w, comp.h) 
			val scaledRasterUri =
				if( arRat >= 0.95 && arRat <= 1.05 ) {
					compRasterUri
				} else {
					resizer.resize( compRasterUri, comp.w, comp.h )
				}
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
		
		val res = imConvert.run(args.toArray[String])
		if( res != 0 ) {
			throw new Exception("convert returned non-zero status!")
		}
		
		datastore.storeAndRemove(destFile)
	}
}
