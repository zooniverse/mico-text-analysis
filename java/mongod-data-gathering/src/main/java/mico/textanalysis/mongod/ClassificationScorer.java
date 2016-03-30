/**
 * Interface for classes that take a Subject and a list of species that 
 * a user has indicated and gives the user's classification a score.
 */

package mico.textanalysis.mongod;

import java.util.ArrayList;
/**
 * @author henrikb
 *
 */
public interface ClassificationScorer {
	/* The score should be a number in the interval [0,1] */
	public double scoreClassification(Subject s, ArrayList<String> userSpecies);
}
