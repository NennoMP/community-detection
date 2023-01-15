/*
 * community,CommunityScore
 *
 * @description: Object representing the community score for a node.
 *
 * @author: matteo.pinna@hotmail.com
 */

package community;

public class CommunityScore {
    public int node;
    public float cpScore; // community participation score

    public CommunityScore(int node, float cpScore) {
        this.node = node;
        this.cpScore = cpScore;
    }

    public int getNode() {
        return node;
    }

    public float getCpScore() {
        return cpScore;
    }
}
