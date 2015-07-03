package io.github.jhg543.mellex.session;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.ObjectName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableDefinitionProvider {

	Map<ObjectName, CreateTableStmt> permanenttable = new ConcurrentHashMap<>();
	Map<ObjectName, CreateTableStmt> volatiletable = new ConcurrentHashMap<>();
	Map<String, CreateTableStmt> c = new ConcurrentHashMap<>();
	CreateTableStmt DUPLICATE_MARKER = new CreateTableStmt();
	private static final Logger log = LoggerFactory.getLogger(TableDefinitionProvider.class);

	public CreateTableStmt queryTable(ObjectName name2) {

		ObjectName name = new ObjectName();
		name.ns.addAll(name2.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$' && x.charAt(1) != '{') {
				name.ns.set(0, "${" + x.substring(1) + "}");
			}
		}

		CreateTableStmt result = null;
		result = permanenttable.get(name);
		if (result != null) {
			return result;
		}

		result = volatiletable.get(name);
		if (result != null) {
			return result;
		}

		/*
		 * if (name.ns.size()>1) { throw new RuntimeException(name +
		 * "meta not found"); }
		 */

		result = c.get(name.ns.get(name.ns.size() - 1));

		if (result == null) {
			log.warn(name + "meta not found");
			CreateTableStmt s = new CreateTableStmt();
			s.dbobj = name;
			s.setInitialized(false);
			return s;
			// throw new RuntimeException(name + "meta not found");
		}

		if (result == DUPLICATE_MARKER) {
			log.warn(name + "meta not found same name found");
			CreateTableStmt s = new CreateTableStmt();
			s.dbobj = name;
			s.setInitialized(false);
			return s;
		}

		log.warn(name + " found table name match " + result.dbobj);

		return result;
	}

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
			if (volatiletable.put(name, stmt) != null) {
				// throw new RuntimeException("duplicate volatile talbe" +
				// name);
			}
		} else {
			if (permanenttable.containsKey(name)) {
				// TODO compare tables
				log.warn("duplicate create table" + name);
			}

			permanenttable.put(name, stmt);

			String tblname = stmt.dbobj.ns.get(stmt.dbobj.ns.size() - 1);
			if (c.put(tblname, stmt) != null) {
				c.put(tblname, DUPLICATE_MARKER);
			}
		}
	}

	public void clearinternal() {
		volatiletable.clear();
	}
}
