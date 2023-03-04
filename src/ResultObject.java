public record ResultObject(String fileName, String prevCheckSum, String currentCheckSum,
                           boolean status) {


	public ResultObject(FileSumObject fileSumObject, boolean isPast) {
		this(fileSumObject.filePath(),
				(isPast) ? fileSumObject.checkSum() : "-",
				(!isPast) ? fileSumObject.checkSum() : "-",
				false);
	}

	@Override
	public String toString() {
		return "fileName='" + fileName + '\'' +
				", prevCheckSum='" + prevCheckSum + '\'' +
				", currentCheckSum='" + currentCheckSum + '\'' +
				", matches=" + status + '\n';
	}
}
