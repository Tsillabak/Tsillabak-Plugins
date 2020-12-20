package net.runelite.client.plugins.EssenceMiner;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ConfigButtonClicked;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.api.MenuEntry;
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
import java.awt.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static net.runelite.client.plugins.EssenceMiner.EssenceMinerState.*;
import static net.runelite.client.plugins.iutils.iUtils.iterating;

@Extension
@PluginDependency(iUtils.class)
@PluginDescriptor(
	name = "EssenceMiner",
	enabledByDefault = false,
	description = "Mines rune essence",
	tags = {"rune, maker, crafting, Tsillabak"},
	type = PluginType.SKILLING
)
@Slf4j
public class EssenceMinerPlugin extends Plugin {
	@Inject
	private Client client;

	@Inject
	private EssenceMinerConfiguration config;

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
	private EssenceMinerOverlay overlay;


	EssenceMinerState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;


	WorldArea VARROCK = new WorldArea(new WorldPoint(3256, 3250, 0), new WorldPoint(3254, 3420, 0));


	int timeout = 0;
	long sleepLength;
	boolean startEssenceMiner;
	private final Set<Integer> itemIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();
	public static Set<Integer> PORTAL = Set.of(NpcID.PORTAL_3088, NpcID.PORTAL_3086);
	public static Set<Integer> OBJ = Set.of(ObjectID.RUNE_ESSENCE_34773);
	public static final int V_EAST_BANK = 12853;
	public static final int ESSENCE_MINE = 11595;
	Rectangle clickBounds;

	@Provides
    EssenceMinerConfiguration provideConfig(ConfigManager configManager) {
		return configManager.getConfig(EssenceMinerConfiguration.class);
	}

	private
	void resetVals() {
		overlayManager.remove(overlay);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startEssenceMiner = false;
		requiredIds.clear();
	}

	@Subscribe
	private
	void onConfigButtonPressed(ConfigButtonClicked configButtonClicked) {
		if (!configButtonClicked.getGroup().equalsIgnoreCase("EssenceMiner")) {
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton")) {
			if (!startEssenceMiner) {
				startEssenceMiner = true;
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
		startEssenceMiner = false;
	}

	@Subscribe
	private
	void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("plankmaker")) {
			return;
		}
		startEssenceMiner = false;
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
	void openBank() {
		GameObject bankTarget = object.findNearestGameObject(10583);
		if (bankTarget != null) {
			targetMenu = new MenuEntry("", "", bankTarget.getId(),
					bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(),
					bankTarget.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
			utils.sendGameMessage("bank clicked");
		}
	}

	private
	void teleportMage() {
		targetNPC = npc.findNearestNpc(2886);
		if (npc != null) {
			targetMenu = new MenuEntry("", "",
					targetNPC.getIndex(), MenuOpcode.NPC_FOURTH_OPTION.getId(), 0, 0, false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
	}


	private
	void mineEssence() {
		targetObject = object.findNearestGameObject(34773);
		if (targetObject != null) {
			targetMenu = new MenuEntry("Mine", "<col=ffff>Essence", targetObject.getId(), 3, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private
	void clickNPCPortal() {

		targetNPC = npc.findNearestNpcWithin(client.getLocalPlayer().getWorldLocation(), 15, PORTAL);
		if (targetNPC != null) {
			targetMenu = new MenuEntry("", "",
					targetNPC.getIndex(), MenuOpcode.NPC_FIRST_OPTION.getId(), 0, 0, false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		} else {
			targetObject = object.findNearestGameObject(34825, 34779);
			if (targetObject != null) {

				targetMenu = new MenuEntry("Mine", "<col=ffff>Essence", targetObject.getId(), 3, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
				menu.setEntry(targetMenu);
				mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			}
		}
	}


	//if (npc == null) {
	//targetObject = object.findNearestGameObject(34825, 34779);
	//targetMenu = new MenuEntry("Mine", "<col=ffff>Essence", targetObject.getId(), 3, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
	//menu.setEntry(targetMenu);
	//mouse.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());


	private
	void clickPortal() {
		targetObject = object.findNearestGameObject(34825, 34779);
		if (targetObject != null) {
			targetMenu = new MenuEntry("Mine", "<col=ffff>Essence", targetObject.getId(), 3, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		} else {
			if (targetObject != null) {
				targetNPC = npc.findNearestNpc(3088, 3086);
				targetMenu = new MenuEntry("", "",
						targetNPC.getIndex(), MenuOpcode.NPC_FIRST_OPTION.getId(), 0, 0, false);
				menu.setEntry(targetMenu);
				mouse.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
			}
		}
	}



	public
	EssenceMinerState getState() {

		if (timeout > 0) {
			playerUtils.handleRun(20, 30);
			return TIMEOUT;
		}
		if (iterating) {
			return ITERATING;
		}
		if (playerUtils.isMoving(beforeLoc)) {
			playerUtils.handleRun(20, 30);
			return MOVING;
		}
		if (player.getWorldArea().intersectsWith(VARROCK) && !inventory.isFull()) {

			return WALK_TO_MINE;
		}

		if (player.getWorldLocation().equals(new WorldPoint(3253, 3401, 0))) {
			return CLICK_AUBURY;
		}

		if (inventory.isEmpty() && client.getLocalPlayer().getWorldLocation().getRegionID() != V_EAST_BANK) {
			return MINE_ESSENCE;
		}

		if (inventory.isFull() && client.getLocalPlayer().getWorldLocation().getRegionID() != V_EAST_BANK && (inventory.containsItem(ItemID.PURE_ESSENCE))) {
			return CLICKING_NPC_PORTAL;
		}
		if (inventory.isFull() && !bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK && (inventory.containsItem(ItemID.PURE_ESSENCE))) {
			return FIND_BANK;
		}
		if (inventory.isFull() && bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK){
			return DEPOSIT_ITEMS;
		}
		if (inventory.isEmpty() && bank.isOpen()&&client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK){
			return WALK_TO_MINE;
		}
		return IDLE;
	}


	@Subscribe
	private
	void onGameTick(GameTick tick) {
		if (!startEssenceMiner) {
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null) {
			if (!client.isResized()) {
				utils.sendGameMessage("Client must be set to resizable");
				startEssenceMiner = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state) {
				case TIMEOUT:
					playerUtils.handleRun(30, 20);
					timeout--;
					break;
				case WALK_TO_MINE:
					walk.sceneWalk(new WorldPoint(3253, 3401, 0), 0, 0);
					timeout = tickDelay();
					break;
				case CLICK_AUBURY:
					teleportMage();
					timeout = tickDelay();
					break;
				case MINE_ESSENCE:
					mineEssence();
					timeout = tickDelay();
					break;
				case CLICKING_PORTAL:
					clickPortal();
					break;
				case CLICKING_NPC_PORTAL:
					clickNPCPortal();
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



















