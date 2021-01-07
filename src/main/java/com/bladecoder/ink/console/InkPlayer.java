package com.bladecoder.ink.console;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import com.bladecoder.ink.runtime.Choice;
import com.bladecoder.ink.runtime.Story;

public class InkPlayer {

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLACK = "\u001B[30m";
	public static final String ANSI_RED = "\u001B[31m";
	public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_YELLOW = "\u001B[33m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String ANSI_CYAN = "\u001B[36m";
	public static final String ANSI_WHITE = "\u001B[37m";

	private final BufferedReader in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
	private final PrintStream out = System.out;
	private final PrintStream err = System.err;
	private boolean isAnsiCapable = false;
	private Story story = null;

	private String filename;

	public static void main(String[] args) {
		InkPlayer player = new InkPlayer();
		player.parseParams(args);
		try {
			player.run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-2);
		}
	}

	InkPlayer() {
		isAnsiCapable = detectIfIsAnsiCapable();
	}

	public void run() throws Exception {
		String json = getJsonString(filename);
		story = new Story(json);
		story.setAllowExternalFunctionFallbacks(true);

		while (story.canContinue() || story.getCurrentChoices().size() > 0) {

			String text = story.continueMaximally();

			out.print(text);

			if (story.hasError()) {
				for (String errorMsg : story.getCurrentErrors()) {
					err.println(errorMsg);
				}
			}

			// Display story.currentChoices list, allow player to choose one
			if (story.getCurrentChoices().size() > 0) {

				out.println();

				int i = 1;
				for (Choice c : story.getCurrentChoices()) {

					if (isAnsiCapable) {
						out.println(ANSI_CYAN + i + ": " + c.getText() + ANSI_RESET);
					} else {
						out.println(i + ": " + c.getText());
					}

					i++;
				}

				story.chooseChoiceIndex(getChoiceIndex(story.getCurrentChoices()));
			}
		}
	}

	private int getChoiceIndex(List<Choice> currentChoices) throws Exception {

		int i = -1;

		while (i < 1 || i > currentChoices.size()) {

			if (isAnsiCapable)
				out.print(ANSI_CYAN + "?> " + ANSI_RESET);
			else
				out.print("?> ");

			String input = in.readLine();

			if (input.equals("help")) {
				out.println("Commands:\n\tload <filename>\n\tsave <filename>\n\tquit\n\t");

			} else if (input.equals("quit") || input.equals("exit")) {
				System.exit(0);
			} else if (input.startsWith("load ")) {
				String filename = input.substring(5).trim();

				if (filename.length() != 0) {
					try {
						String saveString = getJsonString(filename);
						story.getState().loadJson(saveString);
					} catch (IOException e) {
						out.println("Invalid filename!");
					}
				} else {
					out.println("Invalid filename!");
				}
			} else if (input.startsWith("save ")) {
				String saveString = story.getState().toJson();

				String filename = input.substring(5).trim();
				if (filename.length() != 0) {
					try {
						PrintWriter writer = new PrintWriter(filename, "UTF-8");
						writer.println(saveString);
						writer.close();
					} catch (IOException e) {
						out.println("Invalid filename!");
					}
				} else {
					out.println("Invalid filename!");
				}
			} else {

				try {
					i = Integer.parseInt(input);
				} catch (NumberFormatException nfe) {
				}

				if (i < 1 || i > currentChoices.size()) {
					out.println("Invalid choice!");
					i = -1;
				}
			}
		}

		return i - 1;
	}

	private void parseParams(String[] args) {
		if (args.length != 1) {
			out.println("Json filename not specified.");
			usage();
		} else {
			filename = args[0];
		}
	}

	private void usage() {
		out.println("Usage:\n" + "\t InkPlayer <json_filename>\n");

		System.exit(-1);
	}

	private String getJsonString(String filename) throws IOException {

		InputStream is = new FileInputStream(filename);

		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			// Replace the BOM mark
			if(line != null)
				line = line.replace('\uFEFF', ' ');

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			return sb.toString();
		} finally {
			br.close();
		}
	}

	private static boolean detectIfIsAnsiCapable() {
		try {
			if (System.console() == null) {
				return false;
			}

			return !(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
		} catch (Throwable ex) {
			return false;
		}
	}

}
