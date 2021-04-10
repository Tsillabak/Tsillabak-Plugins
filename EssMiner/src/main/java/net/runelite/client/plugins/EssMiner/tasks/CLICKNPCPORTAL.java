package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;

@Slf4j
public class CLICKNPCPORTAL extends Task
{

	@Override
	public boolean validate()
	{
		return inventory.isFull()
				&&player.getWorldLocation()
						.getRegionID()
						!= V_EAST_BANK;
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{
		targetNPC = npc.findNearestNpcWithin(client.getLocalPlayer().getWorldLocation(), 15, PORTAL);
		if (targetNPC != null) {
			targetMenu = new MenuEntry("", "",
					targetNPC.getIndex(), MenuAction.NPC_FIRST_OPTION.getId(), 0, 0, false);
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
}