package togos

import togos.mf.value.ByteBlob

package object picgrid
{
	type Datasource = Function[String,ByteBlob]
	
	/*
	 * This doesn't work because the definition for Updatable doesn't compile:
	type Updatable[X,Y] { def update( k:X, v:Y ):Unit }
	type UpdatablePartialFunction[X,Y] = PartialFunction[X,Y] with Updatable[X,Y]
	type FunctionCache = UpdatablePartialFunction[(String,String),String]
	*/
	
	type FunctionCache = {
		def apply( k:(String,String) ):String
		def update( k:(String,String), v:String ):Unit
	}
	
	type Encoder[Decoded,Encoded] = {
		def apply[Decoded]( i:Decoded ):Encoded
		def unapply[Encoded]( i:Encoded ):Option[Decoded]
	}
}
