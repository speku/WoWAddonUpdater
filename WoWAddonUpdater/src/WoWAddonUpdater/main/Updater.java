package WoWAddonUpdater.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

public class Updater {
	private static final String[] CURSE_FORGE = {"http://wow.curseforge.com/addons/", 
		"user-action user-action-download.*href=\"/addons/(.*)\">Download",
		"user-action user-action-download.*href=\"(.*)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*"};
	private static final String[] CURSE_FORGE_ALPHA = {"http://wow.curseforge.com/addons/%s/files/", 
		"<td class=\"col-file\"><a href=\".*?/files/(.*?)\">",
		"user-action user-action-download.*?href=\"(.*?)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*",
		"Uploaded on.*?title=\"(.*?)\""};
	private static final String DEFAULT_PATH = "J:/World of Warcraft/Interface/AddOns";
	private static final String DEFAULT_DOWNLOAD_PATH = DEFAULT_PATH + "/WoWAddonUpdater";
	private static final boolean DEFAULT_THOROUGH = true;
	private static String path = DEFAULT_PATH;
	private static String downloadPath = DEFAULT_DOWNLOAD_PATH;
	private static boolean thorough = DEFAULT_THOROUGH;
	private static ArrayList<String> detectedPaths = new ArrayList<>();
	private static final Pattern PATTERN_FOLDER_INPUT = Pattern.compile("(.*World of Warcraft).*");
	private static final Pattern PATTERN_FOLDER_SEARCH = Pattern.compile("(.*World of Warcraft)$");
	private static final Pattern DETECT_DOWNLOAD_ZIP = Pattern.compile(".*/([^/]*zip)");
	private static final int SEARCH_DEPTH = 2;
	private static int searchDepth = SEARCH_DEPTH;
	private static final String ADDON_PATH = "/Interface/Addons";
	private static final String TITLE = "\nWoWAddonUpdater\n";
	private static boolean auto = true;
	private static boolean alpha = true;
	private static final String[] sources = {"curse", "wowace"};

	public static void main(String[] args) {
		System.out.println(TITLE);
		if ((auto && !auto()) || !auto) {
			path = prompt(path);
		}
		download(generateDownloadLinks(parseFolder(path, alpha ? CURSE_FORGE_ALPHA : CURSE_FORGE, thorough), alpha ? CURSE_FORGE_ALPHA : CURSE_FORGE), downloadPath);
		unzip(downloadPath, path);
		refreshFolder(downloadPath);
		System.out.println("\nall done - bye!");
	}
	
	private static boolean auto() {
		System.out.println("searching for WoW install...\n");
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] roots = File.listRoots();
		for (File root : roots) {
			if (fsv.getSystemTypeDescription(root).equals("Local Disk")) {
				search(root, 0, PATTERN_FOLDER_SEARCH);
			}
		}
		System.out.println();
		if (detectedPaths.size() == 1) {
			path = detectedPaths.get(0) + ADDON_PATH;
			System.out.println("WoW install found: " + path + "\n");
			return true;
		} else if (detectedPaths.size() > 1) {
			System.out.println("duplicate WoW installs found\nmanual input required!\n");
			int c = 1;
			for (String s : detectedPaths) {
				System.out.println(String.format("%s\t[%d]", s, c++));
			}
			System.out.println();
			return false;
		}
		System.out.println("no WoW installs found\nmanual input required!\n\n");
		return false;
	}
	
	private static void search(File file, int depth, Pattern p) {
		if (depth <= searchDepth) {
			System.out.println("scanning " + file.toString());
			Matcher m = p.matcher(file.toString());
			if (m.find()) {
				detectedPaths.add(m.group(1));
			}
			if (file != null && file.isDirectory() && file.listFiles() != null) {
				for (File f : file.listFiles()) {
					search(f, depth + 1, p);
				}
			}
		}
	}
	
	
	private static String prompt(String path) {
		System.out.println("enter q/quit to exit");
		Scanner scan = new Scanner(System.in);
		String input;
		if (detectedPaths.size() >= 2) {
			System.out.println("select the WoW install to update\n");
			while (scan.hasNext()) {
				input = scan.next().toLowerCase();
				exit(input, scan);
				int numericInput = Integer.parseInt(input);
				if (numericInput <= detectedPaths.size()) {
					path = detectedPaths.get(numericInput - 1) + ADDON_PATH;
					break;
				} else {
					System.out.println("\nselect the WoW install to update\n");
					System.out.println("or enter q/quit to exit");
				}
			}
		}
		System.out.println("Perform thorough update?");
		System.out.println("y/yes n/no\n");
		input = scan.next().toLowerCase();
		exit(input, scan);
		if (input.startsWith("y")) {
			thorough = true;
		}
		if (detectedPaths.size() == 0) {
			System.out.println("skip setup and start?");
			System.out.println("y/yes n/no\n");
			if (scan.next().toLowerCase().startsWith("y")) {
				scan.close();
				return path;
			}
			Matcher m;
			String s = "enter path to root WoW directory\n";
			System.out.println(s);
			scan.nextLine();
			while (scan.hasNextLine()) {
				input = scan.nextLine();
				exit(input, scan);
				m = PATTERN_FOLDER_INPUT.matcher(input);
				if (new File(input).exists() && m.find()) {
					path = m.group(1) + ADDON_PATH;
					break;
				} else {
					System.out.println(s);
				}
			}
		}
		scan.close();
		return path;

	}
	
	private static void exit(String input, Scanner scan) {
		input = input.trim().toLowerCase();
		if (input.equals("q") || input.equals("quit")) {
			if (scan != null) scan.close();
			System.out.println("bye!");
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			} finally {
				System.exit(0);
			}
		}
	}

	private static ArrayList<String> parseFolder(String path, String[] patterns, boolean thorough) {
		System.out.println("\ndetecting addons...\n");
		ArrayList<String> addons = new ArrayList<>();
		File[] files = new File(path).listFiles();
		int totalCount = files.length;
		int currentCount = 0;
		String n;
		File f;
		for (File file : files) {
			boolean hasId = false;
			n = file.getName();
			f = new File(path + "/" + n + "/" + n + ".toc");
			try (Scanner c = new Scanner(f)){
				Pattern p = Pattern.compile(patterns[3]);
				while (c.hasNextLine()) {
					Matcher m = p.matcher(c.nextLine());
					if (m.find()) {
						n = m.group(1);
						addons.add(n);
						hasId = true;
						break;
					}
				}
			} catch (FileNotFoundException e) {
			}
			if (thorough && !hasId) {
				addons.add(n);
			}
			System.out.println(String.format("[%d of %d] detected: %s",  ++currentCount, totalCount, n));
		}
		return new ArrayList<String>(new HashSet<String>(addons));
	}
	
	private static ArrayList<URL> generateDownloadLinks(ArrayList<String> addons, String[] patterns) {
		System.out.println("\nparsing...\n");
		if (addons == null || addons.size() == 0) return null;
		int totalCount = addons.size();
		int currentCount = 0;
		ArrayList<URL> downloadLinks = new ArrayList<>();
		for (String addon : addons) {
			try {
				URL u = alpha ? new URL(String.format(patterns[0], addon)) : 
					new URL(patterns[0] + addon + "/");
				String c = dumpUrl(u);
				Pattern p = Pattern.compile(patterns[1]);
				Matcher m = p.matcher(c);
				if (m.find()) {
					String s = m.group(1);
					u = alpha ? new URL(String.format(patterns[0], addon) + s) : 
						new URL(patterns[0] + s);
					c = dumpUrl(u);
					p = Pattern.compile(patterns[2]);
					m = p.matcher(c);
					if (m.find()) {
						s = m.group(1);
						boolean validSource = false;
						for (String source : sources) {
							if (s.contains(source)) {
								validSource = true;
								break;
							}
						}
						if (validSource) {
							downloadLinks.add(new URL(s));
							System.out.println(String.format("[%d of %d] parsed: %s\t%s", ++currentCount, totalCount, addon, s));
						} else {
							totalCount--;
						}
					}
				}
			} catch (Exception e) {
				totalCount--;
			} 
		}
		return downloadLinks;
	}
	
	private static String dumpUrl(URL u) {
		try (Scanner scan = new Scanner(u.openStream())){
			StringBuffer c = new StringBuffer();
			while (scan.hasNextLine()) {
				c.append(scan.nextLine());
			}
			return c.toString();
		} catch (IOException e) {
		}
		return null;
	}
	
	private static void download(ArrayList<URL> urls, String downloadPath) {
		System.out.println("\ndownloading...\n");
		refreshFolder(downloadPath);
		int totalCount = urls.size();
		int currentCount = 0;
		for (URL u : urls) {
			Matcher m = DETECT_DOWNLOAD_ZIP.matcher(u.toString());
			if (m.find()) {
				try {
					Files.copy(u.openConnection().getInputStream(), Paths.get(downloadPath + "/" + m.group(1)));
					System.out.println(String.format("[%d of %d] downloaded: %s", ++currentCount, totalCount, m.group(1)));
				} catch (IOException e) {
					totalCount--;
				}
			}
		}
	}
	
	private static void unzip(String from, String to) {
		System.out.println("\nextracting...\n");
		File[] files = new File(from).listFiles();
		int totalCount = files.length;
		int currentCount = 0;
		for (File f : files) {
			try {
		        new ZipFile(f).extractAll(to);
		        System.out.println(String.format("[%d of %d] extracted: %s",  ++currentCount, totalCount, f));
		    } catch (ZipException e) {
		    }
		}
	}
	
	private static void refreshFolder(String downloadPath) {
		File folder = new File(downloadPath);
		if (folder.exists()) {
			File[] files = folder.listFiles();
			for (File f : files) {
				f.delete();
			}
		} else {
			folder.mkdir();
		}
	}
}
