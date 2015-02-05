package togos.ccouch2.store

trait AutoStore[A,B] extends Function[A,B] with Sink[B,A]
