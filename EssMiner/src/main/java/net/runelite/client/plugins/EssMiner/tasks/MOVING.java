package net.runelite.client.plugins.EssMiner.tasks;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.Task;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;

@Slf4j
public class MOVING extends Task
{

	@Override
	public boolean validate()
	{
		return playerUtils.isMoving(EssMinerPlugin.beforeLoc);
	}

	@Override
	public String getTaskDescription()
	{
		return EssMinerPlugin.status;
	}

	@Override
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		if (player != null)
		{
			playerUtils.handleRun(20, 30);
			EssMinerPlugin.timeout = tickDelay();
		}
	}
}