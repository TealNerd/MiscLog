package com.biggestnerd.misclog;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;

@Mod(modid="misclog", name="Logger of things", version="v1.0.5")
public class MiscLog {

	@Instance(value="MiscLog")
	public static MiscLog instance;
	Minecraft mc = Minecraft.getMinecraft();
	static List<String> previousPlayerList = new ArrayList();
	Database db;
	Pattern snitch = Pattern.compile("^ \\* ([a-zA-Z0-9_]+) (?:entered|logged out in|logged in to) snitch at .* \\[(.*) ([-]?[0-9]+) ([-]?[0-9]+) ([-]?[0-9]+)\\]$");
	Pattern tpsPattern = Pattern.compile("^TPS from last 1m, 5m, 15m: [*]?([0-9.]+).*$");
	boolean civcraft = false;
	int loop = 420;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(this);
	    MinecraftForge.EVENT_BUS.register(this);
	    
	    try {
			LaunchClassLoader l = (LaunchClassLoader) getClass()
					.getClassLoader();

			Field field = LaunchClassLoader.class
					.getDeclaredField("classLoaderExceptions");

			field.setAccessible(true);
			Set<String> exclusions = (Set) field.get(l);

			exclusions.remove("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}
	    db = new Database();
	    db.connect();
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if(event.phase == TickEvent.Phase.START) {
			doSkyNet();
			if(mc.theWorld != null) {
				if(mc.func_147104_D().serverIP.matches(".*mc\\.civcraft\\.vg(:25565)?")) {
					civcraft = true;
				} else {
					civcraft = false;
				}
			}
		}
	}
	
	@SubscribeEvent
	public void onChat(ClientChatReceivedEvent event) {
		String msg = event.message.getUnformattedText();
		Matcher tpsMatcher = tpsPattern.matcher(msg);
		while(tpsMatcher.find()) {
			event.setCanceled(true);
			float tps = Float.parseFloat(tpsMatcher.group(1));
			db.execute("INSERT INTO tps (tps, time) values ('" + tps + "', '" + System.nanoTime() + "');");
		}
		Matcher snitchMatcher = snitch.matcher(msg);
		while(snitchMatcher.find()) {
			String player = snitchMatcher.group(1);
			int x = Integer.parseInt(snitchMatcher.group(3));
			int y = Integer.parseInt(snitchMatcher.group(4));
			int z = Integer.parseInt(snitchMatcher.group(5));
			String world = snitchMatcher.group(2);
			if(civcraft)
				db.sendSnitch(player, x, y, z, world);
		}
	}
	
	public void doSkyNet() {
		if(mc.theWorld == null || !civcraft) {
			return;
		}
		if(mc.theWorld != null) {
		ArrayList<String> playerList = new ArrayList();
		List players = mc.thePlayer.sendQueue.playerInfoList;
		for(Object o : players) {
			if((o instanceof GuiPlayerInfo)) {
				GuiPlayerInfo info = (GuiPlayerInfo)o;
				
				playerList.add(EnumChatFormatting.getTextWithoutFormattingCodes(info.name));
			}
		}
		ArrayList<String> temp = (ArrayList)playerList.clone();
		playerList.removeAll(previousPlayerList);
		previousPlayerList.removeAll(temp);
		for(String player : previousPlayerList) {
			onPlayerLeave(player);
		}
		for(String player : playerList) {
			onPlayerJoin(player);
		}
		previousPlayerList = temp;
		}
	}
	
	public void onPlayerJoin(String player) {
		db.execute("INSERT INTO logins (player, time) values ('" + player + "', '" + System.nanoTime() + "');");
	}
	
	public void onPlayerLeave(String player) {
		db.execute("INSERT INTO logouts (player, time) values ('" + player + "', '" + System.nanoTime() + "');");
	}
}
