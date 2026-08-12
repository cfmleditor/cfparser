package cfml.parsing.cfscript;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

/**
 * A slice subscript: <code>s[4:13]</code>, <code>s[4:13:2]</code>, or with either bound left out,
 * <code>s[:6]</code> and <code>s[4:]</code>.
 *
 * Extends {@link CFMember} so a walk that already recognises a subscript sees a slice too. The
 * inherited {@link #getExpression()} is the lower bound, which unlike an ordinary subscript may be
 * null.
 */
public class CFSliceMember extends CFMember {
	private static final long serialVersionUID = 1L;

	private CFExpression to;
	private CFExpression by;

	public CFSliceMember(Token _t, CFExpression _from, CFExpression _to, CFExpression _by) {
		super(_t, _from);
		to = _to;
		by = _by;
		if (to != null) {
			to.setParent(this);
		}
		if (by != null) {
			by.setParent(this);
		}
	}

	/** The lower bound, null when omitted as in <code>s[:6]</code>. */
	public CFExpression getFrom() {
		return getExpression();
	}

	/** The upper bound, null when omitted as in <code>s[4:]</code>. */
	public CFExpression getTo() {
		return to;
	}

	/** The step, null unless a third bound was given as in <code>s[4:13:2]</code>. */
	public CFExpression getBy() {
		return by;
	}

	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder("[");
		sb.append(decompileBound(getFrom()));
		sb.append(":");
		sb.append(decompileBound(to));
		if (by != null) {
			sb.append(":").append(by.Decompile(0));
		}
		return sb.append("]").toString();
	}

	private static String decompileBound(CFExpression bound) {
		return bound == null ? "" : bound.Decompile(0);
	}

	@Override
	public List<CFExpression> decomposeExpression() {
		List<CFExpression> retval = new ArrayList<CFExpression>();
		for (CFExpression bound : new CFExpression[] { getFrom(), to, by }) {
			if (bound != null) {
				retval.add(bound);
			}
		}
		return retval;
	}
}
