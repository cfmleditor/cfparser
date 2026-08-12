component {
	// An access type either side of static, with a return type after both.
	public static string function label() { return "a"; }
	static public string function caption() { return "b"; }
	// The combinations that already worked stay working.
	static function plain() {}
	public static function noType() {}
	static string function noAccess() { return "c"; }
	private final numeric function locked() { return 1; }
}
