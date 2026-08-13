component {
	function connect() {
		// Declares and instantiates in one expression -- the shape Lucee uses to pull a
		// Maven dependency in via javaSettings.
		var msal = new component javaSettings='{"maven":["com.microsoft.azure:msal4j:1.23.1"]}' {
			function buildApp() {
				return 1;
			}
		};
		// Without attributes, and the ordinary path forms alongside it.
		var bare = new component { function f() { return 2; } };
		var named = new models.User();
		var javaFile = new java:java.io.File(path);
		return msal;
	}
}
