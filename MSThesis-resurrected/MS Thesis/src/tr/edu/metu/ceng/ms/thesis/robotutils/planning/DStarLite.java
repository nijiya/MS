/*
 *  The MIT License
 * 
 *  Copyright 2010 Prasanna Velagapudi <psigen@gmail.com>.
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package tr.edu.metu.ceng.ms.thesis.robotutils.planning;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import tr.edu.metu.ceng.ms.thesis.modstarlite.util.StateWriter;
import tr.edu.metu.ceng.ms.thesis.robotutils.util.DiffWriter;
import tr.edu.metu.ceng.ms.thesis.robotutils.util.DoubleUtils;

/**
 * This class implements the <b>unoptimized</b> D*-lite algorithm exactly as
 * described in [Koenig 2002]. D*-lite is an incremental variant of the A*
 * search algorithm, meaning that it can cheaply update itself to account for
 * new obstacles.
 * 
 * Source: Koenig, S. and Likhachev, M. 2002. D*lite. In Eighteenth National
 * Conference on Artificial Intelligence (Edmonton, Alberta, Canada, July 28 -
 * August 01, 2002). R. Dechter, M. Kearns, and R. Sutton, Eds. American
 * Association for Artificial Intelligence, Menlo Park, CA, 476-483
 * 
 * @author Prasanna Velagapudi <psigen@gmail.com>
 */
public abstract class DStarLite<State> {

	/**
	 * Defines a suitably large initial capacity for internal data structures to
	 * avoid Java's heuristics reallocating up from scratch.
	 */
	public static final int INITIAL_CAPACITY = 1024;

	protected State _start;

	protected final State _goal;

	final ValueMap _rhs = new ValueMap();

	final ValueMap _g = new ValueMap();

	final KeyQueue _U = new KeyQueue();

	double _Km = 0;

	/**
	 * Initializes a D* search object with the specified start and goal states.
	 * This constructor roughly corresponds to the <i>initialize()<i> function
	 * in the pseudocode of the original paper.
	 * 
	 * @param start
	 *            the desired start state
	 * @param goal
	 *            the desires end state
	 */
	public DStarLite(State start, State goal) {
		_start = start;
		_goal = goal;

		_rhs.put(_goal, 0.0);
		_U.insert(_goal, calculateKey(goal));
	}

	/**
	 * Recomputes the lowest cost path through the map, taking into account any
	 * changes in start location and edge costs. If no path can be found this
	 * will return an empty list.
	 * 
	 * @return a list of states from start to goal
	 */
	public List<State> plan() {
		LinkedList<State> path = new LinkedList<State>();
		State s = _start;
		path.add(s);

		computeShortestPath();
		dumpGandRHS();
		while (!s.equals(_goal)) {
			// If g(sStart) = Inf, then there is no known path
			if (Double.isInfinite(_g.get(s))) {
				return Collections.emptyList();
			}

			Collection<State> succs = succ(s);
			Double minRhs = Double.POSITIVE_INFINITY;
			State minS = null;

			for (State sPrime : succs) {
				double rhsPrime = c(s, sPrime) + _g.get(sPrime);
				if (rhsPrime < minRhs) {
					minRhs = rhsPrime;
					minS = sPrime;
				}
			}
			s = minS;
			path.addLast(s);
		}
		return path;
	}

	private void dumpGandRHS() {
		DiffWriter.getWriter().writeStates("U:", _U, "\ng:", _g, "\nrhs:", _rhs);
		DiffWriter.getWriter().writeDifferences(_g, _rhs);
	}

	/**
	 * Change the start location after initialization. Used to cheaply move
	 * robot along path without needing to replan.
	 * 
	 * @param s
	 *            the new start state
	 */
	public void updateStart(State s) {
		State sLast = _start;
		_start = s;

		_Km += h(sLast, _start);
	}

	public void updateVertex(State u) {

		if (!u.equals(_goal)) {
			// find minimum rhs.
			double minRhs = Double.POSITIVE_INFINITY;
			for (State sPrime : succ(u)) {
				double rhsPrime = c(u, sPrime) + _g.get(sPrime);
				if (rhsPrime < minRhs) {
					minRhs = rhsPrime;
				}
			}
			_rhs.put(u, minRhs);
		}

		if (_U.contains(u)) {
			_U.remove(u);
		}

		if (!DoubleUtils.equals(_g.get(u), _rhs.get(u))) {
			_U.insert(u, calculateKey(u));
		}
	}

	/**
	 * An exact cost function for the distance between two <i>neighboring</i>
	 * states. This function is undefined for non-neighboring states. The
	 * neighbor connectivity is determined by the pred() and succ() functions.
	 * 
	 * @param a
	 *            some initial state
	 * @param b
	 *            some final state
	 * @return the actual distance between the states.
	 */
	protected abstract double c(State a, State b);

	/**
	 * An admissible heuristic function for the distance between two states. In
	 * actual use, the second vertex will always be the goal state.
	 * 
	 * The heuristic must follow these rules: 1) h(a, a) = 0 2) h(a, b) &lt;=
	 * c(a, c) + h(c, b) (where a and c are neighbors)
	 * 
	 * @param a
	 *            some initial state
	 * @param b
	 *            some final state
	 * @return the estimated distance between the states.
	 */
	protected abstract double h(State a, State b);

	/**
	 * Returns the set of predecessor states to the specified state.
	 * 
	 * @param s
	 *            the specified state.
	 * @return A set of predecessor states.
	 */
	protected abstract Collection<State> pred(State s);

	/**
	 * Returns the set of successor states to the specified state.
	 * 
	 * @param s
	 *            the specified state.
	 * @return A set of successor states.
	 */
	protected abstract Collection<State> succ(State s);

	Key calculateKey(State s) {

		double k1 = Math.min(_g.get(s), _rhs.get(s)) + h(_start, s) + _Km;
		double k2 = Math.min(_g.get(s), _rhs.get(s));

		return new Key(k1, k2);
	}

	void computeShortestPath() {

		while (!_U.isEmpty()
				&& (_U.topKey().compareTo(calculateKey(_start)) < 0 || !DoubleUtils
						.equals(_rhs.get(_start), _g.get(_start)))) {

			Key kOld = _U.topKey();
			State u = _U.pop();
			Key kNew = calculateKey(u);

			if (kOld.compareTo(kNew) < 0) {
				_U.insert(u, kNew);
			} else if (_g.get(u) > _rhs.get(u)) {
				_g.put(u, _rhs.get(u));

				for (State s : pred(u)) {
					updateVertex(s);
				}
			} else {
				_g.put(u, Double.POSITIVE_INFINITY);

				for (State s : pred(u)) {
					updateVertex(s);
				}
				updateVertex(u);
			}
		}
	}

	/**
	 * A tuple with two components used to assign priorities to states in the D*
	 * search. Keys are compared according to a lexical ordering, e.g. k &lt k'
	 * iff either k1 &lt k'1 or (k1 = k'1 and k2 &lt k'2).
	 */
	final class Key implements Comparable<Key> {

		final double a;
		final double b;

		public Key(double a, double b) {

			this.a = a;
			this.b = b;
		}

		public int compareTo(Key that) {

			if (this.a < that.a) {
				return -1;
			} else if (this.a > that.a) {
				return 1;
			} else {
				if (this.b < that.b) {
					return -1;
				} else if (this.b > that.b) {
					return 1;
				} else {
					return 0;
				}
			}
		}

		@Override
		public String toString() {
			return "<" + a + "," + b + ">";
		}
	}

	/**
	 * A simple wrapper to a priority queue that internally stores a tuple of
	 * State and Key with the correct comparison and equals operators. This
	 * cheaply implements the lookup behavior expected by D*.
	 */
	final class KeyQueue {

		private PriorityQueue<StateKey> _queue;

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public KeyQueue() {
			_queue = new PriorityQueue(INITIAL_CAPACITY,
					new StateKeyComparator());
		}

		public boolean contains(State s) {
			return _queue.contains(new StateKey(s, null));
		}

		public void insert(State s, Key k) {
			_queue.add(new StateKey(s, k));
		}

		public boolean isEmpty() {
			return _queue.isEmpty();
		}

		public State pop() {
			return _queue.poll().s;
		}

		public void remove(State s) {
			_queue.remove(new StateKey(s, null));
		}

		public Key topKey() {
			return _queue.peek().k;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public String toString() {
			String str = "[";
			PriorityQueue tmp = new PriorityQueue(_queue);

			while (!tmp.isEmpty()) {
				str += tmp.poll().toString();
				if (!tmp.isEmpty())
					str += ",\n";
			}

			return str + "]";
		}
		
		private final class StateKeyComparator implements Comparator<StateKey> {
			public final int compare(StateKey o1, StateKey o2) {
				// Lowest key = highest priority (reversing lexical ordering)
				return o1.k.compareTo(o2.k);
			}
		}

		private final class StateKey {

			private State s;
			private Key k;

			public StateKey(State s, Key k) {
				this.s = s;
				this.k = k;
			}

			@Override
			@SuppressWarnings("unchecked")
			public boolean equals(Object obj) {
				// We do a dangerous unchecked comparison (no null-test, etc.)
				// because this is a private class and should be well-behaved.
				StateKey that = (StateKey) obj;
				return (this.s.equals(that.s));
			}

			@Override
			public int hashCode() {
				int hash = 7;
				hash = 53 * hash + (this.s != null ? this.s.hashCode() : 0);
				return hash;
			}

			@Override
			public String toString() {
				return "[" + s.toString() + "," + k.toString() + "]";
			}
		}
	}

	/**
	 * A simple wrapper to a HashMap that returns infinity if a match is not
	 * found. This cheaply implements the lookup behavior required by D*.
	 */
	public final class ValueMap extends HashMap<State, Double> {

		private static final long serialVersionUID = -4713917567451766865L;

		@Override
		public Double get(Object key) {

			Double res = super.get(key);

			if (res == null) {
				return Double.POSITIVE_INFINITY;
			} else {
				return res;
			}
		}
		
		@Override
		public String toString() {
			String message = "[";
			int i = 0;
			for (State state : keySet()) {
				if (i == keySet().size() - 1) {
					message += state + " = " + get(state) + "]";
				} else {
					message += state + " = " + get(state) + "\n";
				}
				i++;
			}
			return message;
		}
	}
}
