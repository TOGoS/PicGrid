package togos.picgrid

import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.ImageFormat
import togos.picgrid.image.CompoundImage
import togos.picgrid.BlobConversions.stringAsByteBlob
import togos.picgrid.BlobConversions.stringAsByteChunk
import togos.picgrid.BlobConversions.byteChunkAsString
import togos.picgrid.util.URIUtil.uriEncodePathSegment

class CompoundImageHTMLizer(
	val functionCache:FunctionCache,
	val datastore:BlobAutoStore,
	val imageInfoExtractor:ImageInfoExtractor,
	val gridRenderer:(String=>String),
	val referencedUriCallback:(String=>Unit)
) {
	def url( urn:String, name:String ):String = {
		assert( urn != null )
		referencedUriCallback(urn)
		"../" + uriEncodePathSegment(urn) + "/" + uriEncodePathSegment(if(name != null) name else urn)
	}
	
	def header( ciUri:String, ci:CompoundImage ) = {
		val titleBlock = if( ci.generatedFromUri != null ) {
			"<title>Image grid for " + ci.generatedFromUri + "</title>"
		} else {
			null
		}
		
		"<html>\n" +
		"<head>\n" +
		(if(titleBlock != null) titleBlock + "\n" else "") +
		"<meta name=\"generated-from-compound-image\" content=\""+ciUri+"\"/>\n" +
		(if(ci.generatedFromUri != null) "<meta name=\"generated-from-directory\" content=\""+ci.generatedFromUri+"\"/>\n" else "") +
		(if(ci.generatorInfo != null) "<meta name=\"generator-info\" content=\""+ci.generatorInfo+"\"/>\n" else "") +
		"<style>/* <![CDATA[ */\n" +
		"    body {\n" +
		"        background-color: black;\n" +
		"        color: white;\n" +
		"    }\n" +
		"/* ]]> */</style>\n" +
		"</head><body>\n"
	}
	
	def footer( ci:CompoundImage ) = {
		"</body></html>"
	}
	
	def imageDiv( rasterized:String, ci:CompoundImage ) = {
		val s = new StringBuilder
		val bgUrl = url(rasterized, "compond.jpg")
		s.append("<div style=\"display:table; width:100%; height:100%\">\n")
		s.append("<div style=\"display:table-cell; vertical-align:middle;\">\n")
		s.append("<div style=\"position:relative; width:"+ci.width+"px; height:"+ci.height+"px; margin:auto\">\n")
		for( ic <- ci.components ) {
			val imageType = imageInfoExtractor.getImageType(ic.uri)
			val targetUrl =
				if(imageType.isRaster) {
					url(ic.uri,ic.name)
				} else {
					url(pagify( ic.uri ), ic.name+".html")
				}
			
			s.append("<a href=\""+targetUrl+"\" style=\""+
				"display:block; background-image: url('"+bgUrl+"'); "+
				"width:"+ic.width+"px; "+"height:"+ic.height+"px; "+
				"position: absolute; top:"+ic.y+"px; left:"+ic.x+"px; " +
				"background-position: -"+ic.x+" -"+ic.y+"\"></a>\n")
		}
		s.append("</div>\n</div>\n</div>\n")
		s.toString()
	}
	
	def pagify( imageUri:String ):String = {
		var htmlUrn = functionCache(imageUri)
		if( htmlUrn != null ) return htmlUrn 
		
		val imageType = imageInfoExtractor.getImageType(imageUri)
		if( imageType.isRaster ) return imageUri
		
		if( imageType != ImageFormat.COMPOSITE ) {
			throw new Exception("Don't know how to pagify image of type "+imageType)
		}
		
		val rasterizedUrl = gridRenderer( imageUri )
		
		val ci = CompoundImage.unserialize( datastore(imageUri) )
		
		val html = header(imageUri,ci) + imageDiv( rasterizedUrl, ci ) + footer( ci )
		
		htmlUrn = datastore.store( html )
		functionCache(imageUri) = htmlUrn
		htmlUrn
	}
}
