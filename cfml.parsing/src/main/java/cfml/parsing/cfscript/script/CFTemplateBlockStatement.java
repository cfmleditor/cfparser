package cfml.parsing.cfscript.script;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.util.ArrayBuilder;

/**
 * Lucee's template block: markup embedded in cfscript between <code>```</code> fences, the mirror
 * of <code>&lt;cfscript&gt;</code> dropping the other way.
 *
 * The body is markup rather than cfscript, so it is kept as its source text and written back
 * unchanged. What a consumer needs from it is the interpolated <code>#...#</code> expressions,
 * which are parsed and reachable through {@link #getExpressions()} and {@link
 * #decomposeExpression()} -- a variable used only inside a template block is still a use.
 */
public class CFTemplateBlockStatement extends CFParsedStatement {

	private static final long serialVersionUID = 1L;

	private static final String FENCE = "```";

	private String content;
	private List<CFExpression> expressions;

	public CFTemplateBlockStatement(Token _t, String _content, List<CFExpression> _expressions) {
		super(_t);
		content = _content;
		expressions = _expressions == null ? new ArrayList<CFExpression>() : _expressions;
		for (CFExpression expression : expressions) {
			expression.setParent(this);
		}
	}

	/** The markup between the fences, exactly as written, interpolations included. */
	public String getContent() {
		return content;
	}

	/** The <code>#...#</code> expressions inside the block, in source order. */
	public List<CFExpression> getExpressions() {
		return expressions;
	}

	@Override
	public String Decompile(int indent) {
		return FENCE + content + FENCE;
	}

	@Override
	public List<CFExpression> decomposeExpression() {
		return expressions;
	}

	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
}
