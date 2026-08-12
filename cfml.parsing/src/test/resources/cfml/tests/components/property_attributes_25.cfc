component {
	// The typed shorthand with attributes after it: name and type used to be dropped here.
	property string email default="";
	property numeric rank default=0 required=true;
	// The two spellings that already parsed.
	property string nickname;
	property name="createdOn" type="date";
}
