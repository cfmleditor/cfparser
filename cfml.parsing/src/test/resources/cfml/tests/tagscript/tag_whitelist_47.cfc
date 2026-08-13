// Tags that had no token at all. imap is the one bare spelling the corpora use.
imap action="close" connection="testImap";
cfimap action="open" server="mail.example.com";
cfspreadsheet action="read" src="report.xls" query="data";
cfdocument format="pdf" {
	echo("body");
}
// The bare names stay ordinary identifiers -- these are functions and variables in
// real code, and minting bare tokens for them would have reclassified all of it.
dump(results);
trace = 1;
map = {};
xml = "<a/>";
flush = false;
// ... and the cf-prefixed spellings are still usable as names too.
cfdump = 1;
imapSettings = imap;
