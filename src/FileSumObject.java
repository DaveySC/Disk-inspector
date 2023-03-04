public record FileSumObject(String filePath, String checkSum) implements Comparable<FileSumObject>{


	@Override
	public String toString() {
		return filePath + ":" + checkSum + '\n';
	}


	@Override
	public int compareTo(FileSumObject o) {
		return this.filePath.compareTo(o.filePath);
	}
}
