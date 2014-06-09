package tr.edu.metu.ceng.postit.viterbi;

import tr.edu.metu.ceng.postit.data.Sentence;
import tr.edu.metu.ceng.postit.data.Token;

public class Viterbi {

	// prior probability matrix (instance of Transition)
	private Matrix priorMatrix = null;

	// likelihood matrix (instance of Emission)
	private Matrix likelihoodMatrix = null;

	public Viterbi(Matrix priorMatrix, Matrix likelihoodMatrix) {
		this.priorMatrix = priorMatrix;
		this.likelihoodMatrix = likelihoodMatrix;
	}

	public Sentence decode(Sentence sentence) {
		String[] states = priorMatrix.getKeys();
		// observations
		Token[] tokens = sentence.getTokens().toArray(new Token[0]);
		int N = states.length;
		int T = tokens.length;
		double[][] viterbi = new double[N + 1][T + 1];
		String[][] backtrack = new String[N + 1][T + 1];
		// initialize first step
		String firstWord = tokens[0].getWord();
		for (int s = 0; s < N; s++) {
			String state = states[s];
			double v = 1.0;
			// prior probability
			double a = priorMatrix.getItem(HMM.START_SYMBOL, state);
			// likelihood
			double b = likelihoodMatrix.getItem(state, firstWord);
			// max probability
			double log = Math
					.abs(Math.log10(v) + Math.log10(a) + Math.log10(b));
			viterbi[s][0] = log;
			backtrack[s][0] = "";
		}

		// recursive step
		for (int t = 1; t < T; t++) {
			String word = tokens[t].getWord();
			for (int s = 0; s < N; s++) {
				double maxProb = Double.MAX_VALUE;
				double argmax = Double.MAX_VALUE;
				String backtrackArg = "";
				String state = states[s];
				for (int s1 = 0; s1 < N; s1++) {
					double v = viterbi[s1][t - 1];
					if (v == 0.0) {
						continue;
					}
					String previousState = states[s1];
					// prior probability
					double a = priorMatrix.getItem(previousState, state);
					// likelihood
					double b = likelihoodMatrix.getItem(state, word);
					// max probability
					double log = v + Math.abs(Math.log10(a));
					if (log < argmax) {
						argmax = log;
						backtrackArg = previousState;
					}
					log = log + Math.abs(Math.log10(b));
					if (log < maxProb) {
						maxProb = log;
					}
				}
				viterbi[s][t] = maxProb;
				backtrack[s][t] = backtrackArg;
			}
		}

		// terminate step
		viterbi[N][T] = Double.MAX_VALUE;
		for (int s = 0; s < N; s++) {
			String state = states[s];
			// prior probability
			double a = priorMatrix.getItem(state, HMM.END_SYMBOL);
			// max probability
			double likelihood = viterbi[s][T - 1] + Math.abs(Math.log10(a));
			if (likelihood < viterbi[N][T]) {
				viterbi[N][T] = likelihood;
				backtrack[N][T] = state;
			}
		}
		// backtracking
		Sentence res = new Sentence();
		int tagIndex = N;
		int stepIndex = T;
		String tag = backtrack[tagIndex][stepIndex];
		while (tag != null && tag.length() != 0) {
			// set tag for this token.
			Token token = tokens[stepIndex - 1];
			token.setTag(tag);
			res.addToken(token);
			for (int i = 0; i < states.length; i++) {
				if (states[i].equals(tag)) {
					tagIndex = i;
					break;
				}
			}
			tag = backtrack[tagIndex][--stepIndex];
		}

		return res;
	}

}
