package io.github.jhg543.mellex.ASTHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analysis result of a sql block.
 * @author zzz
 *
 */
public class SubQuery {
	private static final Logger log = LoggerFactory.getLogger(SubQuery.class);

	public List<ResultColumn> columns = new ArrayList<>();
	// public List<SubQuery> tables= new ArrayList<>();

	/**
	 * Columns affecting all result columns. usually that's columns in WHERE HAVING GROUPBY QUALIFU clauses.
	 * These will be added to each result column's influence list. 
	 */
	public Influences ci = new Influences();
	
	/**
	 * the database object being operated. select = none , insert/update/create table/view = target table, 
	 */
	public ObjectName dbobj;
	
	
	/**
	 * alias of this sql block. e.g. ( SELECT 1 ) AS A. alias = A
	 */
	private String alias;
	
	
	/**
	 * 
	 */
	private boolean initialized = true;

	
	/**
	 * @return for createTableStmt false means no actual meta found.
	 */
	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * 
	 */
	public ResultColumn searchcol(String name) {
		for (ResultColumn c : columns) {
			if (name.equals(c.name)) {
				return c;
			}
		}
		return null;
	}

	/**
	 * copy result columns with influences from other SubQuery
	 */
	public void copyRC(SubQuery other) {
		for (ResultColumn oc : other.columns) {
			ResultColumn c = new ResultColumn();
			c.inf.addAll(oc.inf);
			c.name = oc.name;
			c.position = oc.position;
			columns.add(c);
		}
	}

	public void copyResultColumnNames(SubQuery other) {
		for (ResultColumn oc : other.columns) {
			ResultColumn c = new ResultColumn();
			c.name = oc.name;
			c.position = oc.position;
			columns.add(c);
		}
	}

	
	public void copyRCCI(SubQuery other) {
		copyRC(other);
		ci.addAll(other.ci);
	}

	/**
	 * merge UNION statements.
	 * @param queries
	 */
	public void compound(List<SubQuery> queries) {
		if (queries.size() > 0) {
			int columncount = -1;
			
			// make sure column count of each query are equal 
			for (SubQuery q : queries) {
				if (columncount == -1) {
					columncount = q.columns.size();
				} else {
					if (columncount != q.columns.size()) {
						throw new RuntimeException("UNION SIZE MISMATCH " + columncount + " " + q.columns.size());
					}
				}
			}
			
			// add influences of each subquery together
			for (int i = 0; i < queries.get(0).columns.size(); ++i) {
				ResultColumn oc = queries.get(0).columns.get(i);
				ResultColumn c = new ResultColumn();
				c.inf.addAll(oc.inf);
				c.name = oc.name;
				c.position = oc.position;
				columns.add(c);
				for (SubQuery q : queries) {
					c.inf.addAll(q.columns.get(i).inf);
				}
				c.inf.unique();
			}

			for (SubQuery q : queries) {
				ci.addAllInClause(q.ci);
			}

			ci.unique();

		}

	}

	
	/**
	 * rewrite alias to database objects
	 * 
	 * @param tables FROM subQuery1 JOIN subQuery2
	 * @param groupbypositions GROUP BY 1,2,4 then {1,2,4}
	 * @param ctestack WITH subQuery1
	 */
	public void resolvenames(List<SubQuery> tables, List<Integer> groupbypositions, List<SubQuery> ctestack) {

		if (!GlobalSettings.isCaseSensitive()) {
			for (ResultColumn c : columns) {
				c.name = c.name.toUpperCase();
			}
		}

		Map<String, SubQuery> aliases = new HashMap<>();
		Map<String, Pair<SubQuery, ResultColumn>> columnnames = new HashMap<>();

		// put tables in join clause
		for (SubQuery subquery : tables) {
			scannames(subquery, aliases, columnnames);
		}

		// put tables in cte
		for (SubQuery subquery : ctestack) {
			scannames(subquery, aliases, columnnames);
		}

		// if FROM one table, any column names resolve to this table.
		SubQuery singlesubquery = null;
		if (tables.size() == 1 && ctestack.size()==0) {
			singlesubquery = tables.get(0);
		} else if (tables.size() ==0 && ctestack.size()==1) {
			singlesubquery = ctestack.get(0);
		}

		// expand "select *"
		List<ResultColumn> expandasterisk = new ArrayList<>();

		for (ResultColumn c : columns) {
			if (c.name.contains("*")) {
				if ("*".equals(c.name)) {
					// select *
					for (SubQuery q : tables) {
						String tablename = q.getAlias();
						if (tablename == null) {
							tablename = q.dbobj.toDotString();
						}

						if (tablename == null) {
							throw new RuntimeException("SUBQUERY NO NAME" + q.toString());
						}
						for (ResultColumn oc : q.columns) {
							ResultColumn newc = new ResultColumn();
							newc.name = tablename + "." + oc.name;
							newc.setObjectName(true);
							newc.inf.addAll(oc.inf);
							expandasterisk.add(newc);
						}
					}
				} else if (c.isObjectName()) {
					// select p1.*
					String tablename = c.name.substring(0, c.name.lastIndexOf('.'));
					SubQuery q = aliases.get(tablename);
					if (q == null) {
						throw new RuntimeException("unknown table name" + tablename);
					}
					for (ResultColumn oc : q.columns) {
						ResultColumn newc = new ResultColumn();
						newc.name = tablename + "." + oc.name;
						newc.setObjectName(true);
						newc.inf.addAll(oc.inf);
						expandasterisk.add(newc);
					}
				} else if (c.name.contains("(*)")) {
					// throw new RuntimeException("unknown count(*) " + c.name);
					// count (*) ---> depends on where clause
					// TODO: log.warn(" Count(*) FOUND");
					expandasterisk.add(c);
				} else {
					expandasterisk.add(c);
				}
			} else {
				expandasterisk.add(c);
			}

		}

		columns = expandasterisk;

		// resolve result column names p1.c1 to c1
		// SELECT p1.c1 FROM p1 
		// ResultColumn(0).name should be "c1", not "p1.c1"
		// the logic is count occurrence of "c1", if it's 1 then rename "xx.c1" to "c1"
		Map<String, Integer> namescounter = new HashMap<String, Integer>();

		for (ResultColumn c : columns) {
			if (c.isObjectName() && !c.hasAlias) {
				String ln = c.name.substring(c.name.lastIndexOf('.') + 1);
				Integer count = namescounter.get(ln);
				if (count == null) {
					count = 0;
				}
				namescounter.put(ln, count + 1);
			}
		}

		for (ResultColumn c : columns) {
			if (c.isObjectName() && !c.hasAlias) {
				String ln = c.name.substring(c.name.lastIndexOf('.') + 1);
				Integer count = namescounter.get(ln);
				if (count == 1) {
					c.name = ln;
				}
			}
		}

		// GROUP BY 1,2,3
		List<Integer> poses = new ArrayList<>(new LinkedHashSet<>(groupbypositions));

		for (int i : poses) {
			ci.addAllInClause(columns.get(i - 1).inf);
		}
		ci.unique();

		List<ResultColumn> newcolumns = new ArrayList<>();

		// rewrite ci
		AtomicReference<Influences> unresolvedci = new AtomicReference<Influences>();
		ci = rewriteinf(ci, aliases, columnnames, singlesubquery, unresolvedci);

		// rewrite  each resultcolumn
		AtomicReference<Influences> temp = new AtomicReference<Influences>();
		for (ResultColumn oc : columns) {
			ResultColumn c = new ResultColumn();
			c.name = oc.name;
			c.position = oc.position;
			c.setObjectName(oc.isObjectName());
			c.hasAlias = oc.hasAlias;
			temp.set(null);
			c.inf = rewriteinf(oc.inf, aliases, columnnames, singlesubquery, temp);
			if (temp.get() != null) {
				c.unresolvedNames = temp.get();
			}
			newcolumns.add(c);
		}

		columns = newcolumns;

		// resolve TD SPECICIC
		// "SELECT B+B AS C, 1 AS B FROM EMPTYTABLE WHERE C>1"
		Map<String, ResultColumn> rcalias = new HashMap<String, ResultColumn>();
		for (ResultColumn c : columns) {
			if (c.hasAlias) {
				rcalias.put(c.name, c);
			}
		}

		for (ResultColumn c : columns) {
			resolveAnotherResultColumnReference(c, rcalias);
		}

		ResultColumn cic = new ResultColumn();
		cic.inf = ci;
		cic.unresolvedNames = unresolvedci.get();
		resolveAnotherResultColumnReference(cic, rcalias);

		// copy where , group clause to each column

		for (ResultColumn c : columns) {
			// if (c.name.contains("*")) {
			// throw new RuntimeException("SELECT *");
			// }
			c.inf.addAllInClause(ci);
			c.inf.unique();

		}

	}

	/**
	 * TD specfic. 
	 * Check if unresolved name is another column's alias. e.g "SELECT B+B AS C, 1 AS B".
	 * if another column also have resolved names, this method will call ME on another column first.
	 * @param c result column to be processes
	 * @param rcalias result colum names
	 */
	private void resolveAnotherResultColumnReference(ResultColumn c, Map<String, ResultColumn> rcalias) {
		// DEPTH FIRST SEARCH
		// unresolvedNames.size> 0 ---> not done
		// unresolvedNames.size==0 ---> in progress
		// unresolvedNamese==null ---> done
		if (c.unresolvedNames == null) {
			return;
		}

		if (c.unresolvedNames.isempty()) {
			throw new RuntimeException("cycle reference or can not determine " + c.name);
		}

		Influences ur = c.unresolvedNames;
		c.unresolvedNames = new Influences();
		for (InfSource source:ur.getSources())
		{
			ObjectName name = source.getSourceObject();
			ResultColumn n = rcalias.get(name.ns.get(0));
			if (n == null) {
				// TD SPECIFIC
				if (name.ns.size() == 1 && name.ns.get(0).equals("DATE")) {
					//	TODO maintain a CONSTANT list
				} else {
					log.error("Column" + name + "not found");
					throw new RuntimeException("Column" + name + "not found");
				}
			} else {
				resolveAnotherResultColumnReference(n, rcalias);
				c.inf.expand(n.inf, source);
			}
		}

		c.inf.unique();
		c.unresolvedNames = null;

	}

	/**
	 * @param inf list of names to expand
	 * @param aliases 
	 * @param columnnames 
	 * @param singlesubquery if FROM more than 1 table then null 
	 * @param unresolved
	 * @return
	 */
	private Influences rewriteinf(Influences inf, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> columnnames, SubQuery singlesubquery,
			AtomicReference<Influences> unresolved) {
		Influences newci = new Influences();
		Influences unresolvedsingleinf = new Influences();
		for (InfSource source:inf.getSources())
		{
			Influences r = rewritecolname(source.getSourceObject(), aliases, columnnames, singlesubquery);
			if (r != null) {
				newci.expand(r, source);
			} else {
				unresolvedsingleinf.add(source.expand(source));
			}

		}

		newci.unique();
		if (!unresolvedsingleinf.isempty()) {
			unresolvedsingleinf.unique();
			unresolved.set(unresolvedsingleinf);
		}
		return newci;
	}

	/**
	 * rewrite Object name to it's influenced database objects
	 * @param originalName
	 * @param aliases
	 * @param unambiguousNames
	 * @param singlesubquery
	 * @return null = UNRESOLVED SINGLE NAME
	 */
	private Influences rewritecolname(ObjectName originalName, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> unambiguousNames, SubQuery singlesubquery) {

		if (originalName.ns.size() > 2) {
			// it's schema.table.column, a database object
			return Influences.ofdirect(originalName);
		}

		if (originalName.ns.size() == 2) {
			// table or tablealias. columnname
			SubQuery tableOfOriginalName = aliases.get(originalName.ns.get(0));
			if (tableOfOriginalName == null) {
				// no alias, it's a database object name
				// TODO if tableOfOriginalName not in FROM clause?
				return Influences.ofdirect(originalName);
			} else {
				if (tableOfOriginalName.dbobj != null) {
					// is db object
					ObjectName rn = new ObjectName();
					rn.ns.addAll(tableOfOriginalName.dbobj.ns);
					rn.ns.add(originalName.ns.get(1));
					return Influences.ofdirect(rn);
				} else {
					// is subquery
					for (ResultColumn c : tableOfOriginalName.columns) {
						if (c.name.equals(originalName.ns.get(1))) {
							return Influences.copyOf(c.inf);
						}
					}
					throw new RuntimeException("col not found " + originalName);
				}
			}

		}
		if (originalName.ns.size() == 1) {
			// columnname
			Pair<SubQuery, ResultColumn> pair = unambiguousNames.get(originalName.ns.get(0));
			if (pair == null) {
				// columnname not found
				if (singlesubquery != null) {
					// TODO check columnname in singlesubquery meta
					ObjectName rn = new ObjectName();
					rn.ns.addAll(singlesubquery.dbobj.ns);
					rn.ns.add(originalName.ns.get(0));
					return Influences.ofdirect(rn);
				} else {
					return null;
					// throw new RuntimeException("need metadata " + name);
				}
			} else {
				if (pair.getLeft() == null) {
					// throw new RuntimeException("duplicate col name " + name);
					// TD SPECIFIC -----> SELECT DATA_DT FROM A INNER JOIN B ON
					// A.DATA_DT = B.DATA_DT WHERE DATA_DT>'2000-01-01'
					//TODO ????
					return null;

				} else {
					return Influences.copyOf(pair.getRight().inf);
				}
			}
		}
		throw new RuntimeException("empty name");
	}

	/**
	 * build up aliases and unambiguousNames
	 * @param subquery
	 * @param out aliases
	 * @param out unambiguousNames e.g table A={column c1,c2} B={c1,c3} then {c1="null,null",c2="A[1],A",c3="B[1],B"}
	 */
	private void scannames(SubQuery subquery, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> unambiguousNames) {
		if (subquery.getAlias() != null) {
			SubQuery oldquery = aliases.put(subquery.getAlias(), subquery);
			if (oldquery != null) {
				throw new RuntimeException("duplicate alias" + subquery.getAlias() + oldquery.getAlias());
			}
		}
		if (subquery.columns != null) // TODO null = unresolved physical table dbobj
		{
			for (ResultColumn c : subquery.columns) {
				if (unambiguousNames.containsKey(c.name)) {
					unambiguousNames.put(c.name, Pair.of((SubQuery) null, (ResultColumn) null));
				} else {
					unambiguousNames.put(c.name, Pair.of(subquery, c));
				}
			}
		}
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {

		this.alias = GlobalSettings.isCaseSensitive() ? alias : alias.toUpperCase();
	}
}
