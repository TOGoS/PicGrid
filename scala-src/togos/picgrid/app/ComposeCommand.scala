package togos.picgrid.app

import togos.picgrid.store.MigratingStore
import togos.picgrid.FunctionCache
import togos.picgrid.file.SLFFunctionCache
import togos.picgrid.file.SLF2FunctionCache
import togos.picgrid.MemoryFunctionCache
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.ImageMagickCommands
import java.io.File
import togos.picgrid.file.FSSHA1Datastore
import java.io.Writer
import java.io.OutputStreamWriter
import togos.picgrid.file.FileUtil
import java.io.FileWriter
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.ImageMagickCropResizer
import togos.picgrid.RowlyGridificationMethod
import togos.picgrid.Gridifier
import togos.picgrid.CompoundImageRasterizer
import togos.picgrid.CompoundImageHTMLizer

object ComposeCommand
{
	def getCache( dir:String, name:String ):FunctionCache = {
		if( dir != null ) {
			new MigratingStore(
				new SLFFunctionCache( new File(dir+"/"+name+".slf") ),
				new SLF2FunctionCache( new File(dir+"/"+name+".slf2") )
			)
		} else {
			new MemoryFunctionCache()
		}
	}
	
	def usage( cmdName:String ) =
		"Usage: "+cmdName+" [options] <source>\n"+
		"Target is the URN of a directory or compound image to render.\n" +
		"Options:\n" +
		"  -v ; be verbose\n" +
		"  -convert-path <exe> ; path to convert.exe\n" +
		"  -function-cache-dir <dir> ; dir to store function results in\n" +
		"  -datastore <dir> ; dir to store output data in\n" +
		"  -datasource <dir> ; dir in which to find input data\n" +
		"  -ms-datasource <dir> ; dir containing dirs in which to find input data\n" +
		"  -resource-log <file> ; where to write generated data URNs; may be '-'\n" +
		"    for standard output, or of the form '| command args ...' to pipe to another\n" +
		"    program.\n" +
		"  -from-directory      ; force to treat source as a directory.\n" +
		"  -from-compound-image ; force to treat source as a compound image.\n" +
		"  -to-compound-image   ; final output is a compound image.\n" +
		"  -to-raster-image     ; final output is a raster image.\n" +
		"  -to-html             ; final output is an HTML page.\n"
	
	def main( cmdName:String, args:Array[String] ) {
		var datastoreDir:String = null
		var datasources:ListBuffer[String] = new ListBuffer[String]()
		var functionCacheDir:String = null
		var verbose = false
		var i = 0
		var sourceUri:String = null
		var refStoragePath:String = null
		var sourceType:String = null
		var targetType:String = "html"
		while( i < args.length ) {
			args(i) match {
				case "-v" =>
					verbose = true
				case "-convert-path" =>
					i += 1
					ImageMagickCommands.convertPath = args(i)
				case "-function-cache-dir" =>
					i += 1
					functionCacheDir = args(i)
				case "-datastore" =>
					i += 1
					datastoreDir = args(i) 
				case "-datasource" =>
					i += 1
					datasources += args(i)
				case "-ms-datasource" => // Multi-sector datasource (e.g. ccouch/data)
					i += 1
					val msd = new File(args(i))
					for( f <- msd.listFiles() ) if( f.isDirectory() ) {
						datasources += f.getPath();
					}
				case "-resource-log" =>
					i += 1
					refStoragePath = args(i)
				case "-from-directory" =>
					sourceType = "directory"
				case "-from-compound-image" =>
					sourceType = "compound-image"
				case "-to-compound-image" =>
					targetType = "compound-image"
				case "-to-raster-image" =>
					targetType = "raster-image"
				case "-to-html" =>
					targetType = "html"
				case arg if !arg.startsWith("-") =>
					sourceUri = arg
				case "-?" | "-h" | "-help" | "--help" =>
					System.out.println(usage(cmdName))
					System.exit(0)
				case arg =>
					System.err.println("Error: Unrecognised argument: "+arg)
					System.err.println(usage(cmdName))
					System.exit(1)
			}
			i += 1
		}
		
		if( datastoreDir == null ) {
			throw new RuntimeException("No -datastore directory specified")
		}
		val datastore = new FSSHA1Datastore(new File(datastoreDir), datasources.toList)
		if( sourceUri == null ) {
			System.err.println("Error: You must specify a source object")
			System.exit(1)
		}
		
		if( sourceType == null ) {
			if( sourceUri.startsWith("rdf-subject:") || sourceUri.startsWith("x-rdf-subject:") || sourceUri.startsWith("x-parse-rdf:") ) {
				sourceType = "directory"
			} else {
				sourceType = "compound-image"
			}
		}
		
		var resource:Process = null
		var refWriter:Writer = null
		val refLogger:String=>Unit = if( refStoragePath == null ) {
			((a:String) => {})
		} else if( refStoragePath.equals("-") ) {
			((a:String) => System.out.println(a) )
		} else if( refStoragePath.startsWith("|") ) {
			val command = refStoragePath.substring(1).trim()
			((a:String) => {
				if( refWriter == null ) {
					resource = Runtime.getRuntime().exec(command)
					refWriter = new OutputStreamWriter( resource.getOutputStream() )
					new Thread() {
						override def run() {
							val is = resource.getInputStream()
							var z = 0
							val buf = new Array[Byte](65536)
							while( z != -1 ) {
								z = is.read( buf )
								if( z > 0 ) System.out.write( buf, 0, z )
							}
							is.close()
						}
					}.start()
				}
				refWriter.write( a + "\n" )
				refWriter.flush()
			})
		} else {
			((a:String) => {
				if( refWriter == null ) {
					val refFile:File = new File(refStoragePath)
					FileUtil.makeParentDirs( refFile )
					refWriter = new FileWriter( refFile )
				}
				refWriter.write( a + "\n" )
			})
		}
		
		val imageInfoExtractor = new ImageInfoExtractor( getCache(functionCacheDir, "image-dimensions"), datastore )
		val resizer = new ImageMagickCropResizer( datastore, ImageMagickCommands.convert )
		val gridificationMethod = new RowlyGridificationMethod
		val gridifier = new Gridifier( getCache(functionCacheDir, "gridification"), datastore, imageInfoExtractor, gridificationMethod )
		val rasterizer:(String=>String) = new CompoundImageRasterizer( getCache(functionCacheDir, "rasterize"), datastore, imageInfoExtractor, resizer, ImageMagickCommands.convert )
		val loggingRasterizer = { a:String =>
			val res = rasterizer(a)
			refLogger(res)
			res
		}
		
		val htmlizer = new CompoundImageHTMLizer( getCache(functionCacheDir, "htmlization"), datastore, imageInfoExtractor, rasterizer, refLogger )
		
		val compoundImageUri = sourceType match {
		case "directory" =>
			val centry = gridifier.gridifyDir( sourceUri, null )
			if( centry == null ) {
				System.err.println("No images found!")
				return
			}
			val cinfo = centry.info
			cinfo.uri
		case "compound-image" =>
			sourceUri
		}

		if( targetType == "compound-image" ) {
			System.out.println( compoundImageUri )
			return
		}
		
		if( verbose ) {
			System.out.println( "# Compound image URI" )
			System.out.println( compoundImageUri )
		}
		
		//// Rasterize
		
		val rasterizationUri = rasterizer( compoundImageUri )

		if( targetType == "raster-image" ) {
			System.out.println( rasterizationUri )
			return
		}

		if( verbose ) {
			System.out.println( "# Raster image URI" )
			System.out.println( rasterizationUri )
		}
		
		//// Pagify

		val pageUri = htmlizer.pagify( compoundImageUri )
		refLogger( pageUri )
		
		if( verbose ) {
			System.out.println( "# Page URI" )
		}
		System.out.println( pageUri );
		
		if( refWriter != null ) refWriter.close()
		if( resource != null ) resource.waitFor()
		
		if( verbose ) {
			System.out.println( "# Page URI (again, in case it scrolled away)" )
			System.out.println( pageUri );
		}
	}
	
	def main( args:Array[String] ) { main("gridify", args) }
}
