import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

/**
 * 
 * @author Tej
 *
 */
public class BrillsPOSTagger {
	/**
	 * Priority queue that stores the best rules
	 */
	private Queue<Best> bestRules;

	/**
	 * Trainng File
	 */
	private File training;
	/**
	 * Most Common Tags for a given word
	 */
	private Map<String, String> mostProbable;
	/**
	 * Lists to maintain states of corpus
	 */
	private List<String> correctTags, currentTags, corpus;

	/**
	 * Initialization
	 * 
	 * @param f
	 * @throws FileNotFoundException
	 */
	public BrillsPOSTagger(File f) throws FileNotFoundException {
		this.training = f;
		bestRules = new PriorityQueue<>();
		mostProbable = new HashMap<>();
		currentTags = new ArrayList<>();
		correctTags = new ArrayList<>();
		corpus = new ArrayList<>();
	}

	/**
	 * Computes most likely tags for given training file
	 * 
	 * @throws FileNotFoundException
	 */
	private void initializeWithMostLikelyTags() throws FileNotFoundException {
		Scanner fileReader = new Scanner(this.training);
		fileReader.useDelimiter("\\s+");
		Map<String, Map<String, Integer>> temp = new HashMap<>();
		Map<String, Integer> score = new HashMap<>();
		while (fileReader.hasNext()) {
			String word_tag[] = fileReader.next().split("_");
			if (word_tag.length > 1) {
				corpus.add(word_tag[0]);
				this.correctTags.add(word_tag[1]);
				if (temp.get(word_tag[0]) == null) {
					temp.put(word_tag[0], new HashMap<>());
				}
				temp.get(word_tag[0]).put(word_tag[1], temp.get(word_tag[0]).getOrDefault(word_tag[1], 0) + 1);
				if (mostProbable.get(word_tag[0]) == null) {
					mostProbable.put(word_tag[0], word_tag[1]);
				} else {
					if (temp.get(word_tag[0]).get(word_tag[1]) > score.getOrDefault(word_tag[0], 0)) {
						score.put(word_tag[0], score.getOrDefault(word_tag[0], 0) + 1);
						mostProbable.put(word_tag[0], word_tag[1]);
					}
				}
			}
		}
		for (int i = 0; i < this.correctTags.size(); i++) {
			this.currentTags.add(mostProbable.get(corpus.get(i)));
		}
		fileReader.close();
	}

	/**
	 * Method that generates 10 best rules
	 * 
	 * @return best rules generated
	 * @throws FileNotFoundException
	 */
	public Queue<Best> generateRules() throws FileNotFoundException {
		initializeWithMostLikelyTags();
		for (int i = 0; i < 10; i++) {
			Best rule = this.getBestInstance();
			bestRules.offer(rule);
		}
		return this.bestRules;
	}

	/**
	 * Method that gets best instance given the current state
	 * 
	 * @return
	 * @throws FileNotFoundException
	 */
	private Best getBestInstance() throws FileNotFoundException {
		String[] tags = { "NN", "VB" };
		Best bestInstance = new Best(0);
		for (String from_tag : tags) {
			for (String to_tag : tags) {
				Map<String, Integer> good_transforms = new HashMap<>();
				Map<String, Integer> bad_transforms = new HashMap<>();
				if (!from_tag.equals(to_tag)) {
					for (int i = 1; i < this.corpus.size(); i++) {
						String current = this.currentTags.get(i);
						String correct = this.correctTags.get(i);
						if (correct.equals(to_tag) && current.equals(from_tag)) {
							good_transforms.put(this.currentTags.get(i - 1),
									good_transforms.getOrDefault(this.currentTags.get(i - 1), 0) + 1);
						} else {
							if (correct.equals(from_tag) && current.equals(from_tag)) {
								bad_transforms.put(this.currentTags.get(i - 1),
										bad_transforms.getOrDefault(this.currentTags.get(i - 1), 0) + 1);
							}
						}
					}
				}

				int score = 0;
				String prevTag = "";
				for (int i = 1; i < this.corpus.size(); i++) {
					int val = good_transforms.getOrDefault(this.currentTags.get(i), 0)
							- bad_transforms.getOrDefault(this.currentTags.get(i), 0);
					if (val > score) {
						score = val;
						prevTag = this.currentTags.get(i);
					}
				}
				if (score > bestInstance.bestScore) {
					bestInstance.bestRule = "Change tag from " + from_tag + " to " + to_tag + " if prev tag is "
							+ prevTag;
					bestInstance.bestScore = score;
					bestInstance.from = from_tag;
					bestInstance.to = to_tag;
					bestInstance.prev = prevTag;
				}
			}
		}

		if (bestInstance.bestScore > 0) {
			for (int i = 1; i < this.corpus.size(); i++) {
				if (this.currentTags.get(i).equals(bestInstance.from)
						&& this.currentTags.get(i - 1).equals(bestInstance.prev)) {
					this.currentTags.set(i, bestInstance.to);
				}
			}
		}
		return bestInstance;
	}

	/**
	 * Object to store the conversion instance
	 * 
	 * @author Tej
	 *
	 */
	class Best implements Comparable<Best> {
		int bestScore;
		String bestRule;
		String from;
		String to;
		String prev;

		public Best(int bS) {
			this.bestScore = bS;
		}

		@Override
		public int compareTo(Best o) {
			return o.bestScore - this.bestScore;
		}
	}

	/**
	 * driver function
	 * 
	 * @param cd
	 * @throws FileNotFoundException
	 */
	public static void main(String[] cd) throws FileNotFoundException {
        if(cd.length != 1) {
            System.out.println("Please enter file name");
            System.out.println("Try - 'java BrillsPOSTagger POSTaggedTrainingSet.txt'");
            System.exit(1);
        }
		BrillsPOSTagger bt = new BrillsPOSTagger(new File(cd[0]));
		Queue<Best> rules = bt.generateRules();
		while (!(rules.isEmpty())) {
			System.out.println(rules.poll().bestRule);
		}
	}
}
