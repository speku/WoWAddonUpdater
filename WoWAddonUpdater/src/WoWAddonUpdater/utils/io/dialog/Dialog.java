package WoWAddonUpdater.utils.io.dialog;

import java.io.InputStream;
import java.util.function.Consumer;

public class Dialog {
	
	private Sentence[] sentences;
	private InputStream in;
	private Consumer<String> out;
	
	public Dialog(InputStream in, Consumer<String> out, Sentence... sentences) {
		this.in = in;
		this.out = out;
		this.sentences = sentences;
	}
	
	public void converse() {
		for (Sentence s : sentences) {
			s.utter(in, out);
		}
	}
}
