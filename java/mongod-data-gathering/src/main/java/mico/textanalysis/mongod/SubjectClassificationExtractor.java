package mico.textanalysis.mongod;

/**
 * Class that runs through all Serengeti classifications from the Serengeti database
 * and outputs a JSONObject to file that, for each subject, contains the total number of 
 * users that have classified it and for each species, how many users have marked
 * it as being present in the subject (the count for "nothing" is also included).
 * 
 * The class only has a main methods. It expects an output file name as an argument.
 * 
 * @author henrikb@cs.umu.se
 *
 */

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.json.*;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;



public class SubjectClassificationExtractor {
	
	public static void main(String[] args) {
		// Check that there is an output file name as parameter
				if(args.length < 1){
					System.out.println("Please specify output file.");
					System.out.println("Quitting.");
					System.exit(0);
				}
				String outfileName = args[0];
				System.out.println(outfileName);
				
				 
				
				try {
					// Connect to a mongod instance running locally and open the
		            // classifications collection.
		            MongoClient mongoClient = new MongoClient( "localhost" );
					DB db = mongoClient.getDB("serengeti");
					DBCollection classifications = db.getCollection("serengeti_classifications");
					
					// A map to hold subjects, referenced by their ID:s
					HashMap<ObjectId,Subject> subjects = new HashMap<ObjectId,Subject>();
					
					// Get a DBCursor with all classifications
					DBCursor cursor = classifications.find();
					
					// A not strictly necessary counter to keep track of the number of
					// classifications that have been processed. Used to be able to output
					// progress.
					int i = 0;
					
					while(cursor.hasNext()){
						
						// The next classification to process
						DBObject classification = cursor.next();
						
						// Get the subject_id
						BasicBSONList subject_ids = (BasicBSONList) classification.get("subject_ids");
						ObjectId subject_id = (ObjectId) subject_ids.get("0");
						
						// Check if the subject of the current classification already has
						// an entry in subjects, otherwise create one.
						if(!subjects.containsKey(subject_id)){
							subjects.put(subject_id, new Subject(subject_id,0));
						}
						// Get the Subject object corresponding to the subject of the 
						// current classification
						Subject s = subjects.get(subject_id);
						
						// Increment the number of users that have classified the subject by one
						s.incrementCount();
						
						// Get the annotations from the classification
						BasicBSONList annotations = (BasicBSONList) classification.get("annotations");
						Set<String> keySet = annotations.keySet();
						Iterator<String> it = keySet.iterator();
						// Get the map from the subject, that for each species, keeps
						// track of how many users have seen the species.
						HashMap<String,Integer> speciesCounts = s.getSpeciesCounts();
						// Iterate over the annotations
						while(it.hasNext()){
							String observation = null;
							DBObject annotation = (DBObject)annotations.get(it.next());
							// Check whether the classification contains some species
							// or a "nothing" entry
							if(annotation.containsField("species")){
								observation = (String)annotation.get("species");
							}else if(annotation.containsField("nothing")){
								observation = "nothing";
							}
							// Increment the species counts for the subject.
							if(observation != null){
								if(!speciesCounts.containsKey(observation)){
									speciesCounts.put(observation, new Integer(1));
								}else{
									Integer oldCount = speciesCounts.get(observation);
									speciesCounts.put(observation, new Integer(oldCount.intValue() + 1));
								}
							}
						}
						
						
						i++;
						// Write to indicate progress for every 100.000th classification
						if(i % 100000 == 0){
							System.out.println(i);
						}
					}
					cursor.close();
					
					// Create a JSONObject to hold the data for all the subjects
					JSONObject allSubjects = new JSONObject();
					
					// Iterate over the subjects, turn them into JSONObjects and add
					// them to allSubjects, referenced by their ObjectId:s
					for(ObjectId subjectId : subjects.keySet()){
						Subject s = subjects.get(subjectId);
						allSubjects.put(subjectId.toString(), s.toJsonWithoutId());
						/*String output = "";
						ObjectId subject_id = it.next();
						Subject subject = subjects.get(subject_id);
						ArrayList<String> goodObservations = subject.species;
						output += subject.toString() + " ";
						HashMap<String,Integer> speciesCounts = subject.getSpeciesCounts();
						Set<String> observations = speciesCounts.keySet();
						Iterator<String> obsIterator = observations.iterator();
						while(obsIterator.hasNext()){
							String observation = obsIterator.next();
							Integer count = speciesCounts.get(observation);
							output += observation + "(" + count + ") ";
							if(count.doubleValue()/subject.getClassificationCount() > 0.5){
								goodObservations.add(observation);
							}
						}*/
					}
					// Write the resulting JSONObject to file
		            FileWriter fWriter = new FileWriter(outfileName);
		            BufferedWriter bWriter = new BufferedWriter(fWriter);
		            allSubjects.write(bWriter);
		            bWriter.close();
		            fWriter.close();
				}catch (Exception e) {
					e.printStackTrace();
				}
	}
}
