// The typed shorthand may carry attributes rather than an equals sign.
param string emailAddress default="";
param numeric age default=18 max=100 min=0;
// Both spellings that already parsed still do -- and the equals form no longer
// decompiles to a bare `param`, which is what it used to do.
param string mode = "live";
param name="url.page" type="numeric" default="1";
