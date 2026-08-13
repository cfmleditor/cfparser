package cfml.parsing.cfscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.Token;

import cfml.CFSCRIPTLexer;
import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.util.ArrayBuilder;

public class CFFullVarExpression extends CFIdentifier {
	
	private static final long serialVersionUID = 1;
	
	// private Token token;
	private List<CFExpression> expressions;
	/**
	 * The operator written before a member, keyed by that member's character offset in the source.
	 * Only the non-default operators are recorded -- <code>::</code> and <code>?.</code> -- so an
	 * absent entry means an ordinary dot.
	 *
	 * Keyed by position rather than by index because the members are gathered through
	 * aggregateResult, where one source construct does not always yield one element: `a[1].b` puts
	 * three expressions in the list with a single dot between them. Source offsets survive that.
	 */
	private Map<Integer, String> memberOperators = new HashMap<Integer, String>();

	public CFFullVarExpression(Token _t, CFExpression _main) {
		super(_t);
		// token = _t;
		expressions = new ArrayList<CFExpression>();
		if (_main != null) {
			expressions.add(_main);
			_main.setParent(this);
		}
	}
	
	public CFIdentifier getIdentifier() {
		return (CFIdentifier) expressions.get(0);
	}
	
	@Override
	public String getScope() {
		return getIdentifier().getScope();
	}
	
	@Override
	public byte getType() {
		return expressions.get(expressions.size() - 1).getType();
	}
	
	@Override
	public boolean isEscapeSingleQuotes() {
		return expressions.get(expressions.size() - 1).isEscapeSingleQuotes();
	}
	
	public void addMember(CFExpression _right) {
		expressions.add(_right);
		if (_right != null) {
			_right.setParent(this);
		}
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		for (CFExpression expression : expressions) {
			if (sb.length() > 0) {
				if (expression.getType() == CFExpression.IDENTIFIER
						&& expression.getToken().getType() == CFSCRIPTLexer.LEFTBRACKET) {
					// Array notation []
				} else if (expression.getType() == CFExpression.IDENTIFIER || expression.getType() == CFExpression.LITERAL) {
					sb.append(memberOperator(expression));
				} else if (expression instanceof CFFunctionExpression
						&& ((CFFunctionExpression) expression).getIdentifier() != null) {
					sb.append(memberOperator(expression));
				}
			}
			sb.append(expression.Decompile(0));
		}
		return sb.toString();
	}
	
	/**
	 * Records that <code>_member</code> was written after <code>_operator</code> rather than a dot.
	 * Called for <code>::</code> and <code>?.</code> only; anything else keeps the default.
	 */
	public void setMemberOperator(CFExpression _member, String _operator) {
		if (_member != null && _member.getToken() != null && _operator != null) {
			memberOperators.put(_member.getToken().getStartIndex(), _operator);
		}
	}

	/** The operator to write before this member: <code>::</code>, <code>?.</code> or a dot. */
	public String getMemberOperator(CFExpression _member) {
		return memberOperator(_member);
	}

	private String memberOperator(CFExpression _member) {
		if (_member == null || _member.getToken() == null) {
			return ".";
		}
		String operator = memberOperators.get(_member.getToken().getStartIndex());
		return operator == null ? "." : operator;
	}

	public List<CFExpression> getExpressions() {
		return expressions;
	}
	
	public CFIdentifier getLastIdentifier() {
		for (int i = expressions.size() - 1; i >= 0; i--) {
			if (expressions.get(i) instanceof CFIdentifier) {
				return (CFIdentifier) expressions.get(i);
			}
		}
		return null;
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		return expressions;
	}
	
	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
	
	@Override
	public String toString() {
		return Decompile(0);
	}
}
