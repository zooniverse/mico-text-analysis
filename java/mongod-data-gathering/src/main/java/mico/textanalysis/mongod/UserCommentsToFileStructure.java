/**
 * 
 */
package mico.textanalysis.mongod;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * @author henrikb
 *
 */
public class UserCommentsToFileStructure {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Check that there is an base output directory 
		if(args.length < 1){
			System.out.println("Please specify output file.");
			System.out.println("Quitting.");
			System.exit(0);
		}
		String baseDirName = args[0];
		CommentsByUserExtractor extractor = new CommentsByUserExtractor();
		Map<Integer,List<String>> comments = extractor.getCommentsByUser();
		int c = 0,u = 0;
		Set<Integer> users = comments.keySet();
		for(Integer user : users){
			u++;
			String userDirName = baseDirName + '/' + user;
			File userDir = new File(userDirName);
			userDir.mkdirs();
			System.out.println('\n' + user);
			List<String> userComments = comments.get(user);
			int cCount = 0;
			for(String comment : userComments){
				c++;
				cCount++;
				String commentFileName = userDirName + '/' + cCount + ".txt"; 
				try{
					// Open the output file
					FileWriter fWriter = new FileWriter(commentFileName);
					PrintWriter writer = new PrintWriter(fWriter);
					writer.write(comment);
					writer.close();
					fWriter.close();
				}catch(IOException e){
					System.err.println("Couldn't write to file");
					System.exit(0);
				}
				System.out.println(comment + '\n');
			}
		}
		System.out.println("c=" +c);
		System.out.println("u=" +u);
	}

}
