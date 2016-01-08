package mico.textanalysis.mongod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class GetUserAccuracy {

	public static void main(String[] args) {
		// Check that there is an output file name as parameter
				if(args.length < 1){
					System.out.println("Please specify output file.");
					System.out.println("Quitting.");
					System.exit(0);
				}
				String outfileName = args[0];
				System.out.println(outfileName);
				
				BufferedWriter writer = null;
				try {
					// Create the File object and a writer for writing to it
					File outFile = new File(outfileName);
		            writer = new BufferedWriter(new FileWriter(outFile));
		            
		            // Open the talk database and get the collection of all discussions
		            MongoClient mongoClient = new MongoClient( "localhost" );
					DB db = mongoClient.getDB("serengeti");
					DBCollection classifications = db.getCollection("serengeti_classifications");
					
					// Get a DBCursor with all classifications
					DBCursor cursor = classifications.find();
					
					HashMap<ObjectId,HashMap<String,Integer>> counts = new HashMap<ObjectId,HashMap<String,Integer>>();
					HashMap<ObjectId,Integer> classificationCounts = new HashMap<ObjectId,Integer>();
					
					int i = 0;
					while(cursor.hasNext()){
						DBObject classification = cursor.next();
						
						// Get all the comments belonging to the discussion
						// Iterate over the comments and write their bodies to file
						BasicBSONList subject_ids = (BasicBSONList) classification.get("subject_ids");
						ObjectId subject_id = (ObjectId) subject_ids.get("0");
						
						if(!classificationCounts.containsKey(subject_id)){
							classificationCounts.put(subject_id, new Integer(1));
						}else{
							Integer oldCount = classificationCounts.get(subject_id);
							classificationCounts.put(subject_id, new Integer(oldCount.intValue() + 1));
						}
						if(!counts.containsKey(subject_id)){
							counts.put(subject_id, new HashMap<String,Integer>());
						}
						
						BasicBSONList annotations = (BasicBSONList) classification.get("annotations");
						Set<String> keySet = annotations.keySet();
						Iterator<String> it = keySet.iterator();
						HashMap<String,Integer> subjectCounts = counts.get(subject_id);
						while(it.hasNext()){
							String observation = null;
							DBObject annotation = (DBObject)annotations.get(it.next());
							if(annotation.containsField("species")){
								observation = (String)annotation.get("species");
							}else if(annotation.containsField("nothing")){
								observation = "nothing";
							}
							if(observation != null){
								if(!subjectCounts.containsKey(observation)){
									subjectCounts.put(observation, new Integer(1));
								}else{
									Integer oldCount = subjectCounts.get(observation);
									subjectCounts.put(observation, new Integer(oldCount.intValue() + 1));
								}
							}
						}
						
						/*if(!counts.containsKey(subject_id)){
							counts.put(subject_id, new HashMap<String,Integer>());
						}else{
							Integer current = counts.get(subject_id);
							Integer updated = new Integer(current.intValue()+1);
							counts.put(subject_id, updated);
						}
						
						System.out.println(subject_id.toString());*/
						i++;
						if(i % 100000 == 0){
							System.out.println(i);
						}
					}
					cursor.close();
					
					Set<ObjectId> subjects = counts.keySet();
					Iterator<ObjectId> it = subjects.iterator();
					HashMap<ObjectId,ArrayList<String>> subjectResults = new HashMap<ObjectId,ArrayList<String>>();
					while(it.hasNext()){
						String output = "";
						ObjectId subject = it.next();
						int total = classificationCounts.get(subject).intValue();
						ArrayList<String> goodObservations = new ArrayList<String>();
						output += subject.toString() + " ";
						HashMap<String,Integer> subjectCounts = counts.get(subject);
						Set<String> observations = subjectCounts.keySet();
						Iterator<String> obsIterator = observations.iterator();
						while(obsIterator.hasNext()){
							String observation = obsIterator.next();
							Integer count = subjectCounts.get(observation);
							output += observation + "(" + count + ") ";
							if(count.doubleValue()/total > 0.5){
								goodObservations.add(observation);
							}
						}
						subjectResults.put(subject, goodObservations);
						/*Object[] result = goodObservations.toArray();
						Arrays.sort(result);
						output += "Total:(" + total + ") ";
						output +="Result:[";
						for(int j = 0 ; j < result.length-1 ; j++){
							output += result[j] + ",";
						}
						if(result.length > 0){
							output += result[result.length-1];
						}
						output += "]\n";
						writer.write(output);*/
						
						
					}
					
					DBCollection users = db.getCollection("serengeti_users");
					// Get a DBCursor with all users
					DBCursor userCursor = users.find();
					int uCount = 0;
					while(userCursor.hasNext()){
						int correctClassifications = 0;
						DBObject user = userCursor.next();
						ObjectId userId = (ObjectId)user.get("_id"); 
						int zooniverseId;
						if(user.containsField("zooniverse_id")){
							zooniverseId = (int)user.get("zooniverse_id");
						}else{
							zooniverseId = 0;
						}
						//System.out.println("User: " + userId.toHexString());
						BasicDBObject query = new BasicDBObject("user_id", userId);
					
						DBCursor userClassifications = classifications.find(query);
						//System.out.println("Count: " + userClassifications.count());
						int userClassificationCount = userClassifications.count();
						while(userClassifications.hasNext()){
							
							DBObject classification = userClassifications.next();
							ObjectId classificationId = (ObjectId)classification.get("_id");
							ObjectId subjectID = (ObjectId)((BasicBSONList)classification.get("subject_ids")).get("0");
							BasicBSONList annotations = (BasicBSONList) classification.get("annotations");
							Set<String> keySet = annotations.keySet();
							Iterator<String> annotationIt = keySet.iterator();
							ArrayList<String> userObs = new ArrayList<String>();
							String observation;
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
							ArrayList<String> consensuses = subjectResults.get(subjectID);
							boolean agrees = true;
							if(consensuses.size() != userObs.size()){
								agrees = false;
							}
							for(String s : userObs){
								if(!consensuses.contains(s)){
									agrees = false;
								}
							}
							if(agrees){
								correctClassifications++;
							}
							//System.out.println(classificationId.toHexString());
							
						}
						double accuracy;
						if(userClassificationCount > 0){
							accuracy = correctClassifications / (new Integer(userClassificationCount).doubleValue());
						}else{
							accuracy = 0;
						}
						uCount++;
						if(uCount % 100 == 0) System.out.println(uCount);
						//System.out.println(uCount + " " + zooniverseId + " " + userId.toString() + " " + userClassificationCount + " " + accuracy);
						writer.write(zooniverseId + "," + userId.toString() + "," + userClassificationCount + "," + accuracy + '\n');
						//System.out.println("");
					}
					
					writer.close();
					
				} catch (Exception e) {
					e.printStackTrace();
				} 

	}

}
