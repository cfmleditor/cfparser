<cfoutput>prefix</cfoutput>
<cfset plain = 1>
<cfset selfClosed = 2 />
<cfset endsWithSlashInString = "a/" />
<cfset division = total / count />
<cffunction name="f">
	<cfreturn selfClosed />
</cffunction>