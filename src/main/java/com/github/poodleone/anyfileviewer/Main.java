package com.github.poodleone.anyfileviewer;

import java.awt.Font;

import javax.swing.UIManager;

import com.github.poodleone.anyfileviewer.gui.MainWindow;

/**
 * メインクラス.
 *
 */
public class Main {
	/**
	 * プログラムのエントリポイントです.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		UIManager.put("Table.font", new javax.swing.plaf.FontUIResource("Monospaced", Font.PLAIN, 12));
		new MainWindow().setVisible(true);
	}
}
