// The thin arrow. Lucee runs => as a closure and -> as a lambda, which capture scope
// differently, so the operator has to survive into the tree rather than being normalised.
increment = (x) -> x + 1;
closure = (x) => x + 1;
addBoth = (a, b) -> { return a + b; };
// A single parameter may drop the parentheses.
double = t -> t * 2;
names = urls.map( target -> target.toString() );
// -- followed by > is a decrement and a comparison, not an arrow.
shrinking = i-- > 0;
// A typed path. The prefix is not part of the path -- java.java.io.File is a different class.
handle = new java:java.io.File(target);
local = new cfml:models.User();
plain = new models.User();
// final declares an immutable local; it is not a synonym for var.
final limit = 10;
var total = 0;
// CT and NCT abbreviate contains and does not contain, and stay usable as names.
if (haystack CT needle) { found(); }
if (haystack NCT needle) { missing(); }
ct = 1;
nct = ct;
