package io.github.jhg543.nyallas.etl.algorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.jhg543.nyallas.etl.ve.DefaultEdgeWithSE;
import io.github.jhg543.nyallas.etl.ve.EdgeETL;
import io.github.jhg543.nyallas.etl.ve.VertexDBCol;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

public class SimRank {

	//private DirectedGraph<DoubleVertex, DefaultEdgeWithSE<DoubleVertex>> sg;

//	public DirectedGraph<DoubleVertex, DefaultEdgeWithSE<DoubleVertex>> getSg() {
//		return sg;
//	}

	
	/*
	private ArrayList<VertexDBCol> vs;
	private ArrayList<DoubleVertex> dvs;
	private double iterConstant = 0.8;
	private boolean usei1AsOldRank = true;
	public int ec = 0;

	public List<DoubleVertex> getSortedDvs() {
		if (usei1AsOldRank) {
			dvs.stream().parallel().forEach(dv -> dv.setRanki2(dv.getRanki1()));
		} else {
			dvs.stream().parallel().forEach(dv -> dv.setRanki1(dv.getRanki2()));
		}
		dvs.sort((x, y) -> Double.compare(y.getRanki1(), x.getRanki1()));
		return dvs;
	}

	public void doiteration(int times) {
		while (times-- > 0) {
			oneIteration();
		}
	}

	public double getIterConstant() {
		return iterConstant;
	}

	public void setIterConstant(double iterConstant) {
		this.iterConstant = iterConstant;
	}

	public static class DoubleVertex {
		private VertexDBCol a;
		private VertexDBCol b;
		private double ranki1;
		private double ranki2;
		private Set<DoubleVertex> incomingVertex = new HashSet<SimRank.DoubleVertex>();

		public Set<DoubleVertex> getIncomingVertex() {
			return incomingVertex;
		}

		public VertexDBCol getA() {
			return a;
		}

		public void setA(VertexDBCol a) {
			this.a = a;
		}

		public VertexDBCol getB() {
			return b;
		}

		public void setB(VertexDBCol b) {
			this.b = b;
		}

		public double getRanki1() {
			return ranki1;
		}

		public void setRanki1(double ranki1) {
			this.ranki1 = ranki1;
		}

		public double getRanki2() {
			return ranki2;
		}

		public void setRanki2(double ranki2) {
			this.ranki2 = ranki2;
		}

	};

	private int getCombinedId(int a, int b, int s) {
		if (a > b) {
			int c = a;
			a = b;
			b = c;
		}
		return s * a - (a * (a - 1) / 2) + b - a;
	}

	public void oneIteration() {
		if (usei1AsOldRank) {
			dvs.stream().parallel().forEach(dv -> {
				if (dv.getA() != dv.getB()) {
					Set<DoubleVertex> es = dv.getIncomingVertex();
					if (es.size() != 0) {
						double sum = 0;
						for (DoubleVertex v : es) {
							sum += v.getRanki1();
						}
						sum /= es.size();
						sum *= iterConstant;
						dv.setRanki2(sum);
					} else {
						dv.setRanki2(0);
					}
				} else {
					dv.setRanki2(1);
				}

			});
		} else {
			dvs.stream().parallel().forEach(dv -> {
				if (dv.getA() != dv.getB()) {
					Set<DoubleVertex> es = dv.getIncomingVertex();
					if (es.size() != 0) {
						double sum = 0;
						for (DoubleVertex v : es) {
							sum += v.getRanki2();
						}
						sum /= es.size();
						sum *= iterConstant;
						dv.setRanki1(sum);
					} else {
						dv.setRanki1(0);
					}
				} else {
					dv.setRanki1(1);
				}

			});
		}
		usei1AsOldRank = !usei1AsOldRank;
	}

	public void convertG2(DirectedGraph<VertexDBCol, EdgeETL> og) {

		vs = new ArrayList<>(og.vertexSet());
		dvs = new ArrayList<SimRank.DoubleVertex>(vs.size() * (vs.size() + 1) / 2);
		for (int i = 0; i < vs.size(); ++i) {
			VertexDBCol a = vs.get(i);
			a.setInternal_id(i);
			for (int j = i; j < vs.size(); ++j) {
				DoubleVertex v = new DoubleVertex();
				dvs.add(v);
				VertexDBCol b = vs.get(j);
				v.setA(a);
				v.setB(b);
				if (i == j) {
					v.setRanki1(1);
				} else {
					v.setRanki1(0);
				}
			}
		}

		for (int i = 0; i < vs.size(); ++i) {
			VertexDBCol a = vs.get(i);
			for (int j = i; j < vs.size(); ++j) {
				VertexDBCol b = vs.get(j);
				DoubleVertex v1 = dvs.get(getCombinedId(i, j, vs.size()));
				Set<EdgeETL> oa = og.outgoingEdgesOf(a);
				Set<EdgeETL> ob = og.outgoingEdgesOf(b);
				for (EdgeETL ea : oa) {
					for (EdgeETL eb : ob) {
						int na = ea.getTarget().getInternal_id();
						int nb = eb.getTarget().getInternal_id();
						DoubleVertex v2 = dvs.get(getCombinedId(na, nb, vs.size()));
						//v2.getIncomingVertex().add(v1);
						ec++;
					}
				}
			}
		}
	}
*/
}
