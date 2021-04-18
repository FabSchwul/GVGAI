package TSI;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Stack;

public class Agent extends AbstractPlayer {

    Vector2d portal;
    ArrayList<Observation> gemas;
    Stack<Types.ACTIONS> camino; // Pila de acciones para llegar al objetivo
    int gemas_objetivo = 9;
    int gemas_conseguidas = 0;


    /**
     * Constructor público con el estado y tiempo transcurrido iniciales.
     *
     * @param stateObs     Estado observable del juego.
     * @param elapsedTimer Temporizador para la creación del controlador.
     */
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        camino = new Stack<>();

        // No es necesario calcular el factor de escala, ya tenemos el método StateObservation.getBlockSize() para eso
        // Creamos una lista de portales ordenados por proximidad al avatar
        ArrayList<Observation>[] portales = stateObs.getPortalsPositions(stateObs.getAvatarPosition());

        // Vemos si hay portales en el mapa
        if (portales == null) {
            portal = null;
        } else {
            // Guardamos el portal más cercano
            portal = portales[0].get(0).position;

            // Convertimos las coordenadas del portal de píxeles a bloques
            portal.mul(1.0 / stateObs.getBlockSize());
        }

        // Vemos si hay gemas en el mapa
        ArrayList<Observation>[] gemas_array = stateObs.getResourcesPositions(stateObs.getAvatarPosition());

        if (gemas_array != null && gemas_array[0].size() >= gemas_objetivo) {
            gemas = gemas_array[0];
        }
    }


    /**
     * Escoge una acción. Esta función se llama cada tick del juego
     * para obtener una acción del jugador.
     *
     * @param stateObs     Estado observable del juego.
     * @param elapsedTimer Temporizador para la creación del controlador.
     * @return Acción para el estado actual.
     */
    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        Types.ACTIONS accion = Types.ACTIONS.ACTION_NIL; // Acción por defecto
        Vector2d posicion_actual = stateObs.getAvatarPosition().mul(1.0 / stateObs.getBlockSize()); // Posición actual del jugador

        if (camino.empty()){
            backTrack(pathfindAStar(stateObs, stateObs.getAvatarPosition().mul(1.0 / stateObs.getBlockSize()), portal));
        }

        if (!camino.empty() && elapsedTimer.remainingTimeMillis() > 0){
            accion = camino.peek();
            camino.pop();

        }
        return accion;
    }

    /**
     * @param stateObs Estado observable del juego.
     * @param nodo     Nodo a analizar
     * @return True si es un obstáculo, false si no lo es
     */
    private boolean esObstaculo(StateObservation stateObs, Node nodo) {
        ArrayList<Observation>[][] grid = stateObs.getObservationGrid();

        for (Observation obs : grid[(int) nodo.position.x][(int) nodo.position.y]) {
            if (obs.itype == 0)
                return true;
        }
        return false;
    }

    /**
     * Cálculo de la función h'(n) mediante la distancia Manhattan
     *
     * @param origen
     * @param destino
     * @return Distancia Manhattan
     */
    private double calcularH(Node origen, Node destino) {
        return (Math.abs(origen.position.x - destino.position.x) +
                Math.abs(origen.position.y - destino.position.y));
    }

    /**
     * Algoritmo A Estrella
     *
     * @param stateObs Estado observable del juego.
     * @param origen   Posición en la que comienza el avatar
     * @param destino  Posición a la que se quiere llegar
     * @return Último nodo del camino óptimo
     */
    private Node pathfindAStar(StateObservation stateObs, Vector2d origen, Vector2d destino) {
        Node inicio = new Node(origen);
        Node fin = new Node(destino);
        Node solucion = null;
        Node menor_coste = inicio;

        boolean destino_alcanzado = false;

        PriorityQueue<Node> listaCerrados = new PriorityQueue<>();
        PriorityQueue<Node> listaAbiertos = new PriorityQueue<>();

        inicio.totalCost = 0;
        inicio.estimatedCost = calcularH(inicio, fin);

        listaAbiertos.add(inicio);

        ArrayList<Types.ACTIONS> actions = new ArrayList<>(stateObs.getAvailableActions());
        actions.remove(0); // No nos interesa la acción de usar el pico en esta práctica

        // Solo salimos del bucle si llegamos al destino o se nos vacía la lista de abiertos
        while (!listaAbiertos.isEmpty() && !destino_alcanzado) {
            // Exploramos el mejor nodo de la lista de abiertos y lo añadimos a la lista de cerrados
            menor_coste = listaAbiertos.poll();
            listaCerrados.add(menor_coste);

            // ¿Hemos llegado al destino?
            destino_alcanzado = menor_coste.equals(fin);

            if (!destino_alcanzado) {
                // Expandimos los 4 hijos del nodo
                for (Types.ACTIONS accion : actions) {
                    Node hijo = new Node(menor_coste);

                    if (accion.equals(Types.ACTIONS.ACTION_LEFT)) {
                        hijo.position.x -= 1;
                    } else if (accion.equals(Types.ACTIONS.ACTION_RIGHT)) {
                        hijo.position.x += 1;
                    } else if (accion.equals(Types.ACTIONS.ACTION_DOWN)) {
                        hijo.position.y += 1;
                    } else if (accion.equals(Types.ACTIONS.ACTION_UP)) {
                        hijo.position.y -= 1;
                    }

                    if (!esObstaculo(stateObs, hijo)) {
                        hijo.parent = menor_coste;
                        hijo.totalCost = menor_coste.totalCost + 1.0;
                        hijo.estimatedCost = calcularH(hijo, fin);
                        hijo.id = ((int)(hijo.position.x) * 100 + (int)(hijo.position.y));

                        if (!listaAbiertos.contains(hijo) && !listaCerrados.contains(hijo)) {
                            listaAbiertos.add(hijo);
                        } else {
                            // Si nos topamos con un hijo que ya ha sido explorado y se encuentra en la lista de abiertos,
                            // podemos volverlo a evaluar para comprobar si el nuevo camino es más óptimo
                            if (listaAbiertos.contains(hijo)) {
                                ArrayList<Node> array_abiertos = new ArrayList<Node>(Arrays.asList(listaAbiertos.toArray(new Node[listaAbiertos.size()])));
                                Node alreadyOpen = array_abiertos.get(array_abiertos.indexOf(hijo));
                                if (alreadyOpen.totalCost > hijo.totalCost) {
                                    listaAbiertos.remove(alreadyOpen);
                                    listaAbiertos.add(hijo);
                                }
                            }
                        }
                    }

                }
            } else {
                solucion = menor_coste;
            }
        }
        return solucion;

    }


    /**
     * Caminamos sobre nuestros pasos para construir el camino
     *
     * @param resultado
     */
    private void backTrack(Node resultado) {
        camino = new Stack<>();

        // Seguimos introduciendo acciones en la pila hasta que alcancemos el nodo origen
        while (resultado != null) {
            if (resultado.parent != null) {
                camino.push(resultado.getMov(resultado.parent));

            }
            resultado = resultado.parent;
        }

    }
}
