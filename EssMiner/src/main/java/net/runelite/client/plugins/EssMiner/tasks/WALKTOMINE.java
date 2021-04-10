package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;

import javax.inject.Inject;

@Slf4j
public class WALKTOMINE extends Task
{
	@Override
	public boolean validate()
	{
		return inventory.isEmpty() && bank.isOpen()&&client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK;
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{
		walk.sceneWalk(new WorldPoint(3253, 3401, 0), 0, 0);
	}
}