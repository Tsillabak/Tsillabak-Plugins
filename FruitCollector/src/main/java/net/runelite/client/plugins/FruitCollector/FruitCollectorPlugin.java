package net.runelite.client.plugins.FruitCollector;

import com.google.inject.Provides;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.iutils.*;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.FruitCollector.FruitCollectorState.*;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "FruitCollector",
	enabledByDefault = false,
	description = "Collects fruit from hosidious and banks",
	tags = {"fruit, collector, farming"}
)
public class FruitCollectorPlugin extends Plugin {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(FruitCollectorPlugin.class);
	@Inject
	private Client client;

	@Inject
	private FruitCollectorConfiguration config;

	@Inject
	private iUtils utils;

	@Inject
	private MouseUtils mouse;

	@Inject
	private PlayerUtils playerUtils;

	@Inject
	private InventoryUtils inventory;

	@Inject
	private InterfaceUtils interfaceUtils;

	@Inject
	private CalculationUtils calc;

	@Inject
	private MenuUtils menu;

	@Inject
	private ObjectUtils object;

	@Inject
	private BankUtils bank;

	@Inject
	private NPCUtils npc;

	@Inject
	private KeyboardUtils key;

	@Inject
	private WalkUtils walk;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private FruitCollectorOverlay overlay;


	FruitCollectorState state;
	GameObject targetObject;
	TileItem groundItem;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	public final WorldPoint DOOR_POINT = new WorldPoint(1798, 3605, 0);
	public final WorldPoint FRUITPOINT = new WorldPoint(1796, 3607, 0);


	int timeout = 0;
	long sleepLength;
	boolean startFruitCollector;
	private final Set<Integer> requiredIds = new HashSet<>();
	public static Set<Integer> Cookingapple = Set.of(ItemID.COOKING_APPLE);
	public static Set<Integer> Banana = Set.of(ItemID.BANANA);
	public static Set<Integer> JangerBerries = Set.of(ItemID.JANGERBERRIES);
	public static Set<Integer> Lemon = Set.of(ItemID.LEMON);
	public static Set<Integer> RedBerries = Set.of(ItemID.REDBERRIES);
	public static Set<Integer> Pineapple = Set.of(ItemID.PINEAPPLE);
	public static Set<Integer> Lime = Set.of(ItemID.LIME);
	public static Set<Integer> Strawberry = Set.of(ItemID.STRAWBERRY);
	public static Set<Integer> StrangeFruit = Set.of(ItemID.STRANGE_FRUIT);
	public static Set<Integer> PapayaFruit = Set.of(ItemID.PAPAYA_FRUIT);
	public static Set<Integer> gfruit = Set.of(ItemID.GOLOVANOVA_FRUIT_TOP);
	public static final int HOSIDIOUS = 6968;
	public static final int FRUIT_REGION = 7224;
	Rectangle clickBounds;

	public FruitCollectorPlugin() {
	}

	@Provides
	FruitCollectorConfiguration provideConfig(ConfigManager configManager) {
		return configManager.getConfig(FruitCollectorConfiguration.class);
	}

	private
	void resetVals() {
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startFruitCollector = false;
		requiredIds.clear();
	}

	@Subscribe
	private
	void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("FruitCollector")) {
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton")) {
			if (!startFruitCollector) {
				startFruitCollector = true;
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
			} else {
				resetVals();
			}
		}
	}

	@Override
	protected
	void shutDown() {
		// runs on plugin shutdown
		overlayManager.remove(overlay);
		log.info("Plugin stopped");
		startFruitCollector = false;
	}

	@Subscribe
	private
	void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("FruitCollector")) {
			return;
		}
		startFruitCollector = false;
	}

	public
	void setLocation() {
		if (client != null && client.getLocalPlayer() != null && client.getGameState().equals(GameState.LOGGED_IN)) {
			skillLocation = client.getLocalPlayer().getWorldLocation();
			beforeLoc = client.getLocalPlayer().getLocalLocation();
		} else {
			log.debug("Tried start bot before being logged in");
			skillLocation = null;
			resetVals();
		}
	}

	private
	long sleepDelay() {
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private
	int tickDelay() {
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(),
												config.tickDelayMin(),
												config.tickDelayMax(),
												config.tickDelayDeviation(),
												config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}
	private
	void openDoor() {

		WallObject closedDoor = object.findWallObjectWithin(DOOR_POINT, 1, ObjectID.DOOR_7452);
		if (closedDoor != null) {

			targetMenu = new MenuEntry("", "", closedDoor.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(),
					closedDoor.getLocalLocation().getSceneX(), closedDoor.getLocalLocation().getSceneY(), false);
			utils.doActionMsTime(targetMenu, closedDoor.getConvexHull().getBounds(), sleepDelay());
		}

	}
	private void stealFruit()
	{

		targetObject=object.findNearestGameObjectWithin(FRUITPOINT,2,28823);

		if (targetObject != null)
		{
			targetMenu = new MenuEntry("Steal-from", "Fruit Stall", targetObject.getId(), MenuAction.GAME_OBJECT_SECOND_OPTION.getId(),
					 targetObject.getSceneMinLocation().getX(),
					targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		} else if (targetObject== null) {
			if (config.Apple())
				inventory.dropItems(Cookingapple, true, 2, 3);
			if (config.Banana())
				inventory.dropItems(Banana, true, 2, 3);
			if (config.Jangerberries())
				inventory.dropItems(JangerBerries, true, 2, 3);
			if (config.Lemon())
				inventory.dropItems(Lemon, true, 2, 3);
			if (config.Redberries())
				inventory.dropItems(RedBerries, true, 2, 3);
			if (config.Pineapple())
				inventory.dropItems(Pineapple, true, 2, 3);
			if (config.Lime())
				inventory.dropItems(Lime, true, 2, 3);
			if (config.Strawberry())
				inventory.dropItems(Strawberry, true, 2, 3);
			if (config.Strangefruit())
				inventory.dropItems(StrangeFruit, true, 2, 3);
			if (config.Papayafruit())
				inventory.dropItems(PapayaFruit, true, 2, 3);
			if (config.Golovanovafruittop())
				inventory.dropItems(gfruit, true, 2, 3);

		}
	}

	private
	void openBank() {
		GameObject bankTarget = object.findNearestGameObject(25808);
		if (bankTarget != null) {
			targetMenu = new MenuEntry("", "", bankTarget.getId(),
					bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(),
					bankTarget.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
			utils.sendGameMessage("bank clicked");
		}
	}
	public
	int checkRunEnergy() {
		try {
			return Integer.parseInt(client.getWidget(160, 23).getText());
		} catch (Exception ignored) {

		}
		return 0;
	}


	public
	FruitCollectorState getState() {

		if (timeout > 0) {
			playerUtils.handleRun(20, 30);
			return TIMEOUT;

		}
		if (playerUtils.isMoving(beforeLoc)) {
			playerUtils.handleRun(20, 30);
			return MOVING;
		}

		if (
				 inventory.isEmpty()
				&& bank.isOpen()
				&&client.getLocalPlayer().getWorldLocation().getRegionID() == HOSIDIOUS){
			return WALK_TO_STALL;
		}
		if(inventory.isEmpty()&&  object.findWallObjectWithin(DOOR_POINT, 1, ObjectID.DOOR_7452)!=null&& client.getLocalPlayer().getWorldLocation().getRegionID() == FRUIT_REGION){
			return OPEN_DOOR;
		}
		if(inventory.isFull()
				&&  object.findWallObjectWithin(DOOR_POINT, 1, ObjectID.DOOR_7452)!=null
				&& client.getLocalPlayer().getWorldLocation().getRegionID() == FRUIT_REGION){
			return OPEN_DOOR;
		}
		if (!inventory.isFull() &&client.getLocalPlayer().getWorldLocation().getRegionID() == FRUIT_REGION) {
			return STEAL_FRUIT;
		}
		if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() != HOSIDIOUS ) {
			return WALK_TO_BANK;
		}
		if (inventory.isFull() && !bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == HOSIDIOUS) {
			return FIND_BANK;
		}
		if (inventory.isFull() && bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == HOSIDIOUS){
			return DEPOSIT_ITEMS;
		}
		return IDLE;
	}


	@Subscribe
	private
	void onGameTick(GameTick tick) {
		if (!startFruitCollector) {
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null) {
			if (!client.isResized()) {
				utils.sendGameMessage("Client must be set to resizable");
				startFruitCollector = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state) {
				case TIMEOUT:
					playerUtils.handleRun(30, 20);
					timeout--;
					break;
				case WALK_TO_STALL:
					walk.sceneWalk(new WorldPoint(1798, 3598, 0), 3, 0);
					timeout = tickDelay();
					break;
				case WALK_TO_BANK:
					walk.sceneWalk(new WorldPoint(1748, 3598, 0), 0, 0);
					timeout = tickDelay();
					break;
				case STEAL_FRUIT:
					stealFruit();
					timeout = tickDelay();
					break;
				case OPEN_DOOR:
					openDoor();
					timeout = tickDelay();
					break;
				case ANIMATING:
					timeout = 1;
					break;
				case MOVING:
					playerUtils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					openBank();
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					bank.depositAll();
					timeout = tickDelay();
					break;
				case IDLE:
					timeout = 1;
					break;


			}

		}
	}

}



















