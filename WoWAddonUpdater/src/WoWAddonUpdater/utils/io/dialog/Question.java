package WoWAddonUpdater.utils.io.dialog;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Function;

public class Question {
	private String content;
	private Choice[] choices;
	private String separator = " or ";
	
	public Question(String content, String separator, Choice... choices) {
		this.content = content;
		this.choices = choices;
		if (separator != null) {
			this.separator = " " + separator + " ";
		}
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Choice[] getChoices() {
		return choices;
	}

	public void setChoices(Choice[] choices) {
		this.choices = choices;
	}
	
	@SuppressWarnings("finally")
	public Object ask(InputStream in, Consumer<String> out) {
		out.accept(content);
		StringBuffer questionAndChoices = new StringBuffer();
		for (Choice c : choices) {
			questionAndChoices.append(c.getContent() + separator);
		}
		questionAndChoices.setLength(questionAndChoices.length() - separator.length());
		out.accept(questionAndChoices.toString());
		try (Scanner scan = new Scanner(in)) {
			while (scan.hasNext()) {
				String input = scan.next();
				for (Choice c : choices) {
					if (c.isChosen(input)) {
						return c.execute();
					}
				}
			}
		} catch(Exception e) {
		} finally {
			return null;
		}
	}
}
