package test.aephyr.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import aephyr.swing.*;

public class CachingListTest implements Runnable {
	
	public static void main(String[] args) {
		
		SwingUtilities.invokeLater(new CachingListTest());
	}
	
	@Override
	public void run() {
		
		JFrame frame = new JFrame(getClass().getSimpleName());
		
//		final CachingList list = new CachingList(new Model());
//		list.setFixedCellHeight(50);
//		list.setFixedCellWidth(100);
		
		final CachingList list = new VariableHeightsCachingList(new Model());
		
		JButton remove = new JButton("Remove");
		remove.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				Container scroll = list.getParent().getParent();
				scroll.getParent().remove(scroll);
			}
		});
		frame.add(remove, java.awt.BorderLayout.NORTH);
		
		frame.add(new JScrollPane(list));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
	}
	
	private static class Model extends AbstractListModel implements CachingList.Model {
		
		private Random random = new Random();
		
		@Override
		public Object getErrorElementAt(int index) {
			return "Error: "+index;
		}

		@Override
		public Callable<?> getLoaderAt(final int index) {
			return new Callable<String>() {
				public String call() {
					int size = 20 + random.nextInt(20);
					return String.format("<html><div style='font-size:"
							+size+"pt'>%03d</div></html>", index+1);
				}
			};
		}

		@Override
		public Object getLoadingElementAt(int index) {
			return "Loading...";
		}

		@Override
		public String getSearchStringAt(int index) {
			return Integer.toString(index);
		}

		@Override
		public int getSize() {
			return 500;
		}

		@Override
		public Object getElementAt(int index) {
			return null;
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
	
	
	
	
}
