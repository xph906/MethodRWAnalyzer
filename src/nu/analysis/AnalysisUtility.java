package nu.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import nu.analysis.values.ArrayDataValue;
import nu.analysis.values.AtomRightValue;
import nu.analysis.values.ConstantValue;
import nu.analysis.values.InstanceFieldValue;
import nu.analysis.values.RightValue;
import nu.analysis.values.StaticFieldValue;
import nu.analysis.values.UndefinedValue;
import soot.SootField;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Ref;
import soot.jimple.StaticFieldRef;

public class AnalysisUtility {
	static Comparator<Value> ValueComparator = (Value a, Value b) -> {
	    return a.toString().compareTo(b.toString());
	};
	
	// mapping 1 -> n
	// Results List<instanceFieldValue>
	static public List<RightValue> fromInstanceFieldRef2InstanceFieldValue(
			InstanceFieldRef v, DefAnalysisMap in) {
		List<RightValue> rs = new ArrayList<RightValue>();
		LinkedList<SootField> fields = new LinkedList<SootField>();

		InstanceFieldRef tmp = (InstanceFieldRef) v;
		Value b = tmp.getBase();
		SootField f = tmp.getField();
		fields.addFirst(f);
		if (in.containsKey(b)) {
			for (RightValue rv : in.get(b)) {
				if (rv instanceof ConstantValue || rv instanceof UndefinedValue) {
					System.out
							.println("ALERT fromInstanceFieldRef2InstanceFieldValue base is const or undefined.");
					System.out.println("  do nothing. " + rv);
				} else if (rv instanceof AtomRightValue) {
					List<SootField> newFields = copyList(fields);
					rs.add(new InstanceFieldValue((AtomRightValue) rv,
							newFields));
				} else if (rv instanceof InstanceFieldValue) {
					InstanceFieldValue ifv = (InstanceFieldValue) rv;
					List<SootField> newFields = ifv.getCloneFields();
					newFields.add(f);
					rs.add(new InstanceFieldValue(ifv.getBase(), newFields));
				} else {
					System.out.println("  DEBUG: getInstanceFieldValue ignore "
							+ rv + " " + rv.getClass());
				}
			}
		} else if (b instanceof Ref) {
			// TODO: needs further testing if ref's base can be ref.
			// Now we assume it shouldn't.
			System.out
					.println("ALERT: getInstanceFieldValue TODO: ref's base can be ref."
							+ b);
		} else {
			System.out
					.println("  ALERT: getInstanceFieldValue: cannot find ref's base.");
		}

		return rs;
	}

	// This function returns a list of ArrayDataValue to be updated for ArrayRef
	// assignment.
	static public List<ArrayDataValue> findLeftOpsForArrayRef(ArrayRef a,
			DefAnalysisMap in) {
		List<ArrayDataValue> rs = new ArrayList<ArrayDataValue>();
		if (in.containsKey(a.getBase())) {
			for (RightValue v : in.get(a.getBase())) {
				ArrayDataValue adv = in.findArrayDataValueFromBase(v);
				if (adv == null) {
					System.out
							.println("ALERT: cannot find ArrayDataValue for: "
									+ v);
					adv = new ArrayDataValue(v);
				}
				rs.add(adv);
			}
		}
		return rs;
	}

	// This function find all ArrayRef's values.
	// It's guaranteed that any resolved value cannot be further resolved to
	// another one
	static public Set<RightValue> resolveArrayRefRightValue(ArrayRef ar,
			DefAnalysisMap in) {
		Set<RightValue> rs = new HashSet<RightValue>();

		Value rv = ar.getBase();
		Set<RightValue> baseSet = in.get(rv);
		if (baseSet == null)
			return rs;

		for (RightValue base : baseSet) {
			ArrayDataValue adv = in.findArrayDataValueFromBase(base);
			if (adv == null) {
				System.out.println("ALERT: Cannot find ARRVAL for base: "
						+ base + ". add base!" + ar);
				// TODO: if cannot find array data, use its base. Think about
				// this design.
				rs.add(base);
			} else {
				for (RightValue v : adv.getData()) {
					rs.add(v);
				}
			}
		}
		return rs;
	}

	// find Value's Right Value.
	// if it's Ref, we might need to resolve its base first and then
	// it's guaranteed that any resulved value can not be further solved to
	// another value.
	static public Set<RightValue> resolveRightValue(Value arg,
			DefAnalysisMap in, String msg) {
		Set<RightValue> values = new HashSet<RightValue>();
		// System.out.println("YYY "+arg);
		if (arg instanceof Constant) {
			values.add(new ConstantValue(arg));
		} else if (!(arg instanceof Ref) && in.containsKey(arg)) {
			// System.out.println("  YYY1 "+arg);
			List<RightValue> tmp = new ArrayList<RightValue>(in.get(arg));
			Collections.sort(tmp, ValueComparator);
			for (RightValue v : tmp)
				values.add(v);
		} else if ((arg instanceof ArrayRef) && in.containsKey(arg)) {
			// TODO: maybe we can remove this branch?
			List<RightValue> tmp = new ArrayList<RightValue>(in.get(arg));
			Collections.sort(tmp, ValueComparator);
			for (RightValue v : tmp)
				values.add(v);
		} else if (arg instanceof Ref) {
			if (arg instanceof ArrayRef) {
				ArrayRef arrRef = (ArrayRef) arg;
				Set<RightValue> tmp = resolveArrayRefRightValue(arrRef, in);
				if (tmp == null) {
					System.out.println("ALERT: cannot resolve array: " + arrRef
							+ " from " + in);
					values = new HashSet<RightValue>();
				} else
					values = tmp;
			} else if (arg instanceof InstanceFieldRef) {
				InstanceFieldRef ifr = (InstanceFieldRef) arg;
				List<RightValue> tmp = fromInstanceFieldRef2InstanceFieldValue(
						ifr, in);
				// System.out.println("    DEBUG:  InstanceFieldRef: found "+tmp.size()+" base values.");
				for (RightValue rv : tmp) {
					if (in.containsKey(rv)) {
						for (RightValue v : in.get(rv))
							values.add(v);
					} else
						values.add(rv);
				}
			} else if (arg instanceof StaticFieldRef) {
				StaticFieldRef sfr = (StaticFieldRef) arg;
				values.add(new StaticFieldValue(sfr.getField()
						.getDeclaringClass(), sfr.getField()));
			} else {
				System.out.println("  ALERT: " + msg + " unknown ref: " + arg);
			}
		} else {
			System.out.println("ALERT: cannot resolve this non-ref value: "
					+ arg);
		}
		return values;
	}

	static public <T> List<T> copyList(List<T> another) {
		List<T> newList = new ArrayList<T>(another.size());
		for (T o : another) {
			newList.add(o);
		}
		return newList;
	}
}
