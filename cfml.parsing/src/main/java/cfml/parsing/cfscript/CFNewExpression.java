package cfml.parsing.cfscript;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.script.CFScriptStatement;
import cfml.parsing.util.ArrayBuilder;

public class CFNewExpression extends CFExpression {
	
	private static final long serialVersionUID = 1L;
	
	private CFExpression componentPath;
	private List<CFExpression> args;
	private String pathPrefix;
	
	public CFNewExpression(Token _t, CFExpression _component, ArrayList<CFExpression> _args) {
		this(_t, _component, null, _args);
	}
	
	/**
	 * Lucee types the path being instantiated: <code>new java:java.io.File(p)</code>. The prefix has
	 * to be kept separate from the path -- writing it back with a dot would name a different class.
	 */
	public CFNewExpression(Token _t, CFExpression _component, String _pathPrefix, ArrayList<CFExpression> _args) {
		super(_t);
		pathPrefix = _pathPrefix;
		componentPath = _component;
		if (componentPath != null) {
			componentPath.setParent(this);
		}
		args = _args;
		if (args != null) {
			args.forEach(arg -> arg.setParent(this));
		}
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");
		if (pathPrefix != null) {
			sb.append(pathPrefix).append(":");
		}
		sb.append(componentPath.Decompile(0));
		sb.append("(");
		for (int i = 0; i < args.size(); i++) {
			sb.append(args.get(i).Decompile(0));
			if (i < args.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		
		return sb.toString();
	}
	
	/** The type prefix of a Lucee path, without the colon, or null for a plain component path. */
	public String getPathPrefix() {
		return pathPrefix;
	}
	
	public CFExpression getComponentPath() {
		return componentPath;
	}
	
	public List getArgs() {
		return args;
	}
	
	@Override
	public List<CFExpression> decomposeExpression() {
		List<CFExpression> retval = new ArrayList<CFExpression>();
		retval.add(componentPath);
		retval.addAll(args);
		return retval;
	}
	
	@Override
	public List<CFScriptStatement> decomposeScript() {
		return ArrayBuilder.createCFScriptStatement();
	}
}
