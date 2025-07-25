package org.nasdanika.common.tests;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.BagOfWordsGenerator;

public class TestBagOfWords {
	
	@Test
	public void test() {
		BagOfWordsGenerator bagOfWordsGenerator = new BagOfWordsGenerator();
		Map<String, Integer> bagOfWords = bagOfWordsGenerator.generate("The quick brown fox jumps over the lazy dog");
		bagOfWords.forEach((w,c) -> System.out.println(w + "\t" + c));
	}

}
