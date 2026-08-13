// A function call as an attribute value. The call's own optional block body used to
// swallow the tag's body, leaving the tag with nothing to match.
query name="results" cachedWithin=createTimeSpan(0,0,0,5) {
	echo("select 1");
}
// The same tag with a plain attribute value, and with no body at all.
query name="other" cachedWithin=timeout {
	echo("select 2");
}
cfdirectory directory=expandPath("/tmp") action="list";
