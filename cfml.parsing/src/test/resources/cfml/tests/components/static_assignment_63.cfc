component {
	static myStatic = "v";
	final myConst = "c";

	function f() {
		var local1 = myStatic;
		return local1;
	}
}
