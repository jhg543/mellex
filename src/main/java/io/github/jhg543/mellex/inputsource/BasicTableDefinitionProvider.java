package io.github.jhg543.mellex.inputsource;

import io.github.jhg543.mellex.ASTHelper.CreateTableStmt;
import io.github.jhg543.mellex.ASTHelper.ObjectName;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicTableDefinitionProvider implements TableDefinitionProvider {

	Function<String, String> symprovider;

	public BasicTableDefinitionProvider(Function<String, String> symprovider) {
		this.symprovider = symprovider;
	}

	Map<String, CreateTableStmt> permanenttable = new ConcurrentHashMap<>();
	Map<String, CreateTableStmt> volatiletable = new ConcurrentHashMap<>();
	Map<String, CreateTableStmt> c = new ConcurrentHashMap<>();
	private static final Logger log = LoggerFactory.getLogger(BasicTableDefinitionProvider.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.github.jhg543.mellex.inputsource.TableDefinitionProvider#queryTable
	 * (io.github.jhg543.mellex.ASTHelper.ObjectName)
	 */
	@Override
	public CreateTableStmt queryTable(ObjectName name2) {
		return queryTable(name2.toDotString());
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

		String normalizedName = symprovider.apply(stmt.dbobj.toDotString());

		if (isvolatile) {
			if (volatiletable.put(normalizedName, stmt) != null) {
				log.warn("duplicate volatile table " + normalizedName);
				// throw new RuntimeException("duplicate volatile talbe" +
				// name);
			}
		} else {
			if (permanenttable.containsKey(normalizedName)) {
				// TODO compare tables
				log.warn("duplicate create table " + normalizedName);
			}

			permanenttable.put(normalizedName, stmt);

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
		return Collections.unmodifiableMap(volatiletable);
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

		String normalizedName = symprovider.apply(name);

		CreateTableStmt result = null;
		result = permanenttable.get(normalizedName);
		if (result != null) {
			return result;
		}

		result = volatiletable.get(normalizedName);

		if (result != null) {
			return result;
		} else {
			log.warn(normalizedName + " meta not found");
			CreateTableStmt s = new CreateTableStmt();
			s.dbobj = ObjectName.fromString(normalizedName);
			s.setInitialized(false);
			return s;
			// throw new RuntimeException(name + "meta not found");
		}

	}

}
