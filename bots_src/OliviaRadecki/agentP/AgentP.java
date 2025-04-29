package agentP;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

public class AgentP extends AbstractionLayerAI {
    /**
     * implementation of a template pair
     *
     * @param <A>
     * @param <B>
     */
    public class Pair<A, B> {

        private final
                A first;
        private final
                B second;

        public
                Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public
                A getFirst() {
            return first;
        }

        public
                B getSecond() {
            return second;
        }
    }
    
    /**
     * A combination of a unit and a distance from a query subject
     */
    public class UnitAndDistance implements Comparable<UnitAndDistance> {

        Unit u;
        int distance;

        public
                UnitAndDistance(Unit u, int distance) {
            this.distance = distance;
            this.u = u;
        }

        @Override
        public
                int compareTo(UnitAndDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }
    
    protected
            UnitTypeTable utt;
    UnitType baseType;
    UnitType barracksType;
    UnitType workerType;
    UnitType lightType;
    UnitType heavyType;
    UnitType rangedType;

    int defenseRange = 10;
    int harvesterAndNearbyResourceRange = 8; // Max distance squared either can be away, harvester as well because it can go in a spot that used to belong to a resource
    int unitsBeingTrained = 0;

    int resourcesUsed = 0;
    List<Unit> nearbyResources = new LinkedList<>();
    List<Unit> harvesters = new LinkedList<>();
    List<Unit> builders = new LinkedList<>();

    /**
     *
     * @param a_utt
     */
    public
            AgentP(UnitTypeTable a_utt) {
        this(a_utt, new AStarPathFinding());
    }

    /**
     *
     * @param a_utt
     * @param a_pf
     */
    public
            AgentP(UnitTypeTable a_utt, PathFinding a_pf) {
        super(a_pf);
        reset(a_utt);
    }

    public
            void reset() {
        super.reset();
    }

    public
            void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        if (utt != null) {
            baseType = utt.getUnitType("Base");
            barracksType = utt.getUnitType("Barracks");
            workerType = utt.getUnitType("Worker");
            lightType = utt.getUnitType("Light");
            heavyType = utt.getUnitType("Heavy");
            rangedType = utt.getUnitType("Ranged");
        }
    }

    public
            AI clone() {
        return new AgentP(utt, pf);
    }

    /**
     *
     * @param player Gets action from the player
     * @param gs current game state
     * @return
     */
    public
            PlayerAction getAction(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player p = gs.getPlayer(player);

        resourcesUsed = 0;
        unitsBeingTrained = 0;
        // friendly units
        List<Unit> friendlyBases = new LinkedList<>();
        List<Unit> friendlyBarracks = new LinkedList<>();
        List<Unit> friendlyWorkers = new LinkedList<>();
        List<Unit> friendlyLightUnits = new LinkedList<>();
        List<Unit> friendlyHeavyUnits = new LinkedList<>();
        List<Unit> friendlyRangedUnits = new LinkedList<>();
        
        // enemy units
        List<Unit> enemyBases = new LinkedList<>();
        List<Unit> enemyBarracks = new LinkedList<>();
        List<Unit> enemyWorkers = new LinkedList<>();
        List<Unit> enemyLightUnits = new LinkedList<>();
        List<Unit> enemyHeavyUnits = new LinkedList<>();
        List<Unit> enemyRangedUnits = new LinkedList<>();
        List<Unit> combinedEnemyUnits = new LinkedList<>();
        // units without claims
        List<Unit> resources = new LinkedList<>();

        // store all units to prevent future checks done on them later
        for (Unit u : pgs.getUnits()) {
            if (u.getPlayer() == p.getID()) {
                if (u.getType() == baseType) {
                    friendlyBases.add(u);
                }
                else if (u.getType() == barracksType) {
                    friendlyBarracks.add(u);
                }
                else if (u.getType() == workerType) {
                    friendlyWorkers.add(u);
                }
                else if (u.getType() == lightType) {
                    friendlyLightUnits.add(u);
                }
                else if (u.getType() == heavyType) {
                    friendlyHeavyUnits.add(u);
                }
                else if (u.getType() == rangedType) {
                    friendlyRangedUnits.add(u);
                }
            }
            else if (u.getPlayer() != p.getID()) {
                if (u.getType() == baseType) {
                    enemyBases.add(u);
                }
                else if (u.getType() == barracksType) {
                    enemyBarracks.add(u);
                }
                else if (u.getType() == workerType) {
                    enemyWorkers.add(u);
                }
                else if (u.getType() == lightType) {
                    enemyLightUnits.add(u);
                }
                else if (u.getType() == heavyType) {
                    enemyHeavyUnits.add(u);
                }
                else if (u.getType() == rangedType) {
                    enemyRangedUnits.add(u);
                }
            }
            if (u.getType().isResource) {
                resources.add(u);
            }
        }
        // create list of all enemies
        combinedEnemyUnits.addAll(enemyWorkers);
        combinedEnemyUnits.addAll(enemyLightUnits);
        combinedEnemyUnits.addAll(enemyHeavyUnits);
        combinedEnemyUnits.addAll(enemyRangedUnits);
        combinedEnemyUnits.addAll(enemyBases);
        combinedEnemyUnits.addAll(enemyBarracks);

        // Unit behaviors
        
        // Calculate training queues
        countTroopsInTraining(friendlyBarracks, friendlyBases, gs);
        
        for (Unit base : friendlyBases) {
            baseBehavior(base, p, gs, friendlyBarracks, friendlyWorkers, friendlyLightUnits, friendlyHeavyUnits, friendlyRangedUnits, combinedEnemyUnits, resources);
        }
        for (Unit barracks : friendlyBarracks) {
            barracksBehavior(barracks, p, pgs, combinedEnemyUnits);
        }
        if ( ! friendlyWorkers.isEmpty()) {
            workerBehavior(friendlyWorkers, friendlyBases, friendlyBarracks, combinedEnemyUnits, p, gs);
        }
        for (Unit lightUnit : friendlyLightUnits) {
            lightUnitBehavior(lightUnit, combinedEnemyUnits, p, gs);
        }
        for (Unit heavyUnit : friendlyHeavyUnits) {
            heavyUnitBehavior(heavyUnit, combinedEnemyUnits, p, gs);
        }
        for (Unit rangedUnit : friendlyRangedUnits) {
            rangedUnitBehavior(rangedUnit,combinedEnemyUnits, p, gs);
        }
        return translateActions(player, gs);
    }
    
    public static void main(String[] args) {
        //System.out.println("AgentP compiled successfully.");
    }
    
    /**
     * returns a distance calculated from a modified distance formula in which
     * the square root is not taken, if all query distances are squared and all
     * distances calculated are not taken with a square root this is more
     * accurate and it is faster to run
     *
     * @param u First unit to compare
     * @param u2 Second unit to compare
     * @return 
     */
    int calcDistNoSquare(Unit u, Unit u2) {
        return (int) (Math.pow(u2.getX() - u.getX(), 2) + Math.pow(u2.getY() - u.getY(), 2));
    }

    /**
     * Returns whether or not u is in range of u2's attack
     *
     * @param u First unit to compare
     * @param u2 Second unit to compare
     * @return Returns if it is in range
     */
    public
            boolean isInAttackRange(Unit u, Unit u2) {
        if (u == u2)
            return false;
        return (Math.pow(u2.getAttackRange(), 2) >= calcDistNoSquare(u, u2));
    }

    /**
     * Side used to record if a unit is on the left or right side of the map
     */
    enum Side {
        LEFT,
        RIGHT
    }
    
    public void countTroopsInTraining(List<Unit> barracks, List<Unit> bases, GameState gs)
    {
        for (Unit barrack : barracks)
        {
            if (!barrack.isIdle(gs))
            {
                ++unitsBeingTrained;
            }
        }
        for (Unit base : bases)
        {
            if (!base.isIdle(gs))
            {
                ++unitsBeingTrained;
            }
        }
        return;
    }

    /**
     * the base will ensure that there is an equal number of dedicated
     * harvesters and nearby resources
     * <p>
     * if there are enough dedicated harvesters, the base is in danger of an
     * attack, and no better defensive unit can be produced or a army unit will
     * not be enough and making a worker will leave money behind for an army
     * unit if barracks are available then the base will produce a defensive
     * worker
     *
     * @param u unit 
     * @param p player Which player the unit belongs to
     * @param gs GameState Current game state
     * @param friendlyBarracks List of Friendly Barracks 
     * @param friendlyWorkers List of Friendly Workers
     * @param friendlyLightUnits List of Friendly Light Units
     * @param friendlyHeavyUnits List of Friendly Heavy Units
     * @param friendlyRangedUnits List of Friendly Ranged Units
     * @param combinedEnemyUnits List of Enemy Units
     * @param resources List of current resources
     */
    public
            void baseBehavior(Unit u, Player p, GameState gs, List<Unit> friendlyBarracks, List<Unit> friendlyWorkers, List<Unit> friendlyLightUnits, List<Unit> friendlyHeavyUnits, List<Unit> friendlyRangedUnits, List<Unit> combinedEnemyUnits, List<Unit> resources) {
        if ( ! u.isIdle(gs)) {
            return;
        }
        PhysicalGameState pgs = gs.getPhysicalGameState();
        int nearbyFriendlyWorkers = 0;
        nearbyResources.clear();
        // count number of nearby workers that can be used as dedicated harvesters
        for (Unit worker : friendlyWorkers) {
            // must square compare distance as the calculated distance formula does not use sqaure root
            if (calcDistNoSquare(u, worker) <= harvesterAndNearbyResourceRange) {
                nearbyFriendlyWorkers ++;
            }
        }
        // count number of nearby resources
        for (Unit resource : resources) {
            // must square compare distance as the calculated distance formula does not use sqaure root
            if (calcDistNoSquare(u, resource) <= harvesterAndNearbyResourceRange) {
                nearbyResources.add(resource);
            }
        }

        // if there are less nearby friendly workers compared to nearby resources, a new worker will be made
        if (nearbyResources.size() > nearbyFriendlyWorkers && p.getResources() >= workerType.cost + resourcesUsed) {
            //System.out.println("Creating dedicated harvester");
            train(u, workerType);
            ++unitsBeingTrained;
            resourcesUsed += workerType.cost;
            return; // Each individual unit can only perform one action
        }

        // the rest of this function only matters if funds exist to make another worker
        if (p.getResources() < workerType.cost + resourcesUsed) {
            return;
        }

        // create a builder if needed
        if (friendlyBarracks.isEmpty() && (p.getResources() >= barracksType.cost + (lightType.cost - 2) + workerType.cost + resourcesUsed || p.getResources() >= barracksType.cost + (heavyType.cost - 2) + workerType.cost + resourcesUsed || p.getResources() >= barracksType.cost + (rangedType.cost - 2) + workerType.cost + resourcesUsed) && builders.isEmpty()) {
            //System.out.println("Prepping to build");
            train(u, workerType);
            ++unitsBeingTrained;
            resourcesUsed += workerType.cost;
            return; // Each individual unit can only perform one action
        }

        int threats = 0;
        // check for threats to the base
        if ( ! combinedEnemyUnits.isEmpty()) {
            for (Unit enemy : combinedEnemyUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, enemy) <= Math.pow(defenseRange, 2)) {
                    // immediate threat
                    if (isInAttackRange(u, enemy)) {
                        //System.out.println("Creating worker for immediate threat");
                        train(u, workerType);
                        ++unitsBeingTrained;
                        resourcesUsed += workerType.cost;
                        return; // Each individual unit can only perform one action
                    }
                    threats ++;
                }
            }
            // find size of nearby units who can fight back
            int nearbyFriendlyArmySize =  - nearbyResources.size(); // prevents dedicated harvesters from being counted in defensible army size
            for (Unit worker : friendlyWorkers) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, worker) <= Math.pow(defenseRange, 2)) {
                    nearbyFriendlyArmySize ++;
                }
            }
            for (Unit lightUnit : friendlyLightUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, lightUnit) <= Math.pow(defenseRange, 2)) {
                    nearbyFriendlyArmySize ++;
                }
            }
            for (Unit heavyUnit : friendlyHeavyUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, heavyUnit) <= Math.pow(defenseRange, 2)) {
                    nearbyFriendlyArmySize ++;
                }
            }
            for (Unit rangedUnit : friendlyRangedUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, rangedUnit) <= Math.pow(defenseRange, 2)) {
                    nearbyFriendlyArmySize ++;
                }
            }

            // check if a larger nearby army is needed - worker cannot immediately offset
            if (nearbyFriendlyArmySize + unitsBeingTrained + 1 <= threats) {

                        train(u, workerType);
                        ++unitsBeingTrained;
                        resourcesUsed += workerType.cost;
            }
        }
    }

    /**
     * If resources exist and a light unit is needed then the barracks should
     * produce a light unit
     *
     * @param u Unit to train
     * @param p Player Connected 
     * @param pgs the physical game state
     * @param combinedEnemyUnits List of Enemy Units
     */
    public
            void barracksBehavior(Unit u, Player p, PhysicalGameState pgs, List<Unit> combinedEnemyUnits) {
        // Check for immediate danger --> If there is, produce light unit
        if ( ! combinedEnemyUnits.isEmpty()) {
            for (Unit enemy : combinedEnemyUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, enemy) <= Math.pow(defenseRange, 2)) {
                    // immediate threat
                    if (isInAttackRange(u, enemy)) {
                        // Produce light unit
                        //System.out.println("Producing light unit for immediate threat");
                        if (p.getResources() >= lightType.cost + resourcesUsed) {
                            train(u, lightType);
                            ++unitsBeingTrained;
                            return;
                        }
                    }                      
                }
            }
        }
        // Check for enemy approaching from far away --> If there is, produce heavy unit   
        if ( ! combinedEnemyUnits.isEmpty()) {
            for (Unit enemy : combinedEnemyUnits) {
                // squaring of defense range required as the distance calculation does not take the square root
                if (calcDistNoSquare(u, enemy) <= Math.pow(defenseRange, 2)) {
                    // not an immediate threat
                    if (! isInAttackRange(u, enemy)) {
                        // Produce heavy unit
                        //System.out.println("Producing heavy unit for faraway threat");
                        if (p.getResources() >= heavyType.cost + resourcesUsed) {
                            train(u, heavyType);
                            ++unitsBeingTrained;
                            return;
                        }
                    }                      
                }
            }
        }
          
        // Default option: produce ranged units
        if (p.getResources() >= rangedType.cost + resourcesUsed) {
            train(u, rangedType);
            ++unitsBeingTrained;
        }           
    }

    /**
     * A number of dedicated harvesters equal to the number of nearby resources
     * will be reserved and the rest will join the army
     *
     * @param workers List of Current Unit workers
     * @param bases List of bases, this normally will be one
     * @param barracks This of barracks
     * @param combinedEnemyUnits List of enemy units
     * @param p Player the unit is connected to
     * @param gs The current GameState
     */
    public
            void workerBehavior(List<Unit> workers, List<Unit> bases, List<Unit> barracks, List<Unit> combinedEnemyUnits, Player p, GameState gs) {
        if (workers.isEmpty()) {
            return;
        }
        PhysicalGameState pgs = gs.getPhysicalGameState();

        List<Unit> freeWorkers = new LinkedList<>();
        for (Unit worker : workers) {
            if (worker.isIdle(gs) &&  ! harvesters.contains(worker) &&  ! builders.contains(worker)) {
                freeWorkers.add(worker);
            }
        }

        // remove units that are dead or too far away to be a dedicated harvester
        for (Unit harvester : harvesters) {
            // harvester is dead
            if ( ! workers.contains(harvester)) {
                harvesters.remove(harvester);
            }
            // unit exists and a distance check can be performed
            else {
                if ( ! bases.isEmpty()) {
                    Unit base = bases.getFirst();
                    if (calcDistNoSquare(base, harvester) > harvesterAndNearbyResourceRange) {
                        harvesters.remove(harvester);
                    }
                }
            }
            if (bases.isEmpty()) {
                harvesters.remove(harvester);
            }
        }

        // remove units from builder list if their task is complete or they died
        for (Unit builder : builders) {
            // builder died
            if ( ! workers.contains(builder)) {
                builders.remove(builder);
            }
            // builder finished building
            else if (builder.isIdle(gs)) {
                freeWorkers.add(builder);
                builders.remove(builder);
            }
        }

        // assign units eligible to be harvesters
        if ( ! freeWorkers.isEmpty() &&  ! bases.isEmpty() &&  ! nearbyResources.isEmpty()) {
            Unit base = bases.getFirst();
            // nearby resources is a global variable calculated previously
            // checks if more dedicated harvesters are needed
            if (harvesters.size() < nearbyResources.size()) {
                // make a priority queue of freeworkers closest to the base that qualify to be a dedicated harvester
                PriorityQueue<UnitAndDistance> potentialHarvesters = new PriorityQueue<>();
                for (Unit worker : freeWorkers) {
                    int distance = calcDistNoSquare(base, worker);
                    if (distance <= harvesterAndNearbyResourceRange) {
                        potentialHarvesters.add(new UnitAndDistance(worker, distance));
                    }
                }
                for (UnitAndDistance harvesterAndDistance : potentialHarvesters) {
                    Unit harvester = harvesterAndDistance.u;
                    if (harvesters.size() == nearbyResources.size()) {
                        break;
                    }
                    harvesters.add(harvester);
                    freeWorkers.remove(harvester);
                }
            }
        }
        // free up harvesters no longer needed
        if (nearbyResources.isEmpty() || bases.isEmpty()) {
            for (Unit harvester : harvesters) {
                if (harvester.getResources() == 0) {
                    freeWorkers.add(harvester);
                    harvesters.remove(harvester);
                }
            }
        }
        else {
            for (Unit harvester : harvesters) {
                if (harvester.isIdle(gs)) {
                    Unit closestBase = null;
                    Unit closestResource = null;
                    int closestDistance = 0;
                    for (Unit resource : nearbyResources) {
                        int distance = calcDistNoSquare(harvester, resource);
                        if (closestResource == null || distance < closestDistance) {
                            closestResource = resource;
                            closestDistance = distance;
                        }
                    }
                    closestDistance = 0;
                    for (Unit base : bases) {
                        int distance = calcDistNoSquare(harvester, base);
                        if (closestBase == null || distance < closestDistance) {
                            closestBase = base;
                            closestDistance = distance;
                        }
                    }
                    AbstractAction aa = getAbstractAction(harvester);
                    if (aa instanceof Harvest h_aa) {
                        if (h_aa.getTarget() != closestResource || h_aa.getBase() != closestBase) {
                            harvest(harvester, closestResource, closestBase);
                        }
                    }
                    else {
                        harvest(harvester, closestResource, closestBase);
                    }
                }
            }
        }

        // builder behavior
        // if no barracks exists and resources exist to build a barracks and at least almost enough for an army unit is left over, a free worker will be made into a worker and build it as long as additional workers already exist to protect it or the enemy only has one unit that can attack
        if ( ! bases.isEmpty() && barracks.isEmpty() && (p.getResources() >= barracksType.cost + (lightType.cost - 2) + resourcesUsed || p.getResources() >= barracksType.cost + (heavyType.cost - 2) + resourcesUsed || p.getResources() >= barracksType.cost + (rangedType.cost - 2) + resourcesUsed)) {
            if ((workers.size() - harvesters.size() >= 3 || combinedEnemyUnits.size() == 2) &&  ! freeWorkers.isEmpty() && builders.isEmpty()) {
                List<Integer> reservedPositions = new ArrayList<>();
                // generate a priority queue of free workers with priority to the one closest to the first friendly base
                PriorityQueue<UnitAndDistance> freeWorkersByDistance = new PriorityQueue<>();
                Unit base = bases.getFirst();
                for (Unit worker : freeWorkers) {
                    freeWorkersByDistance.add(new UnitAndDistance(worker, calcDistNoSquare(base, worker)));
                }
                UnitAndDistance builderAndDistance = freeWorkersByDistance.peek();
                Unit builder = builderAndDistance.u;
                freeWorkers.remove(builder);

                // obtain location of future barracks
                int baseX = base.getX();
                int baseY = base.getY();
                Side baseSideOfMap;
                if (baseX <= pgs.getWidth() / 2) {
                    baseSideOfMap = Side.LEFT;
                }
                else {
                    baseSideOfMap = Side.RIGHT;
                }

                // build future barracks
                switch (baseSideOfMap) {
                    case LEFT -> buildIfNotAlreadyBuilding(builder, barracksType, baseX + 4, baseY + 1, reservedPositions, p, pgs);
                    case RIGHT -> buildIfNotAlreadyBuilding(builder, barracksType, baseX - 2, baseY + 1, reservedPositions, p, pgs);
                }
            }
        }

        // send remaining units to attack
        for (Unit worker : freeWorkers) {
            if (worker.isIdle(gs)) {
                PriorityQueue<UnitAndDistance> enemiesByDistance = new PriorityQueue<>();
                for (Unit enemy : combinedEnemyUnits) {
                    enemiesByDistance.add(new UnitAndDistance(enemy, calcDistNoSquare(worker, enemy)));
                }
                if ( ! enemiesByDistance.isEmpty()) {
                    attack(worker, enemiesByDistance.peek().u);
                }
            }
        }
    }

    /**
     * attacks nearest enemy
     * Function will get called for each light unit
     * 
     * @param friendlyLight - this is the friendly light unit passed
     * @param enemyUnits - list of all the enemy units currently alive
     * @param p - the player
     * @param gs - the game state
     */
    public
            void lightUnitBehavior(Unit friendlyLight, List<Unit> enemyUnits, Player p, GameState gs) {
                if (friendlyLight.isIdle(gs))
                {
                PhysicalGameState pgs = gs.getPhysicalGameState();
                if (enemyUnits.isEmpty())
                {
                   return;
                }
                
                PriorityQueue<UnitAndDistance> enemiesByDistance = new PriorityQueue<>();
                for (Unit enemy : enemyUnits)
                {
                    int distance = calcDistNoSquare(friendlyLight, enemy);
                    UnitAndDistance enemyWithDistance = new UnitAndDistance(enemy, distance);
                    
                    enemiesByDistance.add(enemyWithDistance);
                }
                
                attack(friendlyLight, enemiesByDistance.peek().u);
                }

    }

    /**
     * attacks nearest enemy
     * Function will get called for each heavy unit
     *
     * @param friendlyHeavy - this is the friendly heavy unit
     * @param enemyUnits - this is the list of all the enemy units currently alive
     * @param p - the player
     * @param gs - the game state
     */
    public
            void heavyUnitBehavior(Unit friendlyHeavy, List<Unit> enemyUnits, Player p, GameState gs) {
                if (friendlyHeavy.isIdle(gs))
                {
                PhysicalGameState pgs = gs.getPhysicalGameState();
                if (enemyUnits.isEmpty())
                {
                   return;
                }
                PriorityQueue<UnitAndDistance> enemiesByDistance = new PriorityQueue<>();
                for (Unit enemy : enemyUnits)
                {
                    int distance = calcDistNoSquare(friendlyHeavy, enemy);
                    UnitAndDistance enemyWithDistance = new UnitAndDistance(enemy, distance);

                    enemiesByDistance.add(enemyWithDistance);

                }
                attack(friendlyHeavy, enemiesByDistance.peek().u);
                }

    }

    /**
     * attacks nearest enemy
     * Function will get called for each ranged unit
     * 
     * @param friendlyRanged - this is the friendly ranged unit
     * @param enemyUnits - this is the list of all the enemy units currently alive
     * @param p - the player
     * @param gs - the game state
     */
    public
            void rangedUnitBehavior(Unit friendlyRanged, List<Unit> enemyUnits, Player p, GameState gs) {
            if (friendlyRanged.isIdle(gs))
                {
                PhysicalGameState pgs = gs.getPhysicalGameState();
                if (enemyUnits.isEmpty())
                {
                   return;
                }
                PriorityQueue<UnitAndDistance> enemiesByDistance = new PriorityQueue<>();
                for (Unit enemy : enemyUnits)
                {
                    int distance = calcDistNoSquare(friendlyRanged, enemy);
                    UnitAndDistance enemyWithDistance = new UnitAndDistance(enemy, distance);

                    enemiesByDistance.add(enemyWithDistance);

                }
                attack(friendlyRanged, enemiesByDistance.peek().u);
                }
                
    }

    /**
     * overload for the StarPathFinding path finding
     * @return
     */
    @Override
    public
            List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();

        parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }
}