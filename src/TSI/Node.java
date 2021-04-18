package TSI;

import core.game.StateObservation;
import ontology.Types;
import tools.Direction;
import tools.Vector2d;

import java.lang.reflect.Type;


/**
 * Created by dperez on 13/01/16.
 *
 * Modified by Fabián González Martín on 18/04/21.
 */
public class Node implements Comparable<Node> {

    public double totalCost;
    public double estimatedCost;
    public Node parent;
    public Vector2d position;
    public int id;
    //public int orientation = -1;

    public Node(Vector2d pos)
    {
        estimatedCost = 0.0f;
        totalCost = 1.0f;
        parent = null;
        position = pos;
        id = ((int)(position.x) * 100 + (int)(position.y));
    }

    public Node(Node copiado)
    {
        estimatedCost = copiado.estimatedCost;
        totalCost = copiado.totalCost;
        parent = null;
        position = new Vector2d( copiado.position );
        //orientation = copiado.orientation;
        id = ((int)(position.x) * 100 + (int)(position.y));
    }

    @Override
    public int compareTo(Node n) {
        return Double.compare(this.estimatedCost + this.totalCost, n.estimatedCost + n.totalCost);
    }

    @Override
    public boolean equals(Object o)
    {
        return this.position.equals(((Node)o).position); //&& ((Node) o).orientation == this.orientation;
    }

    /**
     *
     * @param pre Nodo padre a comparar
     * @return Acción realizada para llegar del nodo padre al actual
     */

    public Types.ACTIONS getMov(Node pre) {

        Types.ACTIONS action = Types.ACTIONS.ACTION_NIL;

        if(pre.position.x < this.position.x)
            action = Types.ACTIONS.ACTION_RIGHT;
        if(pre.position.x > this.position.x)
            action = Types.ACTIONS.ACTION_LEFT;

        if(pre.position.y < this.position.y)
            action = Types.ACTIONS.ACTION_DOWN;
        if(pre.position.y > this.position.y)
            action = Types.ACTIONS.ACTION_UP;

        return action;
    }
}