package io.github.jhg543.mellex.ASTHelper.plsql;

public class ObjectReference {

	private ObjectDefinition objectDefinition;
	private int lineNumber;
	private int charPosition;
	private String fileName = "";

	public ObjectDefinition getObjectDefinition() {
		return objectDefinition;
	}

	public void setObjectDefinition(ObjectDefinition objectDefinition) {
		this.objectDefinition = objectDefinition;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int getCharPosition() {
		return charPosition;
	}

	public void setCharPosition(int charPosition) {
		this.charPosition = charPosition;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + charPosition;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result + lineNumber;
		result = prime * result + ((objectDefinition == null) ? 0 : objectDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectReference other = (ObjectReference) obj;
		if (charPosition != other.charPosition)
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (lineNumber != other.lineNumber)
			return false;
		if (objectDefinition == null) {
			if (other.objectDefinition != null)
				return false;
		} else if (!objectDefinition.equals(other.objectDefinition))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Ref [def=" + objectDefinition + ", pos=" + lineNumber + "," + charPosition + ", f=" + fileName + "]";
	}

	public ObjectReference(ObjectDefinition objectDefinition, String fileName, int lineNumber, int charPosition) {
		super();
		this.objectDefinition = objectDefinition;
		this.lineNumber = lineNumber;
		this.charPosition = charPosition;
		this.fileName = fileName;
	}

}
