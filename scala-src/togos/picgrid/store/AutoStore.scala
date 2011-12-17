package togos.picgrid.store

trait AutoStore[A,B] extends Function[A,B] with Sink[B,A]
