package togos.picgrid.app

import togos.picgrid.file.FileUtil
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.ImageMagickCropResizer
import togos.picgrid.layout.Layouter
import togos.picgrid.Gridifier
import togos.picgrid.CompoundImageRasterizer
import togos.picgrid.CompoundImageSimpleHTMLizer
import togos.picgrid.CompoundImageHoverHTMLizer
import togos.picgrid.image.ImageMagickFallbackSource
import togos.picgrid.store.MigratingStore
import togos.picgrid.FunctionCache
import togos.picgrid.file.SLFFunctionCache
import togos.picgrid.file.SLF2FunctionCache
import togos.picgrid.GlobalContext
import togos.picgrid.MemoryFunctionCache
import togos.picgrid.image.ImageMagickCommands
import togos.picgrid.file.FSSHA1Datastore
import java.io.File
import java.io.PrintStream
import scala.collection.mutable.ListBuffer

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
		"  -debug ; dump various info to stderr while running\n" +
		"  -layouter <name>:<w>x<h> ; specify layout algorithm and maximum size\n" +
		"  -page-style <name>  ; specify style of HTML pages to be generated\n" +
		"  -convert-path <exe> ; path to convert.exe\n" +
		"  -function-cache-dir <dir> ; dir to store function results in\n" +
		"  -datastore <dir> ; dir to store output data in\n" +
		"  -datasource <dir> ; dir in which to find input data\n" +
		"  -tolerate-compose-errors ; Carry on when composite image rasterization fails\n" +
		"  -ms-datasource <dir> ; dir containing dirs in which to find input data\n" +
		"  -resource-log <file> ; file ('-' means standard output) to which to write\n" +
		"                       ; URNs of generated and referenced blobs.\n" +
		"                       ; Multiple '-resource-log's may be specified.\n" +
		"  -from-directory      ; force to treat source as a directory.\n" +
		"  -from-compound-image ; force to treat source as a compound image.\n" +
		"  -to-compound-image   ; final output is a compound image.\n" +
		"  -to-raster-image     ; final output is a raster image.\n" +
		"  -to-html             ; final output is an HTML page.\n" +
		"\n" +
		"Layout algorithms:\n" +
		"  borce    ; recursively subdivides the image list both horizontally\n" +
		"           ; and vertically.\n" +
		"  rowly    ; divides the list into rows naievely.\n" +
		"  multifit ; uses multiple algorithms and tries to pick the most\n" +
		"           ; plesant output.\n" +
		"\n" +
		"Page styles:\n" +
		"  simple   ; undecorated image thumbnails\n" +
		"  hover    ; thumbnails have hover borders and labels"
	
	def main( cmdName:String, args:Array[String] ) {
		var datastoreDir:String = null
		var datasources:ListBuffer[String] = new ListBuffer[String]()
		var functionCacheDir:String = null
		var verbose = false
		var i = 0
		var sourceUri:String = null
		var resourceLogPaths = List[String]()
		var sourceType:String = null
		var targetType:String = "html"
		var layouterName:String = "multifit:1280x800"
		var pageStyle:String = "simple"
		while( i < args.length ) {
			args(i) match {
				case "-v" =>
					verbose = true
				case "-debug" =>
					GlobalContext.debuggingEnabled = true
				case "-tolerate-compose-errors" =>
					GlobalContext.tolerateComposeErrors = true
				case "-layouter" =>
					i += 1
					layouterName = args(i)
				case "-page-style" =>
					i += 1
					pageStyle = args(i)
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
					val sectorDirs = msd.listFiles()
					if( sectorDirs == null ) {
						System.err.println("Warning: multi-datasource directory '"+msd+"' does not exist.");
					} else for( f <- sectorDirs ) if( f.isDirectory() ) {
						datasources += f.getPath();
					}
				case "-resource-log" =>
					i += 1
					resourceLogPaths = args(i) :: resourceLogPaths
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
			// TODO: allow file paths
			// TODO: determine type by actually looking at data or object
			if( sourceUri.startsWith("rdf-subject:") || sourceUri.startsWith("x-rdf-subject:") || sourceUri.startsWith("x-parse-rdf:") ) {
				sourceType = "directory"
			} else {
				sourceType = "compound-image"
			}
		}
		
		var resourceLogStreams:Seq[PrintStream] = for( p <- resourceLogPaths ) yield p match {
			case "-" =>
				System.out
			case _ =>
				val resLogFile:File = new File(p)
				FileUtil.makeParentDirs( resLogFile )
				new PrintStream( resLogFile )
		}
		val resourceLogger:String=>Unit = ((r:String) => for( l <- resourceLogStreams ) l.println(r))
		
		val imageInfoExtractor = new ImageInfoExtractor( getCache(functionCacheDir, "image-dimensions"), datastore )
		val resizer = new ImageMagickCropResizer( datastore, ImageMagickCommands.convert,
		                                          new ImageMagickFallbackSource(datastore, ImageMagickCommands.convert) )
		
		val compoundImageUri = sourceType match {
		case "directory" =>
			val layouter = Layouter.fromString(layouterName)
			val gridifier = new Gridifier( getCache(functionCacheDir, "gridification"), datastore, imageInfoExtractor, layouter )
			
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
		
		val rasterizationCacheSuffix = if( CompoundImageRasterizer.version == 1 ) {
			""
		} else {
			"-rastv"+CompoundImageRasterizer.version
		}
		
		val rasterizationCacheFile = "rasterize"+rasterizationCacheSuffix
		val htmlizationCacheFile = "htmlization"+rasterizationCacheSuffix
		// Variations on HTMLization are handled using different keys within
		// that file.
		
		//// Rasterize
		val rasterizer:(String=>String) = new CompoundImageRasterizer(
			getCache(functionCacheDir, rasterizationCacheFile), datastore,
			imageInfoExtractor, resizer, ImageMagickCommands.convert )
		
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
		
		val htmlizer:(String=>String) = pageStyle match {
			case "simple" => new CompoundImageSimpleHTMLizer(
				getCache(functionCacheDir, htmlizationCacheFile), datastore,
				imageInfoExtractor, rasterizer, resourceLogger )
			case "hover" => new CompoundImageHoverHTMLizer(
				getCache(functionCacheDir, htmlizationCacheFile), datastore,
				imageInfoExtractor, rasterizer, resourceLogger )
			case _ =>
				System.err.println("Unrecognised page style: '"+pageStyle+"'")
				System.exit(1) ; null
		}
		
		val pageUri = htmlizer( compoundImageUri )
		resourceLogger( pageUri )
		
		if( verbose ) {
			System.out.println( "# Page URI" )
		}
		System.out.println( pageUri );
		
		for( l <- resourceLogStreams ) l.close()		
		
		if( verbose ) {
			System.out.println( "# Page URI (again, in case it scrolled away)" )
			System.out.println( pageUri );
		}
	}
	
	def main( args:Array[String] ) { main("gridify", args) }
}
