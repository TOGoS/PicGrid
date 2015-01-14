package togos.picgrid.app

import java.io.File
import org.bitpedia.util.Base32
import scala.collection.mutable.ArrayBuffer
import scala.collection.TraversableLike
import togos.blob.ByteBlob
import togos.blob.ByteChunk
import togos.mf.api.Callable
import togos.mf.api.Response
import togos.mf.api.Request
import togos.mf.api.ResponseCodes
import togos.mf.base.BaseResponse
import togos.picgrid.BitprintDigest
import togos.picgrid.file.FileBlob
import togos.picgrid.file.FSSHA1Datastore
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.webserver.WebServer

class WebPathToURNSourceAdapter( val source:(String=>ByteBlob) ) extends (String=>ByteBlob)
{
	val pattern = """/raw/([^/]+)/.*""".r
	
	def apply( webPath:String ) = webPath match {
		case pattern(uri) =>
			System.err.println("Look fer " + uri)
			source(uri)
		case _ =>
			System.err.println("No match: " + webPath)
			null
	}
}

class BlobFunctionCallable( val blobFunction:(String=>ByteBlob) ) extends Callable {
	def call( req:Request ):Response = {
		val b:ByteBlob = blobFunction(req.getResourceName())
		if( b == null ) return BaseResponse.RESPONSE_UNHANDLED
		
		val fmt = ImageInfoExtractor.extractImageType(b)
		val ct = if( fmt == null ) null else fmt.mimeType;
		new BaseResponse( ResponseCodes.NORMAL, b, ct )
	}
}

object IDCommand
{
	def identify( f:File ):String = {
		val digestor = new BitprintDigest()
		val chunkIter = new FileBlob(f).chunkIterator()
		while(chunkIter.hasNext()) {
			val chunk:ByteChunk = chunkIter.next().asInstanceOf[ByteChunk]
			digestor.update(chunk.getBuffer(), chunk.getOffset(), chunk.getSize())
		}
		val hash = digestor.digest
		val sha1Hash = new Array[Byte](20)
		System.arraycopy(hash, 0, sha1Hash, 0, 20);
		val tigerTreeHash = new Array[Byte](24);
		System.arraycopy(hash, 20, tigerTreeHash, 0, 24);
		val sha1String = Base32.encode(sha1Hash)
		return "urn:bitprint:" + sha1String + "." + Base32.encode(tigerTreeHash)
	}

	def usage( cmdName:String ) =
		"Usage: "+cmdName+" <file> ...\n" +
		"\n" +
		"Prints out hashes of named files."

	def main( args:Array[String] ) {
		for( arg <- args ) {
			if( "-?" == arg ) {
				System.out.println(usage("id"))
				System.exit(0)
			}
		}
		for( arg <- args ) {
			if( args.length > 1 ) {
				System.out.print(arg + "\t")
			}
			System.out.println(identify(new File(arg)))
		}
	}
}

object WebServerCommand
{
	def usage( cmdName:String ) =
		"Usage: "+cmdName+" [options]\n"+
		"Options:\n" +
		"  -port <port>\n" +
		"  -repo <repo-root>\n";
	
	def main( cmdName:String, args:Array[String] ) {
		val datastorePaths = new ArrayBuffer[String]
		
		var i = 0
		var port = 80
		while( i < args.length ) {
			if( "-repo".equals(args(i)) ) {
				i += 1
				for( f <- new File(args(i) + "/data" ).listFiles ) {
					if( f.isDirectory() ) {
						datastorePaths += f.getPath()
					}
				}
			} else if( "-port".equals(args(i)) ) {
				i += 1
				port = Integer.parseInt(args(i))
			} else if( "-?".equals(args(i)) ) {
				System.out.println(usage(cmdName))
				System.exit(0)
			} else {
				System.err.println("Unrecognized argument: "+args(i) )
				System.err.println(usage(cmdName))
				System.exit(1)
			}
			i += 1
		}
		
		val resourceCallable = new BlobFunctionCallable(new WebPathToURNSourceAdapter(new FSSHA1Datastore( null, datastorePaths.toList )))
		val indexCallable = new Callable() {
			def call( req:Request ):Response = {
				if( req.getResourceName() != "/" ) return BaseResponse.RESPONSE_UNHANDLED
				
				new BaseResponse( ResponseCodes.NORMAL,
					"To request a resource, use a URL of the format:\n" +
					"   raw/<urn>/<filename>\n" +
					"\n" +
					"For example:\n" +
					"   raw/urn:bitprint:PEFT4NFUHPH2EH5O7ARWA3KOO37WXKFK.MJZBF6ETFXRZ6JE4M5QONFZZEAKUWUYDS3TPBMI/151612-GEDC8683.JPG\n"
				);
			}
		}
		
		val ws = new WebServer()
		ws.port = port
		ws.addRequestHandler( indexCallable )
		ws.addRequestHandler( resourceCallable )
		ws.run()
	}
	
	def main( args:Array[String] ) { main("webserve", args) }
}

object PicGridCommand {
	type Command = { def main( args:Array[String] ):Unit }
	
	val USAGE =
		"Usage: picgrid <subcommand> [options] ...\n" +
		"Sub-commands:\n" +
		"  compose\n" +
		"  webserve\n" +
		"  id\n" +
		"Run '<subcommand> -?' for help with specific commands."
	
	def main( args:Array[String] ) {
		if( args.length == 0 ) {
			System.err.println("No sub-command given")
			System.err.println(USAGE)
			System.exit(1)
		}
		
		val subCmdName = args(0)
		val subCmdArgs = args.slice(1,args.length)
		subCmdName match {
		case "compose" =>
			ComposeCommand.main("picgrid compose", subCmdArgs)
		case "webserve" =>
			WebServerCommand.main(subCmdArgs)
		case "id" =>
			IDCommand.main(subCmdArgs)
		case "-?" | "-h" | "-help" | "--help" =>
			System.out.println(USAGE)
		case _ =>
			System.err.println("Unrecognised command: "+subCmdName)
			System.err.println(USAGE)
			System.exit(1)
		} 
	}
}
