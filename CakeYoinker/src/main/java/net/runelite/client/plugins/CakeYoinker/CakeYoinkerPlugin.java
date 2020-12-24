package net.runelite.client.plugins.CakeYoinker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.*;
import net.runelite.api.TileItem;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.MenuEntry;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.iutils.BankUtils;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InterfaceUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.MenuUtils;
import net.runelite.client.plugins.iutils.MouseUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.WalkUtils;
import net.runelite.client.plugins.iutils.KeyboardUtils;
import net.runelite.client.plugins.iutils.iUtils;



import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.CakeYoinker.CakeYoinkerState.*;
import static net.runelite.client.plugins.iutils.iUtils.iterating;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "CakeYoinker",
	enabledByDefault = false,
	description = "Yoinks cakes and shit",
	tags = {"cakes, shit, yoink, Tsillabak"},
	type = PluginType.SKILLING
)
@Slf4j
public class CakeYoinkerPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private CakeYoinkerConfiguration config;

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
	private CakeYoinkerOverlay overlay;


	CakeYoinkerState state;
	GameObject targetObject;
	TileItem groundItem;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	public final WorldPoint DOOR_POINT = new WorldPoint(2668, 3310, 0);
	public final WorldPoint CAKEPOINT = new WorldPoint(2668, 3310, 0);
	public static Set<Integer> CAKE = Set.of(ItemID.CAKE);
	public static Set<Integer> DROP = Set.of(ItemID.CHOCOLATE_SLICE, ItemID.BREAD);

	int timeout = 0;
	long sleepLength;
	boolean startCakeYoinker;
	private final Set<Integer> requiredIds = new HashSet<>();
	Rectangle clickBounds;

	@Provides
	CakeYoinkerConfiguration provideConfig(ConfigManager configManager) {
		return configManager.getConfig(CakeYoinkerConfiguration.class);
	}

	private
	void resetVals() {
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startCakeYoinker = false;
		requiredIds.clear();
	}

	@Subscribe
	private
	void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("CakeYoinker")) {
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton")) {
			if (!startCakeYoinker) {
				startCakeYoinker = true;
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
		startCakeYoinker = false;
	}

	@Subscribe
	private
	void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("plankmaker")) {
			return;
		}
		startCakeYoinker = false;
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
		int tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private
	void yoinkCake() {


		targetObject = object.findNearestGameObjectWithin(CAKEPOINT, 1, 11730);

		if (targetObject != null&&player.getWorldLocation().equals(new WorldPoint(2669, 3310, 0))) {
			targetMenu = new MenuEntry("Steal-from", "Baker's Stall", targetObject.getId(), MenuOpcode.GAME_OBJECT_SECOND_OPTION.getId(),
					targetObject.getSceneMinLocation().getX(),
					targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());

		} else if (targetObject== null){
			inventory.dropAllExcept(CAKE, true, 1, 1);

		}
	}


	private
	void openBank() {
		GameObject bankTarget = object.findNearestGameObject(10355);
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
	CakeYoinkerState getState() {

		if (timeout > 0) {
			playerUtils.handleRun(20, 30);
			return TIMEOUT;

		}
		if (playerUtils.isMoving(beforeLoc)) {
			playerUtils.handleRun(20, 30);
			return MOVING;
		}
		if (inventory.isEmpty() && bank.isOpen()) {
			return WALK_TO_STALL;
		}
		if (!inventory.isFull()&&(player.getWorldLocation().equals(new WorldPoint(2669, 3310, 0)))) {
			return YOINK_CAKES;
		}
		if (inventory.isFull() && !bank.isOpen()) {
			return FIND_BANK;
		}
		if (inventory.isFull() && bank.isOpen()) {
			return DEPOSIT_ITEMS;

	}
		return IDLE;
	}


	@Subscribe
	private
	void onGameTick(GameTick tick) {
		if (!startCakeYoinker) {
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null) {
			if (!client.isResized()) {
				utils.sendGameMessage("Client must be set to resizable");
				startCakeYoinker = false;
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
					walk.sceneWalk(new WorldPoint(2668, 3310, 0),0,0);
					timeout = tickDelay();
					break;
				case YOINK_CAKES:
					yoinkCake();
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



















