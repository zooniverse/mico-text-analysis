/**
 * An implementation of the ClassificiationScorer interface.
 * 
 * It considers the 'correct answer' (consensus) for a subject to be the list
 * of species (including 'nothing') that has been indicated by at
 * least half of the users that have classified the subject.
 * 
 * It considers a user classification to be correct if it is exactly the same
 * as the consensus (modulo ordering) in which case it returns 1.0. Otherwise
 * it returns 0.0.
 */

package mico.textanalysis.mongod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * @author henrikb
 *
 */
public class MajorityClassificationScorer implements ClassificationScorer {

	/* (non-Javadoc)
	 * @see mico.textanalysis.mongod.ClassificationScorer#scoreClassification(mico.textanalysis.mongod.Subject, java.util.ArrayList)
	 */
	@Override
	public double scoreClassification(Subject s, ArrayList<String> userSpecies) {
		ArrayList<String> consensusSpecies = getConsensusSpecies(s);
		
		//System.out.print("Consensus: ");
		//for(String st : consensusSpecies){
		//	System.out.print(st);
		//}
		//System.out.print('\n');
		boolean agrees = true;
		if(consensusSpecies.size() != userSpecies.size()){
			agrees = false;
			//System.out.println("Wrong size");
		}
		for(String species : userSpecies){
			if(!consensusSpecies.contains(species)){
				agrees = false;
				//System.out.println("consensusSpecies doesn't contain " + s)
			}
			//System.out.print(species + " ");
		}
		//System.out.print("\n");
		if(agrees){
			return 1;
		}else{
			return 0;
		}
	}
	
	private ArrayList<String> getConsensusSpecies(Subject subject) {
		
		ArrayList<String> consensusSpecies = new ArrayList<String>();
		HashMap<String,Integer> speciesCounts = subject.getSpeciesCounts();
		Set<String> observations = speciesCounts.keySet();
		for(String observation : observations){
			//System.out.println("Observation: " + observation);
			Integer count = speciesCounts.get(observation);
			if(count.doubleValue()/subject.getClassificationCount() > 0.5){
					consensusSpecies.add(observation);
			}
		}
	
		return consensusSpecies;
	}

}
