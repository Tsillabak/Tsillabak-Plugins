package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;

@Slf4j
public class FINDBANK extends Task
{



	@Override
	public boolean validate()
	{
		return inventory.isFull()&& !bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK;
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{

		GameObject bankTarget = object.findNearestGameObject(10583);
		if (bankTarget != null) {
			entry = new MenuEntry("", "", bankTarget.getId(),
					bank.getBankMenuOpcode(bankTarget.getId()), bankTarget.getSceneMinLocation().getX(),
					bankTarget.getSceneMinLocation().getY(), false);
			menu.setEntry(entry);
			mouse.delayMouseClick(bankTarget.getConvexHull().getBounds(), sleepDelay());
			utils.sendGameMessage("bank clicked");
		}

	}
}