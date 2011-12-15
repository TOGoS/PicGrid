package togos.picgrid.image

import java.io.BufferedReader
import java.io.InputStreamReader
import scala.collection.mutable.LinkedList
import java.util.ArrayList
import scala.collection.mutable.ListBuffer
import togos.picgrid.DigestUtil
import togos.picgrid.BlobConversions._
import togos.blob.ByteBlob
import togos.blob.util.ByteBlobInputStream

class CompoundImageComponent(
	val x:Integer, val y:Integer, val width:Integer, val height:Integer,
	val uri:String, val name:String
) {
	override def toString():String = {
		x+","+y+","+width+","+height+" "+uri+" name='"+name+"'"
	}
}

class CompoundImage(
	val width:Integer, val height:Integer,
	val components:Seq[CompoundImageComponent],
	val promotedImage1Uri:String, val promotedImage2Uri:String,
	val totalImageCount:Integer, val generatedFromUri:String
) {
	def aspectRatio = width.toFloat / height
	
	def serialize():String = {
		val sb = new StringBuilder()
		sb.append("COMPOUND-IMAGE "+width+","+height+"\n")
		if( promotedImage1Uri != null ) {
			sb.append("PROMOTE1 "+promotedImage1Uri+"\n")
		}
		if( promotedImage2Uri != null ) {
			sb.append("PROMOTE2 "+promotedImage2Uri+"\n")
		}
		for( c <- components ) {
			sb.append("COMPONENT "+c+"\n")
		}
		if( generatedFromUri != null ) {
			sb.append("GENERATED-FROM "+generatedFromUri+"\n")
		}
		sb.append("TOTAL-IMAGE-COUNT "+totalImageCount+"\n")
		sb.toString()
	}
	
	def withoutMetadata = new CompoundImage( width, height, components, null, null, 0, null )
	
	lazy val graphicUrn = "urn:sha1:"+DigestUtil.sha1Base32(withoutMetadata.serialize())
}
object CompoundImage
{
	val CI_LINE        = """^COMPOUND-IMAGE (\d+),(\d+)$""".r
	val PROMOTE1_LINE  = """^PROMOTE1 (\S+)$""".r
	val PROMOTE2_LINE  = """^PROMOTE2 (\S+)$""".r
	val COMPONENT_LINE = """^COMPONENT (\d+),(\d+),(\d+),(\d+) (\S+)(?:\s+name='([^']*)')?$""".r
	val COUNT_LINE     = """^TOTAL-IMAGE-COUNT (\d+)$""".r
	val SOURCE_LINE    = """^GENERATED-FROM (\S+)$""".r
	
	def unserialize( b:ByteBlob ):CompoundImage = {
		val br = new BufferedReader( new InputStreamReader( new ByteBlobInputStream(b.chunkIterator()) ) )
		var line = br.readLine()
		var width,height = 0
		var components = new ListBuffer[CompoundImageComponent]()
		var promotedImage1Uri, promotedImage2Uri : String = null
		var totalImageCount = components.length
		var generatedFromUri:String = null
		while( line != null ) {
			line match {
				case CI_LINE(w,h) =>
					width = w.toInt
					height = h.toInt
				case COUNT_LINE(c) =>
					totalImageCount = c.toInt
				case SOURCE_LINE(c) =>
					generatedFromUri = c
				case PROMOTE1_LINE(uri) =>
					promotedImage1Uri = uri
				case PROMOTE2_LINE(uri) =>
					promotedImage2Uri = uri
				case COMPONENT_LINE(x,y,w,h,uri,name) =>
					components += new CompoundImageComponent(x.toInt,y.toInt,w.toInt,h.toInt,uri,name)
			}
			line = br.readLine()
		}
		new CompoundImage( width, height, components.toList, promotedImage1Uri, promotedImage2Uri, totalImageCount, generatedFromUri )
	}
	
	implicit def compoundImageAsBlob( i:CompoundImage ):ByteBlob = i.serialize()
	implicit def blobAsCompoundImage( b:ByteBlob ):CompoundImage = unserialize(b)
}
