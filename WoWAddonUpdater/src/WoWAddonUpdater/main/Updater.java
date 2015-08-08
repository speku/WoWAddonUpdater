package WoWAddonUpdater.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileSystemView;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

public class Updater {
	private static final String[] CURSE_FORGE_RELEASE = {"http://wow.curseforge.com/addons/", 
		"user-action user-action-download.*?href=\"/addons/(.*?)\">Download",
		"user-action user-action-download.*?href=\"(.*?)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*",
		"Uploaded on.*?title=\"(.*?)\""};
	private static final String[] CURSE_FORGE_ALPHA = {"http://wow.curseforge.com/addons/%s/files/", 
		"<td class=\"col-file\"><a href=\".*?/files/(.*?)\">",
		"user-action user-action-download.*?href=\"(.*?)\">Download", 
		"## X-Curse-Project-ID: (.*)[\\W]*",
		"Uploaded on.*?title=\"(.*?)\""};
	private String rootPath;
	private boolean thorough = true;
	private static final Pattern PATTERN_FOLDER_INPUT = Pattern.compile("(.*World of Warcraft).*");
	private static final Pattern PATTERN_FOLDER_SEARCH = Pattern.compile("(.*World of Warcraft)$");
	private static final Pattern DETECT_DOWNLOAD_ZIP = Pattern.compile(".*/([^/]*zip)");
	private int searchDepth = 2;
	private static final String ADDONS_PATH_POSTFIX = "/Interface/Addons";
	private static final String UPDATER_PATH_POSTFIX = "/Interface/WoWAddonUpdater";
	private static final String TITLE = "WoWAddonUpdater";
	private boolean auto = true;
	private static final String SETTINGS_FILE_NAME = "WoWAddonUpdater.config";
	private boolean getAlpha = true;
	private String[] curseForge;
	private HashMap<String, String[]> addonStorage = new HashMap<>();
	private HashMap<String, Boolean> urlStorage = new HashMap<>();
	private boolean setUp = false;

	public static void main(String[] args) {
		Updater updater = new Updater();
		loadSettings();
		if ((auto && !auto()) || !auto) {
			path = prompt(path);
		}
		curseForge = getAlpha ? CURSE_FORGE_ALPHA : CURSE_FORGE_RELEASE;
		download(generateDownloadLinks(parseFolder(path, curseForge, thorough), curseForge), path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS);
		unzip(path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS, path);
		refreshFolder(path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS);
		exit(null, null);
		dumpSettings(path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS);
	}
	
	public Updater() {
		System.out.println("/n" + TITLE + "/");
		ArrayList<String> detectedRootPaths = search();
			
		}
	}
	
	private static void dumpSettings(String path) {
		File f = new File(path);
		if (!f.exists()) {
			try (ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(f))){
				f.createNewFile();
				oos.writeObject(path);
				oos.writeObject(downloadPath);
				oos.writeObject(thorough);
				oos.writeObject(searchDepth);
				oos.writeObject(auto);
				oos.writeObject(getAlpha);
				oos.writeObject(addonStorage);
				oos.writeObject(setUp);
				oos.writeObject(urlStorage);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	private static sca
	
	private Updater loadSettings(String rootPath) {
		File f = new File(path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS);
		if (f.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(
					new FileInputStream(f))){
				path = (String)ois.readObject();
				downloadPath = (String) ois.readObject();
				thorough = (Boolean) ois.readObject();
				searchDepth = (Integer) ois.readObject();
				auto = (Boolean) ois.readObject();
				getAlpha = (Boolean) ois.readObject();
				addonStorage = (HashMap<String, String[]>) ois.readObject();
				setUp = (Boolean) ois.readObject();
				urlStorage = (HashMap<String, Boolean>) ois.readObject();
				return true;
			} catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}
	
	private static String auto() {
		System.out.println("searching for WoW install...\n");
		String rootPath = null;
		FileSystemView fsv = FileSystemView.getFileSystemView();
		File[] roots = File.listRoots();
		for (File root : roots) {
			if (fsv.getSystemTypeDescription(root).equals("Local Disk")) {
				search(root, 0, PATTERN_FOLDER_SEARCH);
			}
		}
		System.out.println();
		if (detectedPaths.size() == 1) {
			rootPath = detectedPaths.get(0);
			System.out.println("WoW install found: " + rootPath + "\n");
			return rootPath;
		} else if (detectedPaths.size() > 1) {
			System.out.println("duplicate WoW installs found\nmanual input required!\n");
			int c = 1;
			for (String s : detectedPaths) {
				System.out.println(String.format("%s\t[%d]", s, c++));
			}
			System.out.println();
			return null;
		}
		System.out.println("no WoW installs found\nmanual input required!\n\n");
		return null;
	}
	
	private static ArrayList<String> search(File file, int depth, Pattern p) {
		ArrayList<String> detectedPaths = new ArrayList<>();
		class Recursor {
			public void recurse(File file, int depth, Pattern p) {
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
		}
		new Recursor().recurse(file, depth, p);
		return detectedPaths;
	}
	
	private static String prompt(String rootPath) {
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
					rootPath = detectedPaths.get(numericInput - 1);
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
		System.out.println("get alpha versions?");
		System.out.println("y/yes n/no\n");
		input = scan.next().toLowerCase();
		exit(input, scan);
		if (input.startsWith("y")) {
			getAlpha = true;
		} else {
			getAlpha = false;
		}
		if (detectedPaths.size() == 0) {
			System.out.println("skip setup and start?");
			System.out.println("y/yes n/no\n");
			if (scan.next().toLowerCase().startsWith("y")) {
				scan.close();
				return rootPath;
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
					rootPath = m.group(1);
					break;
				} else {
					System.out.println(s);
				}
			}
		}
		scan.close();
		return rootPath;

	}
	
	private static void exit(String input, Scanner scan) {
		if (input != null) {
			input = input.trim().toLowerCase();
		}
		if (input == null || input.equals("q") || input.equals("quit")) {
			if (scan != null) scan.close();
			System.out.println("\nall done!\nbye!");
			if (input == null && setUp) {
				dumpSettings(path.replace("Addons","") + "WoWAddonUpdater/" + SETTINGS);
			}
			try {
				Thread.sleep(1000);
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
		ArrayList<String> result = new ArrayList<String>(new HashSet<String>(addons));
		for (int i = result.size() - 1; i >= 0; i--) {
			if (setUp && addonStorage.get(result.get(i)) != null && addonStorage.get(result.get(i))[3] == null) {
				result.remove(i);
			}
		}
		return result;
	}
	
	private static ArrayList<URL> generateDownloadLinks(ArrayList<String> addons, String[] patterns) {
		System.out.println("\nparsing...\n");
		if (addons == null || addons.size() == 0) return null;
		int totalCount = addons.size();
		int currentCount = 0;
		ArrayList<URL> downloadLinks = new ArrayList<>();
		for (String addon : addons) {
			if (setUp && addonStorage.get(addon) != null && addonStorage.get(addon)[3] == null) {
				totalCount--;
				continue;
			}
			try {
				URL u = getAlpha ? new URL(String.format(patterns[0], addon)) : 
					new URL(patterns[0] + addon + "/");
				String c = dumpUrl(u);
				Pattern p = Pattern.compile(patterns[1]);
				Matcher m = p.matcher(c);
				if (m.find()) {
					String s = m.group(1);
					u = getAlpha ? new URL(String.format(patterns[0], addon) + s) : 
						new URL(patterns[0] + s);
					c = dumpUrl(u);
					p = Pattern.compile(patterns[2]);
					m = p.matcher(c);
					if (m.find()) {
						s = m.group(1);
						downloadLinks.add(new URL(s));
						System.out.println(String.format("[%d of %d] parsed: %s\t%s", ++currentCount, totalCount, addon, s));
						p = Pattern.compile(patterns[4]);
						m = p.matcher(c);
						c = m.find() ? m.group(1) : null;
						String update = (addonStorage.get(addon) != null && !addonStorage.get(addon)[1].equals(c)) ? "u" : null;
						addonStorage.put(addon, new String[]{s, c, update, "u"});
						urlStorage.put(s, update != null);
					}
				}
			} catch (Exception e) {
				totalCount--;
				addonStorage.put(addon, new String[]{null, null, null, null});
			} 
		}
		setUp = true;
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
			if (urlStorage.get(u) != null && urlStorage.get(u)) {
				totalCount--;
				continue;
			}
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
				if (!f.getName().equals(SETTINGS)) {
					f.delete();
				}
			}
		} else {
			folder.mkdir();
		}
	}
}
