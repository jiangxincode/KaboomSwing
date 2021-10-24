/*
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package aephyr.swing;

import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import aephyr.swing.TreeList;

public class TreeListTest implements Runnable {

	public static void main(String[] args) {
		EventQueue.invokeLater(new TreeListTest());
	}
	
	public void run() {
		
		UIManager.LookAndFeelInfo[] lafArray = UIManager.getInstalledLookAndFeels();
		String[] substanceNames = {"Autumn","BusinessBlackSteel","BusinessBlueSteel","ChallengerDeep","CremeCoffee",
				"Creme","DustCoffee","Dust","EmeraldDusk","Gemini","GraphiteAqua","GraphiteGlass","Graphite","Magellan",
				"MistAqua","Moderate","NebulaBrickWall","Nebula","OfficeBlue2007","OfficeSilver2007","Raven","Sahara","Twilight"};
		String[] lafNames = new String[lafArray.length+substanceNames.length];
		for (int i=lafArray.length; --i>=0;) {
			lafNames[i] = lafArray[i].getName();
			if ("Nimbus".equals(lafNames[i])) {
				try {
					UIManager.setLookAndFeel(lafArray[i].getClassName());
				} catch (Exception x) {
					x.printStackTrace();
				}
			}
		}
		for (int i=0; i<substanceNames.length; i++) {
			lafNames[i+lafArray.length] = ' '+substanceNames[i];
		}
		
		String[] america = {
			"Alabama","Alaska","Arizona","Arkansas","California","Colorado",
			"Connecticut","Delaware","Florida","Georgia","Hawaii","Idaho",
			"Illinois","Indiana","Iowa","Kansas","Kentucky","Louisiana",
			"Maine","Maryland","Massachusetts","Michigan","Minnesota",
			"Mississippi","Missouri","Montana","Nebraska","Nevada",
			"New Hampshire","New Jersey","New Mexico","New York",
			"North Carolina","North Dakota","Ohio","Oklahoma","Oregon",
			"Pennsylvania","Rhode Island","South Carolina","South Dakota",
			"Tennessee","Texas","Utah","Vermont","Virginia","Washington",
			"West Virginia","Wisconsin","Wyoming"
		};
		String[] canada = {
			"Alberta","British Columbia","Manitoba","New Brunswick",
			"Newfoundland and Labrador","Nova Scotia","Ontario",
			"Prince Edward Island","Quebec","Saskatchewan"
		};
		String[] china = {
			"Anhui","Fujian","Gansu","Guangdong","Guangxi","Guizhou","Hainan",
			"Hebei","Heilongjiang","Henan","Hubei","Hunan","Inner Mongolia",
			"Jiangsu","Jiangxi","Jilin","Liaoning","Ningxia","Qinghai","Shaanxi",
			"Shandong","Shanxi","Sichuan","Tibet","Xinjiang","Yunnan","Zhejiang"
		};
		String[] europe = {
			"Albania","Andorra","Armenia","Austria","Azerbaijan","Belarus",
			"Belgium","Bosnia and Herzegovina","Bulgaria","Croatia","Cyprus",
			"Czech Republic","Denmark","Estonia","Finland","France","Georgia",
			"Germany","Greece","Hungary","Iceland","Ireland","Italy","Kazakhstan",
			"Latvia","Liechtenstein","Lithuania","Luxembourg","Macedonia","Malta",
			"Moldova","Monaco","Montenegro","Netherlands","Norway","Poland",
			"Portugal","Romania","Russia","San Marino","Serbia","Slovakia",
			"Slovenia","Spain","Sweden","Switzerland","Turkey","Ukraine",
			"United Kingdom","Vatican City"
		};
		String[] india = {
			"Andaman and Nicobar","Andhra Pradesh","Arunachal Pradesh","Assam",
			"Bihar","Chandigarh","Chhattisgarh","Dadra and Nagar Haveli",
			"Daman and Diu","Delhi","Goa","Gujarat","Haryana","Himachal Pradesh",
			"Jammu and Kashmir","Jharkhand","Karnataka","Kerala","Lakshadweep",
			"Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram",
			"Nagaland","Orissa","Pondicherry","Punjab","Rajasthan","Sikkim",
			"Tamil Nadu","Tripura","Uttarakhand","Uttar Pradesh","West Bengal"
		};
		String[] japan = {
			"Chubu","Chugoku","Hokkaido","Kansai","Kanto","Kyushu","Okinawa",
			"Shikoku","Tohoku"
		};
		String[] misc = {
				"Bree", "Dead Marshes", "Edoras", "Fangorn Forest", "Helm's Deep",
				"Hobbiton", "Isengard", "Lothlorien", "Minas Tirith", "Moria",
				"Pelennor Fields", "Rivendell", "Weathertop"
		};
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		root.add(createNamedListNode("America", america));
		root.add(createNamedListNode("Canada", canada));
		root.add(createNamedListNode("China", china));
		root.add(createNamedListNode("Europe", europe));
		root.add(createNamedListNode("India", india));
		root.add(createNamedListNode("Japan", japan));
		root.add(createListNode(misc));
		
		final TreeList treelist = new TreeList(new DefaultTreeModel(root));
		treelist.setRootVisible(false);
		treelist.setShowsRootHandles(true);
		
		root = new DefaultMutableTreeNode();
		root.add(createNode("America", america));
		root.add(createNode("Canada", canada));
		root.add(createNode("China", china));
		root.add(createNode("Europe", europe));
		root.add(createNode("India", india));
		root.add(createNode("Japan", japan));
		final JTree tree = new JTree(new DefaultTreeModel(root));

		
		final JCheckBox widthCheck = new JCheckBox("Fixed Width:", false);
		widthCheck.setOpaque(false);
		final JSpinner width = new JSpinner(new SpinnerNumberModel(200, 100, 500, 10));
		final JCheckBox heightCheck = new JCheckBox("Fixed Height:", false);
		heightCheck.setOpaque(false);
		final JSpinner height = new JSpinner(new SpinnerNumberModel(20, 10, 100, 1));
		final JCheckBox dragEnabled = new JCheckBox("Drag Enabled", false);
		dragEnabled.setOpaque(false);
		final JCheckBox editable = new JCheckBox("Editable", false);
		editable.setOpaque(false);
		final JComboBox alignment = new JComboBox(new Object[] {"Left","Center","Right"});
		final JComboBox layoutOrientation = new JComboBox(new Object[] {"Horizontal","Vertical"});
		final JComboBox componentOrientation = new JComboBox(new Object[] {"Left to Right","Right to Left"});
		final JComboBox laf = new JComboBox(new DefaultComboBoxModel(lafNames));
		laf.setSelectedItem(UIManager.getLookAndFeel().getName());
		
		Box options = Box.createVerticalBox();
		options.add(dragEnabled);
		options.add(editable);
		options.add(box(null, widthCheck, width));
		options.add(box(null, heightCheck, height));
		options.add(box("Alignment", alignment));
		options.add(box("Layout Orientation", layoutOrientation));
		options.add(box("Component Orientation", componentOrientation));
		options.add(box("Look & Feel", laf));
		options.add(Box.createVerticalGlue());
		
		class Listener implements ActionListener, ChangeListener, ItemListener {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == widthCheck) {
					treelist.setListFixedCellWidth(widthCheck.isSelected() ? (Integer)width.getValue() : -1);
				} else if (e.getSource() == heightCheck) {
					treelist.setListFixedCellHeight(heightCheck.isSelected() ? (Integer)height.getValue() : -1);
				} else if (e.getSource() == dragEnabled) {
					treelist.setDragEnabled(dragEnabled.isSelected());
				} else if (e.getSource() == editable) {
					treelist.setEditable(editable.isSelected());
				} else {
					String cmd = e.getActionCommand();
					if (cmd == "Delete") {
//						Selection[] sel = treelist.getSelection();
//						if (sel == null)
//							return;
//						treelist.stopEditing(); // <<< this needs to be inherent in TreeList ?
//						DefaultTreeModel model = (DefaultTreeModel)treelist.getModel();
//						for (Selection s : sel) {
//							if (s.isListPath()) {
//								ListSelectionModel selection = s.getSelectionModel();
//								DefaultListNode node = (DefaultListNode)s.getModel();
//								DefaultListModel list = (DefaultListModel)node.delegate;
//								int min = selection.getMinSelectionIndex();
//								int max = selection.getMaxSelectionIndex();
//								for (int i=max;i>=min;i--) {
//									if (selection.isSelectedIndex(i)) {
//										list.remove(i);
//									}
//								}
//								if (list.isEmpty()) {
//									TreePath p = s.getPath().getParentPath();
//									System.out.println("removeNodeFromParent:\t"+p);
//									model.removeNodeFromParent((MutableTreeNode)p.getLastPathComponent());
//
//								}
//							} else {
//								model.removeNodeFromParent((MutableTreeNode)s.getPath().getLastPathComponent());
//							}
//						}
					}
				}
			}
			public void stateChanged(ChangeEvent e) {
				if (e.getSource() == width) {
					if (widthCheck.isSelected())
						treelist.setListFixedCellWidth((Integer)width.getValue());
				} else if (e.getSource() == height) {
					if (heightCheck.isSelected())
						treelist.setListFixedCellHeight((Integer)height.getValue());
				}
			}
			
			private void setComponentOrientation(ComponentOrientation o) {
				treelist.getParent().getParent().setComponentOrientation(o);
				treelist.setComponentOrientation(o);
				tree.setComponentOrientation(o);
			}
			
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() != ItemEvent.SELECTED)
					return;
				if (e.getSource() == alignment) {
					Object align = alignment.getSelectedItem();
					if (align == "Left") {
						treelist.setListHorizontalAlignment(SwingConstants.LEFT);
					} else if (align == "Center") {
						treelist.setListHorizontalAlignment(SwingConstants.CENTER);
					} else if (align == "Right") {
						treelist.setListHorizontalAlignment(SwingConstants.RIGHT);
					}
				} else if (e.getSource() == layoutOrientation) {
					Object orientation = layoutOrientation.getSelectedItem();
					if (orientation == "Horizontal") {
						treelist.setListLayoutOrientation(JList.HORIZONTAL_WRAP);
					} else if (orientation == "Vertical") {
						treelist.setListLayoutOrientation(JList.VERTICAL_WRAP);
					}
				} else if (e.getSource() == componentOrientation) {
					Object orientation = componentOrientation.getSelectedItem();
					if (orientation == "Left to Right") {
						setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
					} else if (orientation == "Right to Left") {
						setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
					}
				} else if (e.getSource() == laf) {
					// Changing LAFs should not be done during the middle of an
					// event dispatch, but rather in their own self contained event
					// or else some LAFs will throw fits.
					final String name = laf.getSelectedItem().toString();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							try {
								if (name.charAt(0) == ' ') {
									String cls = "org.pushingpixels.substance.api.skin.Substance"+name.substring(1)+"LookAndFeel";
									UIManager.setLookAndFeel(cls);
								} else {
									for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
										if (name.equals(info.getName())) {
											UIManager.setLookAndFeel(info.getClassName());
											break;
										}
									}
								}
								SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(treelist));
							} catch (Exception x) {
								x.printStackTrace();
							}
						}
					});
				}
			}
		}
		Listener l = new Listener();
		widthCheck.addActionListener(l);
		heightCheck.addActionListener(l);
		dragEnabled.addActionListener(l);
		editable.addActionListener(l);
		width.addChangeListener(l);
		height.addChangeListener(l);
		alignment.addItemListener(l);
		layoutOrientation.addItemListener(l);
		componentOrientation.addItemListener(l);
		laf.addItemListener(l);
		
		JPopupMenu popup = new JPopupMenu();
		popup.add("Delete").addActionListener(l);
		treelist.setComponentPopupMenu(popup);
		
		
		tree.setShowsRootHandles(true);
		tree.setRootVisible(false);
		popup = new JPopupMenu();
		popup.add("Delete").addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				TreePath path = tree.getSelectionPath();
				DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
				model.removeNodeFromParent((MutableTreeNode)path.getLastPathComponent());
			}
		});
		tree.setComponentPopupMenu(popup);
		JList list = new JList(canada);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.setVisibleRowCount(0);
		
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Options", options);
		tabs.addTab("Tree", new JScrollPane(tree));
		tabs.addTab("List", new JScrollPane(list));
		
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, new JScrollPane(treelist));

		JFrame frame = new JFrame("TreeList");
		frame.add(split, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 500);
		split.setDividerLocation(200);
		frame.setVisible(true);
	}
	

	
	static Box box(String title, JComponent ... cs) {
		Box box = Box.createHorizontalBox();
		if (title != null)
			box.setBorder(BorderFactory.createTitledBorder(title));
		for (JComponent c : cs)
			box.add(c);
		box.setAlignmentX(0f);
		Dimension s = box.getPreferredSize();
		s.width = Short.MAX_VALUE;
		box.setMaximumSize(s);
		return box;
	}
	
	static DefaultMutableTreeNode createNode(String name, String ... nodes) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
		for (String n : nodes) {
			node.add(new DefaultMutableTreeNode(n));
		}
		return node;
	}
	
	static DefaultListNode createListNode(String ... nodes) {
		DefaultListModel model = new DefaultListModel();
		for (String n : nodes)
			model.addElement(n);
		return new DefaultListNode(model);
	}
	
	static DefaultMutableTreeNode createNamedListNode(String name, String ... nodes) {
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
		DefaultListNode list = createListNode(nodes);
		list.name = name;
		node.add(list);
		return node;
	}
	

	
	static class DefaultListNode extends DefaultMutableTreeNode implements ListModel {
		public DefaultListNode(ListModel model) {
			super(null, false);
			delegate = model;
		}
		ListModel delegate;

		public void addListDataListener(ListDataListener l) {
			delegate.addListDataListener(l);
		}
		public Object getElementAt(int index) {
			return delegate.getElementAt(index);
		}
		public int getSize() {
			return delegate.getSize();
		}
		public void removeListDataListener(ListDataListener l) {
			delegate.removeListDataListener(l);
		}
		
		String name = "";
		public String toString() {
			return name;
		}

	}

}
