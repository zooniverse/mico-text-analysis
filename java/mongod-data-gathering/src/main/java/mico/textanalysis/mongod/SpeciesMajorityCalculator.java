/**
 * 
 */
package mico.textanalysis.mongod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * @author henrikb@cs.umu.se
 *
 */
class SpeciesMajorityCalculator implements MajorityCalculator {
	
	@Override
	public ArrayList<String> getConsensusSpecies(Subject subject) {
		
		ArrayList<String> consensusSpecies = new ArrayList<String>();
		HashMap<String,Integer> speciesCounts = subject.getSpeciesCounts();
		Set<String> observations = speciesCounts.keySet();
		for(String observation : observations){
			Integer count = speciesCounts.get(observation);
			if(count.doubleValue()/subject.getClassificationCount() > 0.5){
					consensusSpecies.add(observation);
			}
		}
	
		return consensusSpecies;
	}

}
