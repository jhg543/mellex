package io.github.jhg543.mellex.session;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortDependencySession {

	List<String> filenames = new ArrayList<String>();

	/**
	 * (k,v) = (s1.t1, 1,2,3) means script 1,2,3 provides ddl for s1.t1
	 */
	Map<String, List<Integer>> pmaps = new HashMap<>();
	Map<String, List<Integer>> cmaps = new HashMap<>();
	Map<String, List<Integer>> cmapsA = new HashMap<>();

	Map<Integer, Set<Integer>> edgesin = new HashMap<Integer, Set<Integer>>();
	Map<Integer, Set<Integer>> edgesout = new HashMap<Integer, Set<Integer>>();
	int ci = 0;
	private static final Logger log = LoggerFactory.getLogger(SortDependencySession.class);

	private static void processTableNameSet(Set<String> ps, Map<String, List<Integer>> maps, int filenumber) {
		for (String s : ps) {
			if (s.length() == 0) {
				continue;
			}
			int x = s.indexOf('.');
			if (x == -1) {
				log.warn("NO DB NAME ---" + s);
			}

			List<Integer> li = maps.get(s);
			if (li == null) {
				li = new ArrayList<Integer>();
				maps.put(s, li);
			}
			li.add(filenumber);
		}
	}

	public void addScript(String id, ScriptBlockTableDependency dep) {
		Integer i = ci;
		filenames.add(id);
		processTableNameSet(dep.getProvide(), pmaps, i);
		processTableNameSet(dep.getConsume(), cmaps, i);
		processTableNameSet(dep.getConsumeAsterisk(), cmapsA, i);
		ci++;
	}

	public List<String> sort(PrintWriter logoutput) {

		int[] invalidBlocks = new int[filenames.size()];
		List<String>[] invcause = new List[filenames.size()];

		for (Integer i = 0; i < filenames.size(); ++i) {
			edgesin.put(i, new HashSet<Integer>());
			edgesout.put(i, new HashSet<Integer>());
		}

		// file x depends on file y on table z
		// to trace circular links
		Map<Pair<Integer, Integer>, List<String>> links = new HashMap<Pair<Integer, Integer>, List<String>>();
		for (String tablename : cmaps.keySet()) {
			List<Integer> cfiles = cmaps.get(tablename);
			List<Integer> pfiles = pmaps.get(tablename);
			if (pfiles != null) {

				for (Integer i : pfiles) {
					Set<Integer> si = edgesout.get(i);
					si.addAll(cfiles);
					for (Integer j : cfiles) {
						Pair<Integer, Integer> p = Pair.of(i, j);
						List<String> tl = links.get(p);
						if (tl == null) {
							tl = new ArrayList<String>();
							links.put(p, tl);
						}
						tl.add(tablename);

					}
				}
				for (Integer i : cfiles) {
					Set<Integer> si = edgesin.get(i);
					si.addAll(pfiles);
				}
			} else {
				// this table has no provider
				if (cmapsA.containsKey(tablename)) {
					if (tablename.startsWith("DWM") || tablename.startsWith("DWP") || tablename.startsWith("DWI")) {
						for (Integer i : cfiles) {
							invalidBlocks[i] = 1;
							if (invcause[i] == null) {
								invcause[i] = new ArrayList<String>();
							}
							invcause[i].add(tablename);
						}
					}
				}
			}
		}

		// width first search to get invalid blocks
		Queue<Integer> qx = new LinkedList<Integer>();
		for (int i = 0; i < filenames.size(); ++i) {
			if (invalidBlocks[i] == 1) {
				qx.add(i);
			}
		}

		while (qx.peek() != null) {
			int i = qx.poll();
			for (Integer j : edgesout.get(i)) {
				if (invalidBlocks[j] == 0) {
					invalidBlocks[j] = 1;
					qx.add(j);
					if (invcause[j] == null) {
						invcause[j] = new ArrayList<String>();
					}
					invcause[j].add(filenames.get(i));
				}
			}
		}

		Queue<Integer> q = new LinkedList<Integer>();
		for (Integer i = 0; i < filenames.size(); ++i) {
			if (edgesin.get(i).size() == 0) {
				q.add(i);
				edgesin.remove(i);
			}
		}

		List<Integer> order = new ArrayList<Integer>(filenames.size());

		while (q.peek() != null) {
			Integer i = q.poll();
			order.add(i);
			for (Integer j : edgesout.get(i)) {
				Set<Integer> ins = edgesin.get(j);
				if (ins != null) {
					ins.remove(i);
					if (ins.size() == 0) {
						q.add(j);
						edgesin.remove(j);
					}
				}
			}
		}

		// try (PrintWriter out = new
		// PrintWriter(Constants.current.getWorkingDir() + "link.txt", "utf-8"))
		// {
		//
		// for (Entry<Pair<Integer, Integer>, List<String>> entry :
		// links.entrySet()) {
		// int a = entry.getKey().getLeft();
		// int b = entry.getKey().getRight();
		// if (edgesin.get(a) != null && edgesin.get(b) != null) {
		// out.println(filenames.get(a) + "<----" + filenames.get(b));
		// out.println(entry.getValue().size());
		// out.println(entry.getValue());
		// }
		// }
		// }

		if (logoutput != null) {

			logoutput.println("------------------------------ REMAINING FILES -----------------------");
			for (Integer i = 0; i < filenames.size(); ++i) {
				if (edgesin.get(i) != null) {
					logoutput.println(filenames.get(i));
				}
			}

			logoutput.println("------------------------------ P ONLY -----------------------");
			Set<String> s = new HashSet<String>();
			s.addAll(pmaps.keySet());
			logoutput.println(s);
			s.removeAll(cmaps.keySet());
			logoutput.println(s.size());
			logoutput.println(s);

			logoutput.println("------------------------------ C ONLY -----------------------");
			s.clear();
			s.addAll(cmaps.keySet());
			logoutput.println(s);
			s.removeAll(pmaps.keySet());
			logoutput.println(s.size());
			logoutput.println(s);
			logoutput.println(s.stream().collect(
					Collectors.groupingBy((Function<String, String>) (x -> (x.indexOf('.') > 0 ? x.substring(0, x.indexOf('.'))
							: x)))));
			logoutput.println("-----------------------INVALID TABLES---------------------------------");
			for (int i = 0; i < filenames.size(); ++i) {
				if (invcause[i] != null) {
					logoutput.print(filenames.get(i));
					logoutput.print("    -------------->  ");
					logoutput.println(invcause[i]);
					logoutput.println();
				}
			}

		}

		List<String> result = new ArrayList<String>(filenames.size());
		for (int i = 0; i < order.size(); ++i) {
			result.add(filenames.get(order.get(i)));
		}
		return result;
	}

}
