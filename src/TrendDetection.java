import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import vn.com.datasection.util.WordTokenizer;

import com.google.gson.Gson;

/**
 * Hello world application to demonstrate storage open, commit and close
 * operations
 */

public class TrendDetection {
	File dbCrawlFile = new File("output/crawl.dat");
	File dbStatsFile = new File("output/stats.dat");
	DB dbStats;
	Gson gson = new Gson();
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

	public static void main(String[] args) throws IOException {
		TrendDetection t = new TrendDetection();
//		t.crawl();
		// Remember to delete old data file to prevent error
//		t.countWord();
//		t.closeStorage();
		 t.debugStore();
	}

	public void countWord() {
		iniStorage();
		WordTokenizer token = new WordTokenizer("models/jvnsensegmenter",
				"data", true);
		String example = "Nếu bạn đang tìm kiếm một chiếc điện thoại Android? Đây là, những smartphone đáng để bạn cân nhắc nhất. Thử linh tinh!";
		token.setString(example);
		System.out.println(token.getString());

		Set<Map.Entry<String, String>> crawlData = getCrawlData().entrySet();
		Iterator<Map.Entry<String, String>> i = crawlData.iterator();
		while (i.hasNext()) {
			Map.Entry<String, String> pageContent = i.next();
			System.out.println(pageContent.getKey());
			PageData pageData = gson.fromJson(pageContent.getValue(),
					PageData.class);
			token.setString(pageData.getContent().toUpperCase());
			String wordSegment = token.getString();
			String[] listWord = wordSegment.split(" ");
			for (String word : listWord) {
				addToDictionary(word, 1);
				Date date;
				try {
					date = simpleDateFormat.parse(pageData.getTime().substring(
							0, 10));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					continue;
				}
				addToStatByDay(date, word, 1);
			}
		}
	}

	public void iniStorage() {
		dbStats = DBMaker.newFileDB(dbStatsFile).closeOnJvmShutdown().make();
	}

	public void closeStorage() {
		dbStats.commit();
		dbStats.close();
	}

	public void debugStore() {
		System.out.println("Get Vocabulary");
		iniStorage();
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap("voca");
		System.out.println(listVocabulary.containsKey("CHO"));
		System.out.println(listVocabulary.get("CHO"));

		String s = "24/02/2014";
		System.out.println("get Day " + s);
		ConcurrentNavigableMap<String, String> words = dbStats.getTreeMap(s);
		System.out.println(words.containsKey("CHO"));
		System.out.println(words.get("CHO"));
	}

	public void crawl() {
		MyCrawler crawler = new MyCrawler();
		crawler.startCrawl("http://tintuc.wada.vn/khoa-hoc-cong-nghe/?p=0", 0);
	}

	public ConcurrentNavigableMap<String, String> getCrawlData() {
		DB db = DBMaker.newFileDB(dbCrawlFile).closeOnJvmShutdown().make();
		return db.getTreeMap("WadaTechData");
	}

	public void addToDictionary(String word, int n) {
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap("voca");
		String strTimes = listVocabulary.get(word);
		int times = 0;
		if (strTimes != null) {
			times = Integer.parseInt(strTimes);
			times = times + n;
		} else {
			times = n;
		}
		listVocabulary.put(word, Integer.toString(times));
	}

	public void addToStatByDay(Date date, String word, int n) {
		String collection = simpleDateFormat.format(date);
		ConcurrentNavigableMap<String, String> listVocabulary = dbStats
				.getTreeMap(collection);
		String strTimes = listVocabulary.get(word);
		int times = 0;
		if (strTimes != null) {
			times = Integer.parseInt(strTimes);
			times = times + n;
		} else {
			times = n;
		}
		listVocabulary.put(word, Integer.toString(times));
	}
}