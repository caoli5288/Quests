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
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class CustomObjective implements Listener {

	private Quests plugin = Quests.getPlugin(Quests.class);
	private String name = null;
	private String author = null;
	public final Map<String, Object> datamap = new HashMap<String, Object>();
	public final Map<String, String> descriptions = new HashMap<String, String>();
	private String countPrompt = "null";
	private String display = "null";
	private boolean enableCount = true;
	private boolean showCount = true;
	private int count = 1;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void addData(String name) {
		datamap.put(name, null);
	}

	public void addDescription(String data, String description) {
		descriptions.put(data, description);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getCountPrompt() {
		return countPrompt;
	}

	public void setCountPrompt(String countPrompt) {
		this.countPrompt = countPrompt;
	}

	public boolean isCountShown() {
		return showCount;
	}

	public void setShowCount(boolean showCount) {
		this.showCount = showCount;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public boolean isEnableCount() {
		return enableCount;
	}

	public void setEnableCount(boolean enableCount) {
		this.enableCount = enableCount;
	}

	public Map<String, Object> getDatamap(Player player, CustomObjective obj, Quest quest) {
		Quester quester = plugin.getQuester(player.getUniqueId());
		if (quester != null) {
			Stage currentStage = quester.getCurrentStage(quest);
			if (currentStage == null)
				return null;
			int index = -1;
			int tempIndex = 0;
			for (me.blackvein.quests.CustomObjective co : currentStage.customObjectives) {
				if (co.getName().equals(obj.getName())) {
					index = tempIndex;
					break;
				}
				tempIndex++;
			}
			if (index > -1) {
				return currentStage.customObjectiveData.get(index);
			}
		}
		return null;
	}

	public void incrementObjective(Player player, CustomObjective obj, int count, Quest quest) {
		Quester quester = plugin.getQuester(player.getUniqueId());
		if (quester == null) {
			return;
		}
		// Check if the player has Quest with objective
		boolean hasQuest = false;
		for (CustomObjective co : quester.getCurrentStage(quest).customObjectives) {
			if (co.getName().equals(obj.getName())) {
				hasQuest = true;
				break;
			}
		}
		if (hasQuest && quester.hasCustomObjective(quest, obj.getName())) {
			if (quester.getQuestData(quest).customObjectiveCounts.containsKey(obj.getName())) {
				int old = quester.getQuestData(quest).customObjectiveCounts.get(obj.getName());
				plugin.getInstance().getQuester(player.getUniqueId()).getQuestData(quest).customObjectiveCounts.put(obj.getName(), old + count);
			} else {
				plugin.getInstance().getQuester(player.getUniqueId()).getQuestData(quest).customObjectiveCounts.put(obj.getName(), count);
			}
			int index = -1;
			for (int i = 0; i < quester.getCurrentStage(quest).customObjectives.size(); i++) {
				if (quester.getCurrentStage(quest).customObjectives.get(i).getName().equals(obj.getName())) {
					index = i;
					break;
				}
			}
			if (index > -1) {
				if (quester.getQuestData(quest).customObjectiveCounts.get(obj.getName()) >= quester.getCurrentStage(quest).customObjectiveCounts.get(index)) {
					quester.finishObjective(quest, "customObj", null, null, null, null, null, null, null, null, null, obj);
				}
			}
			quester.saveData();
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof CustomObjective) {
			CustomObjective other = (CustomObjective) o;
			if (!other.name.equals(name)) {
				return false;
			}
			if (!other.author.equals(name)) {
				return false;
			}
			for (String s : other.datamap.keySet()) {
				if (!datamap.containsKey(s)) {
					return false;
				}
			}
			for (Object val : other.datamap.values()) {
				if (!datamap.containsValue(val)) {
					return false;
				}
			}
			for (String s : other.descriptions.keySet()) {
				if (!descriptions.containsKey(s)) {
					return false;
				}
			}
			for (String s : other.descriptions.values()) {
				if (!descriptions.containsValue(s)) {
					return false;
				}
			}
			if (!other.countPrompt.equals(countPrompt)) {
				return false;
			}
			if (!other.display.equals(display)) {
				return false;
			}
			if (other.enableCount != enableCount) {
				return false;
			}
			if (other.showCount != showCount) {
				return false;
			}
			return other.count == count;
		}
		return false;
	}
}