package cfml.parsing.cfscript.script;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.util.ArrayBuilder;

/**
 * A <code>static { ... }</code> block inside a component.
 *
 * Without this the block had no visitor at all, so ANTLR's default <code>visitChildren</code>
 * flattened it: <code>component { static { myVar = "v"; } }</code> decompiled to
 * <code>component { myVar = 'v' }</code>, losing the fact that the member was static. Members
 * are held in source order and reachable through {@link #decomposeScript()}.
 *
 * A member may carry its own access type -- <code>static { public myVar = "v"; }</code> -- which
 * is recorded per member rather than on the block, since that is where CFML puts it.
 */
public class CFStaticBlockStatement extends CFParsedStatement {

	private static final long serialVersionUID = 1L;

	private final List<CFScriptStatement> members;
	private final List<String> accessTypes;

	public CFStaticBlockStatement(Token _t, List<CFScriptStatement> _members, List<String> _accessTypes) {
		super(_t);
		members = _members == null ? new ArrayList<CFScriptStatement>() : _members;
		accessTypes = _accessTypes == null ? new ArrayList<String>() : _accessTypes;
		for (CFScriptStatement member : members) {
			if (member != null) {
				member.setParent(this);
			}
		}
	}

	/** The block's members, in source order. */
	public List<CFScriptStatement> getMembers() {
		return members;
	}

	/**
	 * The access type written on each member, positionally aligned with {@link #getMembers()}.
	 * Null where a member carried none, which is the common case.
	 */
	public List<String> getAccessTypes() {
		return accessTypes;
	}

	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(Indent(indent));
		sb.append("static {\n");
		for (int i = 0; i < members.size(); i++) {
			sb.append(Indent(indent + 2));
			String accessType = i < accessTypes.size() ? accessTypes.get(i) : null;
			if (accessType != null) {
				sb.append(accessType).append(" ");
			}
			sb.append(members.get(i).Decompile(0));
			sb.append(";\n");
		}
		sb.append(Indent(indent)).append("}");
		return sb.toString();
	}

	@Override
	public List<CFExpression> decomposeExpression() {
		return ArrayBuilder.createCFExpression();
	}

	@Override
	public List<CFScriptStatement> decomposeScript() {
		return members;
	}
}
