/**
 * Class for computing the accuracy of serengeti users.
 * 
 * The main method takes two arguments: an input file and an output file.
 * 
 * The input file is assumed to be a .json file that stores a JSONObject 
 * representing all the serengeti subjects, with their respective counts of
 * classifications per species.
 * 
 * The behavior of the method can be modified by changing the choice of 
 * implementation of the ClassificationScorer (see comment in main method).
 * 
 * The result is written to the output file in csv format with four columns:
 * (1) Zooniverse id (2) ObjectId in the mongod database (3) number of 
 * classifications (4) computed accuracy.
 * 
 * @author: henrikb@cs.umu.se
 */

package mico.textanalysis.mongod;

import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class GetUserAccuracy {

	public static void main(String[] args) {
		// Check that there is an output file name as parameter
				if(args.length < 2){
					System.out.println("Please specify input and output files.");
					System.out.println("Quitting.");
					System.exit(0);
				}
				String inFileName = args[0];
				String outFileName = args[1];
				System.out.println(inFileName);
				System.out.println(outFileName);
				
				// Set the classification scorer implementation
				ClassificationScorer classificationScorer = new MajorityClassificationScorer();
				
				try {
		            
					// Open the serengeti database
		            MongoClient mongoClient = new MongoClient( "localhost" );
					DB db = mongoClient.getDB("serengeti");
					
					// Read all the subjects from file
					HashMap<ObjectId,Subject> subjects = getSubjectsFromFile(inFileName);
					System.out.println(subjects.size());
					
					// Get a DBCursor with all users
					DBCollection users = db.getCollection("serengeti_users");
					DBCursor userCursor = users.find();
					
					DBCollection classifications = db.getCollection("serengeti_classifications");
					
					// Open the output file
					FileWriter fWriter = new FileWriter(outFileName);
		            PrintWriter writer = new PrintWriter(fWriter);
					
		            // Iterate over the users
		            // For each user, calculate the total score (uScore) and
		            // then divide it by the number of classifications the user has done
					int uCount = 0;
					double uScore;
					while(userCursor.hasNext()){
						uScore = 0;
						
						// Get the users ObjectId and Zooniverse id
						DBObject user = userCursor.next();
						ObjectId userId = (ObjectId)user.get("_id"); 
						int zooniverseId;
						if(user.containsField("zooniverse_id")){
							Object tmp = user.get("zooniverse_id");
							if(tmp != null){
								zooniverseId = ((Integer)tmp).intValue();
							}else{
								zooniverseId = 0;
							}
						}else{
							zooniverseId = 0;
						}
						
						// Get a cursor with all the classifications the user has performed
						BasicDBObject query = new BasicDBObject("user_id", userId);
						DBCursor userClassifications = classifications.find(query);
						
						// Get the number of classifications by the user
						int userClassificationCount = userClassifications.count();
						
						// Iterate over the classifications
						while(userClassifications.hasNext()){
							
							DBObject classification = userClassifications.next();
							ObjectId subjectId = (ObjectId)((BasicBSONList)classification.get("subject_ids")).get("0");
							
							// Get the 'annotations' of the classification
							BasicBSONList annotations = (BasicBSONList) classification.get("annotations");
							Set<String> keySet = annotations.keySet();
							Iterator<String> annotationIt = keySet.iterator();
							ArrayList<String> userObs = new ArrayList<String>();
							String observation;
							
							// Iterate over the annotations
							while(annotationIt.hasNext()){
								observation = null;
								DBObject annotation = (DBObject)annotations.get(annotationIt.next());
								if(annotation.containsField("species")){
									observation = (String)annotation.get("species");
								}else if(annotation.containsField("nothing")){
									observation = "nothing";
								}
								if(observation != null){
									userObs.add(observation);
								}
							}
							// Get the score from the classification scorer
							Subject subject = subjects.get(subjectId);
							uScore += classificationScorer.scoreClassification(subject, userObs);
						}
						// Compute the user accuracy
						double accuracy;
						if(uScore > 0.0){
							accuracy = uScore / userClassificationCount;
						}else{
							accuracy = 0;
						}
						
						// Increment the count of users that have been processed
						// If at an even 100, write to stdout to indicate progress
						uCount++;
						if(uCount % 100 == 0) System.out.println(uCount);
						
						// Write the entry for the user to file
						writer.write(zooniverseId + "," + userId.toString() + "," + userClassificationCount + "," + accuracy + '\n');
						
					}
					
					writer.close();
					fWriter.close();
				} catch (Exception e) {
					e.printStackTrace();
				} 

	}
	
	// Method for reading the subjects from the json input file
	private static HashMap<ObjectId,Subject> getSubjectsFromFile(String inFile){
		HashMap<ObjectId,Subject> result = new HashMap<ObjectId,Subject>();
		
		JSONTokener tokener = null;
		try{
			System.out.println("Reading file...");
			tokener = new JSONTokener(new FileInputStream(inFile));
			System.out.println("Done.");
		}catch(FileNotFoundException e){
			System.err.println("Couldn't find the input file file");
		}
		JSONObject subjects = new JSONObject(tokener);
		Iterator<String> it = subjects.keys();
		while(it.hasNext()){
			String subjectId = it.next();
			JSONObject subject = subjects.getJSONObject(subjectId);
			Subject s = new Subject(subjectId, subject);
			result.put(new ObjectId(subjectId), s);
		}
		
		return result;
	}

}
