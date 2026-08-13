<cfoutput>prefix</cfoutput>
<cfset closure = t => t.b()>
<cfset lambda = t -> t.b()>
<cfset parenthesised = (x, y) => x + y>
<cfset nested = getURLs().map( target -> target.toString() )>
<!--- the '>' of a decrement still ends the tag --->
<cfset countdown = i-->
<cfset comparison = total gt 1>
