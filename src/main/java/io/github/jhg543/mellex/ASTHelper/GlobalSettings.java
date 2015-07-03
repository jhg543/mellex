package io.github.jhg543.mellex.ASTHelper;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSettings {
	// TODO SPLIT TO GLOBAL & SESSION META
	Map<ObjectName, CreateTableStmt> permanenttable = new ConcurrentHashMap<>();
	Map<ObjectName, CreateTableStmt> volatiletable = new ConcurrentHashMap<>();
	Map<String, CreateTableStmt> c = new ConcurrentHashMap<>();
	CreateTableStmt DUPLICATE_MARKER = new CreateTableStmt();
	boolean recordSessionDependency = false;
	boolean iscasesensitive = false;
	boolean stoponerror = false;
	List<ObjectName> consume = new ArrayList<ObjectName>();
	List<ObjectName> mustconsume = new ArrayList<ObjectName>();
	List<ObjectName> provide = new ArrayList<ObjectName>();

	private static final Logger log = LoggerFactory.getLogger(GlobalSettings.class);
	private static GlobalSettings meta = new GlobalSettings();

	public static CreateTableStmt queryTable(ObjectName name2) {

		ObjectName name = new ObjectName();
		name.ns.addAll(name2.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$' && x.charAt(1) != '{') {
				name.ns.set(0, "${" + x.substring(1) + "}");
			}
		}

		if (meta.recordSessionDependency) {
			meta.consume.add(name);
		}

		CreateTableStmt result = null;
		result = meta.permanenttable.get(name);
		if (result != null) {
			return result;
		}

		result = meta.volatiletable.get(name);
		if (result != null) {
			return result;
		}

		/*
		 * if (name.ns.size()>1) { throw new RuntimeException(name +
		 * "meta not found"); }
		 */

		result = meta.c.get(name.ns.get(name.ns.size() - 1));

		if (result == null) {
			log.warn(name + "meta not found");
			CreateTableStmt s = new CreateTableStmt();
			s.dbobj = name;
			s.setInitialized(false);
			return s;
			// throw new RuntimeException(name + "meta not found");
		}

		if (result == meta.DUPLICATE_MARKER) {
			if (meta.stoponerror) {
				throw new RuntimeException(name + "found multiple table name match");
			}
			CreateTableStmt s = new CreateTableStmt();
			s.dbobj = name;
			s.setInitialized(false);
			return s;
		}

		log.warn(name + " found table name match " + result.dbobj);

		return result;
	}

	public static void putTable(CreateTableStmt stmt, boolean isvolatile) {

		ObjectName name = new ObjectName();
		name.ns.addAll(stmt.dbobj.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$' && x.charAt(1) != '{') {
				name.ns.set(0, "${" + x.substring(1) + "}");
			}
		}

		if (isvolatile) {
			// SHOULD BE ATOMICREFEREMCE . COMPAREANDSWAP
			// TODO LAZY
			if (meta.volatiletable.put(name, stmt) != null) {
				throw new RuntimeException("duplicate volatile talbe" + name);
			}
		} else {
			if (meta.recordSessionDependency) {
				meta.provide.add(name);
			}
			if (meta.permanenttable.containsKey(name)) {
				log.warn("duplicate create table" + name);
			} else {
				meta.permanenttable.put(name, stmt);
			}
			String tblname = stmt.dbobj.ns.get(stmt.dbobj.ns.size() - 1);
			if (meta.c.put(tblname, stmt) != null) {
				meta.c.put(tblname, meta.DUPLICATE_MARKER);
			}
		}
	}

	public static void setSessionRecord(boolean recordSessionDependency) {
		meta.recordSessionDependency = recordSessionDependency;
	}

	public static boolean isCaseSensitive() {
		return meta.iscasesensitive;
	}

	public static void setCaseSensitive(boolean isCaseSensitive) {
		meta.iscasesensitive = isCaseSensitive;
	}

	public static void clearsession()

	{
		meta.volatiletable.clear();
		if (meta.recordSessionDependency) {
			meta.consume.clear();
			meta.mustconsume.clear();
			meta.provide.clear();
		}
	}

	public static void consumeTable(ObjectName name) {
		if (meta.recordSessionDependency) {
			meta.consume.add(name);
		}
	}

	public static void mustConsumeTable(ObjectName name) {
		if (meta.recordSessionDependency) {
			meta.mustconsume.add(name);
		}
	}

	public static void printrecord(PrintWriter out) {
		Set<String> consume = new LinkedHashSet<String>();
		Set<String> provide = new LinkedHashSet<String>();
		Set<String> internal = new LinkedHashSet<String>();
		Set<String> consumemust = new LinkedHashSet<String>();
		for (ObjectName c : meta.consume) {
			consume.add(c.toDotString());
		}
		for (ObjectName c : meta.provide) {
			provide.add(c.toDotString());
		}
		for (ObjectName c : meta.volatiletable.keySet()) {
			internal.add(c.toDotString());
		}
		for (ObjectName c : meta.mustconsume) {
			consumemust.add(c.toDotString());
		}
		consume.removeAll(provide);
		consume.removeAll(internal);
		consumemust.removeAll(provide);
		consumemust.removeAll(internal);

		out.println("Provide\n" + provide.size() + "\n" + provide.toString());
		out.println("Internal\n" + internal.size() + "\n" + internal.toString());
		out.println("Consume\n" + consume.size() + "\n" + consume.toString());
		out.println("ConsumeM\n" + consumemust.size() + "\n" + consumemust.toString());

	}

}
