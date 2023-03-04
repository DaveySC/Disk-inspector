import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Checksum {
	public enum Mode{DIRECTORY, EXEC_FILE};

	private static Mode workingMode;

	private final static List<ResultObject> result = new ArrayList<>();

	private final static List<FileSumObject> listOfFileSumObjects = new ArrayList<>();
	private final static String MAGIC_WORD = "MAGIC";

	private final static String CHECK_SUM_FILE_NAME = "checksum.chk";

	private final static String EXECUTION_FILE_NAME = "Checker.jar";

	private static String pathToWorkingDirectory;

	private static File workingDirectory;

	private static File checkSumFile;

	private static File executionFile;

	public Checksum(String path, Mode wMode) throws Exception {
		workingMode = wMode;
		pathToWorkingDirectory = (path == null) ? System.getProperty("user.dir") : path;
		workingDirectory = new File(pathToWorkingDirectory);
		if (!workingDirectory.isDirectory())
			throw new FileNotFoundException("Directory by this path : " + workingDirectory.getAbsolutePath() + " not found");
		checkSumFile = new File(pathToWorkingDirectory, CHECK_SUM_FILE_NAME);
		if (!checkSumFile.isFile())
			throw new FileNotFoundException("File with checksum by this path : " + checkSumFile.getAbsolutePath() + "  not found");
		executionFile = new File(getAbsolutePath(Checksum.class), EXECUTION_FILE_NAME);
	}

	public Checksum() throws Exception {
		this(null, Mode.EXEC_FILE);
	}
	private static String getAbsolutePath(Class clazzFromLoadJar) throws Exception {
		URL url = clazzFromLoadJar.getProtectionDomain().getCodeSource().getLocation();
		File f = new File(url.toURI());
		String decoder = URLDecoder.decode(f.getParentFile().getAbsolutePath(), "UTF-8");
		return decoder;
	}

	private byte[] createCheckSum(File file) throws Exception{
		InputStream fis = new FileInputStream(file);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");

		int numRead;
		while ((numRead = fis.read(buffer)) != -1) if (numRead > 0) complete.update(buffer, 0, numRead);
		fis.close();
		return complete.digest();
	}
	
	private String createCheckSumString(File file) throws Exception {
		return StringUtils.getHex(createCheckSum(file));
	}
	
	private boolean checkTheMagicWordInFile() {
		try (InputStream in = new FileInputStream(checkSumFile);
		    BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.equals(MAGIC_WORD)) return true;
			}
		} catch (IOException x) {
			System.err.println(x);
		}
		return false;
	}

	private void writeCheckSumsToFile() {
		try (OutputStream in = new FileOutputStream(checkSumFile);
		     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(in))) {
			for (FileSumObject object : listOfFileSumObjects) {
				writer.write(object.toString());
			}
		} catch (Exception x) {
			System.err.println(x);
		}
	}
	private int compareCheckSums() {
		try (InputStream in = new FileInputStream(checkSumFile);
		    BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			List<FileSumObject> fileSumObjectListFromFile = new ArrayList<>();
			String checkSumFromFile;
			while ((checkSumFromFile = reader.readLine()) != null) {
				String[] parsed = checkSumFromFile.split(":");
				fileSumObjectListFromFile.add(new FileSumObject(parsed[0], parsed[1]));
			}
			Collections.sort(listOfFileSumObjects);
			Collections.sort(fileSumObjectListFromFile);

			int i = 0, j = 0;
			while (i < listOfFileSumObjects.size() || j < fileSumObjectListFromFile.size()) {
				if (i >= listOfFileSumObjects.size()) {
					result.add(new ResultObject(fileSumObjectListFromFile.get(j++), true));
					continue;
				}
				if (j >= fileSumObjectListFromFile.size()){
					result.add(new ResultObject(listOfFileSumObjects.get(i++), false));
					continue;
				}

				FileSumObject ob1 = listOfFileSumObjects.get(i);
				FileSumObject ob2 = fileSumObjectListFromFile.get(j);
				int compareRes = ob1.compareTo(ob2);
				if (compareRes == 0) {
					result.add(new ResultObject(ob1.filePath(),
							ob2.checkSum(),
							ob1.checkSum(),
							ob1.checkSum().equals(ob2.checkSum())));
					i++;
					j++;
					continue;
				}
				if (compareRes < 0) {
					result.add(new ResultObject(listOfFileSumObjects.get(i++), false));
					continue;
				}
				result.add(new ResultObject(fileSumObjectListFromFile.get(j++), true));
			}


		} catch (Exception x) {
			System.err.println(x);
		}
		return 1;
	}

	private void prepareListOfExec() throws Exception {
		listOfFileSumObjects.add(new FileSumObject(executionFile.getName(), createCheckSumString(executionFile)));
	}

	public static String subtractPaths(File from, File what) {
		return from.getAbsolutePath().replace(what.getAbsolutePath(), "");
	}
	private void prepareListOfDir() throws Exception {
		prepareListOfDir(workingDirectory.listFiles());
	}

	private void prepareListOfDir(File[] files) throws Exception {
		if (files == null) return;
		for (File file : files) {
			if (file.equals(checkSumFile)) continue;
			if (file.isDirectory()) prepareListOfDir(file.listFiles());
			else listOfFileSumObjects
					.add(new FileSumObject(subtractPaths(file, workingDirectory), createCheckSumString(file)));
		}
	}

	private int makeCheck() throws Exception {
		if (workingMode == Mode.DIRECTORY) prepareListOfDir();
		else prepareListOfExec();
		
		if (checkTheMagicWordInFile()) {
			System.out.println("Checksums were written to file.");
			writeCheckSumsToFile();
			return 1;
		}
		compareCheckSums();
		System.out.println("-----------------------------RESULT----------------------------");
		System.out.println();
		System.out.format("%-35s%-35s%-35s%-16s", "FILE PATH", "PrevCheckSum", "CurrentCheckSum", "Matches\n");
		System.out.println();
		for (ResultObject object : result) {
			System.out.format("%-35s%-35s%-35s%-16s",
					object.fileName(), object.prevCheckSum(), object.currentCheckSum(), object.status());
			System.out.println();
		}
		System.out.println();
		System.out.println("-------------------------END OF RESULT-------------------------");
		return 1;
	}

	private static void printInfo() {
		System.out.println("""
				To check checksum of execution file : parameters not needed
				To check checksums of files in directory : -directory [path_to_directory]
				""");
	}

	public static void main(String[] args) {
		try {
			if (args.length == 2) {
				if (args[0].equals("-directory")) System.exit(new Checksum(args[1], Mode.DIRECTORY).makeCheck());
			} else if (args.length == 0) {
				System.exit(new Checksum().makeCheck());
			}
			printInfo();
		}  catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}