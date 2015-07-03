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

public class SubQuery {
	private static final Logger log = LoggerFactory.getLogger(SubQuery.class);

	public List<ResultColumn> columns = new ArrayList<>();
	// public List<SubQuery> tables= new ArrayList<>();
	public Influences ci = new Influences();
	public ObjectName dbobj;
	private String alias;
	boolean initialized = true;

	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public ResultColumn searchcol(String name) {
		for (ResultColumn c : columns) {
			if (name.equals(c.name)) {
				return c;
			}
		}
		return null;
	}

	public void copyRC(SubQuery other) {
		for (ResultColumn oc : other.columns) {
			ResultColumn c = new ResultColumn();
			c.inf.copy(oc.inf);
			c.name = oc.name;
			c.position = oc.position;
			columns.add(c);
		}
	}

	public void copyRCCI(SubQuery other) {
		copyRC(other);
		ci.copy(other.ci);
	}

	public void compound(List<SubQuery> queries) {
		if (queries.size() > 0) {
			int columncount = -1;
			for (SubQuery q : queries) {
				if (columncount == -1) {
					columncount = q.columns.size();
				} else {
					if (columncount != q.columns.size()) {
						throw new RuntimeException("UNION SIZE MISMATCH " + columncount + " " + q.columns.size());
					}
				}
			}
			for (int i = 0; i < queries.get(0).columns.size(); ++i) {
				ResultColumn oc = queries.get(0).columns.get(i);
				ResultColumn c = new ResultColumn();
				c.inf.copy(oc.inf);
				c.name = oc.name;
				c.position = oc.position;
				columns.add(c);
				for (SubQuery q : queries) {
					c.inf.copy(q.columns.get(i).inf);
				}
				c.inf.unique();
			}

			for (SubQuery q : queries) {
				ci.copypoi(q.ci);
			}

			ci.unique();

		}

	}

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

		// resolve tolumn names
		SubQuery singlesubquery = null;
		if (tables.size() == 1) {
			singlesubquery = tables.get(0);
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
							newc.isObjectName = true;
							newc.inf.copy(oc.inf);
							expandasterisk.add(newc);
						}
					}
				} else if (c.isObjectName) {
					// select p1.*
					String tablename = c.name.substring(0, c.name.lastIndexOf('.'));
					SubQuery q = aliases.get(tablename);
					if (q == null) {
						throw new RuntimeException("unknown table name" + tablename);
					}
					for (ResultColumn oc : q.columns) {
						ResultColumn newc = new ResultColumn();
						newc.name = tablename + "." + oc.name;
						newc.isObjectName = true;
						newc.inf.copy(oc.inf);
						expandasterisk.add(newc);
					}
				} else if (c.name.contains("(*)")) {
					// throw new RuntimeException("unknown count(*) " + c.name);
					// count (*) ---> depends on where clause
					System.out.println("WARN: Count(*) FOUND");
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

		Map<String, Integer> namescounter = new HashMap<String, Integer>();

		for (ResultColumn c : columns) {
			if (c.isObjectName) {
				String ln = c.name.substring(c.name.lastIndexOf('.') + 1);
				Integer count = namescounter.get(ln);
				if (count == null) {
					count = 0;
				}
				namescounter.put(ln, count + 1);
			}
		}

		for (ResultColumn c : columns) {
			if (c.isObjectName) {
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
			ci.copypoi(columns.get(i - 1).inf);
		}
		ci.unique();

		List<ResultColumn> newcolumns = new ArrayList<>();

		AtomicReference<Influences> unresolvedci = new AtomicReference<Influences>();
		ci = rewriteinf(ci, aliases, columnnames, singlesubquery, unresolvedci);

		AtomicReference<Influences> temp = new AtomicReference<Influences>();
		for (ResultColumn oc : columns) {
			ResultColumn c = new ResultColumn();
			c.name = oc.name;
			c.position = oc.position;
			c.isObjectName = oc.isObjectName;
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
			rewriteResultColumn(c, rcalias);
		}

		ResultColumn cic = new ResultColumn();
		cic.inf = ci;
		cic.unresolvedNames = unresolvedci.get();
		rewriteResultColumn(cic, rcalias);

		// copy where , group clause to each column

		for (ResultColumn c : columns) {
			// if (c.name.contains("*")) {
			// throw new RuntimeException("SELECT *");
			// }
			c.inf.copypoi(ci);
			c.inf.unique();

		}

	}

	private void rewriteResultColumn(ResultColumn c, Map<String, ResultColumn> rcalias) {
		// DEPTH FIRST SEARCH
		// unresolvedNames.size> 0 ---> not done
		// unresolvedNames.size==0 ---> in progress
		// unresolvedNamese==null ---> done
		if (c.unresolvedNames == null) {
			return;
		}

		if (c.unresolvedNames.isempty()) {
			throw new RuntimeException("cycle reference" + c.name);
		}

		Influences ur = c.unresolvedNames;
		c.unresolvedNames = new Influences();
		for (ObjectName name : ur.direct) {
			ResultColumn n = rcalias.get(name.ns.get(0));
			if (n == null) {
				log.error("Column" + name + "not found");
			} else {
				rewriteResultColumn(n, rcalias);
				c.inf.copy(n.inf);
			}
		}

		for (ObjectName name : ur.indirect) {
			ResultColumn n = rcalias.get(name.ns.get(0));
			if (n == null) {
				log.error("Column" + name + "not found");
			} else {
				rewriteResultColumn(n, rcalias);
				c.inf.copypoi(n.inf);
			}
		}

		c.inf.unique();
		c.unresolvedNames = null;

	}

	private Influences rewriteinf(Influences inf, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> columnnames, SubQuery singlesubquery,
			AtomicReference<Influences> unresolved) {
		Influences newci = new Influences();
		Influences unresolvedsingleinf = new Influences();
		for (ObjectName name : inf.direct) {
			Influences r = rewritecolname(name, aliases, columnnames, singlesubquery);
			if (r != null) {
				newci.copy(r);
			} else {
				unresolvedsingleinf.direct.add(name);
			}

		}
		for (ObjectName name : inf.indirect) {
			Influences r = rewritecolname(name, aliases, columnnames, singlesubquery);
			if (r != null) {
				newci.copypoi(r);
			} else {
				unresolvedsingleinf.indirect.add(name);
			}
		}

		newci.unique();
		if (!unresolvedsingleinf.isempty()) {
			unresolvedsingleinf.unique();
			unresolved.set(unresolvedsingleinf);
		}
		return newci;
	}

	// return null = UNRESOLVED SINGLE NAME
	private Influences rewritecolname(ObjectName name, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> columnnames, SubQuery singlesubquery) {

		if (name.ns.size() > 2) {
			return Influences.ofdirect(name);
		}

		if (name.ns.size() == 2) {

			SubQuery subquery = aliases.get(name.ns.get(0));
			if (subquery == null) {
				return Influences.ofdirect(name);
			} else {
				if (subquery.dbobj != null) {
					// is db object
					ObjectName rn = new ObjectName();
					rn.ns.addAll(subquery.dbobj.ns);
					rn.ns.add(name.ns.get(1));
					return Influences.ofdirect(rn);
				} else {
					// is subquery
					for (ResultColumn c : subquery.columns) {
						if (c.name.equals(name.ns.get(1))) {
							return c.inf;
						}
					}
					throw new RuntimeException("col not found " + name);
				}
			}

		}
		if (name.ns.size() == 1) {
			Pair<SubQuery, ResultColumn> pair = columnnames.get(name.ns.get(0));
			if (pair == null) {
				if (singlesubquery != null) {

					ObjectName rn = new ObjectName();
					rn.ns.addAll(singlesubquery.dbobj.ns);
					rn.ns.add(name.ns.get(0));
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
					return null;

				} else {
					return pair.getRight().inf;
				}
			}
		}
		throw new RuntimeException("empty name");
	}

	private void scannames(SubQuery subquery, Map<String, SubQuery> aliases,
			Map<String, Pair<SubQuery, ResultColumn>> columnnames) {
		if (subquery.getAlias() != null) {
			SubQuery oldquery = aliases.put(subquery.getAlias(), subquery);
			if (oldquery != null) {
				throw new RuntimeException("duplicate alias" + subquery.getAlias() + oldquery.getAlias());
			}
		}
		if (subquery.columns != null) // null = unresolved physical table dbobj
		{
			for (ResultColumn c : subquery.columns) {
				if (columnnames.containsKey(c.name)) {
					columnnames.put(c.name, Pair.of((SubQuery) null, (ResultColumn) null));
				} else {
					columnnames.put(c.name, Pair.of(subquery, c));
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
