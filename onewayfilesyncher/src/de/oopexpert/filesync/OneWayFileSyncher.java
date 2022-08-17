package de.oopexpert.filesync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class OneWayFileSyncher {

	private static class FromToTupel {
		
		public FromToTupel(File from, File to) {
			this.from = from;
			this.to = to;
		}
		
		public final File from;
		public final File to;
		
	}
	
	private final File source;
	private final File target;

	public OneWayFileSyncher(File source, File target) {
		this.source = source;
		this.target = target;
	}

	public void start() {
		handle(source, target);
	}
	
	private void handle(File source, File target) {
		
		if (source.isDirectory()) {
			handleDirectory(source, target);
		} else {
			handleFile(source, target);
		}
		
	}

	private void handleFile(File source, File target) {
		
		if (target.isDirectory()) {
			deleteDirectoryRecursively(target);
		}
		
		if (target.exists()) {
			update(source, target);
		} else {
			copy(source, target);
		}
		
	}

	private void handleDirectory(File sourceDirectory, File target) {
		ensureDirectory(target);
		copy(filesOf(sourceDirectory), target);
		delete(allObsoleteFilesInTarget(sourceDirectory, target));
	}

	private File[] filesOf(File sourceDirectory) {
		return sourceDirectory.listFiles();
	}

	private void ensureDirectory(File target) {
		if (target.exists()) {
			if (target.isFile()) {
				target.delete();
				target.mkdir();
			}
		} else {
			target.mkdir();
		}
	}

	private void copy(File[] listFiles, File target) {
		Arrays.asList(listFiles).parallelStream()
		      .map(childFile -> new FromToTupel(childFile, new File(target, childFile.getName())))
		      .forEach(fromTo -> handle(fromTo.from, fromTo.to));
	}

	private void delete(List<File> allObsoleteFilesInTarget) {
		for (File file : allObsoleteFilesInTarget) {
			delete(file);
		}
	}

	private List<File> allObsoleteFilesInTarget(File source, File target) {
		List<File> allObsoleteFiles = new ArrayList<>(Arrays.asList(filesOf(target)));
		allObsoleteFiles.removeAll(Arrays.asList(filesOf(source)).stream().map(file -> new File(target, file.getName())).collect(Collectors.toList()));
		return allObsoleteFiles;
	}

	private void update(File source, File target) {
		
		try {
			if (differs(source, target)) {
				copy(source, target);
			}
		} catch (IOException e) {
			System.out.println("An Error occured while evaluating change state of '" + source.getAbsolutePath() + "': " + e.getMessage());
		}
		
	}

	private boolean differs(File source, File target) throws IOException {
		
		try (FileInputStream fisSource = new FileInputStream(source); 
			 FileInputStream fisTarget = new FileInputStream(target)) {

			byte[] bytesSource = new byte[4096]; // 4 KByte
			byte[] bytesTarget = new byte[4096]; // 4 KByte
	
			int bytesReadSource = fisSource.read(bytesSource);
			int bytesReadTarget = fisTarget.read(bytesTarget);
	
			boolean differs = bytesReadSource != bytesReadTarget;
			
			while (!differs && bytesReadSource != -1) {
	
				differs = !Arrays.equals(bytesSource, bytesTarget);
				
				if (!differs) {
					bytesReadSource = fisSource.read(bytesSource);
					bytesReadTarget = fisTarget.read(bytesTarget);
					differs = bytesReadSource != bytesReadTarget;
				}
				
			}
			
			return differs;
		}
	}

	private void delete(File file) {
		if (file.isDirectory()) {
			deleteDirectoryRecursively(file);
		} else {
			file.delete();
		}
	}

	private void deleteDirectoryRecursively(File directory) {
		for (File childFile : filesOf(directory)) {
			delete(childFile);
		}
		directory.delete();
	}

	private void copy(File source, File target) {
		
		byte[] bytes = new byte[4096]; // 4 KByte
		int offset = 0;

		try (FileInputStream  fisSource = new FileInputStream(source);
			 FileOutputStream fosTarget = new FileOutputStream(target)) {
			
			int bytesRead = fisSource.read(bytes);

			while (bytesRead != -1) {
				fosTarget.write(bytes, 0, bytesRead);
				offset = offset + bytesRead;
				bytesRead = fisSource.read(bytes);
			}
			
		} catch (FileNotFoundException e) {
			System.out.println("An Error occured while copying '" + source.getAbsolutePath() + "': " + e.getMessage());
		} catch (IOException e) {
			System.out.println("An Error occured while copying '" + source.getAbsolutePath() + "': " + e.getMessage());
		}
		
	}

}
