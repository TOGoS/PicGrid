package togos

import java.lang.Integer

import togos.blob.ByteBlob
import togos.blob.ByteChunk
import togos.ccouch2.store.AutoStore
import togos.ccouch2.store.Sink
import togos.ccouch2.store.Store

package object picgrid
{
	/*
	 * This doesn't work because the definition for Updatable doesn't compile:
	type Updatable[X,Y] { def update( k:X, v:Y ):Unit }
	type UpdatablePartialFunction[X,Y] = PartialFunction[X,Y] with Updatable[X,Y]
	type FunctionCache = UpdatablePartialFunction[(String,String),String]
	*/
	
	type Encoder[Decoded,Encoded] = {
		def apply[Decoded]( i:Decoded ):Encoded
		def unapply[Encoded]( i:Encoded ):Option[Decoded]
	}
	
	type FunctionCache = Store[ByteChunk,ByteChunk]
	type BlobSource    = Function[String,ByteBlob]
	type BlobAutoStore = AutoStore[String,ByteBlob]
	type BlobSink      = Sink[ByteBlob,String]
}
