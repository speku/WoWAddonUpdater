package WoWAddonUpdater.utils.io.dialog;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Choice<T> {
	private String content;
	private Supplier<T> operation;
	private Predicate<String> acceptable;
	
	public Choice(String content, Supplier operation, String separator, Predicate<String> acceptable) {
		this.content = content;
		this.operation = operation;
		this.acceptable = acceptable;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Supplier getOperation() {
		return operation;
	}

	public void setOperation(Supplier operation) {
		this.operation = operation;
	}
	
	public T execute() {
		return operation.get();
	}
	
	public boolean isChosen(String s) {
		return acceptable.test(s);
	}


	
	
}
