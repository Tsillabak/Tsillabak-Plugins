package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;
import net.runelite.client.plugins.EssMiner.Task;

@Slf4j
public class DEPOSITITEMS extends Task
{

	@Override
	public boolean validate()
	{
		return inventory.isFull() && bank.isOpen() && client.getLocalPlayer().getWorldLocation().getRegionID() == V_EAST_BANK;
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{
			bank.depositAll();
	}
}