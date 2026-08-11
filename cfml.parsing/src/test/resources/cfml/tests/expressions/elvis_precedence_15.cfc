component {
	function precedence() {
		var add = alpha ?: beta + 1;
		var neg = -alpha ?: beta;
		var addLeft = alpha + beta ?: gamma;
		var cmp = alpha ?: beta eq gamma;
		var cat = alpha ?: beta & "z";
		var grouped = (alpha ?: beta) & "z";
		return add;
	}
}