// instanceOf is a comparison operator; casing is not significant.
isUser = candidate instanceof User;
isAdmin = candidate InstanceOf "admin.User";
// Slicing, with each bound present and each one omitted.
initials = fullName[1:1];
window = readings[start:finish];
everyOther = readings[1:10:2];
head = readings[:6];
tail = readings[4:];
// A plain subscript still takes the subscript path, and [:] is still an empty
// ordered struct rather than a slice with no bounds.
first = readings[1];
empty = [:];
// instanceOf stays usable as an ordinary name. Making it a keyword broke
// `function instanceOf()`, which ColdBox's Matcher and TestBox's Assertion both declare.
instanceOf = candidate;
matched = matcher.instanceOf("admin.User");
