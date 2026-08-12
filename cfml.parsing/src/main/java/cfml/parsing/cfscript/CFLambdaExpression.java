package cfml.parsing.cfscript;

import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.script.CFFuncDeclStatement;
import cfml.parsing.cfscript.script.CFFunctionParameter;

/**
 * An arrow function: <code>(a, b) =&gt; a + b</code> or <code>(a) =&gt; { return a; }</code>.
 *
 * Modelled as an anonymous function so consumers that already recognise
 * {@link CFAnonymousFunctionExpression} see lambdas too. Only the decompiled form differs: a lambda
 * has no <code>function</code> keyword, and an expression body is an implicit return that must not
 * be written back out as one.
 */
public class CFLambdaExpression extends CFAnonymousFunctionExpression {

	private static final long serialVersionUID = 1L;

	/** Non-null when the body is a bare expression rather than a block. */
	private CFExpression expressionBody;

	/** The arrow itself: Lucee runs => as a closure and -> as a lambda, capturing scope differently. */
	private Token operator;

	public CFLambdaExpression(Token _t, Token operator, CFFuncDeclStatement funcDeclStatement,
			CFExpression expressionBody) {
		super(_t, funcDeclStatement);
		this.operator = operator;
		this.expressionBody = expressionBody;
	}

	/** The arrow token, <code>=&gt;</code> or <code>-&gt;</code>. */
	public Token getOperator() {
		return operator;
	}

	/**
	 * True for <code>=&gt;</code>, which Lucee evaluates as a closure over the enclosing scope, as
	 * against <code>-&gt;</code>, which does not capture it. The two are otherwise identical, so
	 * nothing but this tells them apart.
	 */
	public boolean isClosure() {
		return operator != null && "=>".equals(operator.getText());
	}

	/**
	 * The body of an expression-bodied lambda, or null when the body is a block. The same
	 * expression is also reachable as the argument of the implicit
	 * {@link cfml.parsing.cfscript.script.CFReturnStatement} in the function declaration, so
	 * decomposing this expression does not report it twice.
	 */
	public CFExpression getExpressionBody() {
		return expressionBody;
	}

	@Override
	public String Decompile(int indent) {
		CFFuncDeclStatement declaration = getFuncDeclStatement();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		List<CFFunctionParameter> formals = declaration.getFormals();
		for (int i = 0; i < formals.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(formals.get(i));
		}
		sb.append(") ").append(operator == null ? "=>" : operator.getText()).append(" ");
		if (expressionBody != null) {
			sb.append(expressionBody.Decompile(0));
		} else {
			sb.append(declaration.getBody().Decompile(indent));
		}
		return sb.toString();
	}

}
