package togos.picgrid.image

import java.io.File

import scala.collection.mutable.HashMap

import togos.picgrid.file.FileUtil.makeParentDirs
import togos.picgrid.file.FSDatastore
import togos.picgrid.file.FSSHA1Datastore
import togos.picgrid.file.FileBlob
import togos.picgrid.CommandLine
import togos.picgrid.FunctionCache
import togos.picgrid.MemoryFunctionCache

class ImageMagickResizer( val functionCache:FunctionCache, val datastore:FSDatastore, val imConvert:CommandLine )
{
	val imageInfoExtractor = new ImageInfoExtractor(functionCache, datastore)
	
	def resize( infile:File, newWidth:Int, newHeight:Int, outFile:File ):Process = {
		makeParentDirs( outFile )
		// "The 'Box' filter setting is exactly the same as 'point' with one slight variation.
		// When shrinking images it will average, and merge the pixels together.
		// The smaller the resulting image the more pixels will be averaged together."
		// -- https://www.imagemagick.org/Usage/filter/
		// WHICH IS WHAT I WANT, NOT THIS BLURRY CRAP WHEN UPSCALING!
		// Also, if you change this stuff, be sure to update ImageMagickCropResizer, too.
		// And CompoundImageRasterizer.  It's got some imagemagick commands in it, too.
		val filter = "Box";
		val args = Array[String](
			infile.getPath(),
			"-filter",filter,
			"-thumbnail",(newWidth+"x"+newHeight+">"),
			"-quality","85",
			outFile.getPath()
		)
		
		imConvert.start(args)
	}
	
	def resize( origUri:String, boxWidth:Int, boxHeight:Int ):String = {
		val origBlob = datastore( origUri )
		if( origBlob == null ) throw new Exception("Couldn't find "+origUri)
		if( !origBlob.isInstanceOf[FileBlob] ) throw new Exception("Can only work with file blobs; got a "+origBlob.getClass())
		val tempFile = datastore.tempFile(".jpg")
		val imResult = resize( origBlob.asInstanceOf[FileBlob].getFile(), boxWidth, boxHeight, tempFile ).waitFor()
		if( imResult != 0 ) {
			throw new RuntimeException("convert returned non-zero status: "+imResult)
		}
		val len = tempFile.length()
		return datastore.storeAndRemove( tempFile )
	}
}
object ImageMagickResizer
{
	def main( args:Array[String] ) {
		var datastoreDir:File = null
		var inFilename:String = null
		var outFile:File = null
		var w = 512
		var h = 384
		
		var i = 0
		while( i < args.length ) {
			if( "-datastore".equals(args(i)) ) {
				i += 1
				datastoreDir = new File(args(i))
			} else if( "-w".equals(args(i)) ) {
				i += 1
				w = Integer.parseInt(args(i))
			} else if( "-h".equals(args(i)) ) {
				i += 1
				h = Integer.parseInt(args(i))
			} else if( "-o".equals(args(i)) ) {
				i += 1
				outFile = new File(args(i))
			} else if( !args(i).startsWith("-") ) {
				inFilename = args(i)
			} else {
				throw new RuntimeException("Unrecognised argument: "+args(i))
			}
			i += 1
		}
		
		val hm = new HashMap[(String,String),String]()
		hm( ("x","y") ) = "Z"
		hm.isDefinedAt( ("x","y") )
		hm( ("x","y") )
		
		val functionCache:FunctionCache = new MemoryFunctionCache()
		val datastore = if( datastoreDir == null ) null else new FSSHA1Datastore(datastoreDir)
		
		val imr = new ImageMagickResizer( functionCache, datastore, ImageMagickCommands.convert )
		if( datastore == null ) {
			if( inFilename == null ) throw new RuntimeException("No input file specified")
			if( outFile == null ) throw new RuntimeException("No output file specified")
		
			imr.resize( new File(inFilename), w, h, outFile )
		} else {
			val thumbUri = imr.resize( inFilename, w, h )
			System.out.println( "Resize result = "+thumbUri )
		}
	}
}
