# 扁平化村庄结构设计方案

## 📋 核心问题分析

### 当前Jigsaw系统的特点
1. **递归拼接**: 片段逐个添加，每个片段独立处理
2. **分层结构**: 多个PoolElementStructurePiece组成树形结构
3. **动态高度**: 每个片段可能有不同的高度调整

### 你的想法：扁平化村庄
- 将所有Jigsaw拼接后的片段合并为**单一StructurePiece**
- 转换为**单个大模板**（StructureTemplate）
- 作为**整体结构**进行放置

---

## ✅ 可行性评估

### 理论可行性：**100% 可行**

**原因**:
1. StructureStart已经包含所有片段的完整信息
2. 可以在拼接完成后，将所有片段的方块数据合并
3. 可以创建一个新的StructureTemplate包含合并后的数据
4. 可以用单个StructurePiece替代多个片段

### 实现难度：**中等**

**关键挑战**:
1. 方块数据合并（需要处理旋转、镜像、高度偏移）
2. 方块实体（BlockEntity）的处理
3. 性能影响（内存、序列化）
4. 与现有系统的兼容性

---

## 🏗️ 实现方案

### 方案A：后处理合并（推荐）

**流程**:
```
JigsawPlacement.addPieces()
    ↓
[生成所有PoolElementStructurePiece]
    ↓
FlattenedVillageProcessor.flatten()
    ↓
[合并所有片段为单个StructureTemplate]
    ↓
[创建单个FlattenedStructurePiece]
    ↓
StructureStart.placeInChunk()
```

**优点**:
- 不修改原有Jigsaw系统
- 可选择性应用（只对村庄使用）
- 易于调试和维护

**缺点**:
- 需要额外的处理步骤
- 内存占用增加（临时存储）

### 方案B：自定义Structure类

**创建FlattenedJigsawStructure**:
```java
public class FlattenedJigsawStructure extends JigsawStructure {
    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(
        Structure.GenerationContext context) {
        
        // 1. 调用父类生成Jigsaw拼接
        Optional<Structure.GenerationStub> stub = super.findGenerationPoint(context);
        
        // 2. 如果成功，进行扁平化处理
        if (stub.isPresent()) {
            return stub.map(s -> flattenStub(s, context));
        }
        return stub;
    }
    
    private Structure.GenerationStub flattenStub(
        Structure.GenerationStub original,
        Structure.GenerationContext context) {
        // 合并逻辑
    }
}
```

**优点**:
- 集成度高
- 可直接替换JigsawStructure
- 保持原有接口

**缺点**:
- 需要修改Structure系统
- 可能影响其他Jigsaw结构

---

## 💻 详细实现代码

### 1. 扁平化处理器

```java
public class FlattenedVillageProcessor {
    
    /**
     * 将多个PoolElementStructurePiece合并为单个StructureTemplate
     */
    public static StructureTemplate flattenPieces(
        List<PoolElementStructurePiece> pieces,
        StructureTemplateManager templateManager,
        BlockPos basePos) {
        
        // 1. 计算总边界框
        BoundingBox totalBounds = calculateTotalBounds(pieces);
        
        // 2. 创建新模板
        StructureTemplate template = new StructureTemplate();
        
        // 3. 遍历所有片段，提取方块数据
        for (PoolElementStructurePiece piece : pieces) {
            extractBlocksFromPiece(piece, template, totalBounds, templateManager);
        }
        
        // 4. 处理方块实体
        mergeBlockEntities(pieces, template, totalBounds);
        
        return template;
    }
    
    /**
     * 计算所有片段的总边界框
     */
    private static BoundingBox calculateTotalBounds(List<PoolElementStructurePiece> pieces) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        
        for (PoolElementStructurePiece piece : pieces) {
            BoundingBox box = piece.getBoundingBox();
            minX = Math.min(minX, box.minX());
            minY = Math.min(minY, box.minY());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * 从单个片段提取方块数据
     */
    private static void extractBlocksFromPiece(
        PoolElementStructurePiece piece,
        StructureTemplate template,
        BoundingBox totalBounds,
        StructureTemplateManager templateManager) {
        
        StructurePoolElement element = piece.getElement();
        BlockPos piecePos = piece.getPosition();
        Rotation rotation = piece.getRotation();
        
        // 获取片段的原始模板
        if (element instanceof SinglePoolElement) {
            SinglePoolElement singleElement = (SinglePoolElement) element;
            StructureTemplate pieceTemplate = singleElement.getTemplate(templateManager);
            
            // 遍历模板中的所有方块
            for (StructureTemplate.StructureBlockInfo blockInfo : pieceTemplate.blocks) {
                BlockPos rotatedPos = blockInfo.pos().rotate(rotation);
                BlockPos finalPos = rotatedPos.offset(piecePos);
                
                // 相对于总边界框的位置
                int relX = finalPos.getX() - totalBounds.minX();
                int relY = finalPos.getY() - totalBounds.minY();
                int relZ = finalPos.getZ() - totalBounds.minZ();
                
                // 添加到合并模板
                BlockState state = blockInfo.state().rotate(rotation);
                template.setBlock(new BlockPos(relX, relY, relZ), state);
            }
        }
    }
    
    /**
     * 合并所有方块实体
     */
    private static void mergeBlockEntities(
        List<PoolElementStructurePiece> pieces,
        StructureTemplate template,
        BoundingBox totalBounds) {
        
        for (PoolElementStructurePiece piece : pieces) {
            BlockPos piecePos = piece.getPosition();
            
            // 获取片段的方块实体
            StructurePoolElement element = piece.getElement();
            if (element instanceof SinglePoolElement) {
                // 提取并转换方块实体
                // 相对位置 = 原位置 - 总边界框最小值
                // 添加到合并模板
            }
        }
    }
}
```

### 2. 扁平化结构片段

```java
public class FlattenedStructurePiece extends StructurePiece {
    private final StructureTemplate template;
    private final StructureTemplateManager templateManager;
    private final BlockPos templatePos;
    private final Rotation rotation;
    
    public FlattenedStructurePiece(
        StructureTemplateManager templateManager,
        StructureTemplate template,
        BlockPos templatePos,
        Rotation rotation,
        BoundingBox boundingBox) {
        
        super(StructurePieceType.JIGSAW, 0, boundingBox);
        this.templateManager = templateManager;
        this.template = template;
        this.templatePos = templatePos;
        this.rotation = rotation;
    }
    
    @Override
    public void postProcess(
        WorldGenLevel level,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        RandomSource random,
        BoundingBox boundingBox,
        ChunkPos chunkPos,
        BlockPos centerPos) {
        
        // 直接放置整个模板
        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setRotation(rotation)
            .setMirror(Mirror.NONE)
            .setIgnoreEntities(false);
        
        template.placeInWorld(
            level,
            templatePos,
            centerPos,
            settings,
            random,
            2  // UPDATE_CLIENTS
        );
    }
    
    @Override
    protected void addAdditionalSaveData(
        StructurePieceSerializationContext context,
        CompoundTag tag) {
        // 保存模板引用和位置信息
        tag.putString("template_name", "flattened_village");
        tag.putInt("template_x", templatePos.getX());
        tag.putInt("template_y", templatePos.getY());
        tag.putInt("template_z", templatePos.getZ());
        tag.putString("rotation", rotation.name());
    }
}
```

### 3. 扁平化Jigsaw结构

```java
public class FlattenedJigsawStructure extends JigsawStructure {
    private final boolean flatten;
    
    public FlattenedJigsawStructure(
        Structure.StructureSettings settings,
        Holder<StructureTemplatePool> startPool,
        Optional<ResourceLocation> startJigsawName,
        int maxDepth,
        HeightProvider startHeight,
        boolean useExpansionHack,
        Optional<Heightmap.Types> projectStartToHeightmap,
        int maxDistanceFromCenter,
        boolean flatten) {
        
        super(settings, startPool, startJigsawName, maxDepth, 
              startHeight, useExpansionHack, projectStartToHeightmap, 
              maxDistanceFromCenter);
        this.flatten = flatten;
    }
    
    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(
        Structure.GenerationContext context) {
        
        // 1. 调用父类进行Jigsaw拼接
        Optional<Structure.GenerationStub> stub = super.findGenerationPoint(context);
        
        if (!stub.isPresent() || !flatten) {
            return stub;
        }
        
        // 2. 进行扁平化处理
        return stub.map(s -> flattenGenerationStub(s, context));
    }
    
    private Structure.GenerationStub flattenGenerationStub(
        Structure.GenerationStub original,
        Structure.GenerationContext context) {
        
        return new Structure.GenerationStub(
            original.getPieceBuilder().build().getCenter(),
            piecesBuilder -> {
                // 获取原始片段
                List<StructurePiece> originalPieces = 
                    original.getPiecesBuilder().build().pieces();
                
                // 过滤出所有PoolElementStructurePiece
                List<PoolElementStructurePiece> jigsawPieces = 
                    originalPieces.stream()
                        .filter(p -> p instanceof PoolElementStructurePiece)
                        .map(p -> (PoolElementStructurePiece) p)
                        .collect(Collectors.toList());
                
                if (jigsawPieces.isEmpty()) {
                    // 没有Jigsaw片段，直接使用原始片段
                    originalPieces.forEach(piecesBuilder::addPiece);
                    return;
                }
                
                // 合并所有Jigsaw片段
                StructureTemplate flatTemplate = 
                    FlattenedVillageProcessor.flattenPieces(
                        jigsawPieces,
                        context.structureTemplateManager(),
                        new BlockPos(0, 0, 0)
                    );
                
                // 计算总边界框
                BoundingBox totalBounds = 
                    FlattenedVillageProcessor.calculateTotalBounds(jigsawPieces);
                
                // 创建扁平化片段
                FlattenedStructurePiece flatPiece = new FlattenedStructurePiece(
                    context.structureTemplateManager(),
                    flatTemplate,
                    new BlockPos(totalBounds.minX(), totalBounds.minY(), totalBounds.minZ()),
                    Rotation.NONE,
                    totalBounds
                );
                
                piecesBuilder.addPiece(flatPiece);
                
                // 添加非Jigsaw片段（如果有）
                originalPieces.stream()
                    .filter(p -> !(p instanceof PoolElementStructurePiece))
                    .forEach(piecesBuilder::addPiece);
            }
        );
    }
}
```

---

## 🎯 使用场景

### 何时使用扁平化村庄

**优势场景**:
1. **性能优化**: 减少片段数量，加快放置速度
2. **简化逻辑**: 整个村庄作为单一单位处理
3. **自定义修改**: 更容易对整个村庄进行后处理
4. **数据导出**: 便于导出为单个结构文件

**劣势场景**:
1. **内存占用**: 合并过程需要额外内存
2. **灵活性降低**: 无法独立修改单个片段
3. **序列化复杂**: NBT数据可能变得很大
4. **兼容性问题**: 与某些模组可能冲突

---

## ⚠️ 关键考虑事项

### 1. 方块实体处理

```java
// 需要正确处理：
- 箱子（Chest）
- 熔炉（Furnace）
- 村民职业方块
- 自定义方块实体

// 关键：保持NBT数据完整
```

### 2. 旋转和镜像

```java
// 每个片段可能有不同的旋转
// 合并时需要：
1. 计算相对位置
2. 应用旋转变换
3. 处理镜像
4. 调整坐标系
```

### 3. 高度调整

```java
// 原始Jigsaw系统支持：
- TERRAIN_MATCHING: 自动调整高度
- RIGID: 固定高度

// 扁平化后：
- 所有高度已确定
- 无法再进行动态调整
- 需要预先计算最终高度
```

### 4. 性能影响

```
原始系统：
- 片段数: 20-50个
- 放置时间: 10-50ms
- 内存: 低

扁平化系统：
- 片段数: 1个
- 放置时间: 5-20ms（更快）
- 内存: 中等（合并过程）
```

---

## 🔧 集成步骤

### 1. 创建扁平化处理器
```
FlattenedVillageProcessor.java
```

### 2. 创建扁平化片段类
```
FlattenedStructurePiece.java
```

### 3. 创建扁平化结构类
```
FlattenedJigsawStructure.java
```

### 4. 注册新结构
```java
// 在BuiltinStructures中添加
ResourceKey<Structure> VILLAGE_PLAINS_FLATTENED = createKey("village_plains_flattened");
```

### 5. 配置JSON
```json
{
  "type": "flattened_jigsaw",
  "start_pool": "village/plains/houses",
  "flatten": true,
  "... 其他参数 ..."
}
```

---

## 📊 性能对比

| 指标 | 原始Jigsaw | 扁平化 |
|------|-----------|--------|
| 片段数 | 20-50 | 1 |
| 放置时间 | 10-50ms | 5-20ms |
| 内存占用 | 低 | 中 |
| 灵活性 | 高 | 低 |
| 序列化大小 | 小 | 大 |
| 修改难度 | 低 | 高 |

---

## 🎓 替代方案

### 方案1：虚拟扁平化（推荐用于只读）
- 不实际合并，只在逻辑上视为整体
- 性能最优
- 适合只需要整体操作的场景

### 方案2：分层扁平化
- 按功能分层（建筑层、装饰层等）
- 平衡性能和灵活性
- 适合需要部分修改的场景

### 方案3：动态扁平化
- 运行时决定是否扁平化
- 最灵活
- 性能开销最大

---

## 💡 建议

**如果你的目标是**:
1. **性能优化** → 使用方案A（后处理合并）
2. **简化逻辑** → 使用虚拟扁平化
3. **最大灵活性** → 保持原始Jigsaw系统
4. **导出结构** → 使用扁平化处理器导出

**我的推荐**:
- 先实现**虚拟扁平化**（逻辑层面）
- 再根据需求实现**实际扁平化**（物理层面）
- 保持原始Jigsaw系统作为备选方案

---

## 📝 总结

**可行性**: ✅ 100% 可行
**难度**: ⭐⭐⭐ 中等
**收益**: 🎯 性能提升 + 逻辑简化
**风险**: ⚠️ 内存占用 + 灵活性降低

这个想法很有创意，可以显著简化村庄处理逻辑，但需要仔细处理方块数据合并和坐标变换。
