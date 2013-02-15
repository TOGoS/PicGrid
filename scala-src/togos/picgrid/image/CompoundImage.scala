package togos.picgrid.image

import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Integer

import togos.blob.util.ByteBlobInputStream
import togos.blob.ByteBlob
import togos.picgrid.BlobConversions.stringAsByteBlob
import togos.picgrid.util.StringEscape
import togos.picgrid.DigestUtil

class CompoundImageComponent(
	val x:Int, val y:Int, val width:Int, val height:Int,
	val uri:String, val name:String
) {
	override def toString():String = {
		x+","+y+","+width+","+height+" "+uri+(if(name != null) " name='"+StringEscape(name)+"'" else "")
	}
}

class CompoundImage(
	val width:Int, val height:Int,
	val components:Seq[CompoundImageComponent],
	val title:String,
	val promotedImage1Uri:String,
	val promotedImage2Uri:String,
	val generatedFromUri:String,
	val totalImageCount:Int,
	val totalByteCount:Long,
	val generatorInfo:String
) {
	def aspectRatio = width.toFloat / height
	
	def serialize():String = {
		val sb = new StringBuilder()
		sb.append("COMPOUND-IMAGE "+width+","+height+"\n")
		if( promotedImage1Uri != null ) {
			sb.append("PROMOTE1 "+promotedImage1Uri+"\n")
		}
		if( title != null ) {
			sb.append("TITLE '"+StringEscape(title)+"'\n")
		}
		if( promotedImage2Uri != null ) {
			sb.append("PROMOTE2 "+promotedImage2Uri+"\n")
		}
		for( c <- components ) {
			sb.append("COMPONENT "+c+"\n")
		}
		if( totalImageCount != -1 ) {
			sb.append("TOTAL-IMAGE-COUNT "+totalImageCount+"\n")
		}
		if( totalByteCount != -1 ) {
			sb.append("TOTAL-BYTE-COUNT "+totalByteCount+"\n")
		}
		if( generatedFromUri != null ) {
			sb.append("GENERATED-FROM "+generatedFromUri+"\n")
		}
		if( generatorInfo != null ) {
			sb.append("GENERATOR-INFO '"+StringEscape(generatorInfo)+"'\n")
		}
		sb.toString()
	}
	
	def withoutMetadata = new CompoundImage( width, height, components, null, null, null, null, -1, -1, null )
	
	/**
	 * If this compound image's content is exactly equivalent to
	 * that of a simpler image (i.e. the image of a sole component
	 * that exactly fills the entire space), returns the URI of that
	 * image.  Otherwise returns null.
	 *
	 * Metadata, pixel size, and aspect ratio may be different.
	 * It's only really 'equivalent' if those are ignored.
	 */
	def equivalentImageUri = if(
		components.length == 1 &&
		components.head.width == width && components.head.height == height &&
		components.head.x == 0 && components.head.y == 0
	) components.head.uri else null
	
	lazy val graphicUrn = "urn:sha1:"+DigestUtil.sha1Base32(withoutMetadata.serialize())
}
object CompoundImage
{
	val CI_LINE        = """^COMPOUND-IMAGE (\d+),(\d+)$""".r
	val TITLE_LINE     = """^TITLE '((?:[^'\\]|\\.)*)'$""".r
	val PROMOTE1_LINE  = """^PROMOTE1 (\S+)$""".r
	val PROMOTE2_LINE  = """^PROMOTE2 (\S+)$""".r
	val COMPONENT_LINE = """^COMPONENT (\d+),(\d+),(\d+),(\d+) (\S+)(?:\s+name='((?:[^'\\]|\\.)*)')?$""".r
	val COUNT_LINE     = """^TOTAL-IMAGE-COUNT (\d+)$""".r
	val SIZE_LINE      = """^TOTAL-BYTE-COUNT (\d+)$""".r
	val SOURCE_LINE    = """^GENERATED-FROM (\S+)$""".r
	val GENERATOR_LINE = """^GENERATOR-INFO '((?:[^'\\]|\\.)*)'$""".r
	val OTHER_GENERATOR_LINE = """^GENERATOR-INFO (\S+)$""".r
	
	def unserialize( b:ByteBlob ):CompoundImage = {
		val br = new BufferedReader( new InputStreamReader( new ByteBlobInputStream(b.chunkIterator()) ) )
		var line = br.readLine()
		var width,height = 0
		var components = new ListBuffer[CompoundImageComponent]()
		var title:String = null
		var promotedImage1Uri, promotedImage2Uri : String = null
		var totalImageCount = -1
		var totalByteCount  = -1l
		var generatedFromUri:String = null
		var generatorInfo:String = null
		while( line != null ) {
			line match {
				case CI_LINE(w,h) =>
					width = w.toInt
					height = h.toInt
				case COUNT_LINE(c) =>
					totalImageCount = c.toInt
				case SIZE_LINE(c) =>
					totalByteCount = c.toLong
				case TITLE_LINE(StringEscape(c)) =>
				     	title = c
				case PROMOTE1_LINE(uri) =>
					promotedImage1Uri = uri
				case PROMOTE2_LINE(uri) =>
					promotedImage2Uri = uri
				case COMPONENT_LINE(x,y,w,h,uri,StringEscape(name)) =>
					components += new CompoundImageComponent(x.toInt,y.toInt,w.toInt,h.toInt,uri,name)
				case SOURCE_LINE(c) =>
					generatedFromUri = c
				case GENERATOR_LINE(StringEscape(c)) =>
					generatorInfo = c
				case OTHER_GENERATOR_LINE(c) =>
					generatorInfo = c
				case _ =>
					System.err.println("Notice: Unrecognised line in CompoundImage data: "+line)
			}
			line = br.readLine()
		}
		new CompoundImage( width, height, components.toList, title, promotedImage1Uri, promotedImage2Uri, generatedFromUri, totalImageCount, totalByteCount, generatorInfo )
	}
	
	implicit def compoundImageAsBlob( i:CompoundImage ):ByteBlob = i.serialize()
	implicit def blobAsCompoundImage( b:ByteBlob ):CompoundImage = unserialize(b)
}
