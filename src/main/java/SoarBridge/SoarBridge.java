/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package SoarBridge;

import Simulation.Environment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import ws3dproxy.CommandExecException;
import ws3dproxy.CommandUtility;
import ws3dproxy.model.Creature;
import ws3dproxy.model.Thing;
import ws3dproxy.util.Constants;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Danilo Lucentini and Ricardo Gudwin
 */
public class SoarBridge {

    // Log Variable
    Logger logger = Logger.getLogger(SoarBridge.class.getName());

    // SOAR Variables
    Agent agent = null;
    public Identifier inputLink = null;

    // Entity Variables
    Identifier creature;
    Identifier creatureSensor;
    Identifier creatureParameters;
    Identifier creaturePosition;
    Identifier creatureMemory;
    Identifier creatureLeaflets;
    int indices;
    List<Thing> memoria = new ArrayList<>();

    Environment env;
    public Creature c;
    public String input_link_string = "";
    public String output_link_string = "";

    /**
     * Constructor class
     *
     * @param _e Environment
     * @param path Path for Rule Base
     * @param startSOARDebugger set true if you wish the SOAR Debugger to be
     * started
     */
    public SoarBridge(Environment _e, String path, Boolean startSOARDebugger) {
        env = _e;
        c = env.getCreature();
        try {
            ThreadedAgent tag = ThreadedAgent.create();
            agent = tag.getAgent();
            SoarCommands.source(agent.getInterpreter(), path);
            inputLink = agent.getInputOutput().getInputLink();

            // Initialize entities
            creature = null;

            // Debugger line
            if (startSOARDebugger) {
                agent.openDebugger();
            }
        } catch (Exception e) {
            logger.severe("Error while creating SOAR Kernel");
            e.printStackTrace();
        }
        indices = 0;
    }

    private Identifier CreateIdWME(Identifier id, String s) {
        SymbolFactory sf = agent.getSymbols();
        Identifier newID = sf.createIdentifier('I');
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
        return (newID);
    }

    private void CreateFloatWME(Identifier id, String s, double value) {
        SymbolFactory sf = agent.getSymbols();
        DoubleSymbol newID = sf.createDouble(value);
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
    }

    private void CreateStringWME(Identifier id, String s, String value) {
        SymbolFactory sf = agent.getSymbols();
        StringSymbol newID = sf.createString(value);
        agent.getInputOutput().addInputWme(id, sf.createString(s), newID);
    }

    private String getItemType(int categoryType) {
        String itemType = null;

        switch (categoryType) {
            case Constants.categoryBRICK:
                itemType = "BRICK";
                break;
            case Constants.categoryJEWEL:
                itemType = "JEWEL";
                break;
            case Constants.categoryFOOD:
            case Constants.categoryNPFOOD:
            case Constants.categoryPFOOD:
                itemType = "FOOD";
                break;
            case Constants.categoryCREATURE:
                itemType = "CREATURE";
                break;
            case Constants.categoryDeliverySPOT:
                itemType = "DELIVERY";
                break;
        }
        return itemType;
    }

    /**
     * Create the WMEs at the InputLink of SOAR
     */
    private void prepareInputLink() {
        //SymbolFactory sf = agent.getSymbols();
        Creature c = env.getCreature();
        inputLink = agent.getInputOutput().getInputLink();
        try {
            if (agent != null) {
                //SimulationCreature creatureParameter = (SimulationCreature)parameter;
                // Initialize Creature Entity
                creature = CreateIdWME(inputLink, "CREATURE");

                //Initialize Creature Leaflets
                creatureLeaflets = CreateIdWME(creature, "LEAFLETS");
                Identifier creatureLeafletsLeafletMissing = CreateIdWME(creatureLeaflets, "JEWELS");
                HashMap<String, Integer> accumulatedResults = new HashMap<>();
                int i = indices;

                Identifier creatureLeafletsLeaflet = CreateIdWME(creatureLeaflets, "LEAFLET");
                CreateFloatWME(creatureLeafletsLeaflet, "ID", c.getLeaflets().get(i).getID());
                CreateFloatWME(creatureLeafletsLeaflet, "PAYMENT", c.getLeaflets().get(i).getPayment());
                CreateFloatWME(creatureLeafletsLeaflet, "SITUATION", c.getLeaflets().get(i).getSituation());
                // Identifier creatureLeafletsLeafletJewels = CreateIdWME(creatureLeafletsLeaflet,"JEWELS");

                HashMap<String, Integer> result = c.getLeaflets().get(i).getWhatToCollect();

                for (Map.Entry<String, Integer> entry : result.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    accumulatedResults.put(key, accumulatedResults.getOrDefault(key, 0) + value);
                }


                for (Map.Entry<String, Integer> entry : accumulatedResults.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    CreateFloatWME(creatureLeafletsLeafletMissing, key, value);
                    // System.out.println("Type: " + key + ", Missing Number: " + value);
                }

                // Initialize Creature Memory
                creatureMemory = CreateIdWME(creature, "MEMORY");
                // Set Creature Parameters
                Calendar lCDateTime = Calendar.getInstance();
                creatureParameters = CreateIdWME(creature, "PARAMETERS");
                CreateFloatWME(creatureParameters, "MINFUEL", 400);
                CreateFloatWME(creatureParameters, "TIMESTAMP", lCDateTime.getTimeInMillis());
                // Setting creature Position
                creaturePosition = CreateIdWME(creature, "POSITION");
                CreateFloatWME(creaturePosition, "X", c.getPosition().getX());
                CreateFloatWME(creaturePosition, "Y", c.getPosition().getY());
                // Set creature sensors
                creatureSensor = CreateIdWME(creature, "SENSOR");
                // Create Fuel Sensors
                Identifier fuel = CreateIdWME(creatureSensor, "FUEL");
                CreateFloatWME(fuel, "VALUE", c.getFuel());
                // Create Visual Sensors
                Identifier visual = CreateIdWME(creatureSensor, "VISUAL");
                List<Thing> thingsList = (List<Thing>) c.getThingsInVision();

                for (Thing coisaVisão : thingsList){
                    boolean encontrado = false;
                    for (Thing coisaMemoria : memoria) {
                        if (coisaVisão.getName().equals(coisaMemoria.getName())) {
                            encontrado = true;
                            break;
                        }
                    }
                    if (!encontrado) {
                        memoria.add(coisaVisão);
                    }
                }

                for (Thing t : memoria) {
                    Identifier entity = CreateIdWME(visual, "ENTITY");
                    CreateFloatWME(entity, "DISTANCE", GetGeometricDistanceToCreature(t.getX1(), t.getY1(), t.getX2(), t.getY2(), c.getPosition().getX(), c.getPosition().getY()));
                    CreateFloatWME(entity, "X", t.getX1());
                    CreateFloatWME(entity, "Y", t.getY1());
                    CreateFloatWME(entity, "X2", t.getX2());
                    CreateFloatWME(entity, "Y2", t.getY2());
                    CreateStringWME(entity, "TYPE", getItemType(t.getCategory()));
                    CreateStringWME(entity, "NAME", t.getName());
                    CreateStringWME(entity, "COLOR", Constants.getColorName(t.getMaterial().getColor()));
                }
            }
        } catch (Exception e) {
            logger.severe("Error while Preparing Input Link");
            e.printStackTrace();
        }
    }

    private double GetGeometricDistanceToCreature(double x1, double y1, double x2, double y2, double xCreature, double yCreature) {
        float squared_dist = 0.0f;
        double maxX = Math.max(x1, x2);
        double minX = Math.min(x1, x2);
        double maxY = Math.max(y1, y2);
        double minY = Math.min(y1, y2);

        if (xCreature > maxX) {
            squared_dist += (xCreature - maxX) * (xCreature - maxX);
        } else if (xCreature < minX) {
            squared_dist += (minX - xCreature) * (minX - xCreature);
        }

        if (yCreature > maxY) {
            squared_dist += (yCreature - maxY) * (yCreature - maxY);
        } else if (yCreature < minY) {
            squared_dist += (minY - yCreature) * (minY - yCreature);
        }

        return Math.sqrt(squared_dist);
    }

    private void resetSimulation() {
        agent.initialize();
    }

    /**
     * Run SOAR until HALT
     */
    private void runSOAR() {
        agent.runForever();
    }

    private int stepSOAR() {
        agent.runFor(1, RunType.PHASES);
        Phase ph = agent.getCurrentPhase();
        if (ph.equals(Phase.INPUT)) {
            return (0);
        } else if (ph.equals(Phase.PROPOSE)) {
            return (1);
        } else if (ph.equals(Phase.DECISION)) {
            return (2);
        } else if (ph.equals(Phase.APPLY)) {
            return (3);
        } else if (ph.equals(Phase.OUTPUT)) {
            if (agent.getReasonForStop() == null) {
                return (4);
            } else {
                return (5);
            }
        } else {
            return (6);
        }
    }

    private String GetParameterValue(String par) {
        List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());
        List<Wme> Parameters = Wmes.matcher(agent).filter(Commands.get(0));
        String parvalue = "";
        for (Wme w : Parameters) {
            if (w.getAttribute().toString().equals(par)) {
                parvalue = w.getValue().toString();
            }
        }
        return (parvalue);
    }

    /**
     * Process the OutputLink given by SOAR and return a list of commands to
     * WS3D
     *
     * @return A List of SOAR Commands
     */
    private ArrayList<Command> processOutputLink() {
        ArrayList<Command> commandList = new ArrayList<Command>();
        // System.out.println(commandList);

        try {
            if (agent != null) {
                List<Wme> Commands = Wmes.matcher(agent).filter(agent.getInputOutput().getOutputLink());
                // printOutputWMEs();
                for (Wme com : Commands) {
                    String name = com.getAttribute().asString().getValue();
                    System.out.println(name);
                    Command.CommandType commandType = Enum.valueOf(Command.CommandType.class, name);
                    Command command = null;

                    switch (commandType) {
                        case MOVE:
                            Float rightVelocity = null;
                            Float leftVelocity = null;
                            Float linearVelocity = null;
                            Float xPosition = null;
                            Float yPosition = null;
                            rightVelocity = tryParseFloat(GetParameterValue("VelR"));
                            if (rightVelocity == null) {
                                rightVelocity = tryParseFloat("2");
                            }

                            leftVelocity = tryParseFloat(GetParameterValue("VelL"));
                            if (leftVelocity == null) {
                                leftVelocity = tryParseFloat("0");
                            }

                            linearVelocity = tryParseFloat(GetParameterValue("Vel"));
                            if (linearVelocity == null) {
                                linearVelocity = tryParseFloat("0");
                            }

                            xPosition = tryParseFloat(GetParameterValue("X"));
                            yPosition = tryParseFloat(GetParameterValue("Y"));
                            command = new Command(Command.CommandType.MOVE);
                            CommandMove commandMove = (CommandMove) command.getCommandArgument();
                            if (commandMove != null) {
                                if (rightVelocity != null) {
                                    commandMove.setRightVelocity(rightVelocity);
                                }
                                if (leftVelocity != null) {
                                    commandMove.setLeftVelocity(leftVelocity);
                                }
                                if (linearVelocity != null) {
                                    commandMove.setLinearVelocity(linearVelocity);
                                }
                                if (xPosition != null) {
                                    commandMove.setX(xPosition);
                                }
                                if (yPosition != null) {
                                    commandMove.setY(yPosition);
                                }
                                commandList.add(command);
                            } else {
                                logger.severe("Error processing MOVE command");
                            }
                            break;

                        case GET:
                            String thingNameToGet = null;
                            command = new Command(Command.CommandType.GET);
                            CommandGet commandGet = (CommandGet) command.getCommandArgument();
                            if (commandGet != null) {
                                thingNameToGet = GetParameterValue("Name");
                                if (thingNameToGet != null) {
                                    commandGet.setThingName(thingNameToGet);
                                }
                                commandList.add(command);
                            }
                            break;

                        case EAT:
                            String thingNameToEat = null;
                            command = new Command(Command.CommandType.EAT);
                            CommandEat commandEat = (CommandEat) command.getCommandArgument();
                            if (commandEat != null) {
                                thingNameToEat = GetParameterValue("Name");
                                if (thingNameToEat != null) {
                                    commandEat.setThingName(thingNameToEat);
                                }
                                commandList.add(command);
                            }
                            break;

                        case DELIVERY:
                            System.out.println("teste123teste123teste123teste123teste123");
                            String thingNameToDeliver = null;
                            command = new Command(Command.CommandType.DELIVERY);
                            CommandDelivery commandDelivery = (CommandDelivery) command.getCommandArgument();
                            if (commandDelivery != null) {
                                thingNameToDeliver = GetParameterValue("Name");
                                System.out.println(thingNameToDeliver);
                                if (thingNameToDeliver != null) {
                                    commandDelivery.setThingName(thingNameToDeliver);
                                }
                                commandList.add(command);
                            }
                            break;

                        default:
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error while processing commands");
            e.printStackTrace();
        }

        return ((commandList.size() > 0) ? commandList : null);
    }

    /**
     * Perform a complete SOAR step
     *
     * @throws ws3dproxy.CommandExecException
     */
    public void step() throws CommandExecException {
        if (phase != -1) {
            finish_msteps();
        }
        resetSimulation();
        c.updateState();
        prepareInputLink();
        input_link_string = stringInputLink();
        //printInputWMEs();
        runSOAR();
        output_link_string = stringOutputLink();
        printOutputWMEs();
        List<Command> commandList = processOutputLink();
        processCommands(commandList);
        //resetSimulation();
    }

    public void prepare_mstep() {
        resetSimulation();
        c.updateState();
        prepareInputLink();
        input_link_string = stringInputLink();
    }

    public int phase = -1;

    public void mstep() throws CommandExecException {
        if (phase == -1) {
            prepare_mstep();
        }
        phase = stepSOAR();
        if (phase == 5) {
            post_mstep();
            phase = -1;
        }
    }

    public void finish_msteps() throws CommandExecException {
        while (phase != -1) {
            mstep();
        }
    }

    public void post_mstep() throws CommandExecException {
        output_link_string = stringOutputLink();
        // printOutputWMEs();
        List<Command> commandList = processOutputLink();
        processCommands(commandList);
        //resetSimulation();
    }

    private void processCommands(List<Command> commandList) throws CommandExecException {

        if (commandList != null) {
            for (Command command : commandList) {
                switch (command.getCommandType()) {
                    case GET:
                        processGetCommand((CommandGet) command.getCommandArgument());
                        break;

                    case MOVE:
                        processMoveCommand((CommandMove) command.getCommandArgument());
                        break;

                    case EAT:
                        processEatCommand((CommandEat) command.getCommandArgument());
                        break;

                    case DELIVERY:
                        processDeliveryCommand((CommandDelivery) command.getCommandArgument());
                        break;

                    default:
                        System.out.println("Nenhum comando definido ...");
                        // Do nothing
                        break;
                }
            }
        } else {
            System.out.println("comando nulo ...");
        }
    }

    /**
     * Send Move Command to World Server
     *
     * @param soarCommandMove Soar Move Command Structure
     */
    private void processMoveCommand(CommandMove soarCommandMove) throws CommandExecException {
        if (soarCommandMove != null) {
            if (soarCommandMove.getX() != null && soarCommandMove.getY() != null) {
                CommandUtility.sendGoTo("0", soarCommandMove.getRightVelocity(), soarCommandMove.getLeftVelocity(), soarCommandMove.getX(), soarCommandMove.getY());
            } else {
                CommandUtility.sendSetTurn("0", soarCommandMove.getLinearVelocity(), soarCommandMove.getRightVelocity(), soarCommandMove.getLeftVelocity());
            }
        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * Send Get Command to World Server
     *
     * @param soarCommandDelivery Soar Get Command Structure
     */
    private void processDeliveryCommand(CommandDelivery soarCommandDelivery) throws CommandExecException {
        if (soarCommandDelivery != null) {
            try {
                c.deliverLeaflet(soarCommandDelivery.getThingName().replaceAll("\\.0$", ""));
            } catch (Exception e) {
                System.out.println(e);
            }

            int i = indices;
            System.out.println(Long.toString(c.getLeaflets().get(i).getID()).equals(soarCommandDelivery.getThingName().replaceAll("\\.0$", "")));
            if (Long.toString(c.getLeaflets().get(i).getID()).equals(soarCommandDelivery.getThingName().replaceAll("\\.0$", ""))) {
                indices++;
                if(indices>2) System.out.println("Isso é tudo pessoal");
            }
        } else {
            logger.severe("Error processing processDeliveryCommand");
        }
    }

    /**
     * Send Get Command to World Server
     *
     * @param soarCommandGet Soar Get Command Structure
     */
    private void processGetCommand(CommandGet soarCommandGet) throws CommandExecException {
        if (soarCommandGet != null) {
            try {
                Iterator<Thing> iterator = memoria.iterator();
                while (iterator.hasNext()) {
                    Thing coisaMemoria = iterator.next();
                    if (coisaMemoria.getName().equals(soarCommandGet.getThingName())) {
                        iterator.remove();
                        break;
                    }
                }
                System.out.println("getok1");
                c.putInSack(soarCommandGet.getThingName());
                System.out.println("getok2");
            } catch (Exception e) {
                System.out.println("get not ok");
            }
        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * Send Eat Command to World Server
     *
     * @param soarCommandEat Soar Eat Command Structure
     */
    private void processEatCommand(CommandEat soarCommandEat) throws CommandExecException {
        if (soarCommandEat != null) {

            Iterator<Thing> iterator = memoria.iterator();
            while (iterator.hasNext()) {
                Thing coisaMemoria = iterator.next();
                if (coisaMemoria.getName().equals(soarCommandEat.getThingName())) {
                    iterator.remove();
                    break;
                }
            }

            c.eatIt(soarCommandEat.getThingName());
        } else {
            logger.severe("Error processing processMoveCommand");
        }
    }

    /**
     * Try Parse a Float Element
     *
     * @param value Float Value
     * @return The Float Value or null otherwise
     */
    private Float tryParseFloat(String value) {
        Float returnValue = null;

        try {
            returnValue = Float.parseFloat(value);
        } catch (Exception ex) {
            returnValue = null;
        }

        return returnValue;
    }

    public void printWME(Identifier id) {
        printWME(id, 0);

    }

    public void printWME(Identifier id, int level) {
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i = 0; i < level; i++) {
                System.out.print("   ");
            }
            if (testv != null) {
                System.out.print("(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n");
                printWME(testv, level + 1);
            } else {
                System.out.print("(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n");
            }
        }
    }

    public void printInputWMEs() {
        Identifier il = agent.getInputOutput().getInputLink();
        System.out.println("Input --->");
        printWME(il);
    }

    public void printOutputWMEs() {
        Identifier ol = agent.getInputOutput().getOutputLink();
        System.out.println("Output --->");
        printWME(ol);
    }

    public String stringWME(Identifier id) {
        String out = stringWME(id, 0);
        return (out);
    }

    public String stringWME(Identifier id, int level) {
        String out = "";
        Iterator<Wme> It = id.getWmes();
        while (It.hasNext()) {
            Wme wme = It.next();
            Identifier idd = wme.getIdentifier();
            Symbol a = wme.getAttribute();
            Symbol v = wme.getValue();
            Identifier testv = v.asIdentifier();
            for (int i = 0; i < level; i++) {
                out += "   ";
            }
            if (testv != null) {
                out += "(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n";
                out += stringWME(testv, level + 1);
            } else {
                out += "(" + idd.toString() + "," + a.toString() + "," + v.toString() + ")\n";
            }
        }
        return (out);
    }

    public String stringInputLink() {
        Identifier il = agent.getInputOutput().getInputLink();
        String out = stringWME(il);
        return (out);
    }

    public String stringOutputLink() {
        Identifier ol = agent.getInputOutput().getOutputLink();
        String out = stringWME(ol);
        return (out);
    }

    public Identifier getInitialState() {
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.toString().equalsIgnoreCase("S1")) {
                return (id);
            }
        }
        return (null);
    }

    public List<Identifier> getStates() {
        List<Identifier> li = new ArrayList<Identifier>();
        Set<Wme> allmem = agent.getAllWmesInRete();
        for (Wme w : allmem) {
            Identifier id = w.getIdentifier();
            if (id.isGoal()) {
                boolean alreadythere = false;
                for (Identifier icand : li) {
                    if (icand == id) {
                        alreadythere = true;
                    }
                }
                if (alreadythere == false) {
                    li.add(id);
                }
            }
        }
        return (li);
    }

    public Set<Wme> getWorkingMemory() {
        return (agent.getAllWmesInRete());
    }
}
