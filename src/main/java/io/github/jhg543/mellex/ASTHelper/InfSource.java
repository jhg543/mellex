package io.github.jhg543.mellex.ASTHelper;

public class InfSource {
	private ObjectName sourceObject;
	private InfConnection connectionType;

	public InfSource(ObjectName sourceObject, InfConnection connectionType) {
		super();
		this.sourceObject = sourceObject;
		this.connectionType = connectionType;
	}

	public ObjectName getSourceObject() {
		return sourceObject;
	}

	public InfConnection getConnectionType() {
		return connectionType;
	}

	public InfSource expand(InfSource other) {
		if (connectionType == InfConnection.IN_RESULT_EXPRESSION) {
			return other;
		}

		return new InfSource(other.getSourceObject(), InfConnection.IN_CLAUSE);

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connectionType == null) ? 0 : connectionType.hashCode());
		result = prime * result + ((sourceObject == null) ? 0 : sourceObject.hashCode());
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
		InfSource other = (InfSource) obj;
		if (connectionType != other.connectionType)
			return false;
		if (sourceObject == null) {
			if (other.sourceObject != null)
				return false;
		} else if (!sourceObject.equals(other.sourceObject))
			return false;
		return true;
	}

}
