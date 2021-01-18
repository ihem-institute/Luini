package immunity;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class EndosomeInternalVesicleStep {

	

	public static void internalVesicle(Endosome endosome) {	
		
//	Organelles with RabE (ER or TGN) do not form internal vesicles
		if (endosome.getRabContent().containsKey("RabE")
				&& endosome.getRabContent().get("RabE")/endosome.area>0.5){
			return;
		}
//		Organelles with pure RabC (Endosome Recycling Compartment) do not form internal vesicles
		if (endosome.getRabContent().containsKey("RabC")
				&& endosome.getRabContent().get("RabC")/endosome.area>0.95){
			return;
		}
//		if (endosome.getRabContent().containsKey("RabB")
//				&& endosome.getRabContent().get("RabB")/endosome.area>0.95){
//			return;
//		}
	HashMap<String, Set<String>> rabTropism = new HashMap<String, Set<String>>(
				CellProperties.getInstance().getRabTropism());
		
//		lysSurface will allow large structures, rich in RabA or RabD to form several internal vesicles
// Other organelles form a single vesicle (notice that the for loop run at least once)
	int lysSurface = 0;
	if (endosome.getRabContent().containsKey("RabD"))
		{lysSurface = (int) (endosome.getRabContent().get("RabD")/5100d);}// about 1 times the area of and internal vesicle
	
	for (int i = 1; i<=1+lysSurface ; i++)
	{
	double vo = endosome.volume;
	double so = endosome.area;
	// if it a sphere, cannot form an internal vesicle
	//System.out.println("ESFERA" + so * so * so / (vo * vo));
	if (so * so * so / (vo * vo) <= 36.001 * Math.PI) return;
	// if is a tubule do not for  internal vesicles
	boolean isTubule = (endosome.volume/(endosome.area - 2*Math.PI*Cell.rcyl*Cell.rcyl) <=Cell.rcyl/2);
	if (isTubule) return;
	//System.out.println("s/v ratio total" + so * so * so / (vo * vo));
	// if s^3 / v^2 is equal to 36*PI then it is an sphere and cannot form a
	// tubule

	double rIV = Cell.rIV; // Internal vesicle radius
	double vIV = 4 / 3 * Math.PI * Math.pow(rIV, 3); // volume 33510
	double sIV = 4 * Math.PI * Math.pow(rIV, 2);// surface 5026

	if (vo < 2 * vIV)
		return;
	if (so < 2 * sIV)
		return;
	double vp = vo + vIV;
	double sp = so - sIV;
	if (sp * sp * sp / (vp * vp) <= 36 * Math.PI){ 
	//	System.out.println("s/v ratio available " + sp * sp * sp / (vp * vp));
		return;// if the resulting surface cannot embrance the resulting
				// volume
	}
//	double rsphere = Math.pow((0.75 * vp / Math.PI), (1 / 3));
//	double ssphere = 4 * Math.PI * Math.pow(rsphere, 2);
//	if (ssphere >= sp)
//		return;
	// double scylinder = so - ssphere;
	// if (scylinder < sIV * 1.10) return;//if the available membrane is
	// less than the surface of an internal vesicle, stop.
	// the factor 1.01 is to account for an increase in the surface required
	// because the increase in volume
	endosome.area = endosome.area - sIV;
	endosome.volume = endosome.volume + vIV;
	Endosome.endosomeShape(endosome);
	if (endosome.solubleContent.containsKey("mvb")) {
		double content = endosome.solubleContent.get("mvb") + 1;
		endosome.solubleContent.put("mvb", content);
	} else {
		endosome.solubleContent.put("mvb", 1d);
	}
	// Rabs proportinal to the sIV versus the surface of the organelle (so)
	// are released into the cytosol.  Must be divided by the area to be in mM
//	NEW RAB NET.  Since cyto  Rabs are generated by the -> Rabcyto, there is no point
//	to add what is not in the endosomal membrane
//	HashMap<String, Double> rabCell = Cell.getInstance().getRabCell();
//	for (String key1 : endosome.rabContent.keySet()) {
//		if (rabCell.containsKey(key1)) {
//			double sum = endosome.rabContent.get(key1) * sIV / so /so
//					+ rabCell.get(key1);
//			rabCell.put(key1, sum);
//		} else {
//			double sum = endosome.rabContent.get(key1) * sIV / so /so;
//			rabCell.put(key1, sum);
//		}
//	}

	// Cell.getInstance().setRabCell(rabCell);
//	System.out.println("Rab Cellular Content"
//			+ Cell.getInstance().getRabCell());
	// Rabs released to the cytosol are substracted from the rabContent of
	// the organelle
	for (String rab : endosome.rabContent.keySet()) {
		double content1 = endosome.rabContent.get(rab) * (so - sIV) / so;
		endosome.rabContent.put(rab, content1);
	}

	// Membrane content with mvb tropism is degraded (e.g. EGF)
	//this can be established in RabTropism adding in the EGF tropisms "mvb",
	for (String content : endosome.membraneContent.keySet()) {
//		System.out.println(endosome.membraneContent+"\n"+ content + "\n" + " CHOLESTEROL RAB TROPISM " + rabTropism.get(content)+ "  \n"+rabTropism);
		
		if(content.equals("membraneMarker")) {
				if (endosome.membraneContent.get("membraneMarker")>0.9){
					endosome.membraneContent.put("membraneMarker", 1d);
				}
				else{
					endosome.membraneContent.put("membraneMarker", 0d);
				}
						
		}
		else if (rabTropism.get(content).contains("mvb")){				
//			System.out.println(content + " CHOLESTEROL RAB TROPISM " + rabTropism.get(content)+ "  \n"+rabTropism);
//			try {
//			TimeUnit.SECONDS.sleep(2);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}			
			double mem = endosome.membraneContent.get(content) - sIV;

			if (mem <= 0) mem = 0d;
			endosome.membraneContent.put(content, mem);
//			If not special tropism, the membrane content is incorporated 
//			into the internal vesicle proportional to the surface and degraded
		} 
		else 
		{
			
//			System.out.println(content + " RAB TROPISM " + rabTropism.get(content)+ "  \n"+rabTropism);
//			try {
//			TimeUnit.SECONDS.sleep(0);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}			
	

			
			
			
			
		double mem = endosome.membraneContent.get(content) * (so - sIV)
					/ so;
			endosome.membraneContent.put(content, mem);
		}
	}
	// Free membrane is added to the cell
	double cellMembrane = Cell.getInstance().gettMembrane() + sIV;
	Cell.getInstance().settMembrane(cellMembrane);
	}
	}

}
