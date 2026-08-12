// Script-syntax tags: the cf-prefixed spelling, and attributes separated by spaces
// rather than commas. Both are how the engines' own test suites write these.
cffile action="write" file=destination output=body;
cfdirectory( directory=target action="list" name="found" );
// Space at the first junction, comma later on -- both are allowed after the first.
cflog( file=logFile text="started", type="information" );
// A parenthesised tag keeps its body.
cfquery( name="users" datasource=ds ) { echo("SELECT 1"); }
application action="update" NULLSupport=nullSupport;
include template="header.cfm" runOnce=true;
