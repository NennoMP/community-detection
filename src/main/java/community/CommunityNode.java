/*
 * community.CommunityNode
 *
 * @description: Object representing a pair <node, community>.
 *
 * @author: matteo.pinna@hotmail.com
 */

package community;

import java.util.Objects;

public class CommunityNode {
    int nodeId;
    int communityId;

    public CommunityNode(int nodeId, int communityId) {
        this.nodeId = nodeId;
        this.communityId = communityId;
    }

    public int getNodeId() {
        return nodeId;
    }

    public int getCommunityId() {
        return communityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityNode that = (CommunityNode) o;
        return nodeId == that.nodeId &&
                communityId == that.communityId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, communityId);
    }

    @Override
    public String toString() {
        return nodeId + "-" + communityId;
    }
}
