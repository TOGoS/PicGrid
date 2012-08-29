package togos.picgrid

import java.security.MessageDigest

import org.bitpedia.util.Base32

import togos.blob.ByteChunk
import togos.picgrid.BlobConversions.byteBlobAsByteChunk
import togos.picgrid.BlobConversions.byteBlobAsString
import togos.picgrid.BlobConversions.byteChunkAsByteBlob
import togos.picgrid.BlobConversions.stringAsByteArray
import togos.picgrid.BlobConversions.stringAsByteBlob
import togos.picgrid.BlobConversions.stringAsByteChunk
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.layout.Layouter

class Gridifier(
	val functionCache:FunctionCache,
	val datastore:BlobAutoStore,
	val infoExtractor:ImageInfoExtractor,
	val gridificationMethod:Layouter
) {
	val xRdfSubjectRegex = """x-rdf-subject:(.*)""".r
	val imageFilenameRegex = """(?i).*\.(?:png|jpe?g|gif|bmp)$""".r
	
	def getDirEntries( uri:String ):List[DirectoryEntry] = {
		val xRdfSubjectRegex(blobUri) = uri
		val rdfBlob = datastore(blobUri)
		if( rdfBlob == null ) {
			throw new Exception("Cannot find blob "+uri+" to parse directory entries from.");
		}
		RDFDirectoryParser.parseDirectory( rdfBlob )
	}
	
	def gridifyRasterImage( uri:String, name:String ):ImageEntry = {
		val dims = infoExtractor.getImageDimensions( uri )
		if( dims == null ) return null
		val (w,h) = dims
		new ImageEntry( name, new ImageInfo( uri, uri, w, h, 1 ) )
	}
	
	def gridify( e:DirectoryEntry ):ImageEntry = {
		e.targetClass match {
			case DirectoryObjectClass.Blob => e.name match {
				case imageFilenameRegex() => gridifyRasterImage(e.targetUri, e.name)
				case _ => null
			}
			case DirectoryObjectClass.Directory =>
				gridifyDir( e.targetUri, e.name )
		}
	}
	
	def gridify( images:Seq[ImageEntry], name:String, generatedFromUri:String ):ImageEntry = {
		if( images.length == 0 ) return null
		if( images.length == 1 ) return images.head
		
		val components = gridificationMethod.gridify( images )
		
		var totalImageCount = 0
		for( image <- images ) totalImageCount += image.info.totalImageCount
		
		var width, height = 0
		for( c <- components ) {
			if( c.x + c.width  > width  ) width  = c.x + c.width
			if( c.y + c.height > height ) height = c.y + c.height
		}
		
		val ci = new CompoundImage( width, height, components, null, null, totalImageCount, generatedFromUri )
		
		val uri = datastore.store( ci.serialize() )
		new ImageEntry( name, new ImageInfo( uri, generatedFromUri, ci.width, ci.height, ci.totalImageCount ) )
	}
	
	
	def gridifyDir( dir:Seq[DirectoryEntry], name:String, generatedFromUri:String ):ImageEntry = {
		gridify( dir.map( e => gridify(e) ).filter( e => e != null ), name, generatedFromUri )
	}
	
	def hashString( s:String ):String = {
		val sha1 = MessageDigest.getInstance("SHA-1")
		val sha1Hash = sha1.digest( s )
		Base32.encode(sha1Hash)
	}
	
	lazy val configHash = gridificationMethod.configString
	
	def gridifyDir( uri:String, name:String ):ImageEntry = {
		val cacheKey = configHash+":"+uri
		val cachedData:ByteChunk = functionCache( cacheKey )
		if( cachedData != null ) {
			SerializationUtil.unserialize(cachedData).asInstanceOf[ImageEntry]
		} else {
			val res:ImageEntry = gridifyDir( getDirEntries( uri ), name, uri )
			functionCache( cacheKey ) = SerializationUtil.serialize( res )
			res
		}
	}
}
