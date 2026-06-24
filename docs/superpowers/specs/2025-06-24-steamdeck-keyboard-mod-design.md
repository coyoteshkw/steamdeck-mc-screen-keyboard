# Steam Deck Keyboard Mod — 设计文档

**目标平台**：Minecraft 1.21.1, NeoForge  
**创建日期**：2025-06-24  
**状态**：设计稿（待审查）

---

## 1. 动机与目标

在 Steam Deck 上用映射的键盘按键玩 Minecraft Java 版时，输入中文或英文文本需要频繁弹出系统键盘，体验不佳。本 mod 提供一个 MC 原生风格的触屏虚拟键盘：

- 按自定义按键打开/关闭
- 聊天栏和 JEI/EMI 搜索栏自动弹出
- 纯触摸点击交互
- 浮动窗口，可拖拽，可收起

**不做**：手柄适配、摇杆导航（用户已在 Steam Deck 上映射键盘按键）、系统键盘集成、中文输入法、动画效果。

---

## 2. 功能范围

| 功能 | 说明 |
|------|------|
| 手动开关 | 按可自定义按键（默认 K），切换键盘显示/隐藏 |
| 自动弹出 | 当以下场景的文本框获得焦点时自动弹出：聊天栏、JEI/EMI 搜索栏。其他 EditBox 场景可配置是否自动弹出 |
| 字符输入 | 字母 a-z、数字 0-9、空格、退格、回车 |
| Shift 层 | Shift 切换大小写 + 符号（`!@#$%^&*()_+-=[]{};:'",./<>?`），单次 Shift（按下后自动取消）和 Shift Lock（双击锁定） |
| 符号 | MC 常用：`/` `@` `#` `:` `_`；JEI/EMI 常用：`$` `%` `&` `\|` `!` |
| 拖拽 | 长按键盘标题栏/背景拖拽移动位置，坐标持久化 |
| 收起按钮 | 键盘角落有一个 `×` 按钮，点击收起键盘 |

---

## 3. 非功能范围（明确不做）

- ❌ 手柄/摇杆导航键盘
- ❌ 动画/音效反馈
- ❌ 中文输入法/IME
- ❌ 多语言键盘布局（仅 QWERTY）
- ❌ 系统级键盘集成
- ❌ Ctrl+C/V 快捷键
- ❌ Tab 补全
- ❌ 方向键
- ❌ 粘贴/复制功能

---

## 4. 架构

### 4.1 整体结构

```
steamdeck-keyboard/
├── build.gradle
├── gradle.properties
├── src/main/
│   ├── java/com/steamdeck/keyboard/
│   │   ├── SteamDeckKeyboard.java          # @Mod 入口
│   │   ├── config/
│   │   │   └── KeyboardConfig.java         # NeoForge ModConfigSpec
│   │   ├── keyboard/
│   │   │   ├── KeyboardScreen.java         # Screen 覆盖层，承载键盘
│   │   │   ├── KeyboardWidget.java         # 键盘布局容器
│   │   │   ├── KeyWidget.java              # 单个按键控件
│   │   │   └── KeyboardLayout.java         # 布局定义（QWERTY）
│   │   ├── input/
│   │   │   ├── InputSender.java            # 将按键事件注入 EditBox
│   │   │   └── EditBoxTracker.java         # 追踪 EditBox 焦点状态
│   │   └── mixin/
│   │       ├── ChatScreenMixin.java        # 聊天栏自动弹出 + 输入注入
│   │       ├── EditBoxMixin.java           # EditBox 焦点追踪
│   │       └── ScreenMixin.java            # 键盘按键事件处理
│   └── resources/
│       └── META-INF/
│           └── neoforge.mods.toml
```

### 4.2 组件职责

| 组件 | 职责 |
|------|------|
| `SteamDeckKeyboard` | Mod 入口，注册配置、按键绑定、事件监听 |
| `KeyboardConfig` | 可配置项：手动按键绑定、自动弹出开关、键盘位置 |
| `KeyboardScreen` | 覆盖在现有 Screen 上的透明层，承载 KeyboardWidget，支持拖拽 |
| `KeyboardWidget` | 将 KeyboardLayout 渲染成按键网格，管理 Shift 状态，分发触摸到 KeyWidget |
| `KeyWidget` | 单个按键渲染（MC 风格精灵图），处理 `mouseClicked` 触摸事件 |
| `KeyboardLayout` | 定义 QWERTY 布局的行列和按键内容 |
| `InputSender` | 将按键字符/操作（insertText, delete, enter）注入到目标 EditBox |
| `EditBoxTracker` | 监听当前 Screen 上所有 EditBox 的焦点变化，决定是否自动弹出 |
| `ChatScreenMixin` | 注入到 ChatScreen，提供 InputTarget 接口 + 键盘弹出时上移输入框 |
| `EditBoxMixin` | 注入到 EditBox，在焦点获得时通知 EditBoxTracker |
| `ScreenMixin` | 注入到 Screen，转发热键事件给 mod |

### 4.3 技术参考

本设计参考 Controlify 的键盘实现，但大幅简化：

- **Controlify 提供而我们需要的**：KeyboardWidget/KeyWidget 的列对齐布局、KeyFunction 类型系统（字符串/键码/特殊动作）、InputTarget 接口、键盘覆盖 Screen 模式、ChatScreen 上移处理
- **Controlify 有但我们不需要的**：完整的 ScreenProcessor 系统、手柄导航、ControllerEntity 依赖、多布局切换、键盘快捷键绑定、光标移动、粘贴复制

---

## 5. 键盘布局

单一 QWERTY 布局，两层（正常/Shift）：

### 正常层

```
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬───────────┐
│  q  │  w  │  e  │  r  │  t  │  y  │  u  │  i  │  o  │  p  │  ← (退格) │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼───────────┤
│  a  │  s  │  d  │  f  │  g  │  h  │  j  │  k  │  l  │  /  │     ↵     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤  (回车)   │
│  ⇧  │  z  │  x  │  c  │  v  │  b  │  n  │  m  │  ,  │  .  │           │
├─────┴─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤           │
│   空格    │  @  │  #  │  $  │  %  │  !  │  :  │  _  │  &  │     ×     │
│           ├─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┤  (收起)   │
│           │            特殊符号（JEI/EMI 常用）            │           │
└───────────┴───────────────────────────────────────────────┴───────────┘
```

- 第 1 行：10 个字母 + 退格键
- 第 2 行：9 个字母 + `/` + 回车键
- 第 3 行：Shift + 7 个字母 + `,` + `.`
- 第 4 行：空格（占据 4 个单位宽度）+ 8 个特殊符号 + 收起按钮

### Shift 层

```
┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬───────────┐
│  Q  │  W  │  E  │  R  │  T  │  Y  │  U  │  I  │  O  │  P  │  ← (退格) │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼───────────┤
│  A  │  S  │  D  │  F  │  G  │  H  │  J  │  K  │  L  │  ?  │     ↵     │
├─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤  (回车)   │
│[锁定]│  Z  │  X  │  C  │  V  │  B  │  N  │  M  │  ;  │  '  │           │
├─────┴─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┤           │
│   空格    │  (  │  )  │  -  │  +  │  =  │  [  │  {  │  }  │     ×     │
│           ├─────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┤  (收起)   │
│           │               更多符号                          │           │
└───────────┴───────────────────────────────────────────────┴───────────┘
```

特殊说明：
- Shift 行为：单击 Shift → 切换到 Shift 层，按任意字符键后自动回到正常层
- Shift Lock：在已 Shift 状态下再次点击 Shift → 锁定 Shift 层（按键显示"锁定"/高亮），再次点击取消
- 退格和回车在两层中保持不变

---

## 6. 数据流

### 6.1 手动触发流程

```
用户按 K 键
  → ScreenMixin 拦截按键事件
  → SteamDeckKeyboard.onKeyPress() 判断是否为键盘快捷键
  → 如果键盘未打开 → 创建 KeyboardScreen，覆盖当前 Screen
  → 如果键盘已打开 → 关闭 KeyboardScreen，恢复底层 Screen
```

### 6.2 自动弹出流程

```
EditBox.setFocused(true) 被调用
  → EditBoxMixin 检测焦点获得事件
  → EditBoxTracker 判断当前 EditBox 是否属于聊天栏/JEI搜索栏/其他
  → 根据配置决定是否自动弹出
  → 如果自动弹出 → 创建 KeyboardScreen
  → 同时绑定该 EditBox 为 InputTarget
```

### 6.3 按键输入流程

```
用户触摸 KeyWidget
  → KeyWidget.mouseClicked() 触发
  → KeyWidget 解析按键类型（字符/退格/回车/Shift）
  → InputSender:
      - 字符: targetEditBox.insertText() 或 charTyped()
      - 退格: 模拟 Backspace 键
      - 回车: 模拟 Enter 键
      - Shift: 切换 KeyboardWidget 的 shifted 状态，重新渲染
  → KeyboardWidget 重新渲染显示新状态
```

### 6.4 键盘拖拽流程

```
用户在 KeyboardScreen 非按键区域按下鼠标
  → 记录初始位置和初始鼠标坐标
  → 鼠标拖动时更新键盘位置
  → 用户松开时保存位置到 KeyboardConfig → 持久化到配置文件
```

---

## 7. 配置项

使用 NeoForge `ModConfigSpec`，存储在客户端配置文件中。

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `keyboardKey` | `KeyMapping` | `K` (GLFW_KEY_K) | 手动打开/关闭键盘的按键 |
| `autoOpenChat` | `boolean` | `true` | 聊天栏获得焦点时自动弹出 |
| `autoOpenSearch` | `boolean` | `true` | JEI/EMI 搜索栏获得焦点时自动弹出 |
| `autoOpenOthers` | `boolean` | `false` | 其他 EditBox 获得焦点时自动弹出 |
| `keyboardX` | `int` | `-1` | 键盘左上角 X 坐标（-1 表示居中） |
| `keyboardY` | `int` | `-1` | 键盘左上角 Y 坐标（-1 表示底部偏上） |
| `keyboardWidth` | `int` | `-1` | 键盘宽度（-1 表示屏幕宽度的 90%） |
| `keyboardHeight` | `int` | `-1` | 键盘高度（-1 表示屏幕高度的 30%） |
| `opacity` | `double` | `0.85` | 键盘背景不透明度（0.0-1.0） |

---

## 8. 关键实现细节

### 8.1 KeyboardScreen

- 继承 `Screen`，但**不**调用 `Minecraft.setScreen()`，而是像 Controlify 一样使用 `forceSetScreen` 模式
- 渲染时先渲染底层 Screen，再在其上方渲染半透明背景 + 键盘
- 处理鼠标点击：点击在按键上 → 传递给 KeyWidget；点击在键盘空白区域 → 开始拖拽；点击在键盘外部 → 关闭键盘（可配置）
- 有一个 `×` 收起按钮在键盘右上角

### 8.2 KeyboardScreen 与底层 Screen 共存

参考 Controlify 的 `KeyboardOverlayScreen`：
- 使用 Mixin 钩住 `Minecraft.setScreen()` 和 `Screen.removed()`
- 打开 KeyboardScreen 时：
  - 保存当前 Screen 引用
  - 创建 KeyboardScreen 作为覆盖层
  - 阻止底层 Screen 的 `removed()` 被调用
- 关闭 KeyboardScreen 时：
  - 恢复底层 Screen
  - 不重新初始化底层 Screen

### 8.3 聊天栏输入框上移

当聊天栏打开键盘时，输入框需要上移以避免被键盘遮挡：
- 通过 `ChatScreenMixin` 修改聊天输入框的 Y 坐标
- 键盘关闭时恢复原始位置
- 键盘 Y 位置 + 高度 > 输入框原始 Y 时触发上移

### 8.4 JEI/EMI 搜索栏适配

- JEI 使用的搜索栏是 `TextFieldWidget`（或其内部 EditBox）
- EMI 使用类似的 EditBox 组件
- 因为采用方案 A（Mixin 钩住所有 EditBox 焦点事件），自动弹出是通用的，不需要单独适配
- 只需在 `EditBoxTracker` 中正确识别 JEI/EMI 的屏幕类名来判断场景类型

### 8.5 键盘精灵图

MC 原生风格按键：
- 默认状态：石头纹理背景（类似原版按钮 `widget/scroller_background`）
- 按下状态：木板纹理背景（稍亮）
- 文字：使用 Minecraft 的 `Font` 渲染，颜色 `0xFFFFFFFF`
- 按键间距：2px
- 按键圆角：无圆角（像素风格）

---

## 9. 构建与依赖

| 项目 | 规格 |
|------|------|
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge (最新稳定版) |
| Java | 21 |
| Gradle | ModDevGradle 2.x |
| 构建系统 | Gradle 8.8+ |
| Mixin | 需要 Mixin 支持（NeoForge 内置） |

---

## 10. 测试策略

| 测试类型 | 内容 |
|----------|------|
| 功能测试 | 手动按键 K 打开/关闭键盘；按键点击输入字符；Shift 切换；退格删除；回车发送 |
| 自动弹出测试 | 打开聊天栏 → 键盘自动弹出；关闭聊天栏 → 键盘自动收起；JEI/EMI 搜索栏同 |
| 拖拽测试 | 拖拽键盘到不同位置，关闭后重新打开位置保持 |
| 配置测试 | 修改配置文件中的快捷键、自动弹出开关、默认位置 |
| 兼容性测试 | 与 JEI、EMI 同时安装时自动弹出正常；不影响原版游戏其他功能 |

---

## 11. 风险与缓解

| 风险 | 缓解 |
|------|------|
| NeoForge Mixin 与 Fabric Mixin 差异 | 使用 NeoForge 原生 Mixin 机制，参考 NeoForge 文档 |
| `Minecraft.setScreen()` 修改影响其他 mod | 使用 Controlify 的已验证方案：forceSetScreen + 阻止 removed() |
| 键盘布局在不同屏幕分辨率下变形 | 使用 Controlify 的列对齐 + 动态宽度计算方案 |
| JEI/EMI 更新后搜索栏类名变化 | 使用通用 EditBox 焦点检测而非硬编码类名 |
