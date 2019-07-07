package com.github.poodleone.anyfileviewer.record;

import java.nio.file.Path;
import java.util.ArrayList;

import com.github.poodleone.anyfileviewer.RecordFormat;

/**
 * レコードの集合を表すクラスです.
 */
public class RecordSet extends ArrayList<Record> {
	private static final long serialVersionUID = -1L;

	/** レコードの取得元ファイルのパス. */
	private Path path;

	/** ファイル末尾まで読込済みかどうか. */
	private boolean eof;

	/** 先頭レコードのファイル内でのオフセット */
	private int offset;

	/** レコード形式 */
	private RecordFormat format;

	/**
	 * @return レコードの取得元ファイルのパス
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @param path レコードの取得元ファイルのパス
	 */
	public void setPath(Path path) {
		this.path = path;
	}

	/**
	 * @return ファイル末尾まで読込済みかどうか
	 */
	public boolean eof() {
		return eof;
	}

	/**
	 * @param eof ファイル末尾まで読込済みかどうか
	 */
	public void setEof(boolean eof) {
		this.eof = eof;
	}

	/**
	 * @return 先頭レコードのファイル内でのオフセット
	 */
	public int getOffeset() {
		return offset;
	}

	/**
	 * @param offset 先頭レコードのファイル内でのオフセット
	 */
	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * @return レコード形式
	 */
	public RecordFormat getFormat() {
		return format;
	}

	/**
	 * @param format レコード形式
	 */
	public void setFormat(RecordFormat format) {
		this.format = format;
	}

}
