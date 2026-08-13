package cfml.parsing.cfscript.script;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import cfml.parsing.cfscript.CFExpression;
import cfml.parsing.cfscript.CFIdentifier;

public class CFPropertyStatement extends CFParsedAttributeStatement {
	
	private static final long serialVersionUID = 1L;
	
	private static HashSet<String> validAttributes;
	private boolean shorthand = false;
	private CFExpression propertyName;
	private CFExpression propertyType;
	
	/*
	 * cfproperty's attributes, taken from the shipped dictionaries, which agree on all 65 across
	 * cf11 and lucee5. This set previously held cfparam's six -- name, type, default, max, min,
	 * pattern -- which is not a subset of anything cfproperty accepts beyond the first three. Every
	 * ORM and bean property in real code carries getter, setter, persistent, fieldtype or ormtype,
	 * and getValidAttributes() is public, so consumers asking cfparser what a property may carry
	 * were told cfparam's answer. See #39.
	 */
	static {
		validAttributes = new HashSet<String>();
		validAttributes.add("CACHEUSE");
		validAttributes.add("CASCADE");
		validAttributes.add("CFC");
		validAttributes.add("COLUMN");
		validAttributes.add("CONSTRAINED");
		validAttributes.add("DBDEFAULT");
		validAttributes.add("DEFAULT");
		validAttributes.add("DISPLAYNAME");
		validAttributes.add("ELEMENTCOLUMN");
		validAttributes.add("ELEMENTTYPE");
		validAttributes.add("FETCH");
		validAttributes.add("FIELDTYPE");
		validAttributes.add("FKCOLUMN");
		validAttributes.add("FORMULA");
		validAttributes.add("GENERATED");
		validAttributes.add("GENERATOR");
		validAttributes.add("GETTER");
		validAttributes.add("HINT");
		validAttributes.add("INDEX");
		validAttributes.add("INDEXABLE");
		validAttributes.add("INDEXBOOST");
		validAttributes.add("INDEXFIELDNAME");
		validAttributes.add("INDEXLANGUAGE");
		validAttributes.add("INDEXSTORE");
		validAttributes.add("INDEXTOKENIZE");
		validAttributes.add("INSERT");
		validAttributes.add("INVERSE");
		validAttributes.add("INVERSEJOINCOLUMN");
		validAttributes.add("JOINCOLUMN");
		validAttributes.add("LAZY");
		validAttributes.add("LENGTH");
		validAttributes.add("LINKCATALOG");
		validAttributes.add("LINKSCHEMA");
		validAttributes.add("LINKTABLE");
		validAttributes.add("MAPPEDBY");
		validAttributes.add("MISSINGROWIGNORED");
		validAttributes.add("NAME");
		validAttributes.add("NOTNULL");
		validAttributes.add("OPTIMISTICLOCK");
		validAttributes.add("ORDERBY");
		validAttributes.add("ORMTYPE");
		validAttributes.add("PARAMS");
		validAttributes.add("PERSISTENT");
		validAttributes.add("PRECISION");
		validAttributes.add("REMOTINGFETCH");
		validAttributes.add("REQUIRED");
		validAttributes.add("SCALE");
		validAttributes.add("SELECTKEY");
		validAttributes.add("SEQUENCE");
		validAttributes.add("SERIALIZABLE");
		validAttributes.add("SETTER");
		validAttributes.add("SINGULARNAME");
		validAttributes.add("SOURCE");
		validAttributes.add("SQLTYPE");
		validAttributes.add("STRUCTKEYCOLUMN");
		validAttributes.add("STRUCTKEYTYPE");
		validAttributes.add("TABLE");
		validAttributes.add("TYPE");
		validAttributes.add("UNIQUE");
		validAttributes.add("UNIQUEKEY");
		validAttributes.add("UNSAVEDVALUE");
		validAttributes.add("UPDATE");
		validAttributes.add("VALIDATE");
		validAttributes.add("VALIDATEPARAMS");
		validAttributes.add("WHERE");
	}

	/*
	 * validateAttributes is deliberately not called here. It throws, and parseScript does not catch,
	 * so one attribute this set does not know would abort the whole file rather than report an error
	 * on the statement. The set above is pinned to dictionaries that ship with the parser and will
	 * fall behind whatever the next CF release adds, so that failure would land on valid code.
	 */
	public CFPropertyStatement(org.antlr.v4.runtime.Token t, Map<CFIdentifier, CFExpression> _attributes) {
		super(t, _attributes);
	}
	
	@Override
	public String Decompile(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("property");
		if (shorthand) {
			sb.append(" ");
			if (propertyType != null) {
				sb.append(propertyType.Decompile(0));
				sb.append(" ");
			}
			sb.append(propertyName.Decompile(0));
		}
		// The shorthand can carry attributes as well: property string email default="";
		DecompileAttributes(sb);
		return sb.toString();
	}
	
	public static HashSet<String> getValidAttributes() {
		return validAttributes;
	}
	
	public void setIsShortHand(boolean b) {
		shorthand = b;
	}
	
	public void setPropertyName(CFExpression cfExpression) {
		propertyName = cfExpression;
	}
	
	public void setPropertyType(CFExpression cfExpression) {
		propertyType = cfExpression;
	}
	
	public List<CFExpression> decomposeExpression() {
		List<CFExpression> retval = super.decomposeExpression();
		if (propertyName != null) {
			retval.add(propertyName);
		}
		if (propertyType != null) {
			retval.add(propertyType);
		}
		return retval;
	}
	
	public CFExpression getPropertyName() {
		return propertyName;
	}
	
	public CFExpression getPropertyType() {
		return propertyType;
	}
}
