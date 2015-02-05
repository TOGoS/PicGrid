package togos.picgrid

import java.io.File

import java.io.FileNotFoundException
import java.lang.Integer

import scala.collection.mutable.ArrayBuffer

import togos.picgrid.BlobConversions._
import togos.picgrid.file.FSDatastore
import togos.picgrid.file.FileBlob
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.ImageFormat
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.ImageResizer

class CompoundImageRasterizer(
	val functionCache:FunctionCache,
	val datastore:FSDatastore,
	val imageInfoExtractor:ImageInfoExtractor,
	val resizer:ImageResizer,
	val imConvert:CommandLine
) extends Function[String,String]
{
	def apply( imageUri:String ):String = rasterize(imageUri)
	
	def rasterize( imageUri:String ):String = {
		val imageType = imageInfoExtractor.getImageType(imageUri)
		if( imageType == null ) {
			throw new Exception("Couldn't determine type of "+imageUri+" to rasterize")
		}
		if( imageType.isRaster ) return imageUri
		
		if( imageType == ImageFormat.COMPOSITE ) {
			var rasterizedUri:String = functionCache( imageUri )
			if( rasterizedUri == null ) {
				val ciData = datastore( imageUri )
				if( ciData == null ) {
					throw new Exception("Can't find "+imageUri+" to rasterize it.")
				}
				val ci = CompoundImage.unserialize( ciData )
				rasterizedUri = functionCache( ci.graphicUrn )
				if( rasterizedUri == null ) {
					rasterizedUri = rasterize( ci )
					functionCache( ci.graphicUrn ) = rasterizedUri
				}
				functionCache( imageUri ) = rasterizedUri
			}
			return rasterizedUri
		}
		throw new Exception("Don't know how to rasterize image type: "+imageType)
	}
	
	def aspectRatio( w:Int, h:Int ) = w.toFloat / h
	
	def rasterize( ci:CompoundImage ):String = {
		if( ci.equivalentImageUri != null ) {
			return rasterize( ci.equivalentImageUri )
		}
		
		// http://www.imagemagick.org/Usage/layers/
		
		val maxCmdLength = 2048*3 // Not a hard limit, but quit adding args when this is exceeded
		
		// If we need to put together more images than we can fit
		// -draw arguments for in a single command (using 3/4 of 8192 as
		// a guideline since Windows can't handle commands longer than
		// 8191), then draw images in batches, using the previous batch's
		// output as the starting point for the next batch (currentFile)
		
		// Starting points for drawing
		var currentImage = "xc:black"
		// Result file from last iteration
		var currentFile:File = null
		
		var compList = ci.components
		while( compList.length > 0 ) {
			val destFile = datastore.tempFile(".jpg")
			val args = ArrayBuffer[String]( "-size", ci.width + "x" + ci.height, currentImage )
			var argLen = 256
			while( compList.length > 0 && argLen < maxCmdLength ) {
				val comp = compList.head
				compList = compList.tail
				val compRasterUri = rasterize(comp.uri)
				val (origWidth,origHeight) = imageInfoExtractor.getImageDimensions(compRasterUri)
				val arRat = aspectRatio(origWidth, origHeight) / aspectRatio(comp.width, comp.height) 
				val scaledRasterUri =
					if( arRat >= 0.95 && arRat <= 1.05 ) {
						compRasterUri
					} else {
						resizer.resize( compRasterUri, comp.width, comp.height )
					}
				val compRasterBlob = datastore(scaledRasterUri)
				if( compRasterBlob != null && compRasterBlob.isInstanceOf[FileBlob] ) {
					val compRasterFile = compRasterBlob.asInstanceOf[FileBlob].getFile()
					args += "-draw"
						
					val s = "image over "+comp.x+","+comp.y+" "+comp.width+","+comp.height+" '"+compRasterFile+"'"
					args += s
					argLen += s.length() + 10
				} else {
					throw new FileNotFoundException("Couldn't find file for component image "+compRasterUri) 
				}
			}
			args += destFile.getPath()
			val res = imConvert.run(args.toArray[String])
			if( res != 0 ) {
				throw new Exception("convert returned non-zero status!")
			}
			if( currentFile != null ) currentFile.delete()
			currentImage = destFile.getPath()
			currentFile = destFile
		}
		
		datastore.storeAndRemove(currentFile)
	}
}
