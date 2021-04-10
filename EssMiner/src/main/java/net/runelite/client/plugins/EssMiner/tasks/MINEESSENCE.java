package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;
import net.runelite.client.plugins.iutils.InventoryUtils;

import javax.inject.Inject;

@Slf4j
public class MINEESSENCE extends Task
{
	@Inject
	InventoryUtils inventory;

	@Override
	public boolean validate()
	{
		return inventory.isEmpty() && client.getLocalPlayer().getWorldLocation().getRegionID() != V_EAST_BANK;
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{
		targetObject = object.findNearestGameObject(34773);
		if (targetObject != null) {
			targetMenu = new MenuEntry("Mine", "<col=ffff>Essence", targetObject.getId(), 3, targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			menu.setEntry(targetMenu);
			mouse.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}
}