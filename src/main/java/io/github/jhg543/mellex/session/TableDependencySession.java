package io.github.jhg543.mellex.session;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * record table dependency. NOT thread safe
 * 
 * @author jhg543
 *
 */
public class TableDependencySession {

	// TODO SPLIT TO GLOBAL & SESSION META
	boolean iscasesensitive = false;
	List<ObjectName> consume = new ArrayList<ObjectName>();
	List<ObjectName> mustconsume = new ArrayList<ObjectName>();
	List<ObjectName> provide = new ArrayList<ObjectName>();
	Set<ObjectName> volatiletable = new HashSet<ObjectName>();

	private static final Logger log = LoggerFactory.getLogger(TableDependencySession.class);

	public void putTable(CreateTableStmt stmt, boolean isvolatile) {

		ObjectName name = new ObjectName();
		name.ns.addAll(stmt.dbobj.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$' && x.charAt(1) != '{') {
				name.ns.set(0, "${" + x.substring(1) + "}");
			}
		}

		if (isvolatile) {
			volatiletable.add(name);
		} else {

			provide.add(name);
		}
	}

	public void clearsession()

	{
		consume.clear();
		mustconsume.clear();
		provide.clear();
		volatiletable.clear();
	}

	public void consumeTable(ObjectName name) {
		consume.add(name);
	}

	public void mustConsumeTable(ObjectName name) {
		mustconsume.add(name);
	}


	public ScriptBlockTableDependency getRecord()
	{

		Set<String> consume = new LinkedHashSet<String>();
		Set<String> provide = new LinkedHashSet<String>();
		Set<String> internal = new LinkedHashSet<String>();
		Set<String> consumemust = new LinkedHashSet<String>();
		for (ObjectName c : this.consume) {
			consume.add(c.toDotString());
		}
		for (ObjectName c : this.provide) {
			provide.add(c.toDotString());
		}
		for (ObjectName c : this.volatiletable) {
			internal.add(c.toDotString());
		}
		for (ObjectName c : this.mustconsume) {
			consumemust.add(c.toDotString());
		}
		consume.removeAll(provide);
		consume.removeAll(internal);
		consumemust.removeAll(provide);
		consumemust.removeAll(internal);
		return new ScriptBlockTableDependency(provide,consume,consumemust);
	}
}
