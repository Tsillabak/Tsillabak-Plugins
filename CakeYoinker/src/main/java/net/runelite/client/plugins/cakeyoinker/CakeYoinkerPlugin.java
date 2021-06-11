package net.runelite.client.plugins.cakeyoinker;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
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
import java.time.Instant;
import java.util.Set;

import static net.runelite.client.plugins.cakeyoinker.CakeYoinkerState.*;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "CakeYoinker",
	enabledByDefault = false,
	description = "Yoinks cakes and shit",
	tags = {"cakes, shit, yoink, Tsillabak"}
)
public class CakeYoinkerPlugin extends Plugin {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(CakeYoinkerPlugin.class);
	@Inject
	public Client client;

	@Inject
	public CakeYoinkerConfiguration config;

	@Inject
	public	 iUtils utils;

	@Inject
	public MouseUtils mouse;

	@Inject
	public PlayerUtils playerUtils;

	@Inject
	public InventoryUtils inventory;

	@Inject
	public InterfaceUtils interfaceUtils;

	@Inject
	public CalculationUtils calc;

	@Inject
	public MenuUtils menu;

	@Inject
	public ObjectUtils object;

	@Inject
	public BankUtils bank;

	@Inject
	 public  NPCUtils npc;

	@Inject
	public KeyboardUtils key;

	@Inject
	public WalkUtils walk;

	@Inject
	public ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	public CakeYoinkerOverlay overlay;


	CakeYoinkerState state;
	GameObject targetObject;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;

	public final WorldPoint CAKEPOINT = new WorldPoint(2669, 3310, 0);
	public final WorldArea BANKAREA = new WorldArea(new WorldPoint(2659, 3288,0),
													new WorldPoint(  2649, 3278,0));
	public static Set<Integer> CAKE = Set.of(ItemID.CAKE);

	int timeout = 0;
	long sleepLength;
	boolean startCakeYoinker;

	public CakeYoinkerPlugin() {
	}

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
	}

	@Subscribe
	private
	void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("cakeyoinker")) {
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
		if (!event.getGroup().equals("CakeYoinker")) {
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
		sleepLength = calc.randomDelay(config.sleepWeightedDistribution(),
										config.sleepMin(),
										config.sleepMax(),
										config.sleepDeviation(),
										config.sleepTarget());
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
	void yoinkCake() {
		targetObject = object.findNearestGameObjectWithin(
				new WorldPoint(2668, 3310, 0), 1, 11730);
		if (targetObject != null&&player.getWorldLocation().equals(
				new WorldPoint(2669, 3310, 0))) {
			targetMenu = new MenuEntry("Steal-from",
										"Baker's Stall",
										targetObject.getId(),
										MenuAction.GAME_OBJECT_SECOND_OPTION.getId(),
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
			targetMenu = new MenuEntry("", "",
					bankTarget.getId(),
					bank.getBankMenuOpcode(bankTarget.getId()),
					bankTarget.getSceneMinLocation().getX(),
					bankTarget.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(bankTarget.getConvexHull()
											.getBounds(),
											sleepDelay());
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
		if (!inventory.isFull() && player.getWorldLocation().equals(
								 CAKEPOINT)) {
			return YOINK_CAKES;
		}
		if (inventory.isFull() 	&& !bank.isOpen()
								&& player.getWorldArea()
								!= BANKAREA) {
			return WALK_TO_BANK;
		}
		if (inventory.isFull() 	&& player.getWorldArea()
								== BANKAREA){
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
				case WALK_TO_BANK:
					walk.webWalk(new WorldPoint(2655,3286,0),2,false,
					calc.getRandomIntBetweenRange(100,650));
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



















