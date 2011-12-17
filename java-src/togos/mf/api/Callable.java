package togos.mf.api;

public interface Callable
{
	/** Send a request and expect a response in return. */
	public Response call( Request req );
}
