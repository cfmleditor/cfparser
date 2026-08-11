// A lambda passed as a call argument keeps its own body
doubled = arrayMap(list, (item) => item * 2);
// An expression body is an implicit return, not a statement
label = (item) => item.name ?: 'unnamed';
// Lambdas nest
adder = (x) => (y) => x + y;
// Parameters keep their defaults
greet = (name = 'world') => 'hello ' & name;
