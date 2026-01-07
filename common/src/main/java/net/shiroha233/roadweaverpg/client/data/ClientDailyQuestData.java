package net.shiroha233.roadweaverpg.client.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端每日委托数据缓存
 * 存储从服务端同步的每日委托信息
 */
public class ClientDailyQuestData {
    
    private static final ClientDailyQuestData INSTANCE = new ClientDailyQuestData();
    
    private List<ResourceLocation> dailyQuestIds = new ArrayList<>();
    private String refreshDate = "";
    private int timeUntilRefresh = 0;
    private long lastUpdateTime = 0;
    
    private ClientDailyQuestData() {}
    
    public static ClientDailyQuestData getInstance() {
        return INSTANCE;
    }
    
    /**
     * 更新每日委托数据
     */
    public void update(List<ResourceLocation> ids, String date, int timeUntilRefresh) {
        this.dailyQuestIds = new ArrayList<>(ids);
        this.refreshDate = date;
        this.timeUntilRefresh = timeUntilRefresh;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * 获取每日委托ID列表
     */
    public List<ResourceLocation> getDailyQuestIds() {
        return Collections.unmodifiableList(dailyQuestIds);
    }
    
    /**
     * 检查委托是否为今日每日委托
     */
    public boolean isDailyQuest(ResourceLocation questId) {
        return dailyQuestIds.contains(questId);
    }
    
    /**
     * 获取刷新日期
     */
    public String getRefreshDate() {
        return refreshDate;
    }
    
    /**
     * 获取距离下次刷新的剩余时间（秒）
     * 会根据本地时间流逝进行估算
     */
    public int getTimeUntilRefresh() {
        if (lastUpdateTime == 0) return timeUntilRefresh;
        long elapsed = (System.currentTimeMillis() - lastUpdateTime) / 1000;
        return Math.max(0, timeUntilRefresh - (int) elapsed);
    }
    
    /**
     * 格式化剩余时间为可读字符串
     */
    public String getFormattedTimeUntilRefresh() {
        int seconds = getTimeUntilRefresh();
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }
    
    /**
     * 清除缓存
     */
    public void clear() {
        dailyQuestIds.clear();
        refreshDate = "";
        timeUntilRefresh = 0;
        lastUpdateTime = 0;
    }
}
