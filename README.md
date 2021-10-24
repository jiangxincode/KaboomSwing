# KaboomSwing

Based on aephyr: <https://code.google.com/archive/p/aephyr/>

Swing components, accessories, utilities, etc for use in a GUI

## CachingList & CachingTable

JList & JTable extensions that manage the loading and cache of their data.

### Features

#### Cache Size
* The cache size can be customized by specifying a threshold float value between 0.0f and Float.POSITIVE\_INFINITY:
    * 0.0f: Only the visible data is cached
    * 1.0f: The visible data and one block increment in both directions is cached
    * Float.POSITIVE\_INFINITY: All data remains cached once loaded

#### Delayed Loading
* Loading is delayed when the view is undergoing a constant scroll by:
    * Dragging of the JScrollBar thumb
    * Mouse wheel scroll
    * Repeated key presses that induce scrolling (e.g. VK\_DOWN)

## TreeMenuBar

A breadcrumb component that combines JTree & JMenuBar. The structure of the breadcrumb is determined by a TreeModel and a TreePath. Each component in the TreePath has it's own JMenu in the JMenuBar. The popup for each JMenu contains the JTree with its root set to the node for the specified JMenu.

## TreeTable

### Features

#### Tree-aware Drag & Drop
* Provides visual feedback for where a node will be inserted when dragging over the tree column
* All properties of JTable.DropLocation & JTree.DropLocation are included in TreeTable.DropLocation
* Expands nodes that are hovered over similar to JTree but only if the location is over the "path bounds"

#### Sorting & Filtering
* Enable sorting by calling setAutoCreateRowSorter(true)
* DefaultTreeTableSorter allows sorting & filtering of all nodes (Table Header Sorting) and single nodes (Node Specific Sorting)

#### JTree-style Row Heights
* When the row height property is less than or equal to zero, the height of each row is determined by the greatest preferred height of all cell renderers for that row

#### Row Focus
* Enable row focus by calling setColumnFocusEnabled(false)
* The entire row is rendered with the focus border
* JTable's or JTree's key binding actions will be used for VK\_LEFT, VK\_RIGHT, VK\_HOME, VK\_END when column focus is enabled or disabled, respectively

#### Keyboard Selection
* Selects the next row that matches the typed String
* Similar to JTree but it acts upon the focused column or the tree column if column focus is disabled

## MnemonicManager

MnemonicManager determines unique mnemonics for components registered with it. Only components that are actively showing on the screen or components that have the client property key MnemonicManager.IGNORE_SHOWING set to true are considered. Control over the selection process can be custimized via the Mnemonic interface's getPriority(JComponent c) and getPreferredMnemonic(JComponent c, int index) methods. The DefaultMnemonic implementation uses the following order for determining preferred mnemonics: Uppercase letters, lowercase consonants, lowercase vowels, numbers.

### Features

#### Extended Modifiers
* Specify addition modifiers that will trigger the mnemonic's action
* If the extended modifier is 0, then the action will occur when no modifier keys are pressed (mainly for JOptionPane purposes)

#### Generate Mnemonics for JOptionPane
* Use MnemonicsGenerator.registerDialogMnemonicGenerator(JComponent c) to automatically generate mnemonics for JOptionPane dialogs, where c is a JComponent sent as JOptionPane's message parameter.

#### Automatic Updates
* Mnemonics will be reconfigured if a component's text changes
* Mnemonics will automatically be added or removed for JTabbedPane & JMenuBar as tabs & menus are added or removed

#### TitledBorderMnemonic
* A Mnemonic implementation for TitledBorders

## License

* GNU Lesser GPL <http://www.gnu.org/licenses/lgpl.html>