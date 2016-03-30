/**
 * A class with a method to get a map from user id:s to all the comments
 * written by that user.
 * 
 * Assumes that MongoDB is running on localhost and that it contains
 * the 'talk' database.
 */
package mico.textanalysis.mongod;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;
import org.bson.BasicBSONObject;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * @author henrikb@cs.umu.se
 * @author yonasd@cs.umu.se
 *
 */
public class CommentsByUserExtractor {
	protected Map<Integer,List<String>> getCommentsByUser(){
		// Create a map that will map a user name to a list containing all
		// comment bodies written by the user.
		Map<Integer,List<String>> map = new HashMap<Integer,List<String>>();
		
		try{
			// Open the talk database and get the collection of all discussions
        	MongoClient mongoClient = new MongoClient( "localhost" );
        	DB db = mongoClient.getDB("talk");
        	DBCollection discussions = db.getCollection("discussions");
        	
        	BasicDBObject query = new BasicDBObject("project_id", new ObjectId("5077375154558fabd7000001"));
			DBCursor cursor = discussions.find(query);
			
			// Iterate over all Serengeti discussions
			while(cursor.hasNext()){
				DBObject discussion = cursor.next();
				
				// Get all the comments belonging to the discussion
				// Iterate over the comments and add them to the map
				BasicBSONList comments = (BasicBSONList) discussion.get("comments");
				for(Object commentObject : comments){
					BasicBSONObject comment = (BasicBSONObject) commentObject;
					String body = (String)comment.get("body");
					Integer uname = (Integer) comment.get("user_zooniverse_id");
					List<String> ucomments = null;
					if(map.containsKey(uname)){
						ucomments = map.get(uname);
					}else{
						ucomments = new ArrayList<String>();
						map.put(uname, ucomments);
					}
					ucomments.add(body);
				}
			}
			cursor.close();
        }catch (Exception e) {
			e.printStackTrace();
		} 
		return map;
	}
}
