package WoWAddonUpdater.utils.io.dialog;

import java.io.InputStream;
import java.util.function.Consumer;

public class Sentence {
	
	private Question[] questions;
	
	public Sentence(Question... questions) {
		this.questions = questions;
	}
	
	public boolean ask(Question q, InputStream in, Consumer<String> out) {
		return q.ask(in, out) != null;
	}
	
	public void utter(InputStream in, Consumer<String> out) {
		for (Question q : questions) {
			if (ask(q, in, out)) {
				continue;
			}
		}
	}

	public Question[] getQuestions() {
		return questions;
	}

	public void setQuestions(Question[] questions) {
		this.questions = questions;
	}
	
	
}
