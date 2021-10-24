package aephyr.swing;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import aephyr.swing.*;
import aephyr.swing.cache.CachingList;

public class CachingListTest implements Runnable, ActionListener, ListSelectionListener {
	
	private static final int COLUMNS = 8;
	
	private static final int LOADING_DELAY = 100;
	
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new CachingListTest());
	}
	
	private Dimension size;
	
	@Override
	public void run() {
		
		JFrame frame = new JFrame(getClass().getSimpleName());
		
		final CachingList list = new CachingList(new Model(500)) {
			public Dimension getPreferredScrollableViewportSize() {
				if (size != null)
					return new Dimension(size);
				return super.getPreferredScrollableViewportSize();
			}
		};
		list.setCustomLoading(true);
		list.setFixedCellHeight(50);
		list.setFixedCellWidth(100);
		size = list.getPreferredScrollableViewportSize();
		size.width *= COLUMNS;
		list.setVisibleRowCount(0);
		list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		list.addListSelectionListener(this);
		list.registerKeyboardAction(this,
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
				JComponent.WHEN_FOCUSED);
		
		status = new JLabel("0");
//		final CachingList list = new VariableHeightsCachingList(new Model());
		
//		JButton remove = new JButton("Remove");
//		remove.addActionListener(new java.awt.event.ActionListener() {
//			public void actionPerformed(java.awt.event.ActionEvent e) {
//				Container scroll = list.getParent().getParent();
//				scroll.getParent().remove(scroll);
//			}
//		});
//		frame.add(remove, java.awt.BorderLayout.NORTH);
		
		frame.add(new JScrollPane(list,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		frame.add(status, BorderLayout.SOUTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
//		frame.setSize(frame.getWidth()+100, frame.getHeight());
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
	}
	
	private JLabel status;
	
	private static class Model extends AbstractListModel implements CachingList.Model {
		
		Model(int size) {
			List<Integer> lst = list = new ArrayList<Integer>(size);
			for (int i=0; i<size;)
				lst.add(new Integer(++i));
		}
		
		private List<Integer> list;
		
		private Random random = new Random();
		
		@Override
		public Object getErrorElementAt(int index) {
			return "Error: "+index;
		}

		@Override
		public Callable<?> getLoaderAt(int index) {
			final Integer num = list.get(index);
			return new Callable<String>() {
				public String call() {
					try {
						Thread.sleep(random.nextInt(LOADING_DELAY));
					} catch (Exception e) {}
					int size = 20 + random.nextInt(20);
					return String.format("<html><div style='font-size:"
							+size+"pt'>%03d</div></html>", num);
				}
			};
		}

		@Override
		public Object getLoadingElementAt(int index) {
			return "Loading...";
		}

		@Override
		public String getSearchStringAt(int index) {
			return list.get(index).toString();
		}

		@Override
		public int getSize() {
			return list.size();
		}

		@Override
		public Object getElementAt(int index) {
			return list.get(index);
		}
		
		void remove(int index) {
			list.remove(index);
			fireIntervalRemoved(this, index, index);
		}
		
		void remove(int firstIndex, int lastIndex) {
			list.subList(firstIndex, lastIndex+1).clear();
			fireIntervalRemoved(this, firstIndex, lastIndex);
		}
		
	}
	
	
	private static class VariableHeightsCachingList extends CachingList {
		
		VariableHeightsCachingList(Model model) {
			super(model);
		}
		
		RendererAdapter rendererAdapter;
		
		public void setCellRenderer(ListCellRenderer renderer) {
			if (rendererAdapter != null) {
				rendererAdapter.dispose(this);
				rendererAdapter = null;
			}
			super.setCellRenderer(renderer);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (rendererAdapter == null)
						setRendererAdapter();
				}
			});
		}
		
		private void setRendererAdapter() {
			rendererAdapter = new RendererAdapter(this);
			super.setCellRenderer(rendererAdapter);
		}
		
		public void updateUI() {
			if (rendererAdapter != null) {
				super.setCellRenderer(rendererAdapter.renderer);
				rendererAdapter.dispose(this);
				rendererAdapter = null;
			}
			super.updateUI();
			setRendererAdapter();
		}
		
		
		
		private static class RendererAdapter extends JComponent
				implements ListCellRenderer, ListDataListener {
			
			RendererAdapter(JList list) {
				setLayout(new BorderLayout());
				renderer = list.getCellRenderer();
				heights = new SizeSequence(list.getModel().getSize());
				list.getModel().addListDataListener(this);
			}
			
			private ListCellRenderer renderer;
			
			private SizeSequence heights;
			
			private int index;
			
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component c = renderer.getListCellRendererComponent(
						list, value, index, isSelected, cellHasFocus);
				if (c.getParent() != this)
					add(c, BorderLayout.CENTER);
				this.index = index;
				return this;
			}
			
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				int height = heights.getSize(index);
				if (height > 0) {
					size.height = height;
				} else {
					heights.setSize(index, size.height);
				}
				return size;
			}

			@Override
			public void contentsChanged(ListDataEvent e) {
				for (int i=e.getIndex0(), j=e.getIndex1(); i<=j; i++) {
					heights.setSize(i, 0);
				}
			}

			@Override
			public void intervalAdded(ListDataEvent e) {
				heights.insertEntries(e.getIndex0(), e.getIndex1()-e.getIndex0()+1, 0);
			}

			@Override
			public void intervalRemoved(ListDataEvent e) {
				heights.removeEntries(e.getIndex0(), e.getIndex1()-e.getIndex0()+1);
			}

			
			void dispose(JList list) {
				list.getModel().removeListDataListener(this);
			}
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent e) {
		CachingList list = (CachingList)e.getSource();
		Model mdl = (Model)list.getLoadingModel();
		int idx = list.getLeadSelectionIndex();
		if (idx >= 0) {
			status.setText(mdl.getElementAt(idx).toString());
		} else {
			status.setText(null);
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		CachingList list = (CachingList)e.getSource();
		ListSelectionModel sel = list.getSelectionModel();
		int max = sel.getMaxSelectionIndex();
		if (max < 0)
			return;
		int min = sel.getMinSelectionIndex();
		Model mdl = (Model)list.getLoadingModel();
		int idx = min;
		if (max == min) {
			mdl.remove(max);
		} else {
			int last = max;
			for (int i=max; i>=min; i--) {
				if (sel.isSelectedIndex(i)) {
//					FolderManager.delete((Data)mdl.getElementAt(i));
					if (last < 0)
						last = i;
				} else {
					idx++;
					if (last >= 0) {
						mdl.remove(i+1, last);
						last = -1;
					}
				}
			}
			mdl.remove(min, last);
		}
		if (idx < mdl.getSize()) {
			list.setSelectedIndex(idx);
			list.ensureIndexIsVisible(idx);
		}
		
	}
	
	
}
