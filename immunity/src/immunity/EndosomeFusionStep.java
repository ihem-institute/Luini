package immunity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repast.simphony.context.Context;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

public class EndosomeFusionStep {
	private static ContinuousSpace<Object> space;
	private static Grid<Object> grid;
	
	public static void fusion (Endosome endosome) {
		String maxRab = Collections.max(endosome.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey();
// Solo fucionan cisternas.  Esto evita fusi�n entre ves�clas peque�as
		if ((	
				endosome.area < Cell.minCistern)
//				endosome.volume <= 4/3 * Math.PI*Math.pow(Cell.rcyl, 3)
//				|| endosome.area <= 1.1*(2*Math.PI*Math.pow(Cell.rcyl, 2)+2*Math.PI*Cell.rcyl*20))
//				&& !maxRab.equals("RabA")
				) return;
		HashMap<String, Double> rabContent = new HashMap<String, Double>(endosome.getRabContent());
		double areaGolgi = 0d;
		for (String rab : rabContent.keySet()){
			String name = CellProperties.getInstance().rabOrganelle.get(rab);
			if (name.contains("Golgi")) {areaGolgi = areaGolgi + rabContent.get(rab);} 
		}
		boolean isGolgi = false;
		if (areaGolgi/endosome.area >= 0.5) {
			isGolgi = true;
		}
		HashMap<String, Double> membraneContent = new HashMap<String, Double>(endosome.getMembraneContent());
		HashMap<String, Double> solubleContent = new HashMap<String, Double>(endosome.getSolubleContent());
		space = endosome.getSpace();
		grid = endosome.getGrid();
		double cellLimit = 3 * Cell.orgScale;
	
		GridPoint pt = grid.getLocation(endosome);
		// I calculated that the 50 x 50 grid is equivalent to a 750 x 750 nm
		// square
		// Hence, size/15 is in grid units
		int gridSize = (int) Math.round(endosome.size*Cell.orgScale / 15d);// para aumentar fusi�n.  Lo normal es 15d
		GridCellNgh<Endosome> nghCreator = new GridCellNgh<Endosome>(grid, pt,
				Endosome.class, gridSize, gridSize);
		// System.out.println("SIZE           "+gridSize);

		List<GridCell<Endosome>> cellList = nghCreator.getNeighborhood(true);
		List<Endosome> endosomes_to_delete = new ArrayList<Endosome>();
		double vv = endosome.volume;
		double ss = endosome.area;
		boolean isCistern = (ss * ss * ss / (vv * vv) > 36.01 * Math.PI);// is a sphere
//		String maxRab1 = Collections.max(endosome.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey();

		for (GridCell<Endosome> gr : cellList) {

			// include all endosomes
			for (Endosome end : gr.items()) {
				//				if (!(EndosomeAssessCompatibility.compatibles(endosome, end) == 0d)){
				//					System.out.println("DEBE FUSION "+endosome.rabContent + " "+ end.rabContent);
				//				}
				vv = end.volume;
				ss = end.area;
				boolean isCistern2 = (ss * ss * ss / (vv * vv) < 36.01 * Math.PI);
				if (isGolgi){
					if (end != endosome  // it is not itself
							&& (end.volume <= 4/3 * Math.PI*Math.pow(Cell.rcyl, 3))// use to be endosome.volume) // the other is smaller
//							 (Collections.max(endosome.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey() ==
//									Collections.max(end.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey()))
							&& (Math.random() < EndosomeAssessCompatibility.compatibles(endosome, end))) {
						//						if (EndosomeAssessCompatibility.compatibles(endosome, end)==0d){
//												System.out.println("OJO FUSION "+end.volume + " "+ (4/3 * Math.PI*Math.pow(Cell.rcyl, 3)));
//												try {
//												TimeUnit.SECONDS.sleep(3);
//											} catch (InterruptedException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
												//						}
						endosomes_to_delete.add(end);
					}
				}
				else { // it is not a Golgi
					if (end != endosome  // it is not itself
							&& (end.volume < endosome.volume)// use to be endosome.volume) // the other is smaller
//							&& ((!isCistern || !isCistern2)) // at list one is not cistern
//									|| // or they share the same maximal Rab domain
//									(Collections.max(endosome.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey() ==
//									Collections.max(end.rabContent.entrySet(), Map.Entry.comparingByValue()).getKey()))

							&& (Math.random() < EndosomeAssessCompatibility.compatibles(endosome, end))) {
						//						if (EndosomeAssessCompatibility.compatibles(endosome, end)==0d){
						//							System.out.println("OJO FUSION "+endosome.rabContent + " "+ end.rabContent);
						//						}
						endosomes_to_delete.add(end);
					}
					// System.out.println(endosomes_to_delete);
				}
			}
		}
		for (Endosome endosome2 : endosomes_to_delete) {
			// System.out.println(endosome.area+"  AREAS A SUMAR AREAS A SUMAR"+
			// endosome.area);
			endosome.volume = endosome.volume + endosome2.volume;
			endosome.area = endosome.area + endosome2.area;
			//initOrgProp.put("area", area);
			// System.out.println(endosome.area+"  AREAS FINAL");
			endosome.rabContent = sumRabContent(endosome, endosome2);
			endosome.membraneContent = sumMembraneContent(endosome, endosome2);
			endosome.solubleContent = sumSolubleContent(endosome, endosome2);
			Context<Object> context = ContextUtils.getContext(endosome);
			context.remove(endosome2);
		}
		double rsphere = Math.pow(endosome.volume * 3d / 4d / Math.PI, (1d / 3d));
		double size = rsphere;
		endosome.speed = 1d/ size;
		Endosome.endosomeShape(endosome);
		endosome.getEndosomeTimeSeries().clear();
		endosome.getRabTimeSeries().clear();
//		The time series will be re-calculated by COPASI call in the next tick
//		

	}
//	public static void endosomeShape(Endosome end) {
//		double s = end.area;
//		double v = end.volume;
//		double rsphere = Math.pow((v * 3) / (4 * Math.PI), (1 / 3d));
//		double svratio = s / v; // ratio surface volume
//		double aa = rsphere; // initial a from the radius of a sphere of volume
//								// v
//		double cc = aa;// initially, c=a
//		// calculation from s/v for a cylinder that it is more or less the same than for an
//		// ellipsoid
//		// s= 2PIa^2+2PIa*2c and v = PIa^2*2c hence s/v =(1/c)+(2/a)
//		for (int i = 1; i < 5; i++) {// just two iterations yield an acceptable
//										// a-c ratio for plotting
//			aa = 2 / (svratio - 1 / cc);// from s/v ratio
//			cc = v * 3 / (4 * Math.PI * aa * aa);// from v ellipsoid
//		}
//		end.a = aa;
//		end.c = cc;
//	}
	private static HashMap<String, Double> sumRabContent(Endosome endosome1,
			Endosome endosome2) {
		// HashMap<String, Double> map3 = new HashMap<String, Double>();
		// map3.putAll(endosome1.rabContent);
		// map3.forEach((k, v) -> endosome2.rabContent.merge(k, v, (v1, v2) ->
		// v1 + v2));
		// return map3;

		HashMap<String, Double> rabSum = new HashMap<String, Double>();
		for (String key1 : endosome1.rabContent.keySet()) {
			if (endosome2.rabContent.containsKey(key1)) {
				double sum = endosome1.rabContent.get(key1)
						+ endosome2.rabContent.get(key1);
				rabSum.put(key1, sum);
			} else
				rabSum.put(key1, endosome1.rabContent.get(key1));
		}
		for (String key2 : endosome2.rabContent.keySet()) {
			if (!endosome1.rabContent.containsKey(key2)) {
				rabSum.put(key2, endosome2.rabContent.get(key2));
			}
		}

		// System.out.println("rabContentSum" + endosome1.rabContent);
		return rabSum;
	}

	private static HashMap<String, Double> sumMembraneContent(Endosome endosome1,
			Endosome endosome2) {
		HashMap<String, Double> memSum = new HashMap<String, Double>();
		for (String key1 : endosome1.membraneContent.keySet()) {
			if (endosome2.membraneContent.containsKey(key1)) {
				double sum = endosome1.membraneContent.get(key1)
						+ endosome2.membraneContent.get(key1);
				memSum.put(key1, sum);
			} else
				memSum.put(key1, endosome1.membraneContent.get(key1));
		}
		for (String key2 : endosome2.membraneContent.keySet()) {
			if (!endosome1.membraneContent.containsKey(key2)) {
				double sum = endosome2.membraneContent.get(key2);
				memSum.put(key2, sum);
			}
		}
//		// endosome1.membraneContent = memSum;
//		
//		System.out.println("MemEnd 1" +endosome1.membraneContent +
//				"\n MemEnd 2"+ endosome2.membraneContent+ 
//				" \n MemSum" + memSum);
		return memSum;
	}

	private static HashMap<String, Double> sumSolubleContent(Endosome endosome1,
			Endosome endosome2) {
		HashMap<String, Double> solSum = new HashMap<String, Double>();
		for (String key1 : endosome1.solubleContent.keySet()) {
			if (endosome2.solubleContent.containsKey(key1)) {
				double sum = endosome1.solubleContent.get(key1)
						+ endosome2.solubleContent.get(key1);
				solSum.put(key1, sum);
			} else
				solSum.put(key1, endosome1.solubleContent.get(key1));
		}
		for (String key2 : endosome2.solubleContent.keySet()) {
			if (!endosome1.solubleContent.containsKey(key2)) {
				double sum = endosome2.solubleContent.get(key2);
				solSum.put(key2, sum);
			}
		}
		// endosome1.solubleContent = solSum;
		// System.out.println("solubleContentSum" + endosome1.solubleContent);
		return solSum;
	}


}
