package immunity;
import java.util.HashMap;
import java.util.Set;
import java.util.Collections;
import org.COPASI.CTimeSeries;
import org.apache.commons.lang3.StringUtils;

import repast.simphony.engine.environment.RunEnvironment;

public class PlasmaMembraneCopasiStep {
	
	public static void antPresTimeSeriesLoad(PlasmaMembrane plasmaMembrane){
		int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();

		if (plasmaMembrane.getPlasmaMembraneTimeSeries().isEmpty()){			
			callReceptorDynamics(plasmaMembrane);
			timeSeriesLoadintoPlasmaMembrane(plasmaMembrane);

			return;
		} 
		if (tick >= Collections.max(plasmaMembrane.getPlasmaMembraneTimeSeries().keySet())) {
			timeSeriesLoadintoPlasmaMembrane(plasmaMembrane);
			plasmaMembrane.getPlasmaMembraneTimeSeries().clear();
			callReceptorDynamics(plasmaMembrane);

			return;
			}
		if (!plasmaMembrane.getPlasmaMembraneTimeSeries().containsKey(tick)) {
			return;
		}else {
			timeSeriesLoadintoPlasmaMembrane(plasmaMembrane);
			return;

		}
	}
	public static void timeSeriesLoadintoPlasmaMembrane(PlasmaMembrane plasmaMembrane){
//		values in plasmaMembraneTimeSeries are in mM.  Transform back in area and volume units multiplying
//		by area the membrane metabolites and by volume the soluble metabolites
		int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		HashMap<String, Double> presentValues = new HashMap<String, Double>(plasmaMembrane.getPlasmaMembraneTimeSeries().get(tick));
		HashMap<String, Double> pastValues = new HashMap<String, Double>();
		int pastTick = 0;
		if (tick == plasmaMembrane.plasmaMembraneTimeSeries.firstKey()){ ;// (int) (tick + 1 - Cell.timeScale/0.03);
		pastValues = presentValues;
		pastTick = tick;
		} else {
			pastTick = plasmaMembrane.plasmaMembraneTimeSeries.lowerKey(tick);
			pastValues = plasmaMembrane.plasmaMembraneTimeSeries.get(pastTick);
		}
		for (String met :presentValues.keySet()){
//			Organelle (endosomes/Golgi) metabolites are not considered. The reaction should be called from endosomes.
			String met1 = met.substring(0, met.length()-2);
//			metabolites in the Cell are expressed in concentration. I am using the area ratio between PM and Cell 
//			for dilution of the metabilite that is released into the cell.  I may use volume ratio? 
//			Only a fraction of the metabolite in the cell participates
//			in copasi, hence the concentration are added to the existing values.
//			Since the PM is releasing Cyto metabolites at each tick, what must be incorporated is the delta with respect to the previous tick.
//			At tick = 0, nothing is released (pastValues = presentValues)
			if (StringUtils.endsWith(met, "Cy")){
				 if (!Cell.getInstance().getSolubleCell().containsKey(met1)){Cell.getInstance().getSolubleCell().put(met1, 0.0);}
				
					double delta =  presentValues.get(met) - pastValues.get(met);
					double metValue = Cell.getInstance().getSolubleCell().get(met1)
										+ delta * PlasmaMembrane.getInstance().getPlasmaMembraneArea()/Cell.getInstance().getCellArea();
								Cell.getInstance().getSolubleCell().put(met1, metValue);
			}
			
			else if (StringUtils.endsWith(met, "Pm") && CellProperties.getInstance().getSolubleMet().contains(met1)){
				double metValue = presentValues.get(met)* PlasmaMembrane.getInstance().getPlasmaMembraneVolume();
				plasmaMembrane.getSolubleRecycle().put(met1, metValue);
			}
			else if (StringUtils.endsWith(met, "Pm") && CellProperties.getInstance().getMembraneMet().contains(met1)) {
				double metValue = presentValues.get(met)* PlasmaMembrane.getInstance().getPlasmaMembraneArea();
				plasmaMembrane.getMembraneRecycle().put(met1, metValue);
			}
		}
		
	}
	
	public static void callReceptorDynamics(PlasmaMembrane plasmaMembrane) {
// Membrane and soluble metabolites are transformed from the area an volume units to mM.
// From my calculations (see Calculos), dividing these units by the area or the volume of the endosome, transform the 
//the values in mM.  Back from copasi, I recalculate the values to area and volume
//
		PlasmaMembraneCopasi receptorDynamics = PlasmaMembraneCopasi.getInstance();

		Set<String> metabolites = receptorDynamics.getInstance().getMetabolites();
		HashMap<String, Double> localM = new HashMap<String, Double>();
		System.out.println("PM MEMBRENE RECYCLE " + plasmaMembrane.getMembraneRecycle());
		
		for (String met : metabolites) {
			String met1 = met.substring(0, met.length()-2);

			 if (met.endsWith("Pm") && plasmaMembrane.getMembraneRecycle().containsKey(met1)) {
				double metValue = plasmaMembrane.getMembraneRecycle().get(met1)/PlasmaMembrane.getInstance().getPlasmaMembraneArea();
				receptorDynamics.setInitialConcentration(met, Math.round(metValue*1E9d)/1E9d);
				localM.put(met, metValue);
			} else if (met.endsWith("Pm") && plasmaMembrane.getSolubleRecycle().containsKey(met1)) {
				double metValue = Math.abs(plasmaMembrane.getSolubleRecycle().get(met1))/PlasmaMembrane.getInstance().getPlasmaMembraneVolume();
				receptorDynamics.setInitialConcentration(met, Math.round(metValue*1E9d)/1E9d);
				localM.put(met, metValue);
			} else if (met.endsWith("Cy") && Cell.getInstance().getSolubleCell().containsKey(met1)) {
				double metValue = Cell.getInstance().getSolubleCell().get(met1);
				double metLeft = metValue*(Cell.getInstance().getCellVolume() - PlasmaMembrane.getInstance().getPlasmaMembraneVolume())/(Cell.getInstance().getCellVolume());
				Cell.getInstance().getSolubleCell().put(met1, metLeft);
				receptorDynamics.setInitialConcentration(met, Math.round(metValue*1E9d)/1E9d);
				localM.put(met, metValue);
			} else {
				receptorDynamics.setInitialConcentration(met, 0.0);
				localM.put(met, 0.0);
			}
		}
		receptorDynamics.setInitialConcentration("protonCy", 1e-04);
		localM.put("protonCy", 1e-04);

		if (localM.get("protonEn")==null||localM.get("protonEn") < 1e-05){
			receptorDynamics.setInitialConcentration("protonEn", 1e-04);
			localM.put("protonEn", 1e-04);
		}
		
		receptorDynamics.runTimeCourse();
		

		CTimeSeries timeSeries = receptorDynamics.getTrajectoryTask().getTimeSeries();
		int stepNro = (int) timeSeries.getRecordedSteps();
		int metNro = metabolites.size();
		int tick = (int) RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		for (int time = 0; time < stepNro; time = time + 1){
			HashMap<String, Double> value = new HashMap<String, Double>();
			for (int met = 1; met < metNro +1; met = met +1){
				value.put(timeSeries.getTitle(met), timeSeries.getConcentrationData(time, met));
				plasmaMembrane.getPlasmaMembraneTimeSeries().put((int) (tick+time*Cell.timeScale/0.03),value);
			}
		}

		}
	
}

