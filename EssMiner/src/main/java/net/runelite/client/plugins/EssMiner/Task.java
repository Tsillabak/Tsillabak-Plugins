package net.runelite.client.plugins.EssMiner;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.iutils.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Slf4j

public abstract class Task
{

	public Task()
	{
	}

	@Inject
	public Client client;

	@Inject
	public EssMinerConfig config;

	@Inject
	public iUtils utils;

	@Inject
	public MenuUtils menu;

	@Inject
	public MouseUtils mouse;

	@Inject
	public CalculationUtils calc;

	@Inject
	public PlayerUtils playerUtils;

	@Inject
	public ObjectUtils object;
	@Inject
	public BankUtils bank;
	@Inject
	public InventoryUtils inventory;
	@Inject
	public WalkUtils walk;
	@Inject
	public NPCUtils npc;

	public MenuEntry entry;
	public GameObject targetObject;
	public NPC targetNPC;
	public MenuEntry targetMenu;
	public Player player;

	public static Set<Integer> PORTAL = Set.of(NpcID.PORTAL_3088, NpcID.PORTAL_3086);
	public static final int V_EAST_BANK = 12853;

	public abstract boolean validate();

	public long sleepDelay()
	{
		EssMinerPlugin.sleepLength = calc.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return EssMinerPlugin.sleepLength;
	}

	public int tickDelay()
	{
		EssMinerPlugin.tickLength = (int) calc.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		return EssMinerPlugin.tickLength;
	}

	public String getTaskDescription()
	{
		return this.getClass().getSimpleName();
	}

	public void onGameTick(GameTick event)
	{
	}

}
