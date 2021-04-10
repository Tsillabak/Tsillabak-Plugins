package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NpcID;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.NPCUtils;

import javax.inject.Inject;

@Slf4j
public class CLICKAUBURY extends Task {
	@Override
	@Subscribe
	public boolean validate() {
		return
		npc.findNearestNpc(NpcID.AUBURY)!=null;
		// player.getWorldLocation().equals(new WorldPoint(3253, 3401, 0)) && !inventory.isFull();

	}

	@Override
	public String getTaskDescription() {
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event) {
		targetNPC = npc.findNearestNpc(2886);

		targetMenu = new MenuEntry("", "",
				targetNPC.getIndex(), MenuAction.NPC_FOURTH_OPTION.getId(), 0, 0, false);
		menu.setEntry(targetMenu);
		mouse.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
	}
}

