package cfml.parsing.cfscript.script;

import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.util.ArrayBuilder;

public class CFContinueStatement extends CFParsedStatement {
	private static final long serialVersionUID = 1L;
	
	public CFContinueStatement(Token t) {
		super(t);
	}
	
	@Override
	protected String decompileImpl(int indent) {
		return Indent(indent) + "continue";
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		return ArrayBuilder.createCFExpression();
	}
	
	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
}
