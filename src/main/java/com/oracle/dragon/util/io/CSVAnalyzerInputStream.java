package com.oracle.dragon.util.io;

import org.apache.commons.io.input.ReaderInputStream;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A CSV Reader that analysis the streamed data to detect the *potential* DDL for a relational table to ingest it.
 */
public class CSVAnalyzerInputStream extends FilterReader {

	public static int PROBING_ROWS = 10000;

	protected boolean resetBetweenAnalysis;

	protected long rows;

	protected final List<CSVColumn> columns = new ArrayList<>();

	public String getTableDDL(String tableName) {
		final StringBuilder columnsList = new StringBuilder();

		int i = 0;
		for(CSVColumn col:columns) {
			if(i>0) {
				columnsList.append(", ");
			}
			columnsList.append(col.toString());
			i++;
		}

		return String.format("CREATE TABLE %s (%s)", tableName, columnsList);
	}

	enum CSVType {
		UNKNOWN,
		NUMBER,
		DATE,
		VARCHAR2
	}

	static class CSVColumn {
		final String name;
		CSVType type = CSVType.UNKNOWN;
		int length;
		boolean nullable;

		CSVColumn(StringBuilder name) {
			this.name = name.toString().replace(' ', '_');
			name.setLength(0);
		}

		@Override
		public String toString() {
			return name + " " + type + (length > 0 ? "(" + length + " char)" : "") + (nullable ? "" : " NOT NULL");
		}
	}

	public CSVAnalyzerInputStream() {
		super(new StringReader(""));
	}

	public CSVAnalyzerInputStream(CSVFieldsSeparator fieldSeparator) {
		this();
		this.fieldSeparator = fieldSeparator;
	}

	public CSVAnalyzerInputStream(boolean resetBetweenAnalysis) {
		this();
		this.resetBetweenAnalysis = resetBetweenAnalysis;
	}

	protected CSVAnalyzerInputStream(Reader in) {
		super(in);
	}

	public InputStream analyze(Reader in) {
		this.in = in;
		if(resetBetweenAnalysis) internalReset();
		return new ReaderInputStream(this, Charset.defaultCharset());
	}

	private void internalReset() {
		field.setLength(0);
		fieldSeparator = CSVFieldsSeparator.SEMICOLON;
		endOfRowSequenceAndFieldSeparatorNotDetected = true;
		endOfLineSequence = EndOfRow.LF;
		columnIndex = 0;
		string = false;
		rows = 0;
		columns.clear();
	}

	private final StringBuilder field = new StringBuilder();

	public enum CSVFieldsSeparator {
		PIPE,
		COMMA,
		SEMICOLON
	}

	private CSVFieldsSeparator fieldSeparator = CSVFieldsSeparator.SEMICOLON;

	enum EndOfRow {
		LF, // \n
		CR, // \r
		CRLF // \r\n
	}

	public String getRecordDelimiter() {
		switch(endOfLineSequence) {
			case LF: return "\\n";
			case CR: return "\\r";
			case CRLF: return "\\r\\n";
		}

		return "\\n";
	}


	public String getFieldSeparator() {
		switch(fieldSeparator) {
			case COMMA: return ",";
			case PIPE: return "|";
			case SEMICOLON: return ";";
		}

		return ";";
	}

	private boolean endOfRowSequenceAndFieldSeparatorNotDetected = true;
	private EndOfRow endOfLineSequence = EndOfRow.LF;

	protected int columnIndex = 0;
	protected boolean string = false;

	public int read(final char[] cbuf, final int off, final int len) throws IOException {
		final int result = super.read(cbuf, off, len);

		if (result <= 0) {
			// managing row count for EOF
			if (columnIndex == columns.size() - 1) {
				rows++;
			}
		}

		int lastAntiSlash = -1;

		if (rows > PROBING_ROWS) {
			// still need to count rows...
			for (int i = off; i < result; i++) {
				final char c = cbuf[i];
				switch (c) {
					case '\r':
					case '\n':
						if ((c == '\r' && endOfLineSequence == EndOfRow.CRLF) || (c == '\n' && endOfLineSequence == EndOfRow.CR)) {
							continue;
						}

						columnIndex = 0;
						rows++;
						break;

					case '|':
					case ',':
					case ';':
						if (!string) {
							switch (fieldSeparator) {
								case COMMA:
									if (c == '|' || c == ';') {
										continue;
									}
									break;
								case PIPE:
									if (c == ',' || c == ';') {
										continue;
									}
									break;
								case SEMICOLON:
									if (c == '|' || c == ',') {
										continue;
									}
									break;
							}

							columnIndex++;
						}
						break;

					case '\\':
						lastAntiSlash = i;
						break;

					case '\"':
						if (!string || lastAntiSlash == -1 || lastAntiSlash != i - 1) {
							string = !string;
						}
						break;
				}
			}

			return result;
		}

		if (endOfRowSequenceAndFieldSeparatorNotDetected) {
			int cr = 0;
			int lf = 0;
			int pipe = 0;
			int comma = 0;
			int semicolon = 0;
			for (int i = off; i < result; i++) {
				switch (cbuf[i]) {
					case '\r':
						cr++;
						break;
					case '\n':
						lf++;
						break;
					case '|':
						pipe++;
						break;
					case ',':
						comma++;
						break;
					case ';':
						semicolon++;
						break;
				}
			}

			if(!(pipe == comma && comma == semicolon)) {
				if(pipe > comma && pipe > semicolon) {
					fieldSeparator = CSVFieldsSeparator.PIPE;
				} else
				if(comma > pipe && comma > semicolon) {
					fieldSeparator = CSVFieldsSeparator.COMMA;
				} else
				if(semicolon > comma && semicolon > pipe ) {
					fieldSeparator = CSVFieldsSeparator.SEMICOLON;
				}
			}

			if (cr + lf > 0) {
				if (cr == 0) {
					endOfLineSequence = EndOfRow.LF;
				}
				else {
					if (lf == 0) {
						endOfLineSequence = EndOfRow.CR;
					}
					else {
						endOfLineSequence = EndOfRow.CRLF;
					}
				}
			}

			endOfRowSequenceAndFieldSeparatorNotDetected = false;
		}

		for (int i = off; i < result; i++) {
			final char c = cbuf[i];

			switch (c) {
				case '\ufeff':
					// utf-16 mark
					break;
				case '\r':
				case '\n':
					if ((c == '\r' && endOfLineSequence == EndOfRow.CRLF) || (c == '\n' && endOfLineSequence == EndOfRow.CR)) {
						continue;
					}

					if (rows == 0) {
						columns.add(new CSVColumn(field));
					}
					else {
						final String fieldAsString = field.toString();
						final CSVColumn column = columns.get(columnIndex);
						if (fieldAsString.length() > 0) {
							column.type = getType(fieldAsString);
							if (column.type == CSVType.VARCHAR2) {
								column.length = Math.max(column.length, fieldAsString.length());
							}
							//System.out.println("Line " + rows + ": field end of row: " + field);
							field.setLength(0);
						}
						else {
							column.nullable = true;
						}
						columnIndex = 0;
					}
					rows++;
					break;

				case '|':
				case ',':
				case ';':
					if (string) {
						field.append(cbuf[i]);
					}
					else {

						switch (fieldSeparator) {
							case COMMA:
								if (c == '|' || c == ';') {
									field.append(c);
									continue;
								}
								break;
							case PIPE:
								if (c == ',' || c == ';') {
									field.append(c);
									continue;
								}
								break;
							case SEMICOLON:
								if (c == '|' || c == ',') {
									field.append(c);
									continue;
								}
								break;
						}

						if (rows == 0) {
							columns.add(new CSVColumn(field));
						}
						else {
							final String fieldAsString = field.toString();
							final CSVColumn column = columns.get(columnIndex);
							if (fieldAsString.length() > 0) {
								column.type = getType(fieldAsString);
								if (column.type == CSVType.VARCHAR2) {
									column.length = Math.max(column.length, fieldAsString.length());
								}
								//System.out.println("field ; separator: " + field);
								field.setLength(0);
							}
							else {
								column.nullable = true;
							}
							columnIndex++;
						}
					}
					break;

				case '\\':
					lastAntiSlash = i;
					field.append('\\');
					break;

				case '\"':
					if (string && lastAntiSlash >= 0 && lastAntiSlash == i - 1) {
						field.append(c);
					}
					else {
						string = !string;
					}
					break;

				default:
					field.append(c);
					break;
			}
		}

		return result;
	}

	protected final NumberFormat nF = NumberFormat.getNumberInstance();
	protected final DateFormat dF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private CSVType getType(String s) {
		ParsePosition parsePosition;

		nF.parse(s, parsePosition = new ParsePosition(0));

//		System.out.println("typof "+s+": "+parsePosition.getIndex());

		if (parsePosition.getIndex() == s.length()) {
			return CSVType.NUMBER;
		}
		else {
			dF.parse(s, parsePosition = new ParsePosition(0));

			if (parsePosition.getIndex() == s.length()) {
				return CSVType.DATE;
			}
			else {
				return CSVType.VARCHAR2;
			}
		}
	}

	public long getRows() {
		return rows - 1;
	}

	public List<CSVColumn> getHeaderColumns() {
		return columns;
	}

	public static void main(String[] args) throws Throwable {
		final CSVAnalyzerInputStream csvAnalyzerInputStream = new CSVAnalyzerInputStream(true);

		try (InputStream inputStream = new BufferedInputStream(csvAnalyzerInputStream.analyze(new InputStreamReader(new BufferedInputStream(new FileInputStream("test.csv"), 1024 * 1024))))) {
			Files.copy(inputStream, new File("hey.csv").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		System.out.println(csvAnalyzerInputStream.getRows());
		for (CSVColumn col : csvAnalyzerInputStream.getHeaderColumns()) {
			System.out.println("Column: " + col);
		}

		try (InputStream inputStream = new BufferedInputStream(csvAnalyzerInputStream.analyze(new InputStreamReader(new BufferedInputStream(new FileInputStream("customer_names.csv"), 1024 * 1024))))) {
			Files.copy(inputStream, new File("customer_names_hey.csv").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		System.out.println(csvAnalyzerInputStream.getRows());
		for (CSVColumn col : csvAnalyzerInputStream.getHeaderColumns()) {
			System.out.println("Column: " + col);
		}

		try (InputStream inputStream = new BufferedInputStream(csvAnalyzerInputStream.analyze(new InputStreamReader(new BufferedInputStream(new FileInputStream("credit_scoring_100k.csv"), 1024 * 1024))))) {
			Files.copy(inputStream, new File("credit_scoring_100k_hey.csv").toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		System.out.println(csvAnalyzerInputStream.getRows());
		for (CSVColumn col : csvAnalyzerInputStream.getHeaderColumns()) {
			System.out.println("Column: " + col);
		}
	}
}
