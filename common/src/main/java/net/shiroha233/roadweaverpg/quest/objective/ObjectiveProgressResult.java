package net.shiroha233.roadweaverpg.quest.objective;

/**
 * 目标进度检查结果
 * 
 * 设计原理：
 * - 替代原来的int返回值，提供更丰富的语义
 * - 明确区分"跳过"、"无变化"、"有进度"三种情况
 * - 支持携带额外信息（如失败原因）
 */
public abstract class ObjectiveProgressResult {
    
    private ObjectiveProgressResult() {}
    
    /**
     * 跳过更新（事件不匹配或条件不满足）
     * 保持原有进度不变
     */
    public static final class Skip extends ObjectiveProgressResult {
        public static final Skip EVENT_MISMATCH = new Skip("event_mismatch");
        public static final Skip CONDITION_NOT_MET = new Skip("condition_not_met");
        public static final Skip TARGET_MISMATCH = new Skip("target_mismatch");
        public static final Skip LOCATION_MISMATCH = new Skip("location_mismatch");
        
        private final String reason;
        
        public Skip(String reason) {
            this.reason = reason;
        }
        
        public String reason() { return reason; }
    }
    
    /**
     * 进度更新（包含新的进度值）
     * 
     * @param progress 新的进度值（对于累加型目标是增量，对于收集型是绝对值）
     * @param isAbsolute 是否为绝对值（true=直接设置，false=累加）
     */
    public static final class Progress extends ObjectiveProgressResult {
        /** 单次进度（击杀1个、完成1次等） */
        public static final Progress ONE = new Progress(1, false);
        
        private final int progress;
        private final boolean isAbsolute;
        
        public Progress(int progress, boolean isAbsolute) {
            this.progress = progress;
            this.isAbsolute = isAbsolute;
        }
        
        /** 创建累加进度 */
        public static Progress increment(int amount) {
            return new Progress(amount, false);
        }
        
        /** 创建绝对进度 */
        public static Progress absolute(int value) {
            return new Progress(value, true);
        }
        
        public int progress() { return progress; }
        public boolean isAbsolute() { return isAbsolute; }
    }
    
    // ==================== 便捷方法 ====================
    
    /** 是否应该跳过更新 */
    public boolean shouldSkip() {
        return this instanceof Skip;
    }
    
    /** 是否有进度更新 */
    public boolean hasProgress() {
        return this instanceof Progress && ((Progress) this).progress() > 0;
    }
    
    /** 获取进度值（如果是Skip则返回-1） */
    public int getProgressValue() {
        if (this instanceof Progress) {
            return ((Progress) this).progress();
        }
        return -1;
    }
    
    /** 是否为绝对值进度 */
    public boolean isAbsoluteProgress() {
        return this instanceof Progress && ((Progress) this).isAbsolute();
    }
    
    // ==================== 静态工厂方法 ====================
    
    public static ObjectiveProgressResult skip(String reason) {
        return new Skip(reason);
    }
    
    public static ObjectiveProgressResult increment(int amount) {
        return Progress.increment(amount);
    }
    
    public static ObjectiveProgressResult absolute(int value) {
        return Progress.absolute(value);
    }
    
    public static ObjectiveProgressResult one() {
        return Progress.ONE;
    }
}
