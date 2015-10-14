package io.github.jhg543.mellex.inputsource;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.GlobalSettings;
import io.github.jhg543.mellex.ASTHelper.ObjectName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class PureScriptTableDefinitionProvider implements TableDefinitionProvider {

	Map<ObjectName, CreateTableStmt> permanenttable = new ConcurrentHashMap<>();
	Map<ObjectName, CreateTableStmt> volatiletable = new ConcurrentHashMap<>();
	Map<String, CreateTableStmt> c = new ConcurrentHashMap<>();
	CreateTableStmt DUPLICATE_MARKER = new CreateTableStmt();
	private static final Logger log = LoggerFactory.getLogger(PureScriptTableDefinitionProvider.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.jhg543.mellex.inputsource.TableDefinitionProvider#queryTable
	 * (io.github.jhg543.mellex.ASTHelper.ObjectName)
	 */
	@Override
	public CreateTableStmt queryTable(ObjectName name2) {

		ObjectName name = new ObjectName();
		name.ns.addAll(name2.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$') {
				if (x.charAt(1) == '{') {
					name.ns.set(0, x.substring(2, x.length() - 1));
				} else {
					name.ns.set(0, x.substring(1));
				}
			}
			// if (x.charAt(0) == '$' && x.charAt(1) != '{') {
			// name.ns.set(0, "${" + x.substring(1) + "}");
			// }
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.jhg543.mellex.inputsource.TableDefinitionProvider#putTable(
	 * io.github.jhg543.mellex.ASTHelper.CreateTableStmt, boolean)
	 */
	@Override
	public void putTable(CreateTableStmt stmt, boolean isvolatile) {

		ObjectName name = new ObjectName();
		name.ns.addAll(stmt.dbobj.ns);

		if (name.ns.size() > 1) {
			String x = name.ns.get(0);
			if (x.charAt(0) == '$') {
				if (x.charAt(1) == '{') {
					name.ns.set(0, x.substring(2, x.length() - 1));
				} else {
					name.ns.set(0, x.substring(1));
				}
			}
			// if (x.charAt(0) == '$' && x.charAt(1) != '{') {
			// name.ns.set(0, "${" + x.substring(1) + "}");
			// }
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.jhg543.mellex.inputsource.TableDefinitionProvider#getVolatileTables
	 * ()
	 */
	@Override
	public Map<String, CreateTableStmt> getVolatileTables() {
		Map<String, CreateTableStmt> result = volatiletable.entrySet().stream()
				.collect(Collectors.toMap(x -> x.getKey().toDotString(), x -> x.getValue()));
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.jhg543.mellex.inputsource.TableDefinitionProvider#clearinternal
	 * ()
	 */
	@Override
	public void clearVolatileTables() {
		volatiletable.clear();
	}

	@Override
	public CreateTableStmt queryTable(String name) {
		return queryTable(ObjectName.fromString(name));
	}
}
