/*******************************************************************************************************
 * Continued by FlyingPikachu/HappyPikachu with permission from _Blackvein_. All rights reserved.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package me.blackvein.quests;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.codisimus.plugins.phatloots.PhatLootsAPI;
import com.codisimus.plugins.phatloots.loot.CommandLoot;
import com.codisimus.plugins.phatloots.loot.LootBundle;
import com.gmail.nossr50.datatypes.skills.SkillType;
import com.gmail.nossr50.util.player.UserManager;
import com.herocraftonline.heroes.characters.Hero;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import me.blackvein.quests.exceptions.InvalidStageException;
import me.blackvein.quests.util.ItemUtil;
import me.blackvein.quests.util.Lang;
import me.clip.placeholderapi.PlaceholderAPI;
import net.citizensnpcs.api.npc.NPC;

public class Quest {

	public String name;
	public String description;
	public String finished;
	public String region = null;
	public ItemStack guiDisplay = null;
	public int parties = 0;
	public LinkedList<Stage> orderedStages = new LinkedList<Stage>();
	NPC npcStart;
	Location blockStart;
	Quests plugin;
	Event initialEvent;
	// Requirements
	int moneyReq = 0;
	int questPointsReq = 0;
	List<ItemStack> items = new LinkedList<ItemStack>();
	List<Boolean> removeItems = new LinkedList<Boolean>();
	List<String> neededQuests = new LinkedList<String>();
	List<String> blockQuests = new LinkedList<String>();
	List<String> permissionReqs = new LinkedList<String>();
	List<String> mcMMOSkillReqs = new LinkedList<String>();
	List<Integer> mcMMOAmountReqs = new LinkedList<Integer>();
	String heroesPrimaryClassReq = null;
	String heroesSecondaryClassReq = null;
	Map<String, Map<String, Object>> customRequirements = new HashMap<String, Map<String, Object>>();
	Map<String, Map<String, Object>> customRewards = new HashMap<String, Map<String, Object>>();
	public String failRequirements = null;
	//Planner
	public String startPlanner = null;
	public String endPlanner = null;
	public long repeatPlanner = -1;
	public long cooldownPlanner = -1;
	// Rewards
	int moneyReward = 0;
	int questPoints = 0;
	int exp = 0;
	List<String> commands = new LinkedList<String>();
	List<String> permissions = new LinkedList<String>();
	LinkedList<ItemStack> itemRewards = new LinkedList<ItemStack>();
	List<String> mcmmoSkills = new LinkedList<String>();
	List<Integer> mcmmoAmounts = new LinkedList<Integer>();
	List<String> heroesClasses = new LinkedList<String>();
	List<Double> heroesAmounts = new LinkedList<Double>();
	List<String> phatLootRewards = new LinkedList<String>();

	public Stage getStage(int index) {
		try {
			return orderedStages.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	public void nextStage(Quester q) {
		String stageCompleteMessage = q.getCurrentStage(this).completeMessage;
		if (stageCompleteMessage != null) {
			String s = Quests.parseString(stageCompleteMessage, this);
			if(Quests.placeholder != null) {
				s = PlaceholderAPI.setPlaceholders(q.getPlayer(), s);
			}
			q.getPlayer().sendMessage(s);
		}
		if (plugin.useCompass) {
			q.resetCompass();
			q.findCompassTarget();
		}
		if (q.getCurrentStage(this).delay < 0) {
			Player player = q.getPlayer();
			if (q.currentQuests.get(this) == (orderedStages.size() - 1)) {
				if (q.getCurrentStage(this).script != null) {
					plugin.trigger.parseQuestTaskTrigger(q.getCurrentStage(this).script, player);
				}
				if (q.getCurrentStage(this).finishEvent != null) {
					q.getCurrentStage(this).finishEvent.fire(q, this);
				}
				completeQuest(q);
			} else {
				try {
					if (q.getCurrentStage(this).finishEvent != null) {
						q.getCurrentStage(this).finishEvent.fire(q, this);
					}
					setStage(q, q.currentQuests.get(this) + 1);
					/*
					 * yzh update
					 */
					q.saveData();
				} catch (InvalidStageException e) {
					e.printStackTrace();
				}
			}
			if (q.getQuestData(this) != null) {
				q.getQuestData(this).delayStartTime = 0;
				q.getQuestData(this).delayTimeLeft = -1;
			}
		} else {
			q.startStageTimer(this);
		}
		q.updateJournal();
	}

	public void setStage(Quester quester, int stage) throws InvalidStageException {
		if (orderedStages.size() - 1 < stage) {
			throw new InvalidStageException(this, stage);
		}
		Stage currentStage = quester.getCurrentStage(this);
		quester.hardQuit(this);
		quester.hardStagePut(this, stage);
		quester.addEmptiesFor(this, stage);
		if (currentStage.script != null) {
			plugin.trigger.parseQuestTaskTrigger(currentStage.script, quester.getPlayer());
		}
		/*
		 * if (quester.getCurrentStage(this).finishEvent != null) { quester.getCurrentStage(this).finishEvent.fire(quester); }
		 */
		Stage nextStage = quester.getCurrentStage(this);
		if (nextStage.startEvent != null) {
			nextStage.startEvent.fire(quester, this);
		}
		updateCompass(quester, nextStage);
		String stageStartMessage = quester.getCurrentStage(this).startMessage;
		if (stageStartMessage != null) {
			quester.getPlayer().sendMessage(Quests.parseString(stageStartMessage, this));
		}
		String msg = Lang.get(quester.getPlayer(), "questObjectivesTitle");
		msg = msg.replaceAll("<quest>", name);
		quester.getPlayer().sendMessage(ChatColor.GOLD + msg);
		for (String s : quester.getObjectivesReal(this)) {
			if(Quests.placeholder != null) {
				s = PlaceholderAPI.setPlaceholders(quester.getPlayer(), s);
			}
			quester.getPlayer().sendMessage(s);
		}
		quester.updateJournal();
	}

	public boolean updateCompass(Quester quester, Stage nextStage) {
		if (!plugin.useCompass)
			return false;
		Location targetLocation = null;
		if (nextStage == null) {
			return false;
		}
		if (nextStage.citizensToInteract != null && nextStage.citizensToInteract.size() > 0) {
			targetLocation = plugin.getNPCLocation(nextStage.citizensToInteract.getFirst());
		} else if (nextStage.citizensToKill != null && nextStage.citizensToKill.size() > 0) {
			targetLocation = plugin.getNPCLocation(nextStage.citizensToKill.getFirst());
		} else if (nextStage.locationsToReach != null && nextStage.locationsToReach.size() > 0) {
			targetLocation = nextStage.locationsToReach.getFirst();
		}
		if (targetLocation != null) {
			if (targetLocation.getWorld().getName().equals(quester.getPlayer().getWorld().getName())) {
				quester.getPlayer().setCompassTarget(targetLocation);
			}
		}
		return targetLocation != null;
	}

	public String getName() {
		return name;
	}

	public boolean testRequirements(Quester quester) {
		return testRequirements(quester.getPlayer());
	}

	public boolean testRequirements(Player player) {
		Quester quester = plugin.getQuester(player.getUniqueId());
		if (moneyReq != 0 && Quests.economy != null) {
			if (Quests.economy.getBalance(Bukkit.getOfflinePlayer(player.getUniqueId())) < moneyReq) {
				return false;
			}
		}
		PlayerInventory inventory = player.getInventory();
		int num = 0;
		for (ItemStack is : items) {
			for (ItemStack stack : inventory.getContents()) {
				if (stack != null) {
					if (ItemUtil.compareItems(is, stack, true) == 0) {
						num += stack.getAmount();
					}
				}
			}
			if (num < is.getAmount()) {
				return false;
			}
			num = 0;
		}
		for (String s : permissionReqs) {
			if (player.hasPermission(s) == false) {
				return false;
			}
		}
		for (String s : mcMMOSkillReqs) {
			final SkillType st = Quests.getMcMMOSkill(s);
			final int lvl = mcMMOAmountReqs.get(mcMMOSkillReqs.indexOf(s));
			if (UserManager.getPlayer(player).getProfile().getSkillLevel(st) < lvl) {
				return false;
			}
		}
		if (heroesPrimaryClassReq != null) {
			if (plugin.testPrimaryHeroesClass(heroesPrimaryClassReq, player.getUniqueId()) == false) {
				return false;
			}
		}
		if (heroesSecondaryClassReq != null) {
			if (plugin.testSecondaryHeroesClass(heroesSecondaryClassReq, player.getUniqueId()) == false) {
				return false;
			}
		}
		for (String s : customRequirements.keySet()) {
			CustomRequirement found = null;
			for (CustomRequirement cr : plugin.customRequirements) {
				if (cr.getName().equalsIgnoreCase(s)) {
					found = cr;
					break;
				}
			}
			if (found != null) {
				if (found.testRequirement(player, customRequirements.get(s)) == false) {
					return false;
				}
			} else {
				plugin.getLogger().warning("Quester \"" + player.getName() + "\" attempted to take Quest \"" + name + "\", but the Custom Requirement \"" + s 
						+ "\" could not be found. Does it still exist?");
			}
		}
		if (quester.questPoints < questPointsReq) {
			return false;
		}
		if (quester.completedQuests.containsAll(neededQuests) == false) {
			return false;
		}
		for (String q : blockQuests) {
			Quest questObject = new Quest();
			questObject.name = q;
			if (quester.completedQuests.contains(q) || quester.currentQuests.containsKey(questObject)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void completeQuest(Quester q) {
		final Player player = plugin.getServer().getPlayer(q.id);
		q.hardQuit(this);
		if (!q.completedQuests.contains(name)) {
			q.completedQuests.add(name);
		}
		String none = ChatColor.GRAY + "- (" + Lang.get(player, "none") + ")";
		final String ps = Quests.parseString(finished, this);
		for (Map.Entry<Integer, Quest> entry : q.timers.entrySet()) {
			if (entry.getValue().getName().equals(getName())) {
				plugin.getServer().getScheduler().cancelTask(entry.getKey());
				q.timers.remove(entry.getKey());
			}
		}
		org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {

			@Override
			public void run() {
				for (String msg : ps.split("<br>")) {
					player.sendMessage(ChatColor.AQUA + msg);
				}
			}
		}, 40);
		if (moneyReward > 0 && Quests.economy != null) {
			Quests.economy.depositPlayer(q.getOfflinePlayer(), moneyReward);
			none = null;
		}
		if (cooldownPlanner > -1) {
			q.completedTimes.put(this.name, System.currentTimeMillis());
			if (q.amountsCompleted.containsKey(this.name)) {
				q.amountsCompleted.put(this.name, q.amountsCompleted.get(this.name) + 1);
			} else {
				q.amountsCompleted.put(this.name, 1);
			}
		}
		for (ItemStack i : itemRewards) {
			Quests.addItem(player, i);
			none = null;
		}
		for (String s : commands) {
			s = s.replaceAll("<player>", player.getName());
			plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), s);
			none = null;
		}
		for (String s : permissions) {
			Quests.permission.playerAdd(player, s);
			none = null;
		}
		for (String s : mcmmoSkills) {
			UserManager.getPlayer(player).getProfile().addLevels(Quests.getMcMMOSkill(s), mcmmoAmounts.get(mcmmoSkills.indexOf(s)));
			none = null;
		}
		for (String s : heroesClasses) {
			Hero hero = plugin.getHero(player.getUniqueId());
			hero.addExp(heroesAmounts.get(heroesClasses.indexOf(s)), Quests.heroes.getClassManager().getClass(s), player.getLocation());
			none = null;
		}
		LinkedList<ItemStack> phatLootItems = new LinkedList<ItemStack>();
		int phatLootExp = 0;
		LinkedList<String> phatLootMessages = new LinkedList<String>();
		for (String s : phatLootRewards) {
			LootBundle lb = PhatLootsAPI.getPhatLoot(s).rollForLoot();
			if (lb.getExp() > 0) {
				phatLootExp += lb.getExp();
				player.giveExp(lb.getExp());
			}
			if (lb.getMoney() > 0) {
				if (Quests.economy != null) {
					Quests.economy.depositPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), lb.getMoney());
				}
			}
			if (lb.getItemList().isEmpty() == false) {
				phatLootItems.addAll(lb.getItemList());
				for (ItemStack is : lb.getItemList()) {
					Quests.addItem(player, is);
				}
			}
			if (lb.getCommandList().isEmpty() == false) {
				for (CommandLoot cl : lb.getCommandList()) {
					cl.execute(player);
				}
			}
			if (lb.getMessageList().isEmpty() == false) {
				phatLootMessages.addAll(lb.getMessageList());
			}
		}
		if (exp > 0) {
			player.giveExp(exp);
			none = null;
		}
		String complete = Lang.get(player, "questCompleteTitle");
		complete = complete.replaceAll("<quest>", ChatColor.YELLOW + name + ChatColor.GOLD);
		player.sendMessage(ChatColor.GOLD + complete);
		player.sendMessage(ChatColor.GREEN + Lang.get(player, "questRewardsTitle"));
		if (plugin.showQuestTitles) {
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName()
					+ " title " + "{\"text\":\"" + Lang.get(player, "quest") + " " + Lang.get(player, "complete") +  "\",\"color\":\"gold\"}");
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "title " + player.getName()
					+ " subtitle " + "{\"text\":\"" + name + "\",\"color\":\"yellow\"}");
		}
		if (questPoints > 0) {
			player.sendMessage("- " + ChatColor.DARK_GREEN + questPoints + " " + Lang.get(player, "questPoints"));
			q.questPoints += questPoints;
			none = null;
		}
		for (ItemStack i : itemRewards) {
			String text = "error";
			if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
				if (i.getEnchantments().isEmpty()) {
					text = "- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount();
				} else {
					text = "- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() + ChatColor.RESET + ChatColor.GRAY + " " + Lang.get(player, "with") 
							+ ChatColor.DARK_PURPLE;
					for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
						text += " " + Quester.prettyEnchantmentString(e.getKey()) + ":" + e.getValue();
					}
					text += ChatColor.GRAY + " x " + i.getAmount();
				}
			} else if (i.getDurability() != 0) {
				if (i.getEnchantments().isEmpty()) {
					text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY + " x " + i.getAmount();
				} else {
					text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY + " " + Lang.get(player, "with");
					for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
						text += " " + Quester.prettyEnchantmentString(e.getKey()) + ":" + e.getValue();
					}
					text += ChatColor.GRAY + " x " + i.getAmount();
				}
			} else {
				if (i.getEnchantments().isEmpty()) {
					text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " + i.getAmount();
				} else {
					text = "- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " " + Lang.get(player, "with");
					for (Entry<Enchantment, Integer> e : i.getEnchantments().entrySet()) {
						text += " " + Quester.prettyEnchantmentString(e.getKey()) + ":" + e.getValue();
					}
					text += ChatColor.GRAY + " x " + i.getAmount();
				}
			}
			player.sendMessage(text);
			none = null;
		}
		for (ItemStack i : phatLootItems) {
			if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
				if (i.getEnchantments().isEmpty()) {
					player.sendMessage("- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount());
				} else {
					player.sendMessage("- " + ChatColor.DARK_AQUA + ChatColor.ITALIC + i.getItemMeta().getDisplayName() + ChatColor.RESET + ChatColor.GRAY + " x " + i.getAmount() + ChatColor.DARK_PURPLE + " " + Lang.get(player, "enchantedItem"));
				}
			} else if (i.getDurability() != 0) {
				if (i.getEnchantments().isEmpty()) {
					player.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY + " x " + i.getAmount());
				} else {
					player.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ":" + i.getDurability() + ChatColor.GRAY + " x " + i.getAmount() + ChatColor.DARK_PURPLE + " " + Lang.get(player, "enchantedItem"));
				}
			} else {
				if (i.getEnchantments().isEmpty()) {
					player.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " + i.getAmount());
				} else {
					player.sendMessage("- " + ChatColor.DARK_GREEN + ItemUtil.getName(i) + ChatColor.GRAY + " x " + i.getAmount() + ChatColor.DARK_PURPLE + " " + Lang.get(player, "enchantedItem"));
				}
			}
			none = null;
		}
		if (moneyReward > 1) {
			player.sendMessage("- " + ChatColor.DARK_GREEN + moneyReward + " " + ChatColor.DARK_PURPLE + Quests.getCurrency(true));
			none = null;
		} else if (moneyReward == 1) {
			player.sendMessage("- " + ChatColor.DARK_GREEN + moneyReward + " " + ChatColor.DARK_PURPLE + Quests.getCurrency(false));
			none = null;
		}
		if (exp > 0 || phatLootExp > 0) {
			int tot = exp + phatLootExp;
			player.sendMessage("- " + ChatColor.DARK_GREEN + tot + ChatColor.DARK_PURPLE + " " + Lang.get(player, "experience"));
			none = null;
		}
		if (mcmmoSkills.isEmpty() == false) {
			for (String s : mcmmoSkills) {
				player.sendMessage("- " + ChatColor.DARK_GREEN + mcmmoAmounts.get(mcmmoSkills.indexOf(s)) + " " + ChatColor.DARK_PURPLE + s + " " + Lang.get(player, "experience"));
			}
			none = null;
		}
		if (heroesClasses.isEmpty() == false) {
			for (String s : heroesClasses) {
				player.sendMessage("- " + ChatColor.AQUA + heroesAmounts.get(heroesClasses.indexOf(s)) + " " + ChatColor.BLUE + s + " " + Lang.get(player, "experience"));
			}
			none = null;
		}
		if (phatLootMessages.isEmpty() == false) {
			for (String s : phatLootMessages) {
				player.sendMessage("- " + s);
			}
			none = null;
		}
		for (String s : customRewards.keySet()) {
			CustomReward found = null;
			for (CustomReward cr : plugin.customRewards) {
				if (cr.getName().equalsIgnoreCase(s)) {
					found = cr;
					break;
				}
			}
			if (found != null) {
				Map<String, Object> datamap = customRewards.get(found.getName());
				String message = found.getRewardName();
				if (message != null) {
					for (String key : datamap.keySet()) {
						message = message.replaceAll("%" + ((String) key) + "%", ((String) datamap.get(key)));
					}
					player.sendMessage("- " + ChatColor.GOLD + message);
				} else {
					plugin.getLogger().warning("Failed to notify player: Custom Reward does not have an assigned name");
				}
				found.giveReward(player, customRewards.get(s));
			} else {
				plugin.getLogger().warning("Quester \"" + player.getName() + "\" completed the Quest \"" + name + "\", but the Custom Reward \"" + s + "\" could not be found. Does it still exist?");
			}
			none = null;
		}
		if (none != null) {
			player.sendMessage(none);
		}
		q.saveData();
		player.updateInventory();
		q.updateJournal();
		q.findCompassTarget();
	}

	@SuppressWarnings("deprecation")
	public void failQuest(Quester q) {
		if (plugin.getServer().getPlayer(q.id) != null) {
			Player player = plugin.getServer().getPlayer(q.id);
			String title = Lang.get(player, "questTitle");
			title = title.replaceAll("<quest>", ChatColor.DARK_PURPLE + name + ChatColor.AQUA);
			player.sendMessage(ChatColor.AQUA + title);
			player.sendMessage(ChatColor.RED + Lang.get(player, "questFailed"));
			q.hardQuit(this);
			q.saveData();
			player.updateInventory();
		} else {
			q.hardQuit(this);
			q.saveData();
		}
		q.updateJournal();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Quest) {
			Quest other = (Quest) o;
			if (other.blockStart != null && blockStart != null) {
				if (other.blockStart.equals(blockStart) == false) {
					return false;
				}
			} else if (other.blockStart != null && blockStart == null) {
				return false;
			} else if (other.blockStart == null && blockStart != null) {
				return false;
			}
			if (commands.size() == other.commands.size()) {
				for (int i = 0; i < commands.size(); i++) {
					if (commands.get(i).equals(other.commands.get(i)) == false) {
						return false;
					}
				}
			} else {
				return false;
			}
			if (other.description.equals(description) == false) {
				return false;
			}
			if (other.initialEvent != null && initialEvent != null) {
				if (other.initialEvent.equals(initialEvent) == false) {
					return false;
				}
			} else if (other.initialEvent != null && initialEvent == null) {
				return false;
			} else if (other.initialEvent == null && initialEvent != null) {
				return false;
			}
			if (other.exp != exp) {
				return false;
			}
			if (other.failRequirements != null && failRequirements != null) {
				if (other.failRequirements.equals(failRequirements) == false) {
					return false;
				}
			} else if (other.failRequirements != null && failRequirements == null) {
				return false;
			} else if (other.failRequirements == null && failRequirements != null) {
				return false;
			}
			if (other.finished.equals(finished) == false) {
				return false;
			}
			if (other.items.equals(items) == false) {
				return false;
			}
			if (other.itemRewards.equals(itemRewards) == false) {
				return false;
			}
			if (other.mcmmoAmounts.equals(mcmmoAmounts) == false) {
				return false;
			}
			if (other.mcmmoSkills.equals(mcmmoSkills) == false) {
				return false;
			}
			if (other.heroesClasses.equals(heroesClasses) == false) {
				return false;
			}
			if (other.heroesAmounts.equals(heroesAmounts) == false) {
				return false;
			}
			if (other.phatLootRewards.equals(phatLootRewards) == false) {
				return false;
			}
			if (other.moneyReq != moneyReq) {
				return false;
			}
			if (other.moneyReward != moneyReward) {
				return false;
			}
			if (other.name.equals(name) == false) {
				return false;
			}
			if (other.neededQuests.equals(neededQuests) == false) {
				return false;
			}
			if (other.blockQuests.equals(blockQuests) == false) {
				return false;
			}
			if (other.npcStart != null && npcStart != null) {
				if (other.npcStart.equals(npcStart) == false) {
					return false;
				}
			} else if (other.npcStart != null && npcStart == null) {
				return false;
			} else if (other.npcStart == null && npcStart != null) {
				return false;
			}
			if (other.permissionReqs.equals(permissionReqs) == false) {
				return false;
			}
			if (other.heroesPrimaryClassReq != null && heroesPrimaryClassReq != null) {
				if (other.heroesPrimaryClassReq.equals(heroesPrimaryClassReq) == false) {
					return false;
				}
			} else if (other.heroesPrimaryClassReq != null && heroesPrimaryClassReq == null) {
				return false;
			} else if (other.heroesPrimaryClassReq == null && heroesPrimaryClassReq != null) {
				return false;
			}
			if (other.heroesSecondaryClassReq != null && heroesSecondaryClassReq != null) {
				if (other.heroesSecondaryClassReq.equals(heroesSecondaryClassReq) == false) {
					return false;
				}
			} else if (other.heroesSecondaryClassReq != null && heroesSecondaryClassReq == null) {
				return false;
			} else if (other.heroesSecondaryClassReq == null && heroesSecondaryClassReq != null) {
				return false;
			}
			if (other.customRequirements.equals(customRequirements) == false) {
				return false;
			}
			if (other.customRewards.equals(customRewards) == false) {
				return false;
			}
			if (other.permissions.equals(permissions) == false) {
				return false;
			}
			if (other.mcMMOSkillReqs.equals(mcMMOSkillReqs) == false) {
				return false;
			}
			if (other.mcMMOAmountReqs.equals(mcMMOAmountReqs) == false) {
				return false;
			}
			if (other.questPoints != questPoints) {
				return false;
			}
			if (other.questPointsReq != questPointsReq) {
				return false;
			}
			if (other.orderedStages.equals(orderedStages) == false) {
				return false;
			}
			if (other.startPlanner != startPlanner) {
				return false;
			}
			if (other.endPlanner != endPlanner) {
				return false;
			}
			if (other.repeatPlanner != repeatPlanner) {
				return false;
			}
			if (other.cooldownPlanner != cooldownPlanner) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 53 * hash + (this.name != null ? this.name.hashCode() : 0);
		hash = 53 * hash + (this.description != null ? this.description.hashCode() : 0);
		hash = 53 * hash + (this.finished != null ? this.finished.hashCode() : 0);
		hash = 53 * hash + (this.startPlanner  != null ? this.startPlanner.hashCode() : 0);
		hash = 53 * hash + (this.endPlanner  != null ? this.endPlanner.hashCode() : 0);
		hash = 53 * hash + (int) (this.repeatPlanner ^ (this.repeatPlanner >>> 32));
		hash = 53 * hash + (int) (this.cooldownPlanner ^ (this.cooldownPlanner >>> 32));
		hash = 53 * hash + (this.region != null ? this.region.hashCode() : 0);
		hash = 53 * hash + (this.guiDisplay != null ? this.guiDisplay.hashCode() : 0);
		hash = 53 * hash + (this.orderedStages != null ? this.orderedStages.hashCode() : 0);
		hash = 53 * hash + (this.npcStart != null ? this.npcStart.hashCode() : 0);
		hash = 53 * hash + (this.blockStart != null ? this.blockStart.hashCode() : 0);
		hash = 53 * hash + (this.initialEvent != null ? this.initialEvent.hashCode() : 0);
		hash = 53 * hash + this.moneyReq;
		hash = 53 * hash + this.questPointsReq;
		hash = 53 * hash + (this.items != null ? this.items.hashCode() : 0);
		hash = 53 * hash + (this.neededQuests != null ? this.neededQuests.hashCode() : 0);
		hash = 53 * hash + (this.blockQuests != null ? this.blockQuests.hashCode() : 0);
		hash = 53 * hash + (this.permissionReqs != null ? this.permissionReqs.hashCode() : 0);
		hash = 53 * hash + (this.mcMMOSkillReqs != null ? this.mcMMOSkillReqs.hashCode() : 0);
		hash = 53 * hash + (this.mcMMOAmountReqs != null ? this.mcMMOAmountReqs.hashCode() : 0);
		hash = 53 * hash + (this.heroesPrimaryClassReq != null ? this.heroesPrimaryClassReq.hashCode() : 0);
		hash = 53 * hash + (this.heroesSecondaryClassReq != null ? this.heroesSecondaryClassReq.hashCode() : 0);
		hash = 53 * hash + (this.customRequirements != null ? this.customRequirements.hashCode() : 0);
		hash = 53 * hash + (this.customRewards != null ? this.customRewards.hashCode() : 0);
		hash = 53 * hash + (this.failRequirements != null ? this.failRequirements.hashCode() : 0);
		hash = 53 * hash + this.moneyReward;
		hash = 53 * hash + this.questPoints;
		hash = 53 * hash + this.exp;
		hash = 53 * hash + (this.commands != null ? this.commands.hashCode() : 0);
		hash = 53 * hash + (this.permissions != null ? this.permissions.hashCode() : 0);
		hash = 53 * hash + (this.itemRewards != null ? this.itemRewards.hashCode() : 0);
		hash = 53 * hash + (this.mcmmoSkills != null ? this.mcmmoSkills.hashCode() : 0);
		hash = 53 * hash + (this.mcmmoAmounts != null ? this.mcmmoAmounts.hashCode() : 0);
		hash = 53 * hash + (this.heroesClasses != null ? this.heroesClasses.hashCode() : 0);
		hash = 53 * hash + (this.heroesAmounts != null ? this.heroesAmounts.hashCode() : 0);
		hash = 53 * hash + (this.phatLootRewards != null ? this.phatLootRewards.hashCode() : 0);
		return hash;
	}

	public boolean isInRegion(Player player) {
		if (region == null) {
			return true;
		} else {
			ApplicableRegionSet ars = Quests.worldGuard.getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
			Iterator<ProtectedRegion> i = ars.iterator();
			while (i.hasNext()) {
				ProtectedRegion pr = i.next();
				if (pr.getId().equalsIgnoreCase(region)) {
					return true;
				}
			}
			return false;
		}
	}
}
