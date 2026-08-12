// A space anywhere in the attribute list settles it as a tag, because a function call
// needs a comma at every junction. recurse=true used to be swallowed and dropped.
cfdirectory(directory=target, action="list" recurse=true);
// Separated entirely by commas, so this stays an ordinary call.
cfdirectory(directory=target, action="list");
// Positional arguments to a tag-named callee. The first used to come out as a named
// argument with no value, the second used to split into two statements.
cflog(message);
cflog("started");
cffile(source, destination);
