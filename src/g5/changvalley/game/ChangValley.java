package g5.changvalley.game;

import g5.changvalley.Registrar;
import g5.changvalley.Window;
import g5.changvalley.engine.Engine;
import g5.changvalley.engine.GameObject;
import g5.changvalley.game.objects.Player;
import g5.changvalley.game.objects.Rabbit;
import g5.changvalley.game.objects.Terrain;
import g5.changvalley.render.Camera;
import g5.server.PlayerState;
import org.lwjgl.Version;

import static org.lwjgl.glfw.GLFW.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChangValley implements Engine {
    private final ArrayList<GameObject> gameObjects = new ArrayList<>();
    private final Map<UUID, Player> remotePlayers = new HashMap<>();
    // prevent duplicates from being added from cross thread addition
    // so i made it a hashmap instead of a list
    private final Map<UUID, Runnable> renderQueue = new HashMap<>();
    private int panDirection;
    private Player player;
    private Terrain terrain;

    public static void main(String[] args) {
        System.out.println("Chang Valley: Euro Kim, Evan Chang, and Sammy Hajhamid");
        System.out.println("LWJGL version: " + Version.getVersion());
        try {
            NetworkClient.construct();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
        System.out.println("Welcome, " + NetworkClient.getUuid() + "!");
        Registrar.registerGame(new ChangValley());
    }

    public void takeInput() {
        panDirection = Window.pressValue(GLFW_KEY_LEFT) - Window.pressValue(GLFW_KEY_RIGHT);
        player.updateDirectionalVector();
    }

    public void updateState() {
        Camera.orientation.rotateY(panDirection / 32f);
        player.updateState();
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.updateState();
        }
        terrain.update();
    }

    public ArrayList<GameObject> render() {
        // per render calls
        terrain.regenerate();
        for (Map.Entry<UUID, Runnable> runnable : renderQueue.entrySet()) {
            runnable.getValue().run();
            renderQueue.remove(runnable.getKey());
        }

        // this is horrible, horrible, horrible, but ok idc lol
        // we should actually provide a hashmap for gameobjects with specific uids for each container, or really
        // just an array of containers
        // the project is due in like 2 days tho so i dont really want to go back and restructure everything LOL
        ArrayList<GameObject> renderThisFrame = new ArrayList<>(gameObjects);
        player.addTo(renderThisFrame);
        for (Player remotePlayer : remotePlayers.values()) {
            remotePlayer.addTo(renderThisFrame);
        }
        return renderThisFrame;
    }

    public void construct() {
        Camera.position.add(-1, -1, -1);
        Camera.orientation.rotateX(0.2f);

        player = new Player();
        player.forEach((g) -> {
            g.position.set(0, 0, 3f);
            g.orientation.rotateY((float) (Math.PI));
        });
//        player.addTo(gameObjects);

        terrain = Terrain.makeTerrain();
        terrain.position.set(-Terrain.TERRAIN_SIZE / 2f, 3f, Terrain.TERRAIN_SIZE / 2f);
        gameObjects.add(terrain);

        new Rabbit().addTo(gameObjects);

        // prob should use some sort of fixed step updating but like
        // no one cares
        (new Thread(() -> {
            while (Registrar.running) {
                NetworkClient.update(player);
                Thread.onSpinWait();
            }
        })).start();

        (new Thread(() -> {
            while (Registrar.running) {
                updateRemotePlayers();
                Thread.onSpinWait();
            }
        })).start();
    }

    public void updateRemotePlayers() {
        try {
            Map<UUID, PlayerState> newRemotePlayers = NetworkClient.getRemotePlayers();
            for (Map.Entry<UUID, PlayerState> pState : newRemotePlayers.entrySet()) {
                UUID remoteUuid = pState.getKey();
                // dont make a player for ourselves
                if (remoteUuid.equals(NetworkClient.getUuid())) {
                    continue;
                }
                if (!remotePlayers.containsKey(remoteUuid)) {
                    renderQueue.put(remoteUuid, () -> {
                        Player player = new Player();
                        remotePlayers.put(remoteUuid, player);
//                        player.addTo(gameObjects);
                    });
                } else {
                    Player player = remotePlayers.get(remoteUuid);
                    PlayerState state = pState.getValue();
                    player.setPosition(state.getPosition());
                    player.setOrientation(state.getOrientation());
                }
            }
            // bad but no one cares
            for (UUID player: remotePlayers.keySet()) {
                if (!newRemotePlayers.containsKey(player)) {
                    remotePlayers.remove(player);
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
