/**
 * Class used to represent a Snapshot Serengeti subject
 * Includes the serengeti ObjectId, a field for accumulating
 * the number of classifications done on the subject and a map
 * for recording the species choices of the classifications. 
 * 
 * Includes a method for producing a JSON representation of the 
 * subject.
 */
package mico.textanalysis.mongod;

import org.bson.types.ObjectId;
import org.json.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

/**
 * @author henrikb
 *
 */
class Subject {
	protected ObjectId subjectId;
	protected HashMap<String,Integer> speciesCounts = new HashMap<String,Integer>();
	protected int classificationCount = 0;
	
	public Subject(){
	
	}
	
	public Subject (ObjectId id, int c){
		subjectId = id;
		classificationCount = c;
	}
	
	public Subject(String id, JSONObject s){
		subjectId = new ObjectId(id);
		classificationCount = s.getInt("classificationCount");
		speciesCounts = new HashMap<String,Integer>();
		JSONObject sc = s.getJSONObject("speciesCounts");
		Iterator<String> it = sc.keys();
		while(it.hasNext()){
			String species = it.next();
			int count = sc.getInt(species);
			speciesCounts.put(species, new Integer(count));
			//System.out.print(species + " ");
		}
		//System.out.print('\n');
	}
	
	protected void incrementCount(){
		classificationCount++;
	}
	
	protected HashMap<String,Integer> getSpeciesCounts(){
		return speciesCounts;
	}
	
	protected int getClassificationCount(){
		return classificationCount;
	}
	
	protected JSONObject toJsonWithoutId(){
		JSONObject sc = new JSONObject();
		JSONObject result = new JSONObject();
		result.put("classificationCount", classificationCount);
		
		Set<String> speciesNames = speciesCounts.keySet();
		for(String name : speciesNames){
			int c = speciesCounts.get(name).intValue();
			sc.put(name, c);
		}
		result.put("speciesCounts",sc);
		return result;
	}
	
	protected JSONObject toJson(){
		JSONObject sc = new JSONObject();
		JSONObject result = new JSONObject();
		result.put("subjectId", subjectId.toString());
		result.put("classificationCount", classificationCount);
		
		Set<String> speciesNames = speciesCounts.keySet();
		for(String name : speciesNames){
			int c = speciesCounts.get(name).intValue();
			sc.put(name, c);
		}
		result.put("speciesCounts",sc);
		return result;
	}
}
