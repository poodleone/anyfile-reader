package com.github.poodleone.anyfileviewer.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

/**
 * ドロップダウンメニューの表示ボタン.
 */
public class DropDownButton extends AbstractButton {
	private static final long serialVersionUID = 1L;

	/**
	 * ドロップダウンメニューの表示ボタンを生成します.
	 * 
	 * @param text ボタンのテキスト
	 * @param menu ドロップダウンメニュー
	 */
	public DropDownButton(String text, JPopupMenu menu) {
		setLayout(new BorderLayout());

		JToggleButton menuButton = new JToggleButton(text);
		menuButton.setPreferredSize(
				new Dimension(menuButton.getMinimumSize().width, menuButton.getPreferredSize().height));
		menuButton.setBorderPainted(false);
		add(BorderLayout.CENTER, menuButton);

		setMaximumSize(getPreferredSize());

		// ボタンが押されたらプルダウンメニューを表示する
		MouseAdapter ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				menuButton.setSelected(true);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				menuButton.setSelected(false);
			}
		};
		menuButton.addMouseListener(ma);
		menuButton.addActionListener(e -> {
			menu.show(menuButton, 0, menuButton.getSize().height);
		});
	}

}
