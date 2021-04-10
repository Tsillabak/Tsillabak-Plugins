package net.runelite.client.plugins.EssMiner.tasks;

import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.EssMiner.Task;
import net.runelite.client.plugins.EssMiner.EssMinerPlugin;

public class TIMEOUT extends Task
{
	@Override
	public boolean validate()
	{
		return EssMinerPlugin.timeout > 0;
	}

	@Override
	public String getTaskDescription()
	{
		return "Timeout: " + EssMinerPlugin.timeout;
	}

	@Override
	public void onGameTick(GameTick event)
	{
		EssMinerPlugin.timeout--;
	}
}