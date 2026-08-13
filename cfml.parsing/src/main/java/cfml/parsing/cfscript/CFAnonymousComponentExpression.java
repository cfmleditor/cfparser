package cfml.parsing.cfscript;

import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.script.CFCompDeclStatement;
import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.util.ArrayBuilder;

/**
 * An inline component: <code>new component javaSettings='...' { ... }</code>, which declares and
 * instantiates in one expression.
 *
 * Modelled the way {@link CFAnonymousFunctionExpression} models <code>function(){}</code> -- a
 * declaration statement hanging off an expression -- so a walk that already descends into an
 * anonymous function reaches an inline component's members the same way.
 */
public class CFAnonymousComponentExpression extends CFExpression {

	private static final long serialVersionUID = 1L;

	private CFCompDeclStatement componentDeclaration;

	public CFAnonymousComponentExpression(Token _t, CFCompDeclStatement _componentDeclaration) {
		super(_t);
		componentDeclaration = _componentDeclaration;
		if (componentDeclaration != null) {
			componentDeclaration.setParent(this);
		}
	}

	@Override
	public byte getType() {
		return CFExpression.NESTED;
	}

	public CFCompDeclStatement getComponentDeclaration() {
		return componentDeclaration;
	}

	@Override
	public String Decompile(int indent) {
		return "new " + componentDeclaration.Decompile(0);
	}

	@Override
	public List<CFExpression> decomposeExpression() {
		return ArrayBuilder.createCFExpression();
	}

	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement(componentDeclaration);
	}
}
